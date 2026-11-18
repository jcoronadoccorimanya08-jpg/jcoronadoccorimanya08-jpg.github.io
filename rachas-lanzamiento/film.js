// Rachas — «Un día a la vez», Dark Tech Keynote, 1080×1920 @ 24 fps. Pure function of t.
import * as E from './engine.js';
import { monotone } from '/core/lib.js';
const { W, H, PAL, font, rgba, rr, clamp, lerp, seg, ss, eo, ei, eio, back, spring, mulberry, hash, TAU } = E;
E.setAccent('#FF7A1A');
export const DUR = 25.0;
const BEAT = .5;

// ───────── timing (seconds) ─────────
export const VO = { l1: .5, l2: 3.0, l3: 6.6, l4: 9.8, l5: 15.75, l6: 18.5, l7: 22.35 };
export const CAPS = [
  { t0: .4, t1: 2.95, text: 'Todos empezamos el lunes.' },
  { t0: 3.0, t1: 5.6, text: 'Y para el jueves, ya se nos olvidó.' },
  { t0: 6.5, t1: 9.7, text: 'Rachas convierte cada día en una llama.' },
  { t0: 9.75, t1: 12.95, text: 'Marca tu día, y mira crecer tu racha.' },
  { t0: 15.65, t1: 18.45, text: 'Directo en tu pantalla de inicio.' },
  { t0: 18.45, t1: 21.85, text: 'Y a las 22:30, te recuerda no romperla.' },
];
const T = { freeze: 5.6, press: 6.4, snap: 8.0, light0: 9.9, dive: 12.85, orange: 13.2, collapse: 15.6, notif: 18.35, tap: 20.3, off: 21.9, logo: 22.2 };

// ───────── shapes ─────────
function flamePath(cx, cy, s) {           // a teardrop flame, s = height
  const p = new Path2D(), w = s * .36;
  p.moveTo(cx, cy - s * .5);
  p.bezierCurveTo(cx + w * .25, cy - s * .22, cx + w * 1.05, cy - s * .05, cx + w * .95, cy + s * .18);
  p.bezierCurveTo(cx + w * .9, cy + s * .42, cx + w * .45, cy + s * .5, cx, cy + s * .5);
  p.bezierCurveTo(cx - w * .45, cy + s * .5, cx - w * .9, cy + s * .42, cx - w * .95, cy + s * .18);
  p.bezierCurveTo(cx - w * 1.0, cy - s * .02, cx - w * .55, cy - s * .12, cx - w * .2, cy - s * .3);
  p.bezierCurveTo(cx - w * .1, cy - s * .38, cx - w * .05, cy - s * .44, cx, cy - s * .5);
  p.closePath(); return p;
}
function flame(g, cx, cy, s, o = {}) {
  const { alpha = 1, glowR = s * .35, inner = true } = o;
  E.litShape(g, flamePath(cx, cy, s), { accent: true, glowR, alpha, bbox: [cx - s * .4, cy - s * .5, s * .8, s] });
  if (inner) { g.save(); g.globalAlpha *= alpha * .9; g.fillStyle = '#FFC07A'; g.fill(flamePath(cx, cy + s * .2, s * .48)); g.restore(); }
}
function mark(g, cx, cy, s, a = 1) {      // app mark: dark squircle + flame
  g.save(); g.globalAlpha *= a; E.panel(g, cx - s / 2, cy - s / 2, s, s, { r: s * .28, fill: PAL.surf3, shadow: .8 }); flame(g, cx, cy + s * .02, s * .58); g.restore();
}
let TX = [];
function text(g, str, x, y, px, o = {}) {
  const { wt = 500, fam = 'ui', color = PAL.text, align = 'left', a = 1, track = 0, id = null, pass = false } = o;
  if (a <= 0) return;
  g.save(); g.globalAlpha *= a; g.font = font(px, wt, fam); g.fillStyle = color; g.textAlign = align; g.textBaseline = 'alphabetic';
  if (g.letterSpacing !== undefined) g.letterSpacing = `${track * px}px`;
  g.fillText(str, x, y); const w = g.measureText(str).width;
  if (id && a > .5) { const m = g.getTransform(); const x0 = align === 'center' ? x - w / 2 : align === 'right' ? x - w : x; const p0 = m.transformPoint(new DOMPoint(x0, y - px * .8)), p1 = m.transformPoint(new DOMPoint(x0 + w, y + px * .2)); TX.push({ id, text: str, x0: p0.x, y0: p0.y, x1: p1.x, y1: p1.y, pass }); }
  g.restore(); return w;
}

