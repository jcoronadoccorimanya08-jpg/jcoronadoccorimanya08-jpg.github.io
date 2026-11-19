#!/bin/sh
# Fogata — «Mantén vivo tu fuego»: one command from source to the 25 s master and the 15 s cut.
# Voice: if ElevenLabs is reachable, run `python3 voice/eleven.py` once first (it replaces the Kokoro draft in voice/out/).
set -e
LIB=/root/lemo-opuscar
HERE=$(cd "$(dirname "$0")" && pwd)
export LIB
PATH=/root/ff/bin:$PATH
cd "$LIB"
[ -f "$HERE/voice/out/l1.wav" ] || "$LIB/.venv/bin/python" core/tts/tts.py "$HERE/voice/lines.json" "$HERE/voice/out"
node core/render/events.mjs "$HERE" --size 1080x1920
node core/render/video.mjs "$HERE" --fps 24 --workers 4 --size 1080x1920 --out "$HERE/out/video.mp4"
"$LIB/.venv/bin/python" "$HERE/mix.py"
"$LIB/.venv/bin/python" "$HERE/subs.py"
sh core/render/mux.sh "$HERE/out/video.mp4" "$HERE/out/mix.wav" "$HERE/fogata-anuncio.mp4" 24 0
ffmpeg -y -loglevel error -ss 24.6 -i "$HERE/fogata-anuncio.mp4" -frames:v 1 -q:v 2 "$HERE/poster.jpg"
sh "$HERE/cut15.sh"
