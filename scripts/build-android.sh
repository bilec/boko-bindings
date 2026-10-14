#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output=${1:?Usage: build-android.sh OUTPUT_DIRECTORY}
export RUSTFLAGS="-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384"

cargo test --locked --manifest-path "$root/Cargo.toml" -p boko-bindings-core
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 --platform 24 -o "$output" \
    build --release --locked --manifest-path "$root/Cargo.toml" -p boko-android-jni
find "$output" -type f -name 'libboko-*.so' -delete
for abi in arm64-v8a armeabi-v7a x86_64; do
    test -s "$output/$abi/libboko_jni.so"
done