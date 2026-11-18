// Rachas — «Sube un día a la vez». 1080×1920, a pure function of t.
// Coordinates are authored in "final" pixels (the phone framing at zoom 1); strokes are built K× larger
// so they keep their brush detail in the close-ups.
const cv = document.getElementById('c'), ctx = cv.getContext('2d');
const K = 4;
window.DUR = 25.0;
const ACC = '#d2462a', OCHRE = '#e8a33c', FRAME = '#59606b';
const DAYS = [.6, .8, 1, .7, .9, 1, .6], LET = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
const MX = i => 320 + i * 75, BASE = 860, MH = p => 60 + p * 70;
const DAYT = [0.35, 5.0, 5.625, 6.25, 6.875, 7.5, 8.05];   // when each day's mountain starts painting
const DAYD = [2.4, .62, .62, .62, .62, .62, .5];

const M = (pts, w, col, o) => mk(pts.map(p => [p[0] * K, p[1] * K]), w * K, col, o);
const sc = p => p.map(q => [q[0] * K, q[1] * K]);

// ---------- camera ----------
const CK = [[0, 322, 812, 6.0], [4.7, 326, 810, 6.8], [5.0, 328, 810, 6.8], [8.1, 548, 800, 1.95], [8.4, 548, 800, 1.95],
  [9.7, 548, 796, 2.15], [10.2, 548, 796, 2.15], [13.4, 540, 860, 1.0], [17.2, 540, 858, 1.04], [19.0, 540, 575, 1.5],
  [20.4, 540, 600, 1.45], [21.7, 580, 890, 1.42], [25, 580, 885, 1.5]];
const camX = monotone(CK.map(k => [k[0], k[1]])), camY = monotone(CK.map(k => [k[0], k[2]])), camZ = monotone(CK.map(k => [k[0], Math.log(k[3])]));
let CX, CY, Z;
function cam(t) { CX = camX(t); CY = camY(t); Z = Math.exp(camZ(t)); }
const setW = () => ctx.setTransform(Z / K, 0, 0, Z / K, 540 - CX * Z, 960 - CY * Z);
const setF = () => ctx.setTransform(Z, 0, 0, Z, 540 - CX * Z, 960 - CY * Z);
const setS = () => ctx.setTransform(1, 0, 0, 1, 0, 0);
const toS = (x, y) => [(x - CX) * Z + 540, (y - CY) * Z + 960];

// ---------- shapes ----------
function rr(x0, y0, x1, y1, r, n = 6) {
  const out = [], arc = (cx, cy, a0) => { for (let i = 0; i <= n; i++) { const a = a0 + i / n * Math.PI / 2; out.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]); } };
  arc(x1 - r, y0 + r, -Math.PI / 2); arc(x1 - r, y1 - r, 0); arc(x0 + r, y1 - r, Math.PI / 2); arc(x0 + r, y0 + r, Math.PI);
  return out;
}
function jitterPoly(pts, j) { return pts.map(p => [p[0] + rnd(-j, j), p[1] + rnd(-j, j)]); }
function makeWash(pts, col, a, passes = 3, j = 2.2) { const P = []; for (let i = 0; i < passes; i++) P.push(jitterPoly(pts, j)); return { P, col, a }; }
function drawWash(w, am = 1) {
  ctx.fillStyle = rgba(w.col, w.a / w.P.length * 1.6 * am);
  for (const P of w.P) { ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath(); ctx.fill(); }
}
function polyPath(P) { ctx.beginPath(); P.forEach((p, i) => i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])); ctx.closePath(); }

