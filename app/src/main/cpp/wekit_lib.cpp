#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <thread>
#include <chrono>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <dlfcn.h>
#include <cstring>
#include <fstream>
#include <sstream>
#include <zlib.h>
#include <iomanip>
#include "sha256.h"
#include "secrets.h"
#include <sys/system_properties.h>
#include "generated_checksums.h"
#include "generated_hidden_dex.h"
#include "skCrypter.h"

#define LOG_TAG "[WeKit-TAG] wekit-native"

#define ENABLE_WEKIT_LOGS

#if !defined(ENABLE_WEKIT_LOGS)
    #define LOG_SECURE_E(...)
    #define LOG_SECURE(...)
    #define LOG_SECURE_W(...)

#else
#define LOG_SECURE_E(fmt, ...) \
        do { \
            _Pragma("clang diagnostic push") \
            _Pragma("clang diagnostic ignored \"-Wformat-security\"") \
            _Pragma("clang diagnostic ignored \"-Wformat-nonliteral\"") \
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, (char*)skCrypt(fmt), ##__VA_ARGS__); \
            _Pragma("clang diagnostic pop") \
        } while(0)

#define LOG_SECURE(fmt, ...) \
        do { \
            _Pragma("clang diagnostic push") \
            _Pragma("clang diagnostic ignored \"-Wformat-security\"") \
            _Pragma("clang diagnostic ignored \"-Wformat-nonliteral\"") \
            __android_log_print(ANDROID_LOG_INFO, LOG_TAG, (char*)skCrypt(fmt), ##__VA_ARGS__); \
            _Pragma("clang diagnostic pop") \
        } while(0)
        
#define LOG_SECURE_W(fmt, ...) \
        do { \
            _Pragma("clang diagnostic push") \
            _Pragma("clang diagnostic ignored \"-Wformat-security\"") \
            _Pragma("clang diagnostic ignored \"-Wformat-nonliteral\"") \
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, (char*)skCrypt(fmt), ##__VA_ARGS__); \
            _Pragma("clang diagnostic pop") \
        } while(0)

#endif

#define API_EXPORT __attribute__((visibility("default")))
#define INTERNAL_FUNC __attribute__((visibility("hidden")))

// 定义一个特殊的结构体，用于在编译后被Gradle脚本填充
// 我们使用 0xCCCCCCCC 作为占位符，Gradle会寻找 MAGIC_TAG 并填充后面的 part
struct IntegrityStore {
    uint32_t magic_tag;      // 用于定位的魔数
    uint32_t hash_part_1;    // Hash 的第 1 部分 (如: Hash ^ 0xA5A5A5A5)
    uint32_t hash_part_2;    // Hash 的第 2 部分 (如: Hash + 0x12345678)
    uint32_t random_noise;   // 随机噪音，用于混淆
};

__attribute__((section(".data"), visibility("hidden"), used))
volatile IntegrityStore g_integrity_store = {
        0xFEEDDEAD,
        0xCCCCCCCC,
        0xCCCCCCCC,
        0xCCCCCCCC
};

volatile const uint32_t NSIZE __attribute__((used)) = 0x1A2B3C4D;

// 全局校验状态
INTERNAL_FUNC static bool g_signature_valid = false;
INTERNAL_FUNC static bool g_dex_valid = false;
INTERNAL_FUNC static int g_verification_score = 0;

INTERNAL_FUNC static volatile uint8_t g_runtime_mask = 0x00;
INTERNAL_FUNC static volatile uint64_t g_heartbeat = 0;

const char* get_target_so_path() {
#if defined(__aarch64__)
    return "lib/arm64-v8a/libwekit.so";
#elif defined(__arm__)
    return "lib/armeabi-v7a/libwekit.so";
#else
    return "lib/x86_64/libwekit.so";
#endif
}

// 计算内存区域 Hash (保持轻量)
INTERNAL_FUNC static uint32_t calc_mem_hash(void* start, size_t len) {
    auto* p = (uint8_t*)start;
    uint32_t hash = 0x811C9DC5;
    for (size_t i = 0; i < len; ++i) {
        hash ^= p[i];
        hash *= 0x01000193;
    }
    return hash;
}

// 动态重组并获取校验用的 Hash
INTERNAL_FUNC static uint32_t get_expected_integrity_hash() {
    uint32_t reconstructed = g_integrity_store.hash_part_1 ^ 0x5A5A5A5A;
    uint32_t check_part2 = ~reconstructed;
    if (g_integrity_store.hash_part_2 != check_part2) {
        // 数据被篡改，返回一个必定错误的 Hash
        return 0xDEAD0000;
    }

    return reconstructed;
}

INTERNAL_FUNC INTERNAL_FUNC static void process_string_data(const unsigned char *data, unsigned char key, char *output) {
    for (int i = 0; i < 16; i++) {
        output[i] = (data[i] ^ key);
    }
}

INTERNAL_FUNC INTERNAL_FUNC static bool load_config_segment(int segment, char* buffer) {
    switch(segment) {
        case 0:
            process_string_data(ENC_PART1, KEY1, buffer);
            return true;
        case 1:
            process_string_data(ENC_PART2, KEY2, buffer);
            return true;
        case 2:
            process_string_data(ENC_PART3, KEY3, buffer);
            return true;
        case 3:
            process_string_data(ENC_PART4, KEY4, buffer);
            return true;
        default:
            return false;
    }
}

// 解密并组装预埋的 Hash 字符串
INTERNAL_FUNC INTERNAL_FUNC static std::string assemble_verification_data() {
    char parts[4][17]; // 16 bytes + null terminator
    for (int i = 0; i < 4; i++) {
        memset(parts[i], 0, 17);
        if (!load_config_segment(i, parts[i])) {
            return "";
        }
    }

    std::string result;
    for (auto & part : parts) {
        result.append(part, 16);
    }
    return result;
}

INTERNAL_FUNC static std::string sha256_bytes_to_hex(const uint8_t* hash_bytes) {
    std::stringstream ss;
    ss << std::hex << std::uppercase << std::setfill('0');
    for (int i = 0; i < 32; ++i) {
        ss << std::setw(2) << (int)hash_bytes[i];
    }
    return ss.str();
}

INTERNAL_FUNC static bool iequals(const std::string& a, const std::string& b) {
    if (a.size() != b.size()) return false;
    for (size_t i = 0; i < a.size(); ++i) {
        if (tolower(a[i]) != tolower(b[i])) {
            return false;
        }
    }
    return true;
}

INTERNAL_FUNC static bool verify_checksum_internal(const char* input) {
    if (!input) return false;
    std::string expected = assemble_verification_data();
    if (expected.empty()) return false;
    return iequals(std::string(input), expected);
}

INTERNAL_FUNC static std::string get_apk_path() {
    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", getpid());

    std::ifstream maps_file(maps_path);
    if (!maps_file.is_open()) {
        LOG_SECURE_E("Failed to open maps file");
        return "";
    }

    std::string line;
    while (std::getline(maps_file, line)) {
        if (line.find(".apk") != std::string::npos) {
            size_t path_start = line.find('/');
            if (path_start != std::string::npos) {
                std::string apk_path = line.substr(path_start);
                size_t space_pos = apk_path.find(' ');
                if (space_pos != std::string::npos) {
                    apk_path = apk_path.substr(0, space_pos);
                }
                // LOG_SECURE("Found APK path: %s", apk_path.c_str());
                return apk_path;
            }
        }
    }
    LOG_SECURE_E("APK path not found in maps");
    return "";
}

// ==================== ZIP 文件结构定义 ====================

#pragma pack(push, 1)
struct ZipLocalFileHeader {
    uint32_t signature;
    uint16_t version;
    uint16_t flags;
    uint16_t compression;
    uint16_t mod_time;
    uint16_t mod_date;
    uint32_t crc32;
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint16_t filename_length;
    uint16_t extra_length;
};

struct ZipCentralDirHeader {
    uint32_t signature;
    uint16_t version_made;
    uint16_t version_needed;
    uint16_t flags;
    uint16_t compression;
    uint16_t mod_time;
    uint16_t mod_date;
    uint32_t crc32;
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint16_t filename_length;
    uint16_t extra_length;
    uint16_t comment_length;
    uint16_t disk_start;
    uint16_t internal_attr;
    uint32_t external_attr;
    uint32_t local_header_offset;
};

struct ZipEndOfCentralDir {
    uint32_t signature;
    uint16_t disk_number;
    uint16_t central_dir_disk;
    uint16_t num_entries_disk;
    uint16_t num_entries;
    uint32_t central_dir_size;
    uint32_t central_dir_offset;
    uint16_t comment_length;
};

struct DexHeader {
    uint8_t magic[8];
    uint32_t checksum;
    uint8_t signature[20];
    uint32_t file_size;
    uint32_t header_size;
    uint32_t endian_tag;
    uint32_t link_size;
    uint32_t link_off;
    uint32_t map_off;
    uint32_t string_ids_size;
    uint32_t string_ids_off;
    uint32_t type_ids_size;
    uint32_t type_ids_off;
    uint32_t proto_ids_size;
    uint32_t proto_ids_off;
    uint32_t field_ids_size;
    uint32_t field_ids_off;
    uint32_t method_ids_size;
    uint32_t method_ids_off;
    uint32_t class_defs_size;
    uint32_t class_defs_off;
    uint32_t data_size;
    uint32_t data_off;
};
#pragma pack(pop)

// ==================== DEX 完整性校验 ====================

INTERNAL_FUNC static bool find_eocd(int fd, off_t file_size, ZipEndOfCentralDir* eocd) {
    const size_t max_search = 65535 + sizeof(ZipEndOfCentralDir);
    const size_t search_size = (file_size < max_search) ? file_size : max_search;
    off_t search_start = file_size - search_size;
    if (lseek(fd, search_start, SEEK_SET) < 0) return false;
    std::vector<uint8_t> buffer(search_size);
    if (read(fd, buffer.data(), search_size) != (ssize_t)search_size) return false;
    for (ssize_t i = search_size - sizeof(ZipEndOfCentralDir); i >= 0; i--) {
        uint32_t sig = *reinterpret_cast<uint32_t*>(&buffer[i]);
        if (sig == 0x06054b50) {
            memcpy(eocd, &buffer[i], sizeof(ZipEndOfCentralDir));
            return true;
        }
    }
    return false;
}

INTERNAL_FUNC static bool find_entry_in_apk(int fd, const ZipEndOfCentralDir& eocd, const char* target_filename, uint32_t* entry_offset, uint32_t* entry_size) {
    if (lseek(fd, eocd.central_dir_offset, SEEK_SET) < 0) return false;
    for (uint16_t i = 0; i < eocd.num_entries; i++) {
        ZipCentralDirHeader header{};
        if (read(fd, &header, sizeof(header)) != sizeof(header)) return false;
        if (header.signature != 0x02014b50) return false;
        std::vector<char> filename(header.filename_length + 1);
        if (read(fd, filename.data(), header.filename_length) != header.filename_length) return false;
        filename[header.filename_length] = '\0';
        if (lseek(fd, header.extra_length + header.comment_length, SEEK_CUR) < 0) return false;
        if (strstr(filename.data(), target_filename) != nullptr) {
            off_t current_pos = lseek(fd, 0, SEEK_CUR);
            lseek(fd, header.local_header_offset, SEEK_SET);
            ZipLocalFileHeader local_header{};
            if (read(fd, &local_header, sizeof(local_header)) == sizeof(local_header)) {
                if (local_header.signature == 0x04034b50) {
                    *entry_offset = header.local_header_offset + sizeof(ZipLocalFileHeader) + local_header.filename_length + local_header.extra_length;
                    *entry_size = header.uncompressed_size;
                    return true;
                }
            }
            lseek(fd, current_pos, SEEK_SET);
        }
    }
    return false;
}



INTERNAL_FUNC static bool verify_single_entry_crc(int fd, const ZipEndOfCentralDir& eocd, const char* filename, uint32_t expected_crc) {
    uint32_t offset, size;

    if (!find_entry_in_apk(fd, eocd, filename, &offset, &size)) {
        LOG_SECURE_E("[!] Missing expected DEX file: %s", filename);
        return false;
    }

    if (lseek(fd, offset, SEEK_SET) < 0) {
        LOG_SECURE_E("[!] Failed to seek to DEX data: %s", filename);
        return false;
    }

    // 分块读取并计算 CRC32
    std::vector<uint8_t> buffer(8192);
    uLong crc = crc32(0L, Z_NULL, 0);
    uint32_t total_read = 0;

    while (total_read < size) {
        size_t to_read = std::min((size_t)8192, (size_t)(size - total_read));
        ssize_t n = read(fd, buffer.data(), to_read);
        if (n <= 0) break;
        crc = crc32(crc, buffer.data(), n);
        total_read += n;
    }

    if ((uint32_t)crc != expected_crc) {
        LOG_SECURE_E("[!] DEX MODIFIED: %s | Real: %08x, Expected: %08x", filename, (uint32_t)crc, expected_crc);
        return false;
    }

    LOG_SECURE("[V] Verified: %s", filename);
    return true;
}

INTERNAL_FUNC static bool verify_dex_content_directly(const std::string& apk_path) {
    int fd = open(apk_path.c_str(), O_RDONLY);
    if (fd < 0) return false;

    struct stat st{};
    if (fstat(fd, &st) < 0) { close(fd); return false; }
    off_t file_size = st.st_size;

    ZipEndOfCentralDir eocd{};
    if (!find_eocd(fd, file_size, &eocd)) { close(fd); return false; }

    bool all_passed = true;

    for (int i = 0; i < EXPECTED_DEX_COUNT; ++i) {
        std::string filename;
        if (i == 0) {
            filename = "classes.dex";
        } else {
            std::ostringstream ss;
            ss << "classes" << (i + 1) << ".dex";
            filename = ss.str();
        }

        uint32_t expected_crc = EXPECTED_DEX_CRCS[i];

        if (!verify_single_entry_crc(fd, eocd, filename.c_str(), expected_crc)) {
            all_passed = false;
            break;
        }
    }

    close(fd);

    if (all_passed) {
        LOG_SECURE("[V] All %d DEX files verified successfully.", EXPECTED_DEX_COUNT);
    }

    return all_passed;
}

INTERNAL_FUNC static const uint8_t APK_SIG_BLOCK_MAGIC[] = {
        0x41, 0x50, 0x4b, 0x20, 0x53, 0x69, 0x67, 0x20,
        0x42, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x34, 0x32
};
INTERNAL_FUNC static const uint32_t APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a;

INTERNAL_FUNC static void calculate_sha256(const uint8_t* data, size_t length, uint8_t* output) {
    compute_sha256(data, length, output);
}

INTERNAL_FUNC static bool find_apk_signing_block(int fd, off_t file_size, const ZipEndOfCentralDir& eocd, off_t* block_offset, uint64_t* block_size) {
    off_t central_dir_offset = eocd.central_dir_offset;
    if (central_dir_offset < 32) return false;
    off_t magic_offset = central_dir_offset - 16;
    if (lseek(fd, magic_offset, SEEK_SET) < 0) return false;
    uint8_t magic_buffer[16];
    if (read(fd, magic_buffer, 16) != 16) return false;
    if (memcmp(magic_buffer, APK_SIG_BLOCK_MAGIC, 16) != 0) return false;
    off_t size_offset = magic_offset - 8;
    if (lseek(fd, size_offset, SEEK_SET) < 0) return false;
    uint64_t size_footer;
    if (read(fd, &size_footer, 8) != 8) return false;
    *block_size = size_footer;
    *block_offset = central_dir_offset - size_footer - 8;
    if (lseek(fd, *block_offset, SEEK_SET) < 0) return false;
    uint64_t size_header;
    if (read(fd, &size_header, 8) != 8) return false;
    if (size_header != size_footer) return false;
    return true;
}

INTERNAL_FUNC static bool extract_v2_signature(int fd, off_t block_offset, uint64_t block_size, std::vector<uint8_t>& signature_data) {
    off_t pairs_offset = block_offset + 8;
    uint64_t pairs_size = block_size - 24;
    if (lseek(fd, pairs_offset, SEEK_SET) < 0) return false;
    std::vector<uint8_t> pairs_data(pairs_size);
    if (read(fd, pairs_data.data(), pairs_size) != (ssize_t)pairs_size) return false;
    size_t offset = 0;
    while (offset < pairs_size) {
        if (offset + 12 > pairs_size) break;
        uint64_t pair_len;
        memcpy(&pair_len, &pairs_data[offset], 8);
        offset += 8;
        if (offset + pair_len > pairs_size) break;
        uint32_t id;
        memcpy(&id, &pairs_data[offset], 4);
        offset += 4;
        uint64_t value_len = pair_len - 4;
        if (id == APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
            signature_data.resize(value_len);
            memcpy(signature_data.data(), &pairs_data[offset], value_len);
            return true;
        }
        offset += value_len;
    }
    return false;
}

INTERNAL_FUNC static bool extract_certificate_from_v2(const std::vector<uint8_t>& signature_data, std::vector<uint8_t>& certificate) {
    if (signature_data.size() < 4) return false;
    size_t offset = 0;

    uint32_t signers_len;
    if (offset + 4 > signature_data.size()) return false;
    memcpy(&signers_len, &signature_data[offset], 4);
    offset += 4;

    uint32_t signer_len;
    if (offset + 4 > signature_data.size()) return false;
    memcpy(&signer_len, &signature_data[offset], 4);
    offset += 4;

    uint32_t signed_data_len;
    if (offset + 4 > signature_data.size()) return false;
    memcpy(&signed_data_len, &signature_data[offset], 4);
    offset += 4;

    size_t signed_data_end = offset + signed_data_len;
    if (signed_data_end > signature_data.size()) return false;
    uint32_t digests_len;
    if (offset + 4 > signed_data_end) return false;
    memcpy(&digests_len, &signature_data[offset], 4);
    offset += 4 + digests_len;
    uint32_t certificates_len;
    if (offset + 4 > signed_data_end) return false;
    memcpy(&certificates_len, &signature_data[offset], 4);
    offset += 4;

    uint32_t cert_len;
    if (offset + 4 > signed_data_end) return false;
    memcpy(&cert_len, &signature_data[offset], 4);
    offset += 4;

    if (offset + cert_len > signed_data_end) return false;

    certificate.resize(cert_len);
    memcpy(certificate.data(), &signature_data[offset], cert_len);

    LOG_SECURE("Extracted certificate: size=%u", cert_len);
    return true;
}

INTERNAL_FUNC static bool verify_apk_signature_direct(const std::string& apk_path) {
    int fd = open(apk_path.c_str(), O_RDONLY);
    if (fd < 0) {
        LOG_SECURE_E("Failed to open APK");
        return false;
    }

    struct stat st{};
    if (fstat(fd, &st) < 0) { close(fd); return false; }
    off_t file_size = st.st_size;

    ZipEndOfCentralDir eocd{};
    if (!find_eocd(fd, file_size, &eocd)) {
        close(fd);
        LOG_SECURE_E("EOCD not found");
        return false;
    }

    off_t block_offset;
    uint64_t block_size;
    if (!find_apk_signing_block(fd, file_size, eocd, &block_offset, &block_size)) {
        close(fd);
        LOG_SECURE_W("No V2 Signing Block found");
        return false;
    }

    std::vector<uint8_t> signature_data;
    if (!extract_v2_signature(fd, block_offset, block_size, signature_data)) {
        close(fd);
        return false;
    }

    std::vector<uint8_t> certificate;
    if (!extract_certificate_from_v2(signature_data, certificate)) {
        close(fd);
        return false;
    }
    close(fd);

    uint8_t calculated_hash[32];
    calculate_sha256(certificate.data(), certificate.size(), calculated_hash);
    std::string calculated_hex = sha256_bytes_to_hex(calculated_hash);
    std::string expected_hex = assemble_verification_data();

    if (calculated_hex == expected_hex) {
        LOG_SECURE("[V] Certificate Verification PASSED");
        return true;
    } else {
        LOG_SECURE_E("[!] Certificate Verification FAILED!");
        LOG_SECURE_E("[!] Calculated: %s", calculated_hex.c_str());
        LOG_SECURE_E("[!] Expected:   %s", expected_hex.c_str());
        return false;
    }

    if (iequals(calculated_hex, expected_hex)) {
        LOG_SECURE("[V] Certificate Verification PASSED");
        return true;
    } else {
        LOG_SECURE_E("[!] Certificate Verification FAILED! Hash mismatch.");
        return false;
    }
}

INTERNAL_FUNC static bool verify_so_integrity(int fd, const ZipEndOfCentralDir& eocd) {
    uint32_t offset, size;

    const char* target_path = get_target_so_path();

    if (!find_entry_in_apk(fd, eocd, target_path, &offset, &size)) {
        LOG_SECURE_E("Could not find SO in APK: %s", target_path);
        return false;
    }

    uint32_t expected_size = NSIZE;

    if (expected_size == 0x1A2B3C4D) {
#ifdef NDEBUG
        LOG_SECURE_E("[!] FATAL: SO integrity check failed. Binary not patched!");
        return false;
#else
        LOG_SECURE_W("[!] Debug build detected. Skipping SO size check.");
        return true;
#endif
    }

    if (size != expected_size) {
        LOG_SECURE_E("[!] SO MODIFIED! Expected Size: %u, Actual Size in APK: %u", expected_size, size);
        return false;
    }

    LOG_SECURE("[V] SO size verified: %u", size);
    return true;
}

void* fix_thread(void* arg) {
    srand(time(nullptr));
    int delay = 5 + (rand() % 16);
    std::this_thread::sleep_for(std::chrono::seconds(delay));
    abort();
    return nullptr;
}

INTERNAL_FUNC static void onFailed(JNIEnv* env) {
    pthread_t thread_id;
    int result = pthread_create(&thread_id, nullptr, fix_thread, nullptr);
    if (result == 0) pthread_detach(thread_id);
    else abort();
}

INTERNAL_FUNC static int perform_multi_dimensional_verification(JNIEnv* env, const char* signature_hash) {
#if !defined(NDEBUG) || defined(DEBUG)
    LOG_SECURE_W("[WeKit-Debug] Debug Build detected. Bypassing verification.");

    g_signature_valid = true;
    g_dex_valid = true;
    g_verification_score = 100;
    g_runtime_mask = 0x00;
    return 100;
#endif

    int score = 0;
    std::string apk_path = get_apk_path();

    if (verify_checksum_internal(signature_hash)) {
        LOG_SECURE("[V] Java-layer signature verification passed");
        score += 20;
        g_signature_valid = true;
    } else {
        LOG_SECURE_E("[X] FATAL: Java-layer signature verification failed!");
        g_signature_valid = false;
        onFailed(env);
        return 0;
    }

    if (!apk_path.empty()) {
        if (verify_apk_signature_direct(apk_path)) {
            LOG_SECURE("[V] Native APK signature direct verification passed");
            score += 20;
        } else {
            LOG_SECURE_W("[!] FATAL: Native APK signature direct verification failed!");
            onFailed(env);
            return 0;
        }
    }

    if (!apk_path.empty() && verify_apk_signature_direct(apk_path)) {
        score += 20;
        g_dex_valid = true;
    } else {
        LOG_SECURE_E("[X] FATAL: DEX integrity verification failed!");
        g_dex_valid = false;
        onFailed(env);
        return 0;
    }

    if (!apk_path.empty() && verify_dex_content_directly(apk_path)) {
        score += 20;
        g_dex_valid = true;
    } else {
        LOG_SECURE_E("[X] FATAL: DEX integrity verification failed!");
        g_dex_valid = false;
        onFailed(env);
        return 0;
    }

    bool so_check_passed = false;
    if (!apk_path.empty()) {
        int fd = open(apk_path.c_str(), O_RDONLY);
        if (fd >= 0) {
            struct stat st{};
            if (fstat(fd, &st) == 0) {
                ZipEndOfCentralDir eocd{};
                if (find_eocd(fd, st.st_size, &eocd)) {
                    if (verify_so_integrity(fd, eocd)) {
                        so_check_passed = true;
                    }
                } else {
                    LOG_SECURE_E("Failed to find EOCD for SO check");
                }
            }
            close(fd);
        } else {
            LOG_SECURE_E("Failed to open APK for SO check");
        }
    }

    if (so_check_passed) {
        score += 20;
    } else {
        LOG_SECURE_E("[X] FATAL: SO integrity verification failed!");
        onFailed(env);
        return 0;
    }

    LOG_SECURE("=== Verification Passed. Score: %d/100 ===", score);
    return score;
}

// Java_moe_ouom_wekit_loader_core_WeKitNative_doInit
INTERNAL_FUNC static jboolean ni(JNIEnv* env, jobject thiz, jstring signatureHash) {
    if (signatureHash == nullptr) return JNI_FALSE;

    uint32_t state = 100;
    jboolean result = JNI_FALSE;
    const char* hashStr = nullptr;

    // 初始化为锁定
    g_runtime_mask = 0xFF;

    while (state != 0) {
        switch (state) {
            case 100:
                hashStr = env->GetStringUTFChars(signatureHash, nullptr);
                if (hashStr != nullptr) state = 200;
                else state = 999;
                break;

            case 200:
                g_verification_score = perform_multi_dimensional_verification(env, hashStr);
                env->ReleaseStringUTFChars(signatureHash, hashStr);
                if (g_verification_score >= 80) state = 300;
                else state = 400;
                break;

            case 300: // 成功解锁逻辑
            {
#if !defined(NDEBUG) || defined(DEBUG)
                // [Debug] 强制跳过完整性检查
                // 因为 Debug 包没有经过 Gradle Patch，数据是错的，检查必挂
                LOG_SECURE_W("[WeKit-Debug] Skipping Integrity Store check in doInit.");
                g_runtime_mask = 0x00;
                result = JNI_TRUE;
                state = 0;
#else
                // [Release] 检查 Gradle 注入的完整性锚点
                if (get_expected_integrity_hash() == 0xDEAD0000) {
                    state = 500; // 自毁路径
                } else {
                    g_runtime_mask = 0x00;
                    result = JNI_TRUE;
                    state = 0; // 结束
                }
#endif
            }
                break;
            case 400: g_runtime_mask = 0xFF; result = JNI_FALSE; state = 0; break;
            case 500: onFailed(env); state = 0; break;
            case 999: onFailed(env); result = JNI_FALSE; state = 0; break;
        }
    }
    return result;
}

// Java_moe_ouom_wekit_loader_core_WeKitNative_nativeCheck
extern "C" __attribute__((visibility("hidden"), used, noinline))
jlong nc(JNIEnv* env, jobject thiz) {
#if !defined(NDEBUG) || defined(DEBUG)
    LOG_SECURE_W("[WeKit-Debug] nc check BYPASSED.");
    // 防止逻辑差异太大
    g_heartbeat += 0x112233;
    return 0xB00B1E5; // 返回一个特殊的通过码
#endif

    // 如果没有 init 运行过，直接返回错误码
    if (g_runtime_mask == 0xFF) {
        return 0xDEAD;
    }

    uint32_t state = 0x1A;
    jlong result = 0;
    volatile int check_score = 0;

    // 更新心跳，证明我来过这里
    // 攻击者如果直接 return 常量，这一行就不会执行，get_dex 就会发现
    g_heartbeat ^= 0xCAFEBABE;

    while (state != 0xFF) {
        switch (state) {
            case 0x1A: // 检查签名状态
                check_score = g_verification_score;
                if ((g_signature_valid ^ 1) == 0) {
                    // g_signature_valid 为 true 时 (1^1=0)，进入下一步
                    state = 0x2B;
                } else {
                    state = 0xEE;
                }
                break;

            case 0x2B: // 检查 DEX 状态
                if (g_dex_valid) {
                    state = 0x3C;
                } else {
                    state = 0xEE;
                }
                break;

            case 0x3C: // 检查分数和 Mask
                // 逻辑：分数>=80 且 Mask已解锁
                if (check_score >= 80 && g_runtime_mask == 0x00) {
                    state = 0x4D;
                } else {
                    state = 0xEE;
                }
                break;

            case 0x4D: // 成功生成 Magic Code
                // 返回一个基于 score 计算出的复杂数值
                // 只有 score 是 100 分时，结果才是正确的预期值
                result = ((jlong)check_score * 0x1234) ^ 0xABCDEF;
                // 如 100 * 0x1234 ^ 0xABCDEF = 0xB6180 ^ 0xABCDEF = 0xA1AD6F

                state = 0xFF; // 结束
                break;

            case 0xEE: // 失败
                // 返回一个具备迷惑性的随机值
                result = (jlong)check_score ^ 0xDEADDEAD;
                state = 0xFF; // 结束
                break;

            default:
                state = 0x1A;
                break;
        }
    }

    // 再次更新心跳
    g_heartbeat += 1;

    return result;
}
// 定义一个空的结束标记函数，此函数必须紧跟在 nc 函数后面
extern "C" __attribute__((visibility("hidden"), used, noinline))
void nc_end() {
    // 这是一个哨兵，什么都不做，只为了占位
    volatile int x = 1;
}

// Java_moe_ouom_wekit_loader_core_WeKitNative_getHiddenDex
INTERNAL_FUNC static jbyteArray ngd(JNIEnv* env, jobject thiz) {
    uint32_t state = 1;
    jbyteArray result = nullptr;
    std::vector<jbyte> temp_buffer;

    // 关键变量
    uint32_t current_code_hash = 0;
    uint32_t expected_hash = 0;
    uint8_t final_mask = 0xFF;
    int i = 0;

    // 记录调用 nc 之前的心跳值
    uint64_t last_heartbeat = g_heartbeat;

    while (state != 0) {
        switch (state) {
            case 1: // 调用 nc 并验证
            {
                jlong check_val = nc(env, thiz);

                // 心跳值必须改变
                // 如果攻击者 Patch 了 nc 直接 return，heartbeat 不会变
                if (g_heartbeat == last_heartbeat) {
                    LOG_SECURE_E("[!] Integrity Trap: nc() logic bypassed (No heartbeat).");
                    state = 99; // 失败
                    break;
                }

                // 检查返回值逻辑
#if !defined(NDEBUG) || defined(DEBUG)
                bool logic_pass = (check_val == 0xB00B1E5);
#else
                // Release 模式下，期望值必须是 100分 * 0x1234 ^ 0xABCDEF
                // 只要 nc 被 Patch 返回 true (1)，这里 check_val 就是 1，校验失败
                jlong expected_val = ((jlong)100 * 0x1234) ^ 0xABCDEF;
                bool logic_pass = (check_val == expected_val);
#endif

                if (logic_pass) state = 2;
                else {
                    LOG_SECURE_E("[!] Integrity Trap: nc() returned invalid magic code: %llx", check_val);
                    state = 99;
                }
            }
                break;

            case 2:
            {
                // 动态计算 nc 的大小，解决 O3 优化带来的长度问题
                uintptr_t start_addr = (uintptr_t)nc;
                uintptr_t end_addr = (uintptr_t)nc_end;

                size_t func_size = 0;
                if (end_addr > start_addr) {
                    func_size = end_addr - start_addr;
                } else {
                    LOG_SECURE_E("[!] FATAL: nc() logic broken!");
                    func_size = 256;
                }

                // 限制一下最大值，防止读越界
                if (func_size > 1024) func_size = 256;

                // 计算内存 Hash
                current_code_hash = calc_mem_hash((void *) nc, func_size);
                state = 3;
            }

            case 3: // 获取 Gradle 注入的预期 Hash
                expected_hash = get_expected_integrity_hash();
                state = 4;
                break;

            case 4: // 计算最终解密 Mask
            {
#if !defined(NDEBUG) || defined(DEBUG)
                // Debug 模式：因为 Gradle 可能没注入 Hash，强制放行
                LOG_SECURE_W("[WeKit-Debug] Integrity mask FORCED to 0.");
                final_mask = 0;
#else
                // [Release] CodeHash 必须一致，否则 diff != 0
                uint32_t diff = current_code_hash ^ expected_hash;
                uint8_t integrity_mask = diff & 0xFF;

                // 融合 g_runtime_mask
                final_mask = g_runtime_mask | integrity_mask;

                // 发现代码被修改
                if (integrity_mask != 0) {
                    LOG_SECURE_E("[!] FATAL: Code Tampering Detected! Hash: %08x, Expected: %08x", current_code_hash, expected_hash);
                }
#endif

                result = env->NewByteArray(HIDDEN_DEX_SIZE);
                if (result) {
                    temp_buffer.resize(HIDDEN_DEX_SIZE);
                    i = 0;
                    state = 5; // 进入解密循环
                } else {
                    state = 0;
                }
            }
                break;

            case 5: // 循环头
                if (i < HIDDEN_DEX_SIZE) state = 6;
                else state = 7;
                break;

            case 6: // 解密 payload
                // 只要 final_mask != 0，数据即损毁
                temp_buffer[i] = HIDDEN_DEX_DATA[i] ^ HIDDEN_DEX_KEY ^ final_mask;
                i++;
                state = 5;
                break;

            case 7: // 提交数据
                env->SetByteArrayRegion(result, 0, HIDDEN_DEX_SIZE, temp_buffer.data());
                state = 0;
                break;

            case 99: // 惩罚
                onFailed(env);
                result = nullptr;
                state = 0;
                break;
        }
    }
    return result;
}

INTERNAL_FUNC static int
registerNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *methods) {
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, 2) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


// 映射表
INTERNAL_FUNC static const JNINativeMethod gMethods[] = {
        {"doInit",      "(Ljava/lang/String;)Z", (void *) ni},
        {"getHiddenDex","()[B", (void *) ngd}
};

API_EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (registerNativeMethods(env, skCrypt("moe/ouom/wekit/loader/core/WeKitNative"), gMethods) != JNI_TRUE) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}