import { renderFilm, texts, buildEvents, DUR, CAPS } from './film.js';
const g = document.getElementById('c').getContext('2d');
await Promise.all(['400 40px "Inter"', '500 40px "Inter"', '600 40px "Inter"', '700 40px "Inter"', '700 40px "Inter Tight"', '600 40px "Inter Tight"', '500 40px "JetBrains Mono"', '600 40px "JetBrains Mono"'].map(f => document.fonts.load(f, 'Rachas día ñ 0123')));
window.DUR = DUR; window.EV = buildEvents(); window.SUBS = CAPS;
window.render = t => renderFilm(g, t);
window.TEXTS = t => { renderFilm(g, t); return texts(t); };
window.READY = true;
