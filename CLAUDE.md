# Sitio raíz y app «Rachas» — guía para Claude

- Este repositorio es **público**. Nunca pongas datos personales, la llave de firma (`*.jks`) ni contraseñas.
- `.well-known/assetlinks.json` debe tener el paquete `io.github.jcoronadoccorimanya08.rachas` y la huella SHA-256 de la llave con la que se firma el APK. Sin eso, la app no abre a pantalla completa ni recibe datos para los widgets.
- `.nojekyll` tiene que existir: sin él, GitHub Pages no publica `.well-known/`.
- La app Android solo guarda lo que le mandan las apps web por postMessage: lo que muestra el widget (en Bitácora, **solo números**; ver `src/lib/widget.ts` en cada app). No agregues permisos de internet propios ni analítica.
- Permisos permitidos, todos locales: notificaciones, alarmas exactas y arranque del teléfono, solo para el recordatorio de las 22:30. La notificación no muestra nombres de hábitos y en la pantalla de bloqueo solo dice «Hora de anotar tu día».
- `PostMessageService` tiene que estar en el manifiesto: sin él, Chrome no entrega los mensajes de la página y los widgets se quedan sin datos.
- Antes de entregar un APK: `gradle :app:testDebugUnitTest` (reglas y dibujo de los widgets) y revisar las imágenes que genera.