// a mountain: fill strokes bottom → top, shadow on the right slope, an ink ridge last
function mountain(cx, by, h, hw, col, shade, o = {}) {
  const S = [], px = cx + rnd(-hw * .15, hw * .15), py = by - h, nf = o.nf ?? 7, lean = rnd(-.12, .12);
  const half = f => hw * Math.pow(1 - f, .8 + lean), mid = f => lerp(cx, px, Math.pow(f, .7));
  for (let k = 0; k < nf; k++) {
    const f = k / nf, y = by - h * f * .95 - h * .04, hh = half(f) + 3, mx = mid(f);
    S.push(M(qcurve(mx - hh, y + rnd(-2, 2), mx + hh, y + rnd(-2, 2), rnd(-5, 3), 12), h / nf * 1.95, col, { prof: 'even', a: rnd(.5, .64), rough: .35 }));
  }
  S.push(M(qctrl(px + 2, py + 6, px + hw * .45, by - h * .45, cx + hw * .9, by - 2, 14), hw * .5, shade, { prof: 'tip', a: .42, rough: .35 }));
  S.push(M(qctrl(px, py + 2, px - hw * .2, py + h * .25, px - hw * .52, py + h * .5, 10), 1.6, INK2, { prof: 'tip', nb: 0, a: .7, rough: .4 }));
  S.push(M(qctrl(px, py + 2, px + hw * .25, py + h * .2, px + hw * .6, py + h * .42, 10), 1.3, INK2, { prof: 'tip', nb: 0, a: .55, rough: .4 }));
  return { S, px, py };
}
function flame(x, y, s) {
  return [M(qctrl(x, y + s * .55, x - s * .62, y + s * .05, x + s * .05, y - s * .75, 8), s * .62, ACC, { prof: 'leaf', nb: 3, a: .9 }),
    M(qctrl(x + s * .1, y + s * .55, x + s * .55, y, x + s * .02, y - s * .5, 8), s * .45, ACC, { prof: 'leaf', nb: 0, a: .8 }),
    M(qcurve(x, y + s * .5, x + s * .02, y + s * .02, -s * .08, 6), s * .32, OCHRE, { prof: 'leaf', nb: 0, a: .85 })];
}
function circle(cx, cy, r, a0, a1, n = 40) { const o = []; for (let i = 0; i <= n; i++) { const a = a0 + (a1 - a0) * i / n; o.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]); } return o; }

// ---------- text ----------
let TX = [];
function hand(txt, x, y, p, o = {}) {
  if (p <= 0) return;
  const size = o.size || 34, align = o.align || 'left';
  ctx.font = `${o.weight || 600} ${size}px ${o.font || '"Caveat"'}`; ctx.textBaseline = 'alphabetic';
  const w = ctx.measureText(txt).width, x0 = align === 'center' ? x - w / 2 : align === 'right' ? x - w : x;
  ctx.save();
  if (p < 1) { ctx.beginPath(); ctx.rect(x0 - 10, y - size * 1.2, (w + 20) * ss(p), size * 1.8); ctx.clip(); }
  ctx.textAlign = 'left'; ctx.globalAlpha = o.alpha ?? 1;
  if (!o.noHalo) { ctx.lineJoin = 'round'; ctx.lineWidth = size * .2; ctx.strokeStyle = rgba(PAPER, .8); ctx.strokeText(txt, x0, y); }
  ctx.fillStyle = rgba(o.col || INK, o.ink ?? .88); ctx.fillText(txt, x0, y);
  if (o.twice !== false && !o.font) { ctx.fillStyle = rgba(o.col || INK, .35); ctx.fillText(txt, x0 + .5, y + .3); }
  ctx.restore();
  const [sx0, sy0] = toS(x0, y - size * .85), [sx1, sy1] = toS(x0 + w, y + size * .25);
  if (o.id && (o.alpha ?? 1) > .05) TX.push({ id: o.id, text: txt, x0: sx0, y0: sy0, x1: sx1, y1: sy1, pass: o.pass });
}

