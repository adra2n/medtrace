/**
 * MedTrace 注册码验证 (NDK)
 * 
 * 安全性:
 * - 密钥编译到 .so 文件中
 * - 反编译 .so 比反编译 Java/Kotlin 难度大得多
 * - 需要逆向 ARM 汇编才能提取密钥
 * 
 * HMAC-SHA256 实现（自包含，不依赖OpenSSL）
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>

// 包含自动生成的密钥头文件
#include "key.h"

// ============== SHA-256 实现 ==============

typedef struct {
    uint32_t h[8];
    uint64_t total_len;
    uint8_t buffer[64];
    size_t buffer_len;
} SHA256_CTX;

static const uint32_t K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

#define ROTR(x, n) (((x) >> (n)) | ((x) << (32 - (n))))
#define CH(x, y, z) (((x) & (y)) ^ (~(x) & (z)))
#define MAJ(x, y, z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define EP0(x) (ROTR(x, 2) ^ ROTR(x, 13) ^ ROTR(x, 22))
#define EP1(x) (ROTR(x, 6) ^ ROTR(x, 11) ^ ROTR(x, 25))
#define SIG0(x) (ROTR(x, 7) ^ ROTR(x, 18) ^ ((x) >> 3))
#define SIG1(x) (ROTR(x, 17) ^ ROTR(x, 19) ^ ((x) >> 10))

static uint32_t be32(const uint8_t *p) {
    return ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | p[3];
}

static void put_be32(uint8_t *p, uint32_t v) {
    p[0] = v >> 24; p[1] = v >> 16; p[2] = v >> 8; p[3] = v;
}

static void sha256_transform(SHA256_CTX *ctx, const uint8_t block[64]) {
    uint32_t w[64], a, b, c, d, e, f, g, h, t1, t2;
    for (int i = 0; i < 16; i++) w[i] = be32(block + i * 4);
    for (int i = 16; i < 64; i++) w[i] = SIG1(w[i-2]) + w[i-7] + SIG0(w[i-15]) + w[i-16];
    a = ctx->h[0]; b = ctx->h[1]; c = ctx->h[2]; d = ctx->h[3];
    e = ctx->h[4]; f = ctx->h[5]; g = ctx->h[6]; h = ctx->h[7];
    for (int i = 0; i < 64; i++) {
        t1 = h + EP1(e) + CH(e, f, g) + K[i] + w[i];
        t2 = EP0(a) + MAJ(a, b, c);
        h = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
    }
    ctx->h[0] += a; ctx->h[1] += b; ctx->h[2] += c; ctx->h[3] += d;
    ctx->h[4] += e; ctx->h[5] += f; ctx->h[6] += g; ctx->h[7] += h;
}

static void sha256_init(SHA256_CTX *ctx) {
    ctx->h[0] = 0x6a09e667; ctx->h[1] = 0xbb67ae85;
    ctx->h[2] = 0x3c6ef372; ctx->h[3] = 0xa54ff53a;
    ctx->h[4] = 0x510e527f; ctx->h[5] = 0x9b05688c;
    ctx->h[6] = 0x1f83d9ab; ctx->h[7] = 0x5be0cd19;
    ctx->total_len = 0; ctx->buffer_len = 0;
}

static void sha256_update(SHA256_CTX *ctx, const uint8_t *data, size_t len) {
    ctx->total_len += len;
    if (ctx->buffer_len > 0) {
        size_t copy = 64 - ctx->buffer_len;
        if (copy > len) copy = len;
        memcpy(ctx->buffer + ctx->buffer_len, data, copy);
        ctx->buffer_len += copy; data += copy; len -= copy;
        if (ctx->buffer_len == 64) {
            sha256_transform(ctx, ctx->buffer);
            ctx->buffer_len = 0;
        }
    }
    while (len >= 64) {
        sha256_transform(ctx, data);
        data += 64; len -= 64;
    }
    if (len > 0) {
        memcpy(ctx->buffer, data, len);
        ctx->buffer_len = len;
    }
}

static void sha256_final(SHA256_CTX *ctx, uint8_t hash[32]) {
    uint64_t bits = ctx->total_len * 8;
    uint8_t pad = (ctx->buffer_len < 56) ? (56 - ctx->buffer_len) : (120 - ctx->buffer_len);
    uint8_t padbuf[128] = {0};
    padbuf[0] = 0x80;
    sha256_update(ctx, padbuf, pad);
    uint8_t lenpad[8];
    for (int i = 7; i >= 0; i--) { lenpad[i] = bits & 0xff; bits >>= 8; }
    sha256_update(ctx, lenpad, 8);
    for (int i = 0; i < 8; i++) put_be32(hash + i * 4, ctx->h[i]);
}

// ============== HMAC-SHA256 ==============

static void hmac_sha256(const uint8_t *key, size_t key_len,
                        const uint8_t *data, size_t data_len,
                        uint8_t out[32]) {
    uint8_t k_pad[64], o_pad[64], i_hash[32], tmp[32];
    SHA256_CTX ctx;
    
    // 如果 key > 64，先 hash
    uint8_t real_key[32];
    if (key_len > 64) {
        sha256_init(&ctx);
        sha256_update(&ctx, key, key_len);
        sha256_final(&ctx, real_key);
        key = real_key; key_len = 32;
    }
    
    // ipad / opad
    memset(k_pad, 0, 64);
    memcpy(k_pad, key, key_len);
    for (int i = 0; i < 64; i++) {
        o_pad[i] = k_pad[i] ^ 0x5c;
        k_pad[i] = k_pad[i] ^ 0x36;
    }
    
    // inner hash
    sha256_init(&ctx);
    sha256_update(&ctx, k_pad, 64);
    sha256_update(&ctx, data, data_len);
    sha256_final(&ctx, i_hash);
    
    // outer hash
    sha256_init(&ctx);
    sha256_update(&ctx, o_pad, 64);
    sha256_update(&ctx, i_hash, 32);
    sha256_final(&ctx, out);
}

// ============== 密钥和验证逻辑 ==============
// 密钥从 key.h 文件包含（由 generate_key_header.py 自动生成）

/**
 * 验证注册码
 * 
 * @param input_code 用户输入的注册码（如 "ABCD-EFGH-IJKL-MNOP"）
 * @param android_id 设备的 Android ID
 * @return 1=验证成功, 0=验证失败
 */
