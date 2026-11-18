// downloads the OFL fonts into fonts/ and writes fonts.css (run once: node fetch_fonts.mjs)
import fs from 'fs';
const UA = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36';
const get = async (u, o) => { const r = await fetch(u, o); if (!r.ok) throw new Error(`${u}: HTTP ${r.status}`); return r; };
const fams = ['family=Inter:wght@400;500;600;700', 'family=Inter+Tight:wght@600;700', 'family=JetBrains+Mono:wght@400;500;600'];
let out = '', n = 0; fs.mkdirSync('fonts', { recursive: true });
for (const f of fams) {
  let css = await (await get(`https://fonts.googleapis.com/css2?${f}&display=block`, { headers: { 'User-Agent': UA } })).text();
  css = css.replace(/\/\* (cyrillic|greek|vietnamese|cyrillic-ext|greek-ext) \*\/\n@font-face \{[^}]*\}\n/g, '');
  for (const u of [...new Set(css.match(/https:\/\/[^)]+/g) || [])]) { const name = `f${n++}.woff2`; fs.writeFileSync('fonts/' + name, Buffer.from(await (await get(u)).arrayBuffer())); css = css.split(u).join('fonts/' + name); }
  out += css;
}
fs.writeFileSync('fonts.css', out); console.log('font files', n);
