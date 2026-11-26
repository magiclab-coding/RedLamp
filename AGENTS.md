# RedLamp

Repositorio: https://github.com/magiclab-coding/RedLamp

## Trabajo en el proyecto

- Responder en español. Antes de ejecutar cualquier comando, mostrar al usuario su contenido exacto en un bloque de código.
- Al comenzar, leer las instrucciones aplicables y comprobar el remoto, la rama y los cambios locales. Conservar el trabajo existente y continuar con los pasos autorizados.
- Buscar cada petición entre las issues abiertas y cerradas. Actualizar la existente o reabrirla si reaparece el fallo. Registrar el comportamiento actual, el esperado y los pasos para reproducirlo. Incrustar las capturas disponibles en el cuerpo de la issue.
- Verificar las ramas disponibles y el destino solicitado antes de integrar. Limitar el cambio al alcance de la issue. Iniciar el mensaje de commit con una etiqueta como [feat], [bug] o [docs] e incluir la referencia a la issue.
- Probar el cambio desde su punto de entrada habitual. En cambios visuales, verificar el tamaño de pantalla afectado y adjuntar una captura del resultado. En la solicitud de integración, resumir los cambios y las pruebas realizadas.
- Para una entrega en desarrollo, comprobar el commit en la rama acordada. Cuando se solicite publicar, verificar el destino de RedLamp, seguir el despliegue y probar la función publicada.
- Cerrar la issue tras verificar el resultado en el destino solicitado. Registrar rama, commit, pruebas y evidencias. Indicar cualquier comprobación pendiente.

## Versiones de la APK

- `develop` contiene compilaciones en prueba. Incrementa el `versionCode` para cada APK distribuida.
- `main` contiene el código de las versiones estables. Cada publicación estable incrementa la versión mayor y reinicia la menor a cero.
- Publica en el sitio de descargas la APK estable firmada con el certificado de RedLamp. Actualiza `stable.json` en la misma publicación con `versionName`, `versionCode` y el SHA-256 de esa APK. Comprueba que ambos archivos públicos coinciden antes de dar la publicación por finalizada.
- Las versiones de prueba incrementan la versión menor y se anuncian en `preview.json` con los mismos campos que `stable.json`. Ambos canales usan el nombre `Luz-MOES-<versión>.apk` y el mismo certificado.
