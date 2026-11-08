# jcoronadoccorimanya08-jpg.github.io

Raíz del sitio donde viven **Val Q** (`/val-quirurgico/`) y **Bitácora** (`/Bitacora/`).

- `.well-known/assetlinks.json`: verificación (Digital Asset Links) de la app de Android «Rachas», para que abra las dos apps a pantalla completa y pueda recibir su resumen para los widgets.
- `android/`: código de «Rachas», una app de Android que abre Val Q y Bitácora con Chrome (Trusted Web Activity) y pone widgets con la racha, los últimos 7 días y una montaña con el porcentaje de cada día. Las apps web le mandan **solo números**; los datos siguen cifrados en Chrome.

## Compilar el APK

Requiere el Android SDK (plataforma 35) y una llave de firma que **nunca** va en este repositorio:

```bash
cd android
echo "sdk.dir=/ruta/al/android-sdk" > local.properties
RACHAS_KEYSTORE=/ruta/rachas.jks RACHAS_KEYSTORE_PASSWORD=... gradle assembleRelease
```

Si cambia la llave, actualiza la huella SHA-256 en `.well-known/assetlinks.json`.
