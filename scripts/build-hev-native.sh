#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HEV_ROOT="$ROOT/hev-socks5-tunnel"
JNI_DIR="$HEV_ROOT/jni"
JNI_LIBS="$ROOT/app/src/main/jniLibs"
NDK_ROOT="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${NDK_HOME:-}}}"

if [[ -z "$NDK_ROOT" ]]; then
  if command -v ndk-build >/dev/null 2>&1; then
    NDK_BUILD="$(command -v ndk-build)"
  else
    echo "ERROR: Set ANDROID_NDK_HOME (or NDK_HOME), or add ndk-build to PATH." >&2
    exit 1
  fi
else
  NDK_BUILD="$NDK_ROOT/ndk-build"
fi

if [[ ! -x "$NDK_BUILD" ]]; then
  echo "ERROR: ndk-build not found or not executable: $NDK_BUILD" >&2
  exit 1
fi

mkdir -p "$HEV_ROOT"
if [[ ! -f "$JNI_DIR/Android.mk" ]]; then
  rm -rf "$JNI_DIR"
  echo "Cloning HEV SOCKS5 tunnel into hev-socks5-tunnel/jni ..."
  git clone --recursive https://github.com/heiher/hev-socks5-tunnel "$JNI_DIR"
fi

test -f "$JNI_DIR/Android.mk" || { echo "ERROR: jni/Android.mk not found" >&2; exit 1; }

mkdir -p "$JNI_LIBS"
echo "Building HEV native library with ndk-build ..."
"$NDK_BUILD" -C "$HEV_ROOT" \
  NDK_PROJECT_PATH="$HEV_ROOT" \
  NDK_LIBS_OUT="$JNI_LIBS" \
  NDK_OUT="$HEV_ROOT/obj" \
  APP_ABI="arm64-v8a" \
  APP_PLATFORM="android-29" \
  APP_CFLAGS="-O3 -DPKGNAME=hev/htproxy"

for abi in arm64-v8a; do
  lib="$JNI_LIBS/$abi/libhev-socks5-tunnel.so"
  test -s "$lib" || { echo "ERROR: Native library missing: $lib" >&2; exit 1; }
  echo "Built: $lib"
done
