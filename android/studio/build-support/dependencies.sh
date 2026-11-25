#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Uso: bash android/studio/build-support/dependencies.sh DIRECTORIO" >&2
    exit 2
fi

mkdir -p "$1"
cd "$1"
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
matter=https://raw.githubusercontent.com/nordicsemi/kotlin-nrf-matter/9ad6dec0c72f2bdd1bc67b9c78defe5cf4567142/lib/libs
maven=https://repo.maven.apache.org/maven2/com/google/zxing/core/3.5.3

download() {
    local name=$1 url=$2 expected=$3
    curl --fail --location --silent --show-error --output "$work_dir/$name" "$url"
    printf '%s  %s\n' "$expected" "$work_dir/$name" | sha256sum --check
    mv "$work_dir/$name" "$name"
}

download AndroidPlatform.jar "$matter/AndroidPlatform.jar" f3808f16543c808a0267223a2931bb3aeebd8ba689da01ecf76584df6abcc97c
download CHIPController.jar "$matter/CHIPController.jar" 981899764f351465aae9bc919c936bc3a1be5d41ba8c319ce45832440df8e919
download CHIPInteractionModel.jar "$matter/CHIPInteractionModel.jar" 3009bb928f3f519fbc05e0da5c410bdff5f355d2d4d84dbcde3ebe4a5cc5adb7
download zxing.jar "$maven/core-3.5.3.jar" 8d8064c1636fdaef7189dd9055c7d59950a8940a12f2293956446ec3c109fd82