JNIEXPORT jint JNICALL
Java_com_yy_medtrace_data_RegistrationCodeNative_verifyLicense(
    JNIEnv *env,
    jobject thiz,
    jstring input_code,
    jstring android_id) {
    
    // 获取输入参数
    const char *code = (*env)->GetStringUTFChars(env, input_code, NULL);
    const char *device_id_raw = (*env)->GetStringUTFChars(env, android_id, NULL);
    
    if (code == NULL || device_id_raw == NULL) {
        return 0;
    }
    
    // 设备ID转大写（与Python端保持一致）
    char device_id[64];
    int id_len = strlen(device_id_raw);
    if (id_len >= 64) id_len = 63;
    for (int i = 0; i < id_len; i++) {
        char c = device_id_raw[i];
        device_id[i] = (c >= 'a' && c <= 'z') ? c - 32 : c;
    }
    device_id[id_len] = '\0';
    
    // 清理输入：移除横杠，转大写
    char clean_code[64];
    int code_idx = 0;
    for (int i = 0; code[i] != '\0' && code_idx < 63; i++) {
        if (code[i] != '-') {
            clean_code[code_idx++] = (code[i] >= 'a' && code[i] <= 'z') 
                ? code[i] - 32  // 转大写
                : code[i];
        }
    }
    clean_code[code_idx] = '\0';
    
    // 验证长度（24字符 = 12字节）
    if (code_idx != 24) {
        (*env)->ReleaseStringUTFChars(env, input_code, code);
        (*env)->ReleaseStringUTFChars(env, android_id, device_id_raw);
        return 0;
    }
    
    // 计算期望的注册码
    unsigned char expected_hash[32];
    hmac_sha256(SECRET_KEY, sizeof(SECRET_KEY),
                (const unsigned char *)device_id, strlen(device_id),
                expected_hash);
    
    // 转换为十六进制字符串（取前12字节 = 24字符）
    char expected_hex[25];
    for (int i = 0; i < 12; i++) {
        sprintf(expected_hex + i * 2, "%02X", expected_hash[i]);
    }
    expected_hex[24] = '\0';
    
    // 比较（常量时间比较，防止时序攻击）
    int result = 1;
    for (int i = 0; i < 24; i++) {
        if (clean_code[i] != expected_hex[i]) {
            result = 0;
        }
    }
    
    // 释放内存
    (*env)->ReleaseStringUTFChars(env, input_code, code);
    (*env)->ReleaseStringUTFChars(env, android_id, device_id_raw);
    
    return result;
}