// ───────── A: the mess, generated ─────────
const HAB = ['Meditar 10 min', 'Leer 10 páginas', 'Ir al gimnasio', 'Tomar 2 L de agua', 'Dormir antes de las 12', 'Estudiar inglés', 'Caminar 30 min', 'Cero azúcar', 'Escribir el diario', 'Ahorrar un poco', 'Estirar 5 min', 'Sin redes de noche'];
const NC = 150, CARDS = [];
{
  const R = mulberry(77);
  for (let k = 0; k < NC; k++) {
    const u = k / NC, t = k === 0 ? -1 : 1.3 + 4.15 * Math.pow(u, .62);   // spawn times get closer and closer
    const ang = R() * TAU, rad = k === 0 ? 0 : 150 + 1500 * Math.pow(u, .8) * (.55 + .45 * R());
    CARDS.push({ k, t, x: W / 2 + Math.cos(ang) * rad * .8, y: H / 2 - 40 + Math.sin(ang) * rad * 1.15, rot: k === 0 ? 0 : (R() - .5) * .3,
      title: k === 0 ? 'Empiezo el lunes' : HAB[k % HAB.length], app: Math.floor(R() * 7), late: R() < .7, dl: R() * .5 });
  }
}
const CW = 520, CH = 104;
function card(g, c, t, a = 1, s = 1) {
  g.save(); g.translate(c.cx, c.cy); g.rotate(c.r); g.scale(s, s); g.globalAlpha *= a;
  const x = -CW / 2, y = -CH / 2;
  E.panel(g, x, y, CW, CH, { r: 26, fill: '#1A1E28', cheap: c.k > 0, shadow: 1 });
  // empty checkbox
  g.strokeStyle = 'rgba(255,255,255,.35)'; g.lineWidth = 3; rr(g, x + 26, y + 32, 40, 40, 11); g.stroke();
  const late = t > 3.55 + c.dl && c.k !== 0;
  text(g, c.title, x + 88, y + 47, 27, { wt: 600, id: c.k === 0 ? 'c0' : null });
  text(g, late ? '3 días sin marcar' : 'Empiezo el lunes', x + 88, y + 80, 21, { wt: 400, color: late ? '#FF8A8E' : PAL.dim });
  text(g, c.k === 0 ? 'lun' : late ? 'jue' : 'lun', x + CW - 24, y + 40, 18, { wt: 500, color: PAL.mute, align: 'right' });
  if (c.k > 0) E.badge(g, x + CW - 6, y + 4, late ? '3' : '1', 1.25);
  g.restore();
}
const camA = monotone([[0, Math.log(1.78)], [1.3, Math.log(1.7)], [5.6, Math.log(.4)], [6.4, Math.log(.44)]]);

