# Instalar y compilar RedLamp 3.2

Usa Linux o Ubuntu en WSL 2 para compilar. La compilación ensambla una APK de desarrollo a partir de la APK 3.1, que aporta el controlador Matter, los recursos y las bibliotecas ARM64. Los scripts recompilan el código Java recuperado, actualizan el HTML y generan la versión 3.2 sin firma. La APK de desarrollo tiene un DEX distinto del de la APK distribuida.

## 1. Preparar el entorno aislado

Instala Git, JDK 17, Python 3, curl, unzip y Node.js. En Ubuntu o WSL con Ubuntu:

```bash
sudo apt update
sudo apt install git openjdk-17-jdk python3 curl unzip nodejs
java -version
javac -version
```

Si ya hay varios JDK instalados, selecciona Java 17 para esta sesión ajustando el `PATH` antes de continuar. Crea un directorio dedicado. Usa una cuenta de GitHub con acceso al repositorio privado.

```bash
export REDLAMP_DEV="$HOME/redlamp-dev"
mkdir -p "$REDLAMP_DEV"/{tools,sdk,android,avd,deps,out,tmp}
export ANDROID_HOME="$REDLAMP_DEV/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$REDLAMP_DEV/android"
export ANDROID_AVD_HOME="$REDLAMP_DEV/avd"
export APKTOOL_JAR="$REDLAMP_DEV/tools/apktool_2.11.1.jar"
export REDLAMP_BUILD_LIBS="$REDLAMP_DEV/deps"
export TMPDIR="$REDLAMP_DEV/tmp"
git clone --branch develop https://github.com/magiclab-coding/RedLamp.git "$REDLAMP_DEV/source"
cd "$REDLAMP_DEV/source"
git status --short --branch
```

Ejecuta los pasos siguientes en la misma sesión. El SDK, los dispositivos virtuales, las dependencias y las salidas quedan fuera del clon.

## 2. Instalar el SDK y Apktool

Estas instrucciones usan las herramientas de línea de comandos de Android para Linux x86_64. Extrae las herramientas en un SDK nuevo y ejecuta `sdkmanager --licenses` para leer y aceptar las licencias necesarias.

```bash
curl -fL 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' -o "$REDLAMP_DEV/tools/android-cli.zip"
printf '%s  %s\n' '2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258' "$REDLAMP_DEV/tools/android-cli.zip" | sha256sum -c
unzip -q "$REDLAMP_DEV/tools/android-cli.zip" -d "$REDLAMP_DEV/tools/android-cli"
mkdir -p "$ANDROID_HOME/cmdline-tools"
mv "$REDLAMP_DEV/tools/android-cli/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'platforms;android-28' 'build-tools;35.0.0' 'platform-tools'
curl -fL 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL_JAR"
printf '%s  %s\n' '56d59c524fc764263ba8d345754d8daf55b1887818b15cd3b594f555d249e2db' "$APKTOOL_JAR" | sha256sum -c
java -jar "$APKTOOL_JAR" --version
```

## 3. Compilar y pasar las pruebas

Desde `$REDLAMP_DEV/source`, descarga las cuatro dependencias. El script comprueba su SHA-256. El ensamblado verifica también el hash de la APK 3.1 que usa como base.

```bash
bash android/studio/build-support/dependencies.sh "$REDLAMP_BUILD_LIBS"
bash android/studio/build.sh dist/Luz-MOES-3.1.apk "$REDLAMP_DEV/out/RedLamp-3.2-sin-firma.apk"
node android/studio/tests/test-web.cjs
mkdir -p "$REDLAMP_DEV/out/pruebas-java"
javac -d "$REDLAMP_DEV/out/pruebas-java" android/studio/LampAdjustment.java android/studio/tests/AdjustmentTest.java
java -cp "$REDLAMP_DEV/out/pruebas-java" es.luzlocal.matter.AdjustmentTest
BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
"$BUILD_TOOLS/zipalign" -c 4 "$REDLAMP_DEV/out/RedLamp-3.2-sin-firma.apk"
"$BUILD_TOOLS/aapt" dump badging "$REDLAMP_DEV/out/RedLamp-3.2-sin-firma.apk" | grep -E '^(package:|sdkVersion:|native-code:)'
```