// ---------- build ----------
let PAPER_IMG, MT = [], FARW, FARI, PEAKF, PH = {}, WALL = [], NIGHT, CARD, ICONS = [], RING, RINGT, WFL, NOTE_CARD, NFL, DOCKF, SUN, MT8, LEAD = {};
function build() {
  const cols = [['#a9bf9a', '#6f8f74'], ['#86a780', '#557a62'], ['#5d8a6c', '#3f6352']];
  DAYS.forEach((p, i) => withSeed(100 + i, () => { const c = cols[p >= 1 ? 2 : p >= .8 ? 1 : 0]; MT.push(mountain(MX(i), BASE, MH(p), 47, c[0], c[1])); }));
  MT8 = withSeed(200, () => mountain(MX(7), BASE, MH(.75), 47, '#86a780', '#557a62'));
  withSeed(300, () => {
    const xs = [286, 340, 392, 445, 505, 552, 600, 650, 700, 750, 800], ys = [818, 770, 748, 772, 724, 704, 676, 712, 738, 772, 812];
    const ridge = []; for (let i = 0; i < xs.length - 1; i++) ridge.push(...qcurve(xs[i], ys[i], xs[i + 1], ys[i + 1], rnd(-5, 5), 5).slice(0, -1)); ridge.push([800, 812]);
    FARW = [makeWash(sc(ridge.concat([[800, 862], [286, 862]])), '#a3b3c6', .55, 3, 6),
      makeWash(sc(ridge.map(p => [p[0], p[1] + 34]).concat([[800, 862], [286, 862]])), '#90a3b8', .35, 3, 6)];
    FARI = M(ridge, 1.4, '#4f5f75', { prof: 'even', nb: 0, a: .5, rough: .45 });
  });
  PEAKF = withSeed(310, () => flame(600, 660, 16));
  // phone body: a band of slate wash, ink edges
  withSeed(400, () => {
    const band = rr(256, 261, 824, 1459, 59, 8), sides = [];
    const q = band.length / 4;
    for (let s = 0; s < 4; s++) { const pts = band.slice(s * q, (s + 1) * q + 1).concat(s === 3 ? [band[0]] : []); sides.push(M(pts, 22, FRAME, { prof: 'even', nb: 7, a: .85, rough: .18 })); }
    PH.band = sides;
    PH.ink = [M(rr(245, 250, 835, 1470, 70, 8).concat([[245 + 70, 250]]), 1.8, INK2, { prof: 'even', nb: 0, a: .8, rough: .4 }),
      M(rr(267, 272, 813, 1448, 48, 8).concat([[267 + 48, 272]]), 1.4, INK2, { prof: 'even', nb: 0, a: .6, rough: .4 })];
    PH.cam = M(circle(540, 294, 5, 0, Math.PI * 2, 12), 4, INK2, { prof: 'even', nb: 0, a: .85 });
    PH.screen = rr(267, 272, 813, 1448, 48, 8);
    const wc = ['#d3e2e0', '#cbdcd3', '#d9dbc0', '#e7d4b2', '#e4c6a3'];
    wc.forEach((c, i) => { const y = 272 + i * 235 - 30, top = []; for (let x = 240; x <= 840; x += 30) top.push([x, y + 14 * Math.sin(x * .012 + i * 1.7) + rnd(-4, 4)]); WALL.push(makeWash(top.concat([[840, 1480], [240, 1480]]), c, .8)); });
    NIGHT = { col: '#33456b' };
    CARD = makeWash(rr(276, 560, 804, 908, 34, 8), PAPER, .95, 3, 1.6);
    const ic = ['#c9a27a', '#9fb4c4', '#b7a3c4', '#a9bf9a', '#d7b37c', '#8fa7b5', '#c49a8f', '#9cb39a'];
    for (let r = 0; r < 2; r++) for (let c = 0; c < 4; c++) ICONS.push(makeWash(rr(296 + c * 128, 1000 + r * 140, 380 + c * 128, 1084 + r * 140, 24, 4), ic[r * 4 + c], .75, 3, 1.6));
    for (let c = 0; c < 4; c++) ICONS.push(makeWash(rr(296 + c * 128, 1320, 380 + c * 128, 1404, 24, 4), c ? ['#b7a3c4', '#9fb4c4', '#c9a27a'][c - 1] : PAPER, .85, 3, 1.6));
    DOCKF = flame(338, 1364, 18);
    RING = M(circle(735, 628, 40, -Math.PI / 2, Math.PI * 1.5, 40), 9, '#d9d2c2', { prof: 'even', nb: 3, a: .8 });
    RINGT = M(circle(735, 628, 40, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * .6, 30), 10, '#5d8a6c', { prof: 'even', nb: 4, a: .9 });
    WFL = flame(330, 628, 22);
    NOTE_CARD = makeWash(rr(287, 330, 793, 470, 28, 8), PAPER, .95, 3, 1.5);
    NFL = flame(326, 382, 14);
    SUN = makeWash(circle(420, 0, 26, 0, Math.PI * 2, 24), '#e9b45c', .75, 3, 1.4);
  });
  // pen leader lines for the notes
  withSeed(500, () => {
    LEAD.d1 = M(qcurve(337, 760, 352, 742, -3, 8), .35, INK2, { prof: 'even', nb: 0, a: .8, rough: .3 });
    LEAD.racha = M(qcurve(190, 560, 300, 612, -12, 10), 1.3, INK2, { prof: 'even', nb: 0, a: .8, rough: .3 });
    LEAD.hoy = M(qcurve(895, 540, 780, 610, 14, 10), 1.3, INK2, { prof: 'even', nb: 0, a: .8, rough: .3 });
    LEAD.sem = M(qcurve(150, 985, 300, 860, 16, 10), 1.3, INK2, { prof: 'even', nb: 0, a: .8, rough: .3 });
    LEAD.man = M(qcurve(858, 708, 848, 772, 8, 8), 1.1, INK2, { prof: 'even', nb: 0, a: .8, rough: .3 });
  });
}

