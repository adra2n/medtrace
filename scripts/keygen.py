#!/usr/bin/env python3
"""
RSA 密钥对生成器
运行: python3 keygen.py
"""
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
import os

def generate_keypair():
    """生成 RSA 密钥对"""
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )
    public_key = private_key.public_key()
    return private_key, public_key

def save_keys(private_key, public_key, output_dir="keys"):
    """保存密钥到文件"""
    os.makedirs(output_dir, exist_ok=True)
    
    # 保存私钥 (PEM格式)
    private_pem = private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )
    with open(os.path.join(output_dir, "private_key.pem"), "wb") as f:
        f.write(private_pem)
    
    # 保存公钥 (PEM格式)
    public_pem = public_key.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    with open(os.path.join(output_dir, "public_key.pem"), "wb") as f:
        f.write(public_pem)
    
    # 保存公钥 (Android用的字符串格式)
    public_str = public_pem.decode('utf-8')
    with open(os.path.join(output_dir, "public_key.txt"), "w") as f:
        f.write(public_str)
    
    print(f"✅ 密钥对已生成到 {output_dir}/")
    print(f"   - private_key.pem (私钥，保密！)")
    print(f"   - public_key.pem (公钥)")
    print(f"   - public_key.txt (公钥字符串，嵌入App用)")

if __name__ == "__main__":
    private_key, public_key = generate_keypair()
    save_keys(private_key, public_key)
