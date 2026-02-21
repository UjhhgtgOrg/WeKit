import sys
import random


def generate_c_header(hex_string, keys=None):
    """
    生成 secrets.h C头文件

    Args:
        hex_string: APK签名的SHA256哈希值（64位十六进制字符串）
        keys: 4个密钥的列表，如果不提供则随机生成

    Returns:
        C头文件内容的字符串
    """
    # 验证输入：必须是64个十六进制字符（0-9, A-F）
    hex_string = hex_string.strip().upper()
    if len(hex_string) != 64 or not all(c in "0123456789ABCDEF" for c in hex_string):
        raise ValueError("输入必须是64个十六进制字符（0-9, A-F）")

    # 如果没有提供keys，随机生成4个密钥（0x00-0xFF）
    if keys is None:
        keys = [random.randint(0x00, 0xFF) for _ in range(4)]
    elif len(keys) != 4:
        raise ValueError("keys必须是包含4个密钥的列表")

    # 将字符串转换为ASCII字节数组（64字节）
    plaintext = [ord(c) for c in hex_string]

    # 分段加密（每段16字节）
    encrypted_segments = []
    for i in range(4):
        start = i * 16
        segment = plaintext[start : start + 16]
        key = keys[i]
        encrypted = [b ^ key for b in segment]
        encrypted_segments.append(encrypted)

    # 生成C头文件
    output = "#pragma once\n\n"
    for i, (key, enc_data) in enumerate(zip(keys, encrypted_segments), 1):
        output += f"static const unsigned char KEY{i} = 0x{key:02X};\n"
        output += f"static const unsigned char ENC_PART{i}[] = {{\n"
        # 格式化为每行8个字节
        for j in range(0, 16, 8):
            line_bytes = enc_data[j : j + 8]
            line = ", ".join(f"0x{b:02X}" for b in line_bytes)
            output += f"        {line}"
            if j < 8:  # 第一行需要逗号
                output += ","
            output += "\n"
        output += "};\n\n"

    return output.strip()


def main():
    if len(sys.argv) != 2:
        print("用法: python generate_secrets_h.py <64位SHA256签名>", file=sys.stderr)
        print(
            "示例: python generate_secrets_h.py 156B65C9CBE827BF0BB22F9E00BEEC3258319CE8A15D2A3729275CAF71CEDA21",
            file=sys.stderr,
        )
        sys.exit(1)

    try:
        # 默认使用随机密钥生成
        result = generate_c_header(sys.argv[1])
        print(result)
    except ValueError as e:
        print(f"错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
