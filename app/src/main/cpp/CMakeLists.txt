cmake_minimum_required(VERSION 3.18.1)
project("medtrace")

# 添加 native 库（自包含 HMAC-SHA256，不需要 OpenSSL）
add_library(
    "license_verify"
    SHARED
    "license_verify.c"
)

# 链接 log 库
find_library(log-lib log)
target_link_libraries("license_verify" ${log-lib})