Comprueba que la salida indique `es.luzlocal.multi`, `versionName=3.2`, `versionCode=7`, API mínima 28 y `arm64-v8a`.

## 4. Instalar la 3.2 distribuida en el Xperia

En el Xperia, activa las opciones de desarrollador y la depuración USB. Conecta el cable y acepta la huella RSA del ordenador en el teléfono. Si compilas en WSL, puedes usar `adb.exe` de Android SDK Platform-Tools en Windows con el móvil conectado por USB a Windows. Usa una ruta de APK accesible para Windows cuando ejecutes `adb.exe`.

```bash
ADB="$ANDROID_HOME/platform-tools/adb"
"$BUILD_TOOLS/apksigner" verify --print-certs dist/Luz-MOES-3.1.apk
"$BUILD_TOOLS/apksigner" verify --print-certs dist/Luz-MOES-3.2.apk
"$ADB" devices -l
```

Ambas APK distribuidas comparten el certificado SHA-256 `369533971c66d7893bad6fbacd1432b064560728559424785fad5276cacff7c3`. Si tienes otros dispositivos conectados, copia el identificador del Xperia que devuelve `adb devices -l`.

```bash
read -r -p 'Identificador del Xperia en adb: ' XPERIA_SERIAL
"$ADB" -s "$XPERIA_SERIAL" install -r dist/Luz-MOES-3.2.apk
"$ADB" -s "$XPERIA_SERIAL" shell dumpsys package es.luzlocal.multi | grep -E 'versionCode=|versionName='
```

