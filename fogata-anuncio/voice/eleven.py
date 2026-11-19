# ElevenLabs narration: voice/lines.json → voice/out/<id>.wav (+ dur.json). The API key is injected by the session's proxy.
# usage: python voice/eleven.py [voice_id]   (default: the first Spanish male voice in the account)
import json, os, sys, io, urllib.request, numpy as np, soundfile as sf
HERE = os.path.dirname(os.path.abspath(__file__)); API = 'https://api.elevenlabs.io'
def req(path, body=None):
    r = urllib.request.Request(API + path, data=json.dumps(body).encode() if body else None, headers={'Content-Type': 'application/json', 'xi-api-key': 'proxy-injected'})
    return urllib.request.urlopen(r, timeout=120).read()
vid = sys.argv[1] if len(sys.argv) > 1 else None
if not vid:
    vs = json.loads(req('/v2/voices?page_size=100'))['voices']
    pick = [v for v in vs if v.get('labels', {}).get('gender') == 'male'] or vs
    vid = pick[0]['voice_id']; print('voice', pick[0]['name'])
lines = json.load(open(os.path.join(HERE, 'lines.json'), encoding='utf-8')); out = os.path.join(HERE, 'out'); os.makedirs(out, exist_ok=True); dur = {}
for L in lines:
    pcm = req(f'/v1/text-to-speech/{vid}?output_format=pcm_44100', {'text': L['text'], 'model_id': 'eleven_multilingual_v2', 'language_code': 'es',
              'voice_settings': {'stability': .45, 'similarity_boost': .8, 'style': .35, 'use_speaker_boost': True, 'speed': 1.0}})
    a = np.frombuffer(pcm, np.int16).astype(np.float32) / 32768
    nz = np.where(np.abs(a) > np.abs(a).max() * .02)[0]; a = a[max(0, nz[0] - 1300): nz[-1] + 3500]
    sf.write(os.path.join(out, L['id'] + '.wav'), a, 44100); dur[L['id']] = round(len(a) / 44100, 3); print(L['id'], dur[L['id']], L['text'])
json.dump(dur, open(os.path.join(out, 'dur.json'), 'w'), indent=1)