function sceneMess(g, t, flyP = null) {
  const zoom = Math.exp(camA(Math.min(t, 6.4))), cam = E.camera({ x: W / 2, y: H / 2 - 40, zoom, rot: t < T.freeze ? -.02 * seg(t, 1.5, 5.5) : -.022 });
  E.bg(g); E.grid(g, { ox: W / 2 - (W / 2) * cam.proj(0, 0).s, oy: 0, scale: cam.proj(0, 0).s, alpha: .7 });
  const tt = Math.min(t, T.freeze);   // freeze: everything stops
  cam.apply(g);
  for (const c of CARDS) {
    if (tt < c.t) continue;
    const p = seg(tt, c.t, c.t + .26), drop = 1 + .14 * (1 - back(p, 2.2));
    let cx = c.x, cy = c.y, r = c.rot, s = drop, a = clamp(p * 3);
    if (flyP) {      // B: everything flies into the flame
      const d = Math.hypot(c.x - W / 2, c.y - (H / 2 - 40)) / 1800, q = eio(seg(flyP.t, T.press + .25 + d * .55, T.press + .75 + d * .55));
      if (q >= 1) continue;
      const fx = flyP.x, fy = flyP.y, bend = (c.x > W / 2 ? 1 : -1) * 260 * (1 - q) * q * 4;
      cx = lerp(c.x, fx, q) + bend * .4; cy = lerp(c.y, fy, q) - bend * .3; r = c.rot * (1 - q) + q * 1.2; s = lerp(1, .04, eio(q)); a = 1 - ss(seg(q, .75, 1));
    }
    card(g, { ...c, cx, cy, r }, tt, a, s);
  }
  g.setTransform(1, 0, 0, 1, 0, 0);
  E.vignette(g, .55);
  return cam;
}

// ───────── C: the grid ─────────
const TILE = 112, GAP = 18, GX = (W - (7 * TILE + 6 * GAP)) / 2, GY = 820;
const SLOTS = E.snapGrid(7, 3, TILE, GAP, GX, GY);
const LIT = SLOTS.map((s, i) => T.light0 + i * .125);
const DAYS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
function tile(g, x, y, sz, lit, i, t) {
  const p = new Path2D(); p.roundRect(x - sz / 2, y - sz / 2, sz, sz, sz * .24);
  if (lit > 0) {
    E.litShape(g, p, { bbox: [x - sz / 2, y - sz / 2, sz, sz] });
    const pop = 1 + .12 * (1 - back(lit, 2));
    g.save(); g.translate(x, y); g.scale(pop, pop); g.translate(-x, -y);
    const q = new Path2D(); q.roundRect(x - sz / 2, y - sz / 2, sz, sz, sz * .24);
    E.litShape(g, q, { accent: true, glowR: 22, alpha: clamp(lit * 2.5) });
    g.save(); g.globalAlpha = clamp(lit * 2.5); g.fillStyle = 'rgba(10,11,15,.55)'; g.fill(flamePath(x, y + 2, sz * .44)); g.restore();
    g.restore();
  } else E.litShape(g, p, { bbox: [x - sz / 2, y - sz / 2, sz, sz] });
}
function sceneGrid(g, t) {
  const z = t < T.dive ? lerp(1, 1.06, ss(seg(t, T.snap, T.dive))) : Math.exp(lerp(Math.log(1.06), Math.log(9), ei(seg(t, T.dive, T.orange))));
  const last = SLOTS[20], fx = lerp(W / 2, last.x, ss(seg(t, T.dive, T.orange - .1))), fy = lerp(H / 2 - 20, last.y, ss(seg(t, T.dive, T.orange - .1)));
  const cam = E.camera({ x: fx, y: fy, zoom: z });
  E.bg(g); E.grid(g, { ox: cam.proj(0, 0).x, oy: cam.proj(0, 0).y, scale: cam.proj(0, 0).s, alpha: .8 });
  cam.apply(g);
  // header: the flame climbs to the top and grows with every lit day
  const up = eo(seg(t, T.press + 1.0, T.snap)), nLit = LIT.filter(l => t >= l).length;
  const fyH = lerp(H / 2 - 40, 560, up), fs = lerp(150, 120, up) * (1 + .012 * nLit) * (1 + .06 * Math.exp(-(t - (LIT[nLit - 1] ?? -9)) * 10));
  flame(g, W / 2, fyH, fs);
  text(g, 'TU RACHA', W / 2, 700, 22, { fam: 'mono', wt: 500, color: PAL.dim, align: 'center', track: .22, a: ss(seg(t, T.snap, T.snap + .3)), id: 'racha' });
  DAYS.forEach((d, i) => text(g, d, GX + TILE / 2 + i * (TILE + GAP), GY - 22, 20, { fam: 'mono', wt: 500, color: PAL.mute, align: 'center', a: ss(seg(t, T.snap + .2, T.snap + .5)), id: 'day' + i, pass: true }));
  SLOTS.forEach((s, i) => {
    const d = (Math.abs(s.i - 3) + s.j) * .025, p = seg(t, T.snap - .25 + d, T.snap + .2 + d);
    if (p <= 0) return;
    const pose = E.flyPose({ x: W / 2, y: fyH, s: .1, rot: (i % 2 ? 1 : -1) * .6 }, { x: s.x, y: s.y, s: 1, rot: 0 }, p, { over: 1.3 });
    g.save(); g.translate(pose.x, pose.y); g.rotate(pose.rot); g.scale(pose.s, pose.s);
    tile(g, 0, 0, TILE, t >= LIT[i] ? seg(t, LIT[i], LIT[i] + .25) : 0, i, t); g.restore();
  });
  g.setTransform(1, 0, 0, 1, 0, 0); E.vignette(g, .5);
}