// ---------- subtitles ----------
const SUBS = [
  { t0: .6, t1: 3.3, text: ['Un día no cambia nada.'] },
  { t0: 4.9, t1: 7.6, text: ['Pero un día, y otro, y otro más…'] },
  { t0: 7.7, t1: 9.9, text: ['…se vuelven montaña.'] },
  { t0: 10.5, t1: 14.8, text: ['Rachas pinta cada día que cumples,', 'en tu pantalla de inicio.'] },
  { t0: 17.5, t1: 20.9, text: ['Y a las 22:30, te recuerda', 'anotar tu día.'] },
];
window.SUBS = SUBS;
function subs(t) {
  setS();
  for (const s of SUBS) {
    if (t < s.t0 || t > s.t1) continue;
    const a = ss(seg(t, s.t0, s.t0 + .2)) * (1 - ss(seg(t, s.t1 - .2, s.t1)));
    ctx.font = 'italic 500 54px "Cormorant Garamond"'; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic'; ctx.lineJoin = 'round';
    s.text.forEach((ln, i) => {
      const y = 1640 + (i - (s.text.length - 1)) * 64;
      ctx.lineWidth = 16; ctx.strokeStyle = rgba(PAPER, .93 * a); ctx.strokeText(ln, 540, y);
      ctx.fillStyle = rgba(INK, .86 * a); ctx.fillText(ln, 540, y);
    });
  }
}

