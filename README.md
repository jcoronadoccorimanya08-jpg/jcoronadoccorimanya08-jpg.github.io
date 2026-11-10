# jcoronadoccorimanya08-jpg.github.io

Raíz del sitio donde viven **Val Q** (`/val-quirurgico/`) y **Bitácora** (`/Bitacora/`).

- `.well-known/assetlinks.json`: verificación (Digital Asset Links) de la app de Android «Rachas», para que abra las dos apps a pantalla completa y pueda recibir su resumen para los widgets.
- `android/`: código de «Rachas», una app de Android que abre Val Q y Bitácora con Chrome (Trusted Web Activity) y pone widgets 4×2 con la racha, el día de hoy y la semana (una montaña por día). Las apps web le mandan solo lo que muestra el widget (Bitácora, **solo números**); los datos siguen cifrados en Chrome.
  - El de Bitácora muestra, sin nombres, cada hábito por color y orden: % de días limpios, días seguidos y cómo va hoy.
  - Los widgets cambian de día solos a medianoche (la app web manda también mañana y pasado mañana) y desde las 21:00 muestran un borde si falta marcar.
  - A las 22:30 llega un recordatorio para anotar el día (si ya anotaste todo, no aparece). Sin internet: alarmas y notificaciones locales.

## Compilar el APK

Requiere el Android SDK (plataforma 35) y una llave de firma que **nunca** va en este repositorio:

```bash
cd android
echo "sdk.dir=/ruta/al/android-sdk" > local.properties
RACHAS_KEYSTORE=/ruta/rachas.jks RACHAS_KEYSTORE_PASSWORD=... gradle assembleRelease
```

Si cambia la llave, actualiza la huella SHA-256 en `.well-known/assetlinks.json`.

## Probar los widgets sin teléfono

`gradle :app:testDebugUnitTest` dibuja los widgets con datos de ejemplo (Robolectric) y guarda las imágenes en `app/build/previews` (o en `RACHAS_PREVIEWS`). Con `RACHAS_PREVIEW_RES=1` también actualiza la vista previa de la lista de widgets (`res/drawable-nodpi/preview_*.png`).

## Cómo llegan los datos al widget

1. La actividad de lanzamiento hace `warmup()`, abre una sesión y valida `use_as_origin` contra `assetlinks.json`.
2. Cuando la página terminó de cargar, pide el canal (`requestPostMessageChannel`). Chrome entra por `PostMessageService` (declarado en el manifiesto: sin él no llega nada).
3. La página recibe el puerto sin ventana de origen y con origen `android-app://<sitio>` (Chrome actual convierte `android-app://<sitio>/<paquete>` en un origen y el paquete se pierde), manda el resumen y la app responde `rachas:ok`. En Ajustes de cada app se ve «Widget conectado ✓».
4. Si algo falla, el widget vacío muestra el último paso (`Diag.kt`): «Sin abrir desde Rachas», «Conectando…», «Conectada, sin datos», etc. Si la app web se abre sin pasar por Rachas (por ejemplo, con el ícono que instaló Chrome), avisa en Hoy.

Los íconos de Rachas llevan una llamita para no confundirlos con los que instala Chrome (que se llaman igual pero no hablan con el widget).