// ───────── D: the number ─────────
function sceneNumber(g, t) {
  g.setTransform(1, 0, 0, 1, 0, 0);
  g.fillStyle = PAL.accent; g.fillRect(0, 0, W, H);
  const gr = g.createRadialGradient(W / 2, H * .4, 0, W / 2, H * .4, H * .7); gr.addColorStop(0, 'rgba(255,255,255,.18)'); gr.addColorStop(1, 'rgba(0,0,0,.18)'); g.fillStyle = gr; g.fillRect(0, 0, W, H);
  const k = T.orange;
  E.scramble(g, '21', W / 2, 1060, t, [k + .45, k + .62], { px: 560, wt: 700, fam: 'disp', color: '#0A0B0F', align: 'center', tracking: -.05, start: k + .05 });
  text(g, 'DÍAS SEGUIDOS', W / 2, 1180, 40, { fam: 'mono', wt: 600, color: 'rgba(10,11,15,.8)', align: 'center', track: .2, a: ss(seg(t, k, k + .12)), id: 'dias' });
  if (t > k + .62) TX.push({ id: 'n21', text: '21', x0: 250, y0: 640, x1: 830, y1: 1070 });
}

// ───────── E: the phone ─────────
const PX = 260, PY = 330, PW = 560, PH = 1180, WX = 290, WY = 560, WW = 500, WH = 300;
const [phoneCv, phoneG] = [document.createElement('canvas'), null];
function drawPhone(g, t) {
  // body
  E.panel(g, PX, PY, PW, PH, { r: 78, fill: '#0E1016', shadow: 1.2 });
  g.save(); rr(g, PX + 14, PY + 14, PW - 28, PH - 28, 64); g.clip();
  const gr = g.createLinearGradient(0, PY, 0, PY + PH); gr.addColorStop(0, '#171B26'); gr.addColorStop(1, '#0D0F15'); g.fillStyle = gr; g.fillRect(PX, PY, PW, PH);
  E.glow(g, PX + PW * .75, PY + 140, 420, PAL.accent, .07);
  g.restore();
  g.fillStyle = '#05060A'; g.beginPath(); g.arc(W / 2, PY + 42, 9, 0, TAU); g.fill();
  // the widget
  E.panel(g, WX, WY, WW, WH, { r: 40, fill: PAL.surf2, shadow: 1 });
  const done = ss(seg(t, T.tap + .25, T.tap + .75)), streak = t > T.tap + .55 ? '22' : '21';
  flame(g, WX + 62, WY + 74, 62, { glowR: 16 });
  if (t < T.tap + .55) text(g, '21', WX + 110, WY + 104, 82, { fam: 'disp', wt: 700, id: 'w21' });
  else E.scramble(g, '22', WX + 110, WY + 104, t, [T.tap + .75], { px: 82, wt: 700, fam: 'disp', start: T.tap + .55 }), t > T.tap + .8 && TX.push({ id: 'w22', text: '22', x0: 0, y0: 0, x1: 1, y1: 1, pass: true });
  text(g, 'días seguidos', WX + 36, WY + 148, 24, { wt: 500, color: PAL.dim, id: 'wds' });
  // today's ring
  const rx = WX + WW - 86, ry = WY + 92, frac = lerp(.6, 1, done);
  g.lineWidth = 12; g.lineCap = 'round'; g.strokeStyle = 'rgba(255,255,255,.08)'; g.beginPath(); g.arc(rx, ry, 48, 0, TAU); g.stroke();
  g.save(); g.strokeStyle = PAL.accent; g.shadowColor = rgba(PAL.accent, .7); g.shadowBlur = 12 + 20 * done; g.beginPath(); g.arc(rx, ry, 48, -Math.PI / 2, -Math.PI / 2 + TAU * frac); g.stroke(); g.restore();
  text(g, done > .5 ? '5/5' : '3/5', rx, ry + 9, 26, { fam: 'mono', wt: 600, align: 'center', id: done > .5 ? 'w55' : 'w35', pass: done > .5 });
  text(g, 'HOY', rx, ry + 82, 18, { fam: 'mono', wt: 500, color: PAL.mute, align: 'center', track: .2, id: 'hoy' });
  // the week: one mountain per day
  const H7 = [.6, .8, 1, .7, .9, 1, lerp(.6, 1, done)];
  H7.forEach((h, i) => {
    const bx = WX + 40 + i * 62, by = WY + WH - 36, hh = 26 + h * 56, p = new Path2D();
    p.moveTo(bx, by); p.lineTo(bx + 26, by - hh); p.lineTo(bx + 52, by); p.closePath();
    if (i === 6 && done < .5) E.litShape(g, p, { bbox: [bx, by - hh, 52, hh] });
    else E.litShape(g, p, { accent: true, glowR: 8, alpha: .9, bbox: [bx, by - hh, 52, hh] });
  });
  // generic icons
  for (let r = 0; r < 3; r++) for (let c = 0; c < 4; c++) { const ix = PX + 52 + c * 122, iy = 960 + r * 150; E.panel(g, ix, iy, 92, 92, { r: 26, fill: PAL.surf2, shadow: .6, cheap: true }); g.fillStyle = 'rgba(255,255,255,.07)'; rr(g, ix + 30, iy + 30, 32, 32, 9); g.fill(); }
  mark(g, PX + 98, 960 + 3 * 150 + 46, 92);
}
function notifCard(g, t) {
  const p = seg(t, T.notif, T.notif + .3); if (p <= 0) return;
  const y = lerp(PY - 40, 400, back(p, 1.6)), x = PX + 24, w = PW - 48, h = 140;
  g.save(); g.globalAlpha = clamp(p * 3);
  E.panel(g, x, y, w, h, { r: 36, fill: '#202533', shadow: 1.2 });
  mark(g, x + 52, y + 52, 52);
  text(g, 'RACHAS · 22:30', x + 96, y + 44, 18, { fam: 'mono', wt: 500, color: PAL.dim, track: .12, id: 'n1' });
  text(g, 'Hora de anotar tu día', x + 96, y + 82, 30, { wt: 600, id: 'n2' });
  text(g, 'Te faltan 2 hábitos de hoy.', x + 96, y + 116, 22, { wt: 400, color: PAL.dim, id: 'n3' });
  // the tap
  const q = seg(t, T.tap, T.tap + .45);
  if (q > 0 && q < 1) { g.globalAlpha = 1 - q; g.fillStyle = 'rgba(255,255,255,.18)'; g.beginPath(); g.arc(x + w * .62, y + h * .55, 20 + 90 * eo(q), 0, TAU); g.fill(); }
  g.restore();
}
function scenePhone(g, t) {
  E.bg(g, { cy: H * .3 }); E.grid(g, { ox: W / 2, oy: H / 2, alpha: .6 });
  const z = Math.exp(monotone([[T.collapse, 0], [18.0, Math.log(1.04)], [19.1, Math.log(1.32)], [21.3, Math.log(1.36)], [T.off, Math.log(1.3)]])(t));
  const cy = monotone([[T.collapse, H / 2], [18.0, H / 2], [19.1, 760], [T.off, 760]])(t);
  const cam = E.camera({ x: W / 2, y: cy, zoom: z });
  // light reveal of the whole phone
  const [c, pg] = E.buf('phone', W, H); drawPhone(pg, t); notifCard(pg, t);
  const off = seg(t, T.off, T.logo);
  cam.apply(g);
  if (off > 0) {      // CRT-off: squash to a line, then to a point at the flame
    const sy = off < .6 ? lerp(1, .006, ei(off / .6)) : .006, sx = off < .6 ? 1 : lerp(1, .0, ei((off - .6) / .4));
    g.translate(W / 2, PY + PH / 2); g.scale(sx, sy); g.translate(-W / 2, -(PY + PH / 2));
    g.globalAlpha = 1; g.drawImage(c, 0, 0);
    g.setTransform(1, 0, 0, 1, 0, 0); E.glow(g, W / 2, cam.proj(W / 2, PY + PH / 2).y, 260 * (1 - off * .6), PAL.accent, .5 * off);
  } else E.lightReveal(g, c, 0, 0, W, H, seg(t, T.collapse + .05, T.collapse + 1.25), { base: .08, soft: 320, glint: .2, angle: 1.1 });
  g.setTransform(1, 0, 0, 1, 0, 0);
  // the orange card collapses into the widget
  const k = seg(t, T.collapse, T.collapse + .4);
  if (k < 1) {
    const e = eio(k), p0 = cam.proj(WX, WY), p1 = cam.proj(WX + WW, WY + WH);
    const x0 = lerp(0, p0.x, e), y0 = lerp(0, p0.y, e), x1 = lerp(W, p1.x, e), y1 = lerp(H, p1.y, e);
    g.fillStyle = rgba(PAL.accent, 1 - ss(seg(k, .7, 1))); rr(g, x0, y0, x1 - x0, y1 - y0, lerp(0, 40, e)); g.fill();
  }
  E.vignette(g, .5);
}

