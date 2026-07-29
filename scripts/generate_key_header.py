#!/usr/bin/env python3
"""
从.secret_key文件生成C头文件
在NDK构建前运行此脚本
"""
import os
import sys

def generate_key_header():
    """生成key.h头文件"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    key_file = os.path.join(script_dir, ".secret_key")
    header_file = os.path.join(script_dir, "..", "app", "src", "main", "cpp", "key.h")
    
    # 读取密钥
    if not os.path.exists(key_file):
        print(f"❌ 错误: 未找到 {key_file}")
        sys.exit(1)
    
    with open(key_file, 'r') as f:
        key_hex = f.read().strip()
    
    if len(key_hex) != 64:
        print(f"❌ 错误: 密钥长度不正确")
        sys.exit(1)
    
    # 转换为C数组
    key_bytes = bytes.fromhex(key_hex)
    c_array = ', '.join(f'0x{b:02X}' for b in key_bytes)
    
    # 生成头文件
    header_content = f"""// 自动生成的密钥文件 - 请勿手动编辑！
// 由 scripts/generate_key_header.py 生成
// 源文件: .secret_key

#ifndef KEY_H
#define KEY_H

static const unsigned char SECRET_KEY[32] = {{
    {c_array}
}};

#endif // KEY_H
"""
    
    with open(header_file, 'w') as f:
        f.write(header_content)
    
    print(f"✅ 已生成 {header_file}")

if __name__ == "__main__":
    generate_key_header()
