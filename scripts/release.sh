#!/usr/bin/env bash
#
# MedTrace 发版脚本
# 用法: ./scripts/release.sh <版本名> <versionCode> [product仓路径]
# 例:   ./scripts/release.sh v3.2.4 20 ../product
#
# 做的事:
#   1. 强制从 dev 分支构建（禁止在 master 上直接发版，防止污染稳定基板）
#   2. clean + assembleRelease，杜绝 Gradle UP-TO-DATE 缓存导致的旧包
#   3. aapt 校验产物 versionName/versionCode 与预期一致，不一致直接失败
#   4. 复制到 product 仓（若提供路径）并提示后续手动发 GitHub Release
#
set -euo pipefail

VERSION_NAME="${1:-}"
VERSION_CODE="${2:-}"
PRODUCT_DIR="${3:-}"

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "用法: $0 <版本名> <versionCode> [product仓路径]" >&2
  echo "例:   $0 v3.2.4 20 ../product" >&2
  exit 1
fi

# ---- 0. 环境 ----
if [[ -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME=/usr/local/share/android-sdk
fi
export JAVA_HOME="$(/usr/libexec/java_home -v 22 2>/dev/null || echo "$JAVA_HOME")"
BUILD_TOOLS_DIR="$ANDROID_HOME/build-tools/35.0.0"
AAPT="$BUILD_TOOLS_DIR/aapt"

if [[ ! -x "$AAPT" ]]; then
  echo "错误: 找不到 aapt ($AAPT)" >&2
  exit 1
fi

cd "$(dirname "$0")/.."
REPO_ROOT="$(pwd)"

# ---- 1. 禁止在 master 上发版 ----
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT_BRANCH" == "master" ]]; then
  echo "错误: 不能在 master 分支直接发版。请切到 dev 分支后运行本脚本。" >&2
  exit 1
fi
echo ">> 当前分支: $CURRENT_BRANCH (非 master，OK)"

# ---- 2. 工作区干净检查 ----
if [[ -n "$(git status --porcelain)" ]]; then
  echo "警告: 工作区有未提交改动，继续前请确认这些改动应纳入本次发版。" >&2
  git status --short
fi

# ---- 3. clean + assembleRelease ----
echo ">> clean + assembleRelease ..."
./gradlew :app:clean :app:assembleRelease

APK="$REPO_ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "错误: 未生成 APK: $APK" >&2
  exit 1
fi

# ---- 4. aapt 校验版本号 ----
ACTUAL_NAME="$("$AAPT" dump badging "$APK" 2>/dev/null | grep "version" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
ACTUAL_CODE="$("$AAPT" dump badging "$APK" 2>/dev/null | grep "version" | sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p")"

if [[ "$ACTUAL_NAME" != "$VERSION_NAME" ]]; then
  echo "错误: versionName 不匹配! 期望=$VERSION_NAME 实际=$ACTUAL_NAME" >&2
  echo "      请先修改 app/build.gradle.kts 的 versionName 再运行本脚本。" >&2
  exit 1
fi
if [[ "$ACTUAL_CODE" != "$VERSION_CODE" ]]; then
  echo "错误: versionCode 不匹配! 期望=$VERSION_CODE 实际=$ACTUAL_CODE" >&2
  echo "      请先修改 app/build.gradle.kts 的 versionCode 再运行本脚本。" >&2
  exit 1
fi
echo ">> 版本校验通过: $ACTUAL_NAME (code $ACTUAL_CODE)"

# ---- 5. 复制到 product 仓 ----
if [[ -n "$PRODUCT_DIR" ]]; then
  DEST_DIR="$PRODUCT_DIR/MedTrace"
  if [[ ! -d "$DEST_DIR" ]]; then
    echo "错误: product 仓 MedTrace 目录不存在: $DEST_DIR" >&2
    exit 1
  fi
  DEST="$DEST_DIR/MedTrace-$ACTUAL_NAME.apk"
  cp "$APK" "$DEST"
  echo ">> 已复制到: $DEST"
  echo ">> 提醒: 请到 product 仓 git add/commit/push，并手动在 GitHub 创建 Release。"
else
  echo ">> 未提供 product 仓路径，APK 位于: $APK"
fi

echo ""
echo "发版产物就绪: $ACTUAL_NAME (code $ACTUAL_CODE)"
echo "后续步骤:"
echo "  1. git tag $ACTUAL_NAME && git push origin $ACTUAL_NAME"
echo "  2. 在 medtrace 源码仓与 product 仓分别创建 GitHub Release 并上传 APK"
echo "  3. 更新三个 README 的版本号"
