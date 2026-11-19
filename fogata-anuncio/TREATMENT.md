# Fogata — «Mantén vivo tu fuego»

Anuncio vertical 1080×1920 a 24 fps, estilo Dark Tech Keynote, para Reels, TikTok e Instagram/Facebook (Reels y Stories). Hay dos cortes: 25 s (`fogata-anuncio.mp4`) y 15 s para anuncios pagados (`fogata-anuncio-15s.mp4`).

**Nombre.** «Fogata» es el lanzador (antes «Rachas») y enciende dos apps: Val Q y Bitácora. Tus hábitos son la leña y la racha es el fuego. En la búsqueda no apareció ninguna app de hábitos con ese nombre; solo «La Fogata», una app de pedidos de comida.

**Gancho (0–2 s).** Desde el primer cuadro: una avalancha de recordatorios con insignias rojas y el golpe «EMPIEZO EL LUNES», tres veces. Luego cambia a «¿Y EL JUEVES?» en rojo. Se congela y queda en silencio.

**Giro.** Golpe y aparece la llama. Las tarjetas entran en el fuego y sale el nombre: «Esto es Fogata».

**Producto real.** La pantalla de inicio tiene el widget y los íconos de Val Q y Bitácora con su llamita. Se abre Val Q («Avance · Tu ciclo»: días ganados, promedio, semanas, «Tu montaña»). Luego desliza a Bitácora («Análisis · Cómo vas»: racha actual, mejor racha, días limpios). Las pantallas están redibujadas en código a partir de las capturas reales, con números de ejemplo, no tus datos.

**Clímax.** El «21» gigante, con «DÍAS SEGUIDOS».

**Recordatorio.** Llega la notificación de las 22:30. Al tocarla, el anillo pasa a 5/5 y la racha a 22.

**Cierre y llamado a la acción.** Logo Fogata, «Mantén vivo tu fuego.», botón «Gratis en Android», «Link en la bio» y los dos íconos.

**Sonido.** En el gancho, marimba dispersa y desafinada con golpes graves. Del giro en adelante, una sola familia afinada (vibráfono y glockenspiel), con bombo suave y shaker. Silencio digital en el congelado. Si existe `music/track.wav` (una pista con licencia), `mix.py` la usa en lugar de la partitura sintetizada.

**Voz.** El guion está en `voice/lines.json`. Por ahora es un borrador con Kokoro (`em_alex`); `voice/eleven.py` lo reemplaza por ElevenLabs cuando la llave funcione.
