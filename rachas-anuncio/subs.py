# subtitles_es.srt from the cues burned into the picture (main.js SUBS) plus the closing line
import json, os, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
cues = [
 {"t0": .6, "t1": 3.3, "text": "Un día no cambia nada."},
 {"t0": 4.9, "t1": 7.6, "text": "Pero un día, y otro, y otro más…"},
 {"t0": 7.7, "t1": 9.9, "text": "…se vuelven montaña."},
 {"t0": 10.5, "t1": 14.8, "text": "Rachas pinta cada día que cumples,\nen tu pantalla de inicio."},
 {"t0": 17.5, "t1": 20.9, "text": "Y a las 22:30, te recuerda\nanotar tu día."},
 {"t0": 21.5, "t1": 24.9, "text": "Rachas. Sube un día a la vez."},
]
p = os.path.join(HERE, 'out', 'cues.json'); os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(cues, open(p, 'w'), ensure_ascii=False)
subprocess.run([os.path.join(LIB, '.venv/bin/python'), os.path.join(LIB, 'core/render/srt.py'), p, os.path.join(HERE, 'subtitles_es.srt')], check=True)