// ───────── F: logo ─────────
function sceneLogo(g, t) {
  E.bg(g); E.grid(g, { ox: W / 2, oy: H / 2, alpha: .5 });
  const p = seg(t, T.logo, T.logo + .35), s = 200 * (back(p, 1.8));
  if (p > 0) mark(g, W / 2, 800, Math.max(1, s));
  const a = ss(seg(t, T.logo + .12, T.logo + .32));
  g.save(); g.translate(0, (1 - eo(seg(t, T.logo + .12, T.logo + .4))) * 18);
  text(g, 'Rachas', W / 2, 1060, 128, { fam: 'disp', wt: 700, align: 'center', track: -.03, a, id: 'title' });
  text(g, 'Un día a la vez.', W / 2, 1140, 46, { wt: 400, color: PAL.dim, align: 'center', a: ss(seg(t, T.logo + .2, T.logo + .36)), id: 'tag' });
  g.restore();
  text(g, 'PARA ANDROID', W / 2, 1250, 22, { fam: 'mono', wt: 500, color: PAL.mute, align: 'center', track: .3, a: ss(seg(t, T.logo + .3, T.logo + .45)), id: 'and' });
  E.vignette(g, .55);
}

// ───────── frame ─────────
function captions(g, t) {
  g.setTransform(1, 0, 0, 1, 0, 0);
  for (const c of CAPS) {
    if (t < c.t0 || t > c.t1) continue;
    const a = Math.min(seg(t, c.t0, c.t0 + .16), 1 - seg(t, c.t1 - .12, c.t1));
    E.toast(g, c.text, a, { y: 1730, px: 38, dense: t < T.freeze ? 1 : 0 });
  }
}
export function renderFilm(g, t) {
  TX = [];
  g.setTransform(1, 0, 0, 1, 0, 0); g.globalAlpha = 1; g.globalCompositeOperation = 'source-over';
  if (t < T.press) sceneMess(g, t);
  else if (t < T.snap) {
    sceneGrid(g, t);                        // the flame (and the grid to come) under the flying cards
    const fy = lerp(H / 2 - 40, 560, eo(seg(t, T.press + 1.0, T.snap)));
    g.save(); sceneMessOver(g, t, fy); g.restore();
  }
  else if (t < T.orange) sceneGrid(g, t);
  else if (t < T.collapse) sceneNumber(g, t);
  else if (t < T.logo) scenePhone(g, t);
  else sceneLogo(g, t);
  // the press: the flame appears after the freeze
  captions(g, t);
}
function sceneMessOver(g, t, fy) {
  // the cards, seen through the same camera as the freeze, fly into the flame
  const zoom = Math.exp(camA(6.4)), cam = E.camera({ x: W / 2, y: H / 2 - 40, zoom, rot: -.022 });
  const target = { x: W / 2, y: H / 2 - 40 + (fy - (H / 2 - 40)) / cam.proj(0, 0).s * 0 };
  cam.apply(g);
  const tt = T.freeze;
  for (const c of CARDS) {
    const d = Math.hypot(c.x - W / 2, c.y - (H / 2 - 40)) / 1800, q = eio(seg(t, T.press + .2 + d * .5, T.press + .75 + d * .5));
    if (q >= 1) continue;
    const bend = (c.x > W / 2 ? 1 : -1) * 300 * (1 - q) * q * 4;
    const cx = lerp(c.x, target.x, q) + bend * .4, cy = lerp(c.y, target.y, q) - bend * .3;
    card(g, { ...c, cx, cy, r: c.rot * (1 - q) + q * 1.2 }, tt, 1 - ss(seg(q, .75, 1)), lerp(1, .03, eio(q)));
  }
  g.setTransform(1, 0, 0, 1, 0, 0);
}
export const texts = t => TX;

