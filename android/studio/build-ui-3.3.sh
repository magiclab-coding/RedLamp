#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Uso: bash android/studio/build-ui-3.3.sh BASE_3.2.apk SALIDA-SIN-FIRMA.apk" >&2
    exit 2
fi

: "${ANDROID_SDK_ROOT:?Indica el directorio del SDK de Android}"
: "${APKTOOL_JAR:?Indica la ruta a Apktool 2.11.1}"

source_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
base_apk=$(realpath "$1")
output_apk=$(realpath -m "$2")
if [[ "$base_apk" == "$output_apk" ]]; then
    echo "La APK base y la salida deben tener rutas distintas." >&2
    exit 2
fi
printf '%s  %s\n' '6afa989859f0c4eeb8cbca79786cd9e58577149453a7c3fd974d03a34ec881d9' "$base_apk" | sha256sum --check --status

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
java -jar "$APKTOOL_JAR" d --no-src -p "$work_dir/framework" -o "$work_dir/package" "$base_apk"
sed -i -e 's/^  versionCode: 7$/  versionCode: 8/' -e 's/^  versionName: 3\.2$/  versionName: 3.3/' "$work_dir/package/apktool.yml"
grep -qx '  versionCode: 8' "$work_dir/package/apktool.yml"
grep -qx '  versionName: 3.3' "$work_dir/package/apktool.yml"
java -jar "$APKTOOL_JAR" b -p "$work_dir/framework" -o "$work_dir/rebuilt.apk" "$work_dir/package"
python3 "$source_dir/build-support/assemble.py" "$base_apk" "$work_dir/rebuilt.apk" - \
    "$source_dir/studio.html" "$work_dir/unsigned.apk"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/zipalign" -f 4 "$work_dir/unsigned.apk" "$output_apk"
