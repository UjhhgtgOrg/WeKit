#pragma once

#include <string>
#include <vector>

// 定义 __forceinline 以适配 Android Clang
#ifndef __forceinline
#if defined(_MSC_VER)
// MSVC 保持原样
#elif defined(__GNUC__) || defined(__clang__)
// Android/Linux 使用 attribute always_inline
#define __forceinline __attribute__((always_inline)) inline
#else
#define __forceinline inline
#endif
#endif

// 仅在非内核模式下引入标准库
#ifndef _KERNEL_MODE
#include <type_traits>
#endif

namespace skc
{
#ifdef _KERNEL_MODE
    // 内核模式下的简易 type_traits 实现
    namespace std_impl {
        template <class _Ty> struct remove_reference { using type = _Ty; };
        template <class _Ty> struct remove_reference<_Ty&> { using type = _Ty; };
        template <class _Ty> struct remove_reference<_Ty&&> { using type = _Ty; };
        template <class _Ty> using remove_reference_t = typename remove_reference<_Ty>::type;

        template <class _Ty> struct remove_const { using type = _Ty; };
        template <class _Ty> struct remove_const<const _Ty> { using type = _Ty; };
        template <class _Ty> using remove_const_t = typename remove_const<_Ty>::type;
    }
    template<class _Ty> using clean_type = typename std_impl::remove_const_t<std_impl::remove_reference_t<_Ty>>;
#else
    // 用户模式（Android NDK）直接使用 std
    template<class _Ty> using clean_type = typename std::remove_const_t<std::remove_reference_t<_Ty>>;
#endif

    template <int _size, char _key1, char _key2, typename T>
    class skCrypter
    {
    public:
        __forceinline constexpr skCrypter(T* data)
        {
            crypt(data);
        }

        __forceinline T* get()
        {
            return _storage;
        }

        __forceinline int size()
        {
            return _size;
        }

        __forceinline  char key()
        {
            return _key1;
        }

        __forceinline  T* encrypt()
        {
            if (!isEncrypted())
                crypt(_storage);

            return _storage;
        }

        __forceinline  T* decrypt()
        {
            if (isEncrypted())
                crypt(_storage);

            return _storage;
        }

        __forceinline bool isEncrypted()
        {
            return _storage[_size - 1] != 0;
        }

        __forceinline void clear()
        {
            for (int i = 0; i < _size; i++)
            {
                _storage[i] = 0;
            }
        }

        __forceinline operator T* ()
        {
            decrypt();
            return _storage;
        }

    private:
        __forceinline constexpr void crypt(T* data)
        {
            for (int i = 0; i < _size; i++)
            {
                _storage[i] = data[i] ^ (_key1 + i % (1 + _key2));
            }
        }

        T _storage[_size]{};
    };
}


#define skCrypt(str) skCrypt_key(str, __TIME__[4], __TIME__[7])
#define skCrypt_key(str, key1, key2) []() { \
            constexpr static auto crypted = skc::skCrypter \
                <sizeof(str) / sizeof(str[0]), key1, key2, skc::clean_type<decltype(str[0])>>((skc::clean_type<decltype(str[0])>*)str); \
                    return crypted; }()