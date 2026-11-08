# Sitio raíz y app «Rachas» — guía para Claude

- Este repositorio es **público**. Nunca pongas datos personales, la llave de firma (`*.jks`) ni contraseñas.
- `.well-known/assetlinks.json` debe tener el paquete `io.github.jcoronadoccorimanya08.rachas` y la huella SHA-256 de la llave con la que se firma el APK. Sin eso, la app no abre a pantalla completa ni recibe datos para los widgets.
- `.nojekyll` tiene que existir: sin él, GitHub Pages no publica `.well-known/`.
- La app Android solo guarda lo que le mandan las apps web por postMessage (solo números, ver `src/lib/widget.ts` en cada app). No agregues permisos de internet propios ni analítica.
