# RedLamp · Luz MOES

Aplicación Android para controlar bombillas MOES. El trabajo de migración a GitHub se sigue en la [issue #2](https://github.com/magiclab-coding/RedLamp/issues/2). El código fuente y las APK históricas están pendientes de importación en esta rama.

## Entorno de desarrollo Android

1. Clona el repositorio privado con una cuenta de GitHub con acceso y abre la rama de desarrollo.

   ```bash
   git clone https://github.com/magiclab-coding/RedLamp.git
   cd RedLamp
   git switch develop
   ```

2. Instala un JDK con `javac`, Python 3, Apktool 2.11.1 y el SDK de Android. La compilación recuperada usaba `android.jar` de Android 9 (API 28) y las herramientas `d8`, `zipalign` y `apksigner` de Android 15. Instala también Platform Tools para usar `adb`. En Android Studio puedes instalar los componentes desde SDK Manager. Identifica la ruta de cada componente en tu equipo.

3. Al recuperar el árbol de compilación, conserva la APK base exacta de cada versión. La copia inspeccionada contiene `android/casa`, `android/multi` y `android/studio`. Studio contiene `Studio.java`, `Rooms.java`, `CameraPage.java`, `studio.html` y el icono. El procedimiento observado recompilaba Java con `javac`, generaba DEX con D8 y repaquetaba la base con Apktool. Los comandos exactos de ensamblado, los archivos de apoyo y la comprobación de una compilación limpia quedan pendientes en la issue #2.

4. Para preparar una actualización compatible, conserva el identificador de aplicación, aumenta `versionCode` y firma con el mismo certificado que la APK instalada. La clave privada y su contraseña se gestionan fuera de Git. Alinea la APK antes de firmarla y compara los certificados antes de instalar. Sustituye los nombres de archivo por las copias locales.

   ```bash
   apksigner verify --print-certs Luz-MOES-3.1.apk
   apksigner verify --print-certs Luz-MOES-3.2.apk
   adb devices
   adb install -r Luz-MOES-3.2.apk
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
| 6 | Luz-MOES-3.2.apk según el registro de publicación | `89f08fa41ed1530afb82ee38b1192f3cbaa3cb1c` |

[Descarga de Luz MOES](https://luz-moes-android.martint-rubens.chatgpt.site)

### Entregas anteriores localizadas

| Archivo | Bytes | Origen |
| --- | ---: | --- |
| Instalar-Luz-MOES.html | 7857212 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_f2a2cd93c6ec8191b22374cc9e31f360&account_id=personal) |
| Reparar-Luz-MOES.html | 99848 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_0a10f20f77788191b7fa586e617f493d&account_id=personal) |
| Luz-MOES-compacta(1).apk | 5654043 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_f642d9a2fc348191ba9f37fe44bfdbf7&account_id=personal) |
| Luz-MOES-compacta.apk | 5727469 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_f494e571365881919aca2d494ecb8a6f&account_id=personal) |
| Luz-MOES-1.0.1(1).apk | 24674607 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_bc840a2b9e508191a8d0a444a376f25c&account_id=personal) |
| Luz-MOES-1.0.1.apk | 24995053 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_613f87cf176081919968d5adddc25b36&account_id=personal) |
| Luz-MOES-Android.zip | 24995123 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_4a15fba0e3688191bf0abaa5edc60791&account_id=personal) |
| Luz-MOES-1.0.apk | 24994993 | [Abrir archivo](https://chatgpt.com/library?file_id=libfile_61741c34d8108191855a77612c8c8e8d&account_id=personal) |

Para cerrar la migración, recuperar el historial original y los archivos adicionales, calcular SHA-256 para detectar duplicados y verificar los archivos tras subirlos. Completar el procedimiento de compilación a partir del entorno histórico y reproducir la compilación en una copia limpia.

## Flujo de cambios

Consulta [AGENTS.md](AGENTS.md). Registra cada petición en una issue y reutiliza la original si reaparece. Añade capturas en el cuerpo de la issue cuando existan. Trabaja desde `develop`, referencia la issue en los commits y verifica el resultado en la rama o el despliegue solicitado.
