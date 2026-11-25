# Compilar RedLamp 3.2

En Linux o WSL, instala un JDK 17 con `javac`, Python 3, curl, Node.js, Apktool 2.11.1, Android SDK API 28 y Android Build Tools 35.0.0. Usa el gestor de SDK de Android Studio para instalar el SDK y sus herramientas.

Desde la raíz del repositorio, en la rama `develop`, ajusta las rutas al SDK y a Apktool y ejecuta:

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export APKTOOL_JAR="$HOME/herramientas/apktool-2.11.1.jar"
export REDLAMP_BUILD_LIBS="$PWD/dependencias"
bash android/studio/build-support/dependencies.sh "$REDLAMP_BUILD_LIBS"
bash android/studio/build.sh dist/Luz-MOES-3.1.apk "$PWD/RedLamp-3.2-sin-firma.apk"
node android/studio/tests/test-web.cjs
mkdir -p pruebas-java
javac -d pruebas-java android/studio/LampAdjustment.java android/studio/tests/AdjustmentTest.java
java -cp pruebas-java es.luzlocal.matter.AdjustmentTest
```

La compilación usa la APK 3.1 como base para conservar el controlador Matter, las bibliotecas ARM64 y los recursos. El Java y el HTML recuperados permiten modificar la interfaz y su integración con ese controlador. La APK de desarrollo se genera sin firma y contiene un DEX diferente del publicado.

La entrega original lista para instalar es `dist/Luz-MOES-3.2.apk`. Las APK 3.1 y 3.2 distribuidas comparten el identificador de aplicación y el certificado de firma. Para que una compilación propia actualice la versión instalada, se necesita la clave privada de ese certificado. Consulta el [README](README.md) para recuperar el historial original y verificar la instalación.
