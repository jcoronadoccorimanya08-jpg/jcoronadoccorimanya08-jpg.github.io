# subtitles_es.srt from the cues burned into the picture (main.js SUBS) plus the closing line (the end card shows it as the logo)
import json, os, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
cues = [
 {"t0": .05, "t1": 2.95, "text": "¿Cuántas veces dijiste: «empiezo el lunes»?"},
 {"t0": 2.95, "t1": 5.85, "text": "¿Y cuántas veces llegó el jueves, y nada?"},
 {"t0": 5.95, "t1": 7.7, "text": "Esto es Fogata."},
 {"t0": 7.7, "t1": 11.3, "text": "Tus hábitos viven en dos apps: Val Q y Bitácora."},
 {"t0": 11.3, "t1": 15.3, "text": "Cada día cumplido sube tu montaña, y alimenta tu racha."},
 {"t0": 17.6, "t1": 21.2, "text": "Y a las 22:30, te recuerda no apagar tu fuego."},
 {"t0": 21.45, "t1": 23.55, "text": "Fogata. Mantén vivo tu fuego."},
 {"t0": 23.55, "t1": 25.3, "text": "Gratis en Android."},
]
p = os.path.join(HERE, 'out', 'cues.json'); os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(cues, open(p, 'w'), ensure_ascii=False)
subprocess.run([os.path.join(LIB, '.venv/bin/python'), os.path.join(LIB, 'core/render/srt.py'), p, os.path.join(HERE, 'subtitles_es.srt')], check=True)
