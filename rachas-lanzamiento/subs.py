# subtitles_es.srt from the cues burned into the picture (main.js SUBS) plus the closing line (the end card shows it as the logo)
import json, os, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
cues = [
 {"t0": .4, "t1": 2.95, "text": "Todos empezamos el lunes."},
 {"t0": 3.0, "t1": 5.6, "text": "Y para el jueves, ya se nos olvidó."},
 {"t0": 6.5, "t1": 9.7, "text": "Rachas convierte cada día en una llama."},
 {"t0": 9.75, "t1": 12.95, "text": "Marca tu día, y mira crecer tu racha."},
 {"t0": 15.65, "t1": 18.45, "text": "Directo en tu pantalla de inicio."},
 {"t0": 18.45, "t1": 21.85, "text": "Y a las 22:30, te recuerda no romperla."},
 {"t0": 22.3, "t1": 24.95, "text": "Rachas. Un día a la vez."},
]
p = os.path.join(HERE, 'out', 'cues.json'); os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(cues, open(p, 'w'), ensure_ascii=False)
subprocess.run([os.path.join(LIB, '.venv/bin/python'), os.path.join(LIB, 'core/render/srt.py'), p, os.path.join(HERE, 'subtitles_es.srt')], check=True)