// ---------- the frame ----------
function mistY(x, t) { const p = eio(seg(t, 20.55, 21.75)); return lerp(1560, 180, p) + 26 * Math.sin(x * .011 + t * 1.6) + 12 * Math.sin(x * .031 + 2); }
function phone(t) {
  if (t < 10.15) return;
  const gone = t > 21.8; if (gone) return;
  ctx.save();
  if (t > 20.55) { setF(); ctx.beginPath(); ctx.moveTo(-400, -600); ctx.lineTo(1480, -600); for (let x = 1480; x >= -400; x -= 20) ctx.lineTo(x, mistY(x, t)); ctx.closePath(); ctx.clip(); }
  setF();
  // wallpaper blooms out from the widget
  const bloom = eo(seg(t, 10.25, 11.7));
  ctx.save(); polyPath(PH.screen); ctx.clip();
  if (bloom < 1) { ctx.beginPath(); for (let i = 0; i <= 60; i++) { const a = i / 60 * Math.PI * 2, r = bloom * 1050 * (1 + .07 * Math.sin(a * 5 + 1) + .04 * Math.sin(a * 11)); ctx.lineTo(540 + Math.cos(a) * r, 730 + Math.sin(a) * r); } ctx.closePath(); ctx.clip(); }
  for (const w of WALL) drawWash(w);
  const nightP = eio(seg(t, 17.1, 18.1));
  if (nightP > 0) { ctx.fillStyle = rgba(NIGHT.col, .5 * nightP); ctx.beginPath(); ctx.moveTo(240, 260); ctx.lineTo(840, 260); for (let x = 840; x >= 240; x -= 20) ctx.lineTo(x, lerp(260, 1480, nightP) + 18 * Math.sin(x * .02)); ctx.closePath(); ctx.fill(); }
  ctx.restore();
  // widget card, icons
  drawWash(CARD, ss(seg(t, 10.9, 11.7)));
  ICONS.forEach((w, i) => drawWash(w, ss(seg(t, 12.7 + i * .06, 13.1 + i * .06))));
  // moon
  if (nightP > 0) { ctx.fillStyle = rgba(PAPER, .9 * ss(seg(t, 17.7, 18.3))); ctx.beginPath(); ctx.arc(728, 515, 17, 0, Math.PI * 2); ctx.fill(); ctx.fillStyle = rgba('#33456b', .5 * ss(seg(t, 17.7, 18.3))); ctx.beginPath(); ctx.arc(736, 509, 14, 0, Math.PI * 2); ctx.fill(); }
  setW();
  drawList(ctx, PH.band, ss(seg(t, 10.35, 12.3)));
  drawList(ctx, PH.ink, seg(t, 11.6, 12.9)); drawS(ctx, PH.cam, seg(t, 12.6, 12.9));
  drawList(ctx, DOCKF, seg(t, 13.0, 13.5));
  drawList(ctx, WFL, seg(t, 11.8, 12.3));
  drawS(ctx, RING, seg(t, 12.2, 12.7)); drawS(ctx, RINGT, eo(seg(t, 12.5, 13.3)));
  setF();
  hand('21 días', 362, 641, seg(t, 12.0, 12.7), { font: '"IBM Plex Mono"', weight: 500, size: 32, noHalo: true, id: 'w21' });
  hand('3/5', 735, 637, seg(t, 12.9, 13.2), { font: '"IBM Plex Mono"', weight: 500, size: 22, align: 'center', noHalo: true, id: 'w35' });
  // notification
  const nP = ss(seg(t, 18.15, 18.6));
  if (nP > 0) {
    drawWash(NOTE_CARD, nP); setW(); drawList(ctx, NFL, seg(t, 18.3, 18.7)); setF();
    hand('RACHAS · 22:30', 352, 378, seg(t, 18.35, 18.7), { font: '"IBM Plex Mono"', weight: 500, size: 19, noHalo: true, col: '#6a5a4c', id: 'n1' });
    hand('Hora de anotar tu día', 306, 440, seg(t, 18.45, 19.0), { font: '"Cormorant Garamond"', weight: 600, size: 40, noHalo: true, id: 'n2' });
  }
  ctx.restore();
  if (t > 20.55) { // mist band along the erasing edge
    setF(); for (let k = 0; k < 3; k++) { ctx.fillStyle = rgba(PAPER, .32); ctx.beginPath(); for (let x = -400; x <= 1480; x += 20) ctx.lineTo(x, mistY(x, t) - 30 + k * 26); for (let x = 1480; x >= -400; x -= 20) ctx.lineTo(x, mistY(x, t) + 70 + k * 30); ctx.closePath(); ctx.fill(); }
  }
}