`-r` solicita una reinstalación conservando los datos. La actualización 3.1 → 3.2 quedó confirmada en el Xperia físico en la [incidencia #2](https://github.com/magiclab-coding/RedLamp/issues/2).

## 5. Firmar una compilación propia para pruebas

Guarda una clave de desarrollo en el entorno aislado. `keytool` y `apksigner` piden las contraseñas de forma interactiva.

```bash
mkdir -p "$REDLAMP_DEV/keys"
keytool -genkeypair -keystore "$REDLAMP_DEV/keys/redlamp-dev.p12" -storetype PKCS12 -alias redlamp-dev -keyalg RSA -keysize 2048 -validity 3650
"$BUILD_TOOLS/apksigner" sign --ks "$REDLAMP_DEV/keys/redlamp-dev.p12" --ks-key-alias redlamp-dev --out "$REDLAMP_DEV/out/RedLamp-3.2-desarrollo.apk" "$REDLAMP_DEV/out/RedLamp-3.2-sin-firma.apk"
"$BUILD_TOOLS/apksigner" verify --print-certs "$REDLAMP_DEV/out/RedLamp-3.2-desarrollo.apk"
```

Instala esta variante en un dispositivo de pruebas limpio y compatible con ARM64. Para actualizar una instalación distribuida hay que firmar con la clave original y mantener el identificador de aplicación. Conserva el almacén de claves fuera del repositorio.

## 6. Instalar el emulador con el perfil Xperia XZ1 Compact

El dispositivo virtual reproduce Android 9 (API 28), la pantalla de 4,6 pulgadas y 720 × 1280 píxeles, con 320 dpi y 4 GB de RAM. Usa una imagen de Google APIs. La interfaz del sistema y los periféricos son los del emulador de Android.

En Windows, puedes usar Device Manager de Android Studio y crear un perfil de hardware personalizado con esos parámetros. En Linux x86_64, instala la imagen de Android 9 y crea el perfil desde la sesión de los pasos anteriores:

```bash
IMAGE='system-images;android-28;google_apis;x86_64'
AVD_NAME=Xperia_XZ1_Compact_API_28
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'emulator' "$IMAGE"
printf 'no\n' | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD_NAME" -k "$IMAGE" -d pixel
python3 - "$ANDROID_AVD_HOME/$AVD_NAME.avd/config.ini" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
settings = {
    "avd.ini.displayname": "Xperia XZ1 Compact (G8441) - Android 9",
    "hw.lcd.width": "720",
    "hw.lcd.height": "1280",
    "hw.lcd.density": "320",
    "hw.ramSize": "4096",
    "hw.initialOrientation": "Portrait",
    "showDeviceFrame": "no",
}
lines = [line for line in path.read_text().splitlines() if line.partition("=")[0].strip() not in settings]
lines.extend(f"{key} = {value}" for key, value in settings.items())
path.write_text("\n".join(lines) + "\n")
PY
EMU="$ANDROID_HOME/emulator/emulator"
"$EMU" -list-avds
"$EMU" -accel-check
"$EMU" -avd "$AVD_NAME" -no-snapshot
```

En Linux, la aceleración necesita KVM. Como comprobación puntual en un host sin KVM, Android Emulator permite arrancar con `ANDROID_I_WANT_MY_TCG=yes` y `-accel off`. El arranque por software puede tardar varios minutos y no está soportado para uso habitual.

Desde otra terminal, define `ADB` con la ruta de Platform-Tools y comprueba el arranque y las arquitecturas del invitado:

```bash
ADB="$HOME/redlamp-dev/sdk/platform-tools/adb"
"$ADB" -e wait-for-device
"$ADB" -e shell getprop sys.boot_completed
"$ADB" -e shell getprop ro.build.version.sdk
"$ADB" -e shell getprop ro.product.cpu.abilist
"$ADB" -e shell wm size
"$ADB" -e shell wm density
```

Cuando `sys.boot_completed` sea `1`, la API debe indicar `28` y la pantalla `720x1280`. Instala RedLamp solo si la lista de arquitecturas incluye `arm64-v8a`. Cambia al clon antes de ejecutar:

```bash
cd "$HOME/redlamp-dev/source"
"$ADB" -e install dist/Luz-MOES-3.1.apk
"$ADB" -e install -r dist/Luz-MOES-3.2.apk
"$ADB" -e shell dumpsys package es.luzlocal.multi | grep -E 'versionCode=|versionName='
```

En el entorno Linux x86_64 de RedLamp se verificó el arranque por software de Android 9 con API 28, 720 × 1280 y 320 dpi. La imagen inició con `x86_64,x86`. La APK requiere `arm64-v8a`.

Para probar Android 9 con la ABI del Xperia en un equipo compatible con emulación ARM64, busca `system-images;android-28;google_apis;arm64-v8a` en el SDK Manager. En Android Studio, usa el mismo perfil de hardware en Device Manager, elige **Android 9 / API 28 / Google APIs / ARM 64 v8a** y nombra el dispositivo `Xperia_XZ1_Compact_API_28_ARM64`. Tras el arranque, confirma `arm64-v8a` con `adb shell getprop ro.product.cpu.abilist` antes de instalar las APK. En un host x86_64, una imagen x86_64 de Android 11 que anuncie `arm64-v8a` sirve para ensayar la aplicación con otra API. Usa el Xperia físico para comprobar el emparejamiento y control de la bombilla Matter.

[Android SDK Manager](https://developer.android.com/tools/sdkmanager) · [Aceleración del emulador](https://developer.android.com/studio/run/emulator-acceleration) · [ABI admitidas por imágenes de Android](https://developer.android.com/studio/releases/emulator) · [Guía Sony Xperia XZ1 Compact con Android 9](https://www-support-downloads.sonymobile.com/g8441/userguide_EN_G8441_5_Android9.0.pdf)

## Generar la actualización 3.3 de interfaz

Desde la raíz del clon, con el SDK y Apktool configurados como en los pasos 1 y 2:

```bash
bash android/studio/build-ui-3.3.sh dist/Luz-MOES-3.2.apk "$REDLAMP_DEV/out/Luz-MOES-3.3-sin-firma.apk"
node android/studio/tests/test-web.cjs
```

El script verifica la APK base, actualiza su manifiesto a 3.3 (código 8) y sustituye la interfaz HTML. Conserva el código DEX, las bibliotecas Matter y los demás recursos de 3.2. La APK firmada con el certificado original está en `dist/Luz-MOES-3.3.apk` de la rama de desarrollo.