// ───────── sound events ─────────
export function buildEvents() {
  const ev = [];
  CARDS.forEach(c => { if (c.t > 0) ev.push({ t: c.t, type: 'card', k: c.k, pan: clamp((c.x - W / 2) / 900, -1, 1) }); });
  ev.push({ t: 0.05, type: 'card', k: 0, pan: 0 });
  ev.push({ t: T.press, type: 'press' });
  CARDS.forEach(c => { const d = Math.hypot(c.x - W / 2, c.y - (H / 2 - 40)) / 1800; if (c.k % 3 === 0) ev.push({ t: T.press + .75 + d * .5, type: 'land', k: c.k }); });
  ev.push({ t: T.snap - .05, type: 'snap' });
  LIT.forEach((t, i) => ev.push({ t, type: 'lit', i }));
  ev.push({ t: T.dive, type: 'dive' }, { t: T.orange + .05, type: 'digits', d: .6 }, { t: T.orange + .62, type: 'number' });
  ev.push({ t: T.collapse, type: 'sweep' }, { t: T.notif, type: 'notif' }, { t: T.tap, type: 'tap' }, { t: T.tap + .3, type: 'ring' }, { t: T.tap + .55, type: 'digits', d: .2 }, { t: T.tap + .75, type: 'lock' });
  ev.push({ t: T.off, type: 'crt' }, { t: T.logo, type: 'logo' });
  return ev.sort((a, b) => a.t - b.t);
}
