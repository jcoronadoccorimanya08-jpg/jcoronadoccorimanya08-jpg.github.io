#!/bin/sh
# 15-second cut for paid ads: hook + flame + the apps + logo/CTA, cut from the 25-second master.
set -e
HERE=$(cd "$(dirname "$0")" && pwd); PATH=/root/ff/bin:$PATH
IN="$HERE/fogata-anuncio.mp4"; OUT="$HERE/fogata-anuncio-15s.mp4"
# segments (s): hook 0–5.4 · flame 5.9–7.35 · apps 11.25–15.3 · logo and CTA 21.25–25.4
ffmpeg -y -loglevel error -i "$IN" -filter_complex "\
[0:v]trim=0:5.4,setpts=PTS-STARTPTS[v0];[0:a]atrim=0:5.4,asetpts=PTS-STARTPTS,afade=t=out:st=5.3:d=0.1[a0];\
[0:v]trim=5.9:7.35,setpts=PTS-STARTPTS[v1];[0:a]atrim=5.9:7.35,asetpts=PTS-STARTPTS,afade=t=out:st=1.37:d=0.08[a1];\
[0:v]trim=11.25:15.3,setpts=PTS-STARTPTS[v2];[0:a]atrim=11.25:15.3,asetpts=PTS-STARTPTS,afade=t=in:d=0.06,afade=t=out:st=3.97:d=0.08[a2];\
[0:v]trim=21.25:25.4,setpts=PTS-STARTPTS[v3];[0:a]atrim=21.25:25.4,asetpts=PTS-STARTPTS,afade=t=in:d=0.06[a3];\
[v0][a0][v1][a1][v2][a2][v3][a3]concat=n=4:v=1:a=1[v][a]" -map "[v]" -map "[a]" -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -r 24 -c:a aac -b:a 256k -movflags +faststart "$OUT"
echo "$OUT"
