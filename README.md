# RedLamp · Luz MOES

Aplicación Android para controlar bombillas MOES. El trabajo de migración a GitHub se sigue en la [issue #2](https://github.com/magiclab-coding/RedLamp/issues/2). El código Android está en `android/`, las APK del sitio en `dist/` y las entregas anteriores en `archive/`.

## Estado de la recuperación

Se recuperaron los seis commits de origen y las APK históricas. La copia Git original se comprobó con `git bundle verify`. El archivo `git bundle` conserva los seis commits con sus identificadores originales.

El ensamblado local de 3.2 se verificó a partir de la APK 3.1 y el código recuperado. El proceso usa Java 17 para compilar a nivel Java 8, Android SDK API 28, Build Tools 35.0.0 y Apktool 2.11.1. Sustituye el DEX de la interfaz, el HTML y el manifiesto en una copia de la APK base y alinea el resultado. La APK sin firma declara `es.luzlocal.multi`, versión 3.2 (`versionCode` 7) y arquitectura `arm64-v8a`.

Los tres JAR de Matter para la compilación están en [Nordic Semiconductor, revisión 9ad6dec](https://github.com/nordicsemi/kotlin-nrf-matter/tree/9ad6dec0c72f2bdd1bc67b9c78defe5cf4567142/lib/libs). ZXing Core 3.5.3 está en [Maven Central](https://central.sonatype.com/artifact/com.google.zxing/core/3.5.3). Se descargaron las cuatro dependencias y sus SHA-256 coinciden con las copias recuperadas.

Las APK publicadas 3.1 y 3.2 comparten el certificado SHA-256 `369533971c66d7893bad6fbacd1432b064560728559424785fad5276cacff7c3`. La prueba de actualización con una bombilla real en Android 9 sigue pendiente.

## Entorno de desarrollo Android

1. Clona el repositorio privado con una cuenta de GitHub con acceso y abre la rama de desarrollo.

   ```bash
   git clone https://github.com/magiclab-coding/RedLamp.git
   cd RedLamp
   git switch develop
   ```

2. Instala un JDK con `javac`, Python 3, Apktool 2.11.1 y el SDK de Android. La compilación recuperada usaba `android.jar` de Android 9 (API 28) y las herramientas `d8`, `zipalign` y `apksigner` de Android 15. Instala también Platform Tools para usar `adb`. En Android Studio puedes instalar los componentes desde SDK Manager. Identifica la ruta de cada componente en tu equipo.

3. Sigue [COMPILAR.md](COMPILAR.md) para preparar las dependencias, ensamblar la APK 3.2 y ejecutar las pruebas de Java y de la interfaz.

4. Para preparar una actualización compatible, conserva el identificador de aplicación, aumenta `versionCode` y firma con el mismo certificado que la APK instalada. La clave privada y su contraseña se gestionan fuera de Git. Alinea la APK antes de firmarla y compara los certificados antes de instalar. Ejecuta los comandos desde la raíz del repositorio con las herramientas del SDK en el `PATH`.

   ```bash
   apksigner verify --print-certs dist/Luz-MOES-3.1.apk
   apksigner verify --print-certs dist/Luz-MOES-3.2.apk
   adb devices
   adb install -r dist/Luz-MOES-3.2.apk
   ```

5. En el Sony Xperia con Android 9, prueba la actualización sobre 3.1 y comprueba que se conservan las bombillas configuradas. Abre una tarjeta, prueba el menú de opciones y comprueba que los deslizadores mantienen el valor al soltarlos. Registra los resultados y las capturas en la issue correspondiente.

## Historial identificado

Los commits de esta tabla pertenecen al repositorio de origen del sitio de descargas. La versión del sitio y la versión de la aplicación son numeraciones distintas.

| Versión del sitio | APK incorporada en origen | Commit de origen |
| --- | --- | --- |
| 1 | Luz-MOES.apk (compacta) | `e33af7801ac564b4fbc345fa77ff65c00bd8c9a82` |
| 2 | Luz-MOES-Casa.apk | `1dca16c06bcf68bf466a13e9de049549eba92f18` |
| 3 | Luz-MOES-Multi.apk | `9acbb1c69f90b3175a84d5cece1e0a59e630a26c` |
| 4 | Luz-MOES-3.apk | `8ceb17aa7585c816ecf3dda881314b93648fb643` |
| 5 | Luz-MOES-3.1.apk | `8170a53f059d705fcb2429b9514df6b2e10d266f` |
| 6 | Luz-MOES-3.2.apk | `89f08fa41ed1530afb82ee38b1192f3cbaa3cb1c` |

[Descarga de Luz MOES](https://luz-moes-android.martint-rubens.chatgpt.site)

### Entregas anteriores localizadas

| Archivo | Ubicación |
| --- | --- |
| Luz-MOES-compacta.apk | `archive/` |
| Luz-MOES-compacta(1).apk | `archive/` |
| Luz-MOES-1.0.apk | `archive/parts/` |
| Luz-MOES-1.0.1.apk | `archive/parts/` |
| Luz-MOES-1.0.1(1).apk | `archive/parts/` |

Las dos APK compactas están en `archive/`. La compacta sin sufijo coincide byte a byte con `dist/Luz-MOES.apk`. El ZIP original contiene la APK 1.0. La firma de las dos APK etiquetadas `(1)` queda sin verificar.

### Recuperar las APK antiguas y el historial Git

Desde la raíz del repositorio, en Linux o WSL, ejecuta:

```bash
mkdir -p recuperados
cat archive/parts/Luz-MOES-1.0.apk.part-* > recuperados/Luz-MOES-1.0.apk
cat archive/parts/Luz-MOES-1.0.1.apk.part-* > recuperados/Luz-MOES-1.0.1.apk
cat archive/parts/'Luz-MOES-1.0.1(1).apk'.part-* > 'recuperados/Luz-MOES-1.0.1(1).apk'
cat archive/parts/RedLamp-historial-original.bundle.part-* > recuperados/RedLamp-historial-original.bundle
sha256sum -c archive/SHA256SUMS
git bundle verify recuperados/RedLamp-historial-original.bundle
git clone --branch main recuperados/RedLamp-historial-original.bundle RedLamp-historial
```

El archivo `archive/SHA256SUMS` verifica las APK directas, las piezas y los archivos reconstruidos. El clon `RedLamp-historial` contiene los seis commits originales del sitio.

## Flujo de cambios

Consulta [AGENTS.md](AGENTS.md). Registra cada petición en una issue y reutiliza la original si reaparece. Añade capturas en el cuerpo de la issue cuando existan. Trabaja desde `develop`, referencia la issue en los commits y verifica el resultado en la rama o el despliegue solicitado.