function render(t) {
  TX = [];
  setS(); ctx.globalAlpha = 1; ctx.drawImage(PAPER_IMG, 0, 0);
  cam(t);
  phone(t);
  // sunrise at the end, behind the ridge
  const sunP = ss(seg(t, 21.9, 22.9));
  if (sunP > 0) { setF(); ctx.save(); ctx.translate(0, lerp(735, 690, eo(seg(t, 21.9, 23.6)))); drawWash(SUN, sunP); ctx.restore(); }
  setW();
  const fp = eo(seg(t, 8.45, 9.25));
  if (fp > 0) { ctx.save(); if (fp < 1) { ctx.beginPath(); for (let y = 2600; y <= 3500; y += 40) ctx.lineTo(K * lerp(280, 810, fp) + 40 * Math.sin(y * .01), y); ctx.lineTo(0, 3500); ctx.lineTo(0, 2600); ctx.closePath(); ctx.clip(); } FARW.forEach(w => drawWash(w)); ctx.restore(); drawS(ctx, FARI, fp); }
  drawList(ctx, PEAKF, seg(t, 9.05, 9.4));
  MT.forEach((m, i) => drawList(ctx, m.S, (i === 6 ? .999 : 1) * seg(t, DAYT[i], DAYT[i] + DAYD[i])));
  drawList(ctx, MT8.S, .42 * seg(t, 22.5, 23.4));     // tomorrow: the brush lifts mid-way
  setF();
  LET.forEach((l, i) => { const p = seg(t, DAYT[i] + .3, DAYT[i] + .5); if (p > 0) hand(l, MX(i), 891, p, { font: '"IBM Plex Mono"', weight: 400, size: 16, align: 'center', noHalo: true, col: '#5b4d40', id: 'd' + i, pass: true, alpha: t > 21.6 ? 1 - ss(seg(t, 21.0, 21.6)) : 1 }); });
  // notes
  if (t < 5.6) { const a = 1 - ss(seg(t, 4.9, 5.4)); setW(); drawS(ctx, LEAD.d1, seg(t, 2.3, 2.6), null, a); setF(); hand('día 1', 353, 741, seg(t, 2.5, 3.1), { size: 10, alpha: a, id: 'dia1' }); }
  const na = 1 - ss(seg(t, 16.9, 17.4));
  if (t > 13.0 && na > 0) {
    setW(); drawS(ctx, LEAD.racha, seg(t, 13.0, 13.35), null, na); drawS(ctx, LEAD.hoy, seg(t, 13.6, 13.95), null, na); drawS(ctx, LEAD.sem, seg(t, 14.2, 14.55), null, na); setF();
    hand('tu racha', 200, 548, seg(t, 13.2, 13.8), { size: 50, align: 'right', alpha: na, id: 'racha' });
    hand('hoy', 880, 528, seg(t, 13.8, 14.2), { size: 50, align: 'left', alpha: na, id: 'hoy' });
    hand('tu semana', 140, 1032, seg(t, 14.4, 15.0), { size: 50, align: 'center', alpha: na, id: 'sem' });
  }
  if (t > 22.9) { setW(); drawS(ctx, LEAD.man, seg(t, 22.9, 23.2)); setF(); hand('mañana', 858, 700, seg(t, 23.0, 23.5), { size: 26, align: 'center', id: 'man' }); }
  // title
  if (t > 20.95) {
    setF();
    hand('Rachas', 580, 1050, seg(t, 20.95, 21.7), { font: '"Cormorant Garamond"', weight: 600, size: 150, align: 'center', noHalo: true, id: 'title' });
    hand('Sube un día a la vez.', 580, 1128, seg(t, 21.75, 22.6), { size: 50, align: 'center', id: 'tag' });
    hand('PARA ANDROID', 580, 1172, seg(t, 22.3, 22.7), { font: '"IBM Plex Mono"', weight: 400, size: 17, align: 'center', noHalo: true, col: '#6a5a4c', id: 'and' });
  }
  subs(t);
}
window.render = render;
window.TEXTS = t => { render(t); return TX; };

// sound events (exported by events.mjs → mix.py)
const EV = [];
DAYT.forEach((t0, i) => EV.push({ t: t0, type: 'brush', d: DAYD[i] + .1, g: i ? .55 : .7, pan: (MX(i) - 545) / 400 }));
EV.push({ t: 2.3, type: 'pencil', d: .8 }, { t: 8.45, type: 'brush', d: .9, g: .8, pan: 0 }, { t: 8.45, type: 'swell', d: .8 }, { t: 9.05, type: 'flame', d: .4 },
  { t: 10.25, type: 'bloom', d: 1.5 }, { t: 10.35, type: 'brush', d: 2.0, g: .5, pan: -.5 }, { t: 11.2, type: 'brush', d: 1.1, g: .4, pan: .5 }, { t: 11.8, type: 'flame', d: .4 },
  { t: 12.2, type: 'brush', d: 1.0, g: .35, pan: .5 }, { t: 12.7, type: 'dabs', d: 1.2 },
  { t: 13.0, type: 'pencil', d: .8 }, { t: 13.6, type: 'pencil', d: .6 }, { t: 14.2, type: 'pencil', d: .8 },
  { t: 17.0, type: 'crickets', d: 4.3 }, { t: 17.1, type: 'brush', d: 1.0, g: .35, pan: 0 }, { t: 18.2, type: 'ding', d: 1 },
  { t: 20.5, type: 'mist', d: 1.4 }, { t: 20.95, type: 'brush', d: .8, g: .45, pan: 0 }, { t: 21.75, type: 'pencil', d: .9 },
  { t: 22.5, type: 'brush', d: .9, g: .5, pan: .6 }, { t: 22.9, type: 'pencil', d: .6 });
window.EV = EV;

(async () => {
  PAPER_IMG = new Image(); PAPER_IMG.src = 'paper.jpg'; await PAPER_IMG.decode();
  await Promise.all(['600 40px "Caveat"', '600 40px "Cormorant Garamond"', 'italic 500 40px "Cormorant Garamond"', '500 30px "IBM Plex Mono"', '400 30px "IBM Plex Mono"'].map(f => document.fonts.load(f, 'Ráchas día ñ·')));
  build();
  window.READY = true;
})();
