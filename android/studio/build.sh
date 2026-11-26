#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 && $# -ne 4 ]]; then
    echo "Uso: bash android/studio/build.sh BASE_3.1.apk SALIDA-SIN-FIRMA.apk [VERSION CODIGO]" >&2
    exit 2
fi

: "${ANDROID_SDK_ROOT:?Indica el directorio del SDK de Android}"
: "${APKTOOL_JAR:?Indica la ruta a Apktool 2.11.1}"
: "${REDLAMP_BUILD_LIBS:?Indica el directorio de los JAR de compilación}"

version_name=${3:-3.2}
version_code=${4:-7}
[[ "$version_name" =~ ^[0-9]+(\.[0-9]+)+$ && "$version_code" =~ ^[0-9]+$ ]] || exit 2

source_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
base_apk=$(realpath "$1")
output_apk=$(realpath -m "$2")
if [[ "$base_apk" == "$output_apk" ]]; then
    echo "La APK base y la salida deben tener rutas distintas." >&2
    exit 2
fi
printf '%s  %s\n' '3076cbcd14a988fa283dbe8d51f6d824ca3a30441e59d1e67852bbe1bee1cbd9' "$base_apk" | sha256sum --check --status

android_jar="$ANDROID_SDK_ROOT/platforms/android-28/android.jar"
build_tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
class_path="$android_jar:$REDLAMP_BUILD_LIBS/*"
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
mkdir "$work_dir/stub" "$work_dir/classes" "$work_dir/dex"

javac --release 8 -cp "$class_path" -d "$work_dir/stub" "$source_dir/build-support/Luz.java"
javac --release 8 -cp "$class_path:$work_dir/stub" -d "$work_dir/classes" "$source_dir"/*.java
jar cf "$work_dir/changes.jar" -C "$work_dir/classes" .
"$build_tools/d8" --release --min-api 28 --lib "$android_jar" --output "$work_dir/dex" "$work_dir/changes.jar"

java -jar "$APKTOOL_JAR" d --no-src -p "$work_dir/framework" -o "$work_dir/package" "$base_apk"
sed -i -e "s/^  versionCode: 6$/  versionCode: $version_code/" -e "s/^  versionName: 3\.1$/  versionName: $version_name/" "$work_dir/package/apktool.yml"
grep -Fqx "  versionCode: $version_code" "$work_dir/package/apktool.yml"
grep -Fqx "  versionName: $version_name" "$work_dir/package/apktool.yml"
python3 - "$work_dir/package/AndroidManifest.xml" <<'PY_MANIFEST'
from pathlib import Path
import sys

manifest = Path(sys.argv[1])
xml = manifest.read_text()
assert xml.count("<application ") == 1 and xml.count("</application>") == 1
xml = xml.replace("<application ", '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n    <application ', 1)
xml = xml.replace("</application>", '<receiver android:name="es.luzlocal.matter.StableUpdate" android:exported="false" />\n    </application>', 1)
manifest.write_text(xml)
PY_MANIFEST
java -jar "$APKTOOL_JAR" b -p "$work_dir/framework" -o "$work_dir/rebuilt.apk" "$work_dir/package"

python3 "$source_dir/build-support/assemble.py" "$base_apk" "$work_dir/rebuilt.apk" \
    "$work_dir/dex/classes.dex" "$source_dir/studio.html" "$work_dir/unsigned.apk"
"$build_tools/zipalign" -f 4 "$work_dir/unsigned.apk" "$output_apk"
