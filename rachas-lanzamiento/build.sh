#!/bin/sh
# Rachas — «Un día a la vez» (Dark Tech Keynote): one command from source to film.
set -e
LIB=/root/lemo-opuscar
HERE=$(cd "$(dirname "$0")" && pwd)
export LIB
[ -f "$LIB/core/render/video.mjs" ] || { echo "set LIB to the library folder"; exit 1; }
PATH=/root/ff/bin:$PATH   # ffmpeg 7+ (mux.sh needs it)
cd "$LIB"
"$LIB/.venv/bin/python" core/tts/tts.py "$HERE/voice/lines.json" "$HERE/voice/out"
node core/render/events.mjs "$HERE" --size 1080x1920
node core/render/video.mjs "$HERE" --fps 24 --workers 4 --size 1080x1920 --out "$HERE/out/video.mp4"
"$LIB/.venv/bin/python" "$HERE/mix.py"
"$LIB/.venv/bin/python" "$HERE/subs.py"
sh core/render/mux.sh "$HERE/out/video.mp4" "$HERE/out/mix.wav" "$HERE/rachas-lanzamiento.mp4" 24 0
ffmpeg -y -loglevel error -ss 24.5 -i "$HERE/rachas-lanzamiento.mp4" -frames:v 1 -q:v 2 "$HERE/poster.jpg"
