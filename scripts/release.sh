#!/usr/bin/env bash
#
# MedTrace 发版脚本
# 用法: ./scripts/release.sh <版本名> <versionCode>
# 例:   ./scripts/release.sh v3.7.0 28
#
# 做的事:
#   1. 强制从 dev 分支构建（禁止在 master 上直接发版，防止污染稳定基板）
#   2. clean + assembleRelease，杜绝 Gradle UP-TO-DATE 缓存导致的旧包
#   3. aapt 校验产物 versionName/versionCode 与预期一致，不一致直接失败
#   4. 提示后续手动创建 GitHub Release 并上传 APK
#
set -euo pipefail

VERSION_NAME="${1:-}"
VERSION_CODE="${2:-}"

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "用法: $0 <版本名> <versionCode>" >&2
  echo "例:   $0 v3.7.0 28" >&2
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

echo ""
echo "发版产物就绪: $ACTUAL_NAME (code $ACTUAL_CODE)"
echo "APK 位置: $APK"
echo ""
echo "后续步骤:"
echo "  1. git tag $ACTUAL_NAME && git push origin $ACTUAL_NAME"
echo "  2. 创建 GitHub Release 并上传 APK:"
echo "     gh release create $ACTUAL_NAME $APK --title \"医迹 $ACTUAL_NAME\" --notes \"发布说明\""
echo "  3. 更新 README 版本号"
