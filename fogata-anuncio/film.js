// Fogata — «Mantén vivo tu fuego». Dark Tech Keynote, 1080×1920 @ 24 fps. Pure function of t.
// The app screens (Val Q, Bitácora) are redrawn in code from the real apps' layout, with demo numbers.
import * as E from './engine.js';
import { monotone } from '/core/lib.js';
const { W, H, PAL, font, rgba, rr, clamp, lerp, seg, ss, eo, ei, eio, back, mulberry, hash, TAU } = E;
E.setAccent('#FF7A1A');
export const DUR = 25.4;
const MINT = '#2EE6A8', BLUE = '#5AB0F0', TEAL = '#3FD3B0';

// ───────── timing ─────────
export const VO = { l1: .1, l2: 2.95, l3: 6.0, l4: 7.75, l5: 11.3, l6: 17.7, l7: 21.5, l8: 23.6 };
export const CAPS = [
  { t0: .05, t1: 2.95, text: '¿Cuántas veces dijiste: «empiezo el lunes»?' },
  { t0: 2.95, t1: 5.85, text: '¿Y cuántas veces llegó el jueves, y nada?' },
  { t0: 5.95, t1: 7.7, text: 'Esto es Fogata.' },
  { t0: 7.7, t1: 11.3, text: 'Tus hábitos viven en dos apps: Val Q y Bitácora.' },
  { t0: 11.3, t1: 15.3, text: 'Cada día cumplido sube tu montaña, y alimenta tu racha.' },
  { t0: 17.6, t1: 21.2, text: 'Y a las 22:30, te recuerda no apagar tu fuego.' },
];
export const T = { jueves: 2.95, freeze: 5.4, press: 5.9, phone: 7.45, hiQ: 9.3, hiB: 10.1, tapQ: 10.85, openQ: 11.0, toB: 13.1, dive: 15.0, orange: 15.3, back: 17.6, notif: 17.75, tap: 19.8, off: 21.2, logo: 21.45 };

// ───────── helpers ─────────
let TX = [];
function text(g, str, x, y, px, o = {}) {
  const { wt = 500, fam = 'ui', color = PAL.text, align = 'left', a = 1, track = 0, id = null, pass = false } = o;
  if (a <= 0) return 0;
  g.save(); g.globalAlpha *= a; g.font = font(px, wt, fam); g.fillStyle = color; g.textAlign = align; g.textBaseline = 'alphabetic';
  if (g.letterSpacing !== undefined) g.letterSpacing = `${track * px}px`;
  g.fillText(str, x, y); const w = g.measureText(str).width;
  if (id && a > .5 && g === MAIN) { const m = g.getTransform(), x0 = align === 'center' ? x - w / 2 : align === 'right' ? x - w : x; const p0 = m.transformPoint(new DOMPoint(x0, y - px * .8)), p1 = m.transformPoint(new DOMPoint(x0 + w, y + px * .2)); TX.push({ id, text: str, x0: p0.x, y0: p0.y, x1: p1.x, y1: p1.y, pass }); }
  g.restore(); return w;
}
let MAIN = null;
function flamePath(cx, cy, s) {
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
  const { alpha = 1, glowR = s * .35 } = o;
  E.litShape(g, flamePath(cx, cy, s), { accent: true, glowR, alpha, bbox: [cx - s * .4, cy - s * .5, s * .8, s] });
  g.save(); g.globalAlpha *= alpha * .9; g.fillStyle = '#FFC07A'; g.fill(flamePath(cx, cy + s * .2, s * .48)); g.restore();
}
function mark(g, cx, cy, s, a = 1) { g.save(); g.globalAlpha *= a; E.panel(g, cx - s / 2, cy - s / 2, s, s, { r: s * .28, fill: PAL.surf3, shadow: .8 }); flame(g, cx, cy + s * .02, s * .58); g.restore(); }
// the two app icons, as on the phone (with the Fogata flame badge)
function iconValQ(g, cx, cy, s, badge = true) {
  g.save(); g.fillStyle = '#0E161D'; rr(g, cx - s / 2, cy - s / 2, s, s, s * .3); g.fill(); g.strokeStyle = 'rgba(255,255,255,.08)'; g.lineWidth = 1.5; g.stroke();
  g.lineCap = 'round'; g.lineWidth = s * .09;
  g.strokeStyle = '#1C8FA0'; g.beginPath(); g.arc(cx, cy, s * .3, 0, TAU); g.stroke();
  g.strokeStyle = MINT; g.beginPath(); g.arc(cx, cy, s * .3, -Math.PI / 2, Math.PI * .95); g.stroke();
  g.lineWidth = s * .1; g.beginPath(); g.moveTo(cx - s * .14, cy + s * .01); g.lineTo(cx - s * .03, cy + s * .12); g.lineTo(cx + s * .17, cy - s * .11); g.stroke();
  g.restore(); if (badge) badgeFlame(g, cx + s * .33, cy + s * .33, s * .34);
}
function iconBit(g, cx, cy, s, badge = true) {
  g.save(); g.fillStyle = '#0F2622'; rr(g, cx - s / 2, cy - s / 2, s, s, s * .3); g.fill(); g.strokeStyle = 'rgba(255,255,255,.08)'; g.lineWidth = 1.5; g.stroke();
  g.strokeStyle = TEAL; g.lineWidth = s * .07; g.lineJoin = 'round'; g.lineCap = 'round';
  rr(g, cx - s * .2, cy - s * .27, s * .42, s * .54, s * .07); g.stroke();
  g.beginPath(); g.moveTo(cx - s * .1, cy - s * .27); g.lineTo(cx - s * .1, cy + s * .27); g.stroke();
  for (let i = 0; i < 3; i++) { g.beginPath(); g.moveTo(cx + s * .0, cy - s * .12 + i * s * .12); g.lineTo(cx + s * .12, cy - s * .12 + i * s * .12); g.stroke(); }
  g.restore(); if (badge) badgeFlame(g, cx + s * .33, cy + s * .33, s * .34);
}
function badgeFlame(g, cx, cy, s) { g.save(); g.fillStyle = '#1A1C22'; g.beginPath(); g.arc(cx, cy, s / 2, 0, TAU); g.fill(); flame(g, cx, cy + s * .03, s * .62, { glowR: 6 }); g.restore(); }

// ───────── A: the hook. A wall of "empiezo el lunes" from frame one ─────────
const HAB = ['Meditar 10 min', 'Leer 10 páginas', 'Ir al gimnasio', 'Tomar 2 L de agua', 'Dormir antes de las 12', 'Estudiar inglés', 'Caminar 30 min', 'Cero azúcar', 'Escribir el diario', 'Ahorrar un poco', 'Menos sal', 'Sin redes de noche'];
const NC = 170, CARDS = [];
{
  const R = mulberry(91);
  for (let k = 0; k < NC; k++) {
    const u = k / NC, t = .03 + 5.2 * Math.pow(u, 1.5), ang = R() * TAU, rad = 120 + 1500 * Math.pow(u, .75) * (.5 + .5 * R());
    CARDS.push({ k, t, x: W / 2 + Math.cos(ang) * rad * .85, y: H / 2 + Math.sin(ang) * rad * 1.2, rot: (R() - .5) * .34, title: HAB[k % HAB.length], dl: R() * .6 });
  }
}
const CW = 520, CH = 104;
function card(g, c, t, a = 1, s = 1) {
  g.save(); g.translate(c.cx, c.cy); g.rotate(c.r); g.scale(s, s); g.globalAlpha *= a;
  const x = -CW / 2, y = -CH / 2, late = t > T.jueves + c.dl;
  E.panel(g, x, y, CW, CH, { r: 26, fill: '#1A1E28', cheap: true, shadow: 1 });
  g.strokeStyle = 'rgba(255,255,255,.35)'; g.lineWidth = 3; rr(g, x + 26, y + 32, 40, 40, 11); g.stroke();
  text(g, c.title, x + 88, y + 47, 27, { wt: 600 });
  text(g, late ? '3 días sin marcar' : 'Empiezo el lunes', x + 88, y + 80, 21, { wt: 400, color: late ? '#FF8A8E' : PAL.dim });
  text(g, late ? 'jue' : 'lun', x + CW - 24, y + 40, 18, { color: PAL.mute, align: 'right' });
  E.badge(g, x + CW - 6, y + 4, late ? '3' : '1', 1.25);
  g.restore();
}
const camA = monotone([[0, Math.log(1.0)], [T.freeze, Math.log(.48)], [T.press, Math.log(.52)]]);
const stampT = [.08, .5, .92];
function shake(t) { let s = 0; for (const k of [...stampT, T.jueves]) { const d = t - k; if (d > 0 && d < .25) s += 14 * Math.exp(-d * 18) * Math.sin(d * 90); } return s; }
function sceneHook(g, t) {
  const tt = Math.min(t, T.freeze), zoom = Math.exp(camA(Math.min(t, T.press))), sh = shake(tt);
  const cam = E.camera({ x: W / 2, y: H / 2, zoom, rot: -.015 * seg(tt, 0, 5), sx: sh, sy: sh * .6 });
  E.bg(g); E.grid(g, { ox: cam.proj(0, 0).x, oy: cam.proj(0, 0).y, scale: cam.proj(0, 0).s, alpha: .7 });
  cam.apply(g);
  for (const c of CARDS) { if (tt < c.t) continue; const p = seg(tt, c.t, c.t + .22); card(g, { ...c, cx: c.x, cy: c.y, r: c.rot }, tt, clamp(p * 3), 1 + .16 * (1 - back(p, 2.2))); }
  g.setTransform(1, 0, 0, 1, 0, 0); E.vignette(g, .6);
  // the giant words, stamped
  const lun = tt < T.jueves, st = lun ? stampT.filter(k => tt >= k).pop() ?? 0 : T.jueves, d = tt - st, pop = 1 + .35 * Math.exp(-d * 16);
  g.save(); g.translate(W / 2 + sh, 900); g.scale(pop, pop);
  g.shadowColor = 'rgba(0,0,0,.85)'; g.shadowBlur = 60;
  if (lun) { text(g, 'EMPIEZO', 0, -40, 150, { fam: 'disp', wt: 700, align: 'center', track: -.03, id: 'hk1' }); text(g, 'EL LUNES', 0, 110, 150, { fam: 'disp', wt: 700, align: 'center', track: -.03, color: '#FFFFFF' }); }
  else { text(g, '¿Y EL', 0, -40, 150, { fam: 'disp', wt: 700, align: 'center', track: -.03, color: '#FF6B70', id: 'hk2' }); text(g, 'JUEVES?', 0, 110, 150, { fam: 'disp', wt: 700, align: 'center', track: -.03, color: '#FF6B70' }); }
  g.restore();
}
function sceneFlame(g, t) {        // the press: the flame appears, every card flies into it
  E.bg(g); E.grid(g, { ox: W / 2, oy: H / 2, alpha: .7 });
  const zoom = Math.exp(camA(T.press)), cam = E.camera({ x: W / 2, y: H / 2, zoom, rot: -.015 });
  const p = seg(t, T.press, T.press + .3), s = 230 * back(p, 2.2) * (1 + .04 * Math.sin(t * 9) * seg(t, T.press + .3, T.press + .6));
  g.setTransform(1, 0, 0, 1, 0, 0);
  E.glow(g, W / 2, 860, 520, PAL.accent, .18 * p);
  flame(g, W / 2, 860, Math.max(1, s));
  cam.apply(g);
  for (const c of CARDS) {
    const d = Math.hypot(c.x - W / 2, c.y - H / 2) / 1800, q = eio(seg(t, T.press + .15 + d * .5, T.press + .7 + d * .5));
    if (q >= 1) continue;
    const bend = (c.x > W / 2 ? 1 : -1) * 300 * (1 - q) * q * 4, tx = W / 2, ty = H / 2 + (860 - H / 2) / zoom;
    card(g, { ...c, cx: lerp(c.x, tx, q) + bend * .4, cy: lerp(c.y, ty, q) - bend * .3, r: c.rot * (1 - q) + q * 1.2 }, T.freeze, 1 - ss(seg(q, .75, 1)), lerp(1, .03, eio(q)));
  }
  g.setTransform(1, 0, 0, 1, 0, 0);
  const a = ss(seg(t, T.press + .35, T.press + .6));
  g.save(); g.translate(0, (1 - eo(seg(t, T.press + .35, T.press + .7))) * 20);
  text(g, 'Fogata', W / 2, 1150, 150, { fam: 'disp', wt: 700, align: 'center', track: -.03, a, id: 'name1', pass: true }); g.restore();
  E.vignette(g, .55);
}

// ───────── the phone and its screens (drawn in "app space", 905×1940) ─────────
const PX = 164, PY = 140, PW = 752, PH = 1580, SX = PX + 14, SY = PY + 14, SW = PW - 28, SH = PH - 28, AS = 1.25, AW = Math.round(SW * AS), AH = Math.round(SH * AS);
function statusBar(g) { text(g, '22:30', 46, 64, 30, { wt: 600 }); g.fillStyle = 'rgba(255,255,255,.85)'; rr(g, AW - 96, 42, 50, 24, 7); g.fill(); }
// home screen
const ICX = [128, 343, 558, 773], ICY0 = 760, ICS = 150;
const GENERIC = ['Notas', 'Reloj', 'Fotos', 'Mapas', 'Cámara', 'Música', 'Clima', 'Correo', 'Agenda', 'Archivos', 'Radio', 'Juegos'];
const ICO_POS = { valq: [1, 2], bit: [2, 2] };
function home(g, t, st) {
  const gr = g.createLinearGradient(0, 0, 0, AH); gr.addColorStop(0, '#1B2130'); gr.addColorStop(.55, '#151821'); gr.addColorStop(1, '#2A2321'); g.fillStyle = gr; g.fillRect(0, 0, AW, AH);
  E.glow(g, AW * .8, 260, 600, PAL.accent, .06);
  statusBar(g);
  // the Fogata widget (4×2)
  const wx = 50, wy = 140 + 236 * eo(st.notif), ww = AW - 100, wh = 400;
  E.panel(g, wx, wy, ww, wh, { r: 50, fill: PAL.surf2, shadow: 1 });
  flame(g, wx + 82, wy + 96, 82, { glowR: 18 });
  if (!st.scr) text(g, String(st.streak), wx + 148, wy + 134, 108, { fam: 'disp', wt: 700, id: 'w' + st.streak });
  else E.scramble(g, String(st.streak), wx + 148, wy + 134, t, [st.lockAt], { px: 108, wt: 700, fam: 'disp', start: st.scrAt });
  text(g, 'días seguidos', wx + 48, wy + 196, 32, { color: PAL.dim, id: 'wds' });
  const rx = wx + ww - 120, ry = wy + 120, frac = lerp(.6, 1, st.done);
  g.lineWidth = 16; g.lineCap = 'round'; g.strokeStyle = 'rgba(255,255,255,.08)'; g.beginPath(); g.arc(rx, ry, 64, 0, TAU); g.stroke();
  g.save(); g.strokeStyle = PAL.accent; g.shadowColor = rgba(PAL.accent, .7); g.shadowBlur = 14 + 24 * st.done; g.beginPath(); g.arc(rx, ry, 64, -Math.PI / 2, -Math.PI / 2 + TAU * frac); g.stroke(); g.restore();
  text(g, st.done > .5 ? '5/5' : '3/5', rx, ry + 12, 34, { fam: 'mono', wt: 600, align: 'center', id: st.done > .5 ? 'w55' : 'w35', pass: st.done > .5 });
  text(g, 'HOY', rx, ry + 106, 22, { fam: 'mono', color: PAL.mute, align: 'center', track: .2, id: 'hoy' });
  [.6, .8, 1, .7, .9, 1, lerp(.6, 1, st.done)].forEach((h, i) => {
    const bx = wx + 52 + i * 84, by = wy + wh - 46, hh = 34 + h * 74, p = new Path2D(); p.moveTo(bx, by); p.lineTo(bx + 36, by - hh); p.lineTo(bx + 72, by); p.closePath();
    if (i === 6 && st.done < .5) E.litShape(g, p, { bbox: [bx, by - hh, 72, hh] }); else E.litShape(g, p, { accent: true, glowR: 8, alpha: .92, bbox: [bx, by - hh, 72, hh] });
  });
  // icons
  let n = 0;
  for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) {
    const x = ICX[c], y = ICY0 + r * 250;
    if (r === 2 && c === 1) { const hl = st.hiQ; ring(g, x, y, hl); iconValQ(g, x, y, ICS); text(g, 'Val Q', x, y + 120, 34, { align: 'center', wt: 500, id: 'lq' }); continue; }
    if (r === 2 && c === 2) { const hl = st.hiB; ring(g, x, y, hl); iconBit(g, x, y, ICS); text(g, 'Bitácora', x, y + 120, 34, { align: 'center', wt: 500, id: 'lb' }); continue; }
    g.save(); g.globalAlpha = st.dim; E.panel(g, x - ICS / 2, y - ICS / 2, ICS, ICS, { r: 44, fill: PAL.surf3, shadow: .5, cheap: true });
    g.fillStyle = 'rgba(255,255,255,.09)'; rr(g, x - 28, y - 28, 56, 56, 16); g.fill();
    text(g, GENERIC[n++ % GENERIC.length], x, y + 120, 34, { align: 'center', color: PAL.dim }); g.restore();
  }
  // notification
  const np = st.notif; if (np > 0) {
    const y = lerp(-200, 40, back(np, 1.5)), x = 34, w = AW - 68, h = 196;
    g.save(); g.globalAlpha = clamp(np * 3); E.panel(g, x, y, w, h, { r: 48, fill: '#232838', shadow: 1.3 });
    mark(g, x + 74, y + 74, 76);
    text(g, 'FOGATA · 22:30', x + 136, y + 62, 24, { fam: 'mono', color: PAL.dim, track: .12, id: 'n1' });
    text(g, 'Hora de anotar tu día', x + 136, y + 112, 40, { wt: 600, id: 'n2' });
    text(g, 'Te faltan 2 hábitos de hoy.', x + 136, y + 160, 30, { color: PAL.dim, id: 'n3' });
    if (st.tapQ > 0 && st.tapQ < 1) { g.globalAlpha = 1 - st.tapQ; g.fillStyle = 'rgba(255,255,255,.2)'; g.beginPath(); g.arc(x + w * .6, y + h * .55, 30 + 120 * eo(st.tapQ), 0, TAU); g.fill(); }
    g.restore();
  }
  if (st.tapIcon > 0 && st.tapIcon < 1) { g.save(); g.globalAlpha = 1 - st.tapIcon; g.fillStyle = 'rgba(255,255,255,.22)'; g.beginPath(); g.arc(ICX[1], ICY0 + 500, 40 + 110 * eo(st.tapIcon), 0, TAU); g.fill(); g.restore(); }
}
function ring(g, x, y, a) { if (a <= 0) return; g.save(); g.globalAlpha = a; g.strokeStyle = PAL.accent; g.lineWidth = 5; g.shadowColor = rgba(PAL.accent, .9); g.shadowBlur = 24; rr(g, x - ICS / 2 - 14, y - ICS / 2 - 14, ICS + 28, ICS + 28, 54); g.stroke(); g.restore(); }

// Val Q — «Avance · Tu ciclo» (layout of the real app; demo numbers)
const QD = [100, 60, 40, 60, 80, 70, 75, 85, 90, 80, 95, 100, 90, 100];
const avg = arr => arr.map((v, i) => { const s = arr.slice(Math.max(0, i - 6), i + 1); return s.reduce((a, b) => a + b, 0) / s.length; });
function chart(g, x, y, w, h, daily, meta, p, lineCol, areaCol) {
  E.panel(g, x, y, w, h, { r: 44, fill: '#111620', shadow: .6 });
  const cx0 = x + 110, cx1 = x + w - 50, cy0 = y + 70, cy1 = y + h - 120, Y = v => lerp(cy1, cy0, v / 100);
  [100, 75, 50, 25, 0].forEach(v => { g.fillStyle = 'rgba(255,255,255,.07)'; g.fillRect(cx0, Y(v), cx1 - cx0, 1.5); text(g, String(v), cx0 - 22, Y(v) + 9, 26, { color: PAL.dim, align: 'right' }); });
  g.save(); g.setLineDash([14, 12]); g.strokeStyle = 'rgba(255,255,255,.45)'; g.lineWidth = 2.5; g.beginPath(); g.moveTo(cx0, Y(meta)); g.lineTo(cx1, Y(meta)); g.stroke(); g.restore();
  text(g, `meta ${meta}%`, cx1, Y(meta) - 16, 26, { color: PAL.dim, align: 'right' });
  const A = avg(daily), n = daily.length, X = i => lerp(cx0, cx1, i / 13), last = Math.min(n - 1, p * (n - 1));
  g.save(); g.beginPath(); g.rect(cx0 - 10, cy0 - 30, (cx1 - cx0) * (last / 13) + 20, cy1 - cy0 + 60); g.clip();
  const path = (arr) => { const P = new Path2D(); arr.forEach((v, i) => { const xx = X(i), yy = Y(v); if (!i) P.moveTo(xx, yy); else { const xp = X(i - 1), yp = Y(arr[i - 1]); P.bezierCurveTo((xp + xx) / 2, yp, (xp + xx) / 2, yy, xx, yy); } }); return P; };
  const area = path(A); const ar = new Path2D(area); ar.lineTo(X(n - 1), cy1); ar.lineTo(X(0), cy1); ar.closePath();
  const gr = g.createLinearGradient(0, cy0, 0, cy1); gr.addColorStop(0, rgba(areaCol, .55)); gr.addColorStop(1, rgba(areaCol, .03)); g.fillStyle = gr; g.fill(ar);
  g.strokeStyle = areaCol; g.lineWidth = 6; g.stroke(area);
  g.strokeStyle = lineCol; g.lineWidth = 3.5; g.stroke(path(daily));
  daily.forEach((v, i) => { g.fillStyle = v >= meta ? areaCol : lineCol; g.beginPath(); g.arc(X(i), Y(v), 8, 0, TAU); g.fill(); });
  g.restore();
  text(g, '9 nov', cx0, cy1 + 50, 26, { color: PAL.dim }); text(g, 'día 14', cx1, cy1 + 50, 26, { color: PAL.dim, align: 'right' });
  g.fillStyle = areaCol; rr(g, x + 50, y + h - 52, 44, 22, 11); g.fill(); text(g, 'Tu montaña (promedio 7 días)', x + 110, y + h - 33, 28, { color: PAL.text });
}
function count(t, t0, to, d = .7) { return Math.round(to * eo(seg(t, t0, t0 + d))); }
function navBar(g, active, col) {
  g.fillStyle = '#0E1118'; g.fillRect(0, AH - 200, AW, 200); g.fillStyle = 'rgba(255,255,255,.06)'; g.fillRect(0, AH - 200, AW, 2);
  ['Hoy', 'Premios', active, 'Ajustes'].forEach((l, i) => {
    const x = AW * (i + .5) / 4, y = AH - 120;
    if (i === 2) { g.fillStyle = rgba(col, .16); rr(g, x - 70, y - 62, 140, 76, 38); g.fill(); }
    g.strokeStyle = i === 2 ? col : PAL.dim; g.lineWidth = 5; g.lineCap = 'round';
    if (i === 0) { g.beginPath(); g.arc(x, y - 24, 26, 0, TAU); g.stroke(); g.beginPath(); g.moveTo(x - 11, y - 24); g.lineTo(x - 2, y - 15); g.lineTo(x + 13, y - 33); g.stroke(); }
    if (i === 1) { rr(g, x - 26, y - 44, 52, 44, 6); g.stroke(); g.beginPath(); g.moveTo(x, y - 44); g.lineTo(x, y); g.stroke(); }
    if (i === 2) { for (let k = 0; k < 4; k++) { g.beginPath(); g.moveTo(x - 18 + k * 12, y - 4); g.lineTo(x - 18 + k * 12, y - 18 - [8, 26, 16, 30][k]); g.stroke(); } }
    if (i === 3) { for (let k = 0; k < 3; k++) { g.beginPath(); g.moveTo(x - 26, y - 40 + k * 16); g.lineTo(x + 26, y - 40 + k * 16); g.stroke(); } }
    text(g, l, x, y + 52, 32, { wt: 600, color: i === 2 ? col : PAL.dim, align: 'center' });
  });
}
function statCard(g, x, y, w, h, num, suf, label, fam = 'ui', wt = 700) {
  E.panel(g, x, y, w, h, { r: 40, fill: '#121722', shadow: .4 });
  const nw = text(g, num, x + 44, y + 120, 92, { fam, wt });
  if (suf) text(g, suf, x + 48 + nw, y + 120, 42, { fam, wt: 600, color: PAL.text });
  text(g, label, x + 44, y + 186, 34, { color: PAL.dim });
}
function valq(g, t) {
  g.fillStyle = '#0B0E13'; g.fillRect(0, 0, AW, AH); statusBar(g);
  const t0 = T.openQ + .15;
  text(g, 'AVANCE', 50, 180, 30, { wt: 700, color: MINT, track: .18 });
  text(g, 'Tu ciclo', 48, 280, 92, { wt: 700, track: -.03, id: 'qt' });
  text(g, '9 nov – 6 feb', 50, 346, 36, { color: PAL.dim });
  const cw = (AW - 100 - 30) / 2;
  statCard(g, 50, 390, cw, 236, String(count(t, t0, 18)), '', 'días ganados');
  statCard(g, 80 + cw, 390, cw, 236, String(count(t, t0, 78)), '%', 'promedio diario');
  statCard(g, 50, 652, cw, 236, String(count(t, t0, 2)), '/13', 'semanas ganadas');
  statCard(g, 80 + cw, 652, cw, 236, String(69 + Math.round(21 * (1 - eo(seg(t, t0, t0 + .7))))), '', 'días por delante');
  text(g, 'TU MONTAÑA', 56, 974, 32, { wt: 700, color: PAL.dim, track: .14 }); text(g, 'toca para ver un día', AW - 56, 974, 32, { color: PAL.dim, align: 'right' });
  chart(g, 50, 1010, AW - 100, 700, QD, 65, eio(seg(t, t0 + .2, t0 + 1.6)), BLUE, MINT);
  navBar(g, 'Avance', MINT);
}
const BD = [80, 100, 100, 80, 100, 100, 100, 90, 100, 100, 100, 100, 100, 100];
function bitacora(g, t) {
  g.fillStyle = '#050608'; g.fillRect(0, 0, AW, AH); statusBar(g);
  const t0 = T.toB + .2;
  text(g, 'ANÁLISIS', 50, 180, 30, { wt: 700, color: TEAL, track: .18 });
  text(g, 'Cómo vas', 48, 286, 104, { fam: '"Barlow Condensed"', wt: 700, id: 'bt' });
  text(g, '9 nov – 6 feb · 24 días registrados', 50, 350, 36, { color: PAL.dim });
  const cw = (AW - 100 - 30) / 2, BC = '"Barlow Condensed"';
  statCard(g, 50, 390, cw, 236, String(count(t, t0, 21, .9)), '', 'racha actual', BC);
  statCard(g, 80 + cw, 390, cw, 236, String(count(t, t0, 21, .9)), '', 'mejor racha', BC);
  statCard(g, 50, 652, cw, 236, String(count(t, t0, 24, .9)), '', 'días limpios', BC);
  statCard(g, 80 + cw, 652, cw, 236, String(count(t, t0, 89, .9)), '%', 'de tus días', BC);
  text(g, 'TU MONTAÑA', 56, 974, 32, { wt: 700, color: PAL.dim, track: .14 }); text(g, 'toca para ver un día', AW - 56, 974, 32, { color: PAL.dim, align: 'right' });
  chart(g, 50, 1010, AW - 100, 700, BD, 100, eio(seg(t, t0 + .2, t0 + 1.5)), BLUE, TEAL);
  navBar(g, 'Análisis', TEAL);
}
function phoneScene(g, t) {
  E.bg(g, { cy: H * .3 }); E.grid(g, { ox: W / 2, oy: H / 2, alpha: .55 });
  const st = { streak: t > T.tap + .55 ? 22 : 21, scr: t > T.tap + .55 && t < T.tap + .8, scrAt: T.tap + .55, lockAt: T.tap + .78, done: ss(seg(t, T.tap + .25, T.tap + .75)),
    hiQ: ss(seg(t, T.hiQ, T.hiQ + .2)) * (1 - ss(seg(t, T.tapQ, T.tapQ + .2))), hiB: ss(seg(t, T.hiB, T.hiB + .2)) * (1 - ss(seg(t, T.tapQ, T.tapQ + .2))),
    dim: t < T.back ? lerp(1, .45, ss(seg(t, T.hiQ - .3, T.hiQ))) : 1, notif: seg(t, T.notif, T.notif + .3), tapQ: seg(t, T.tap, T.tap + .45), tapIcon: seg(t, T.tapQ, T.tapQ + .4) };
  const [hc, hg] = E.buf('home', AW, AH); home(hg, t, st);
  let screen = hc;
  if (t >= T.openQ && t < T.back) {
    const [ac, ag] = E.buf('app', AW, AH);
    if (t < T.toB + .35) { valq(ag, t); }
    const [bc, bg2] = E.buf('app2', AW, AH);
    const [sc, sg] = E.buf('scr', AW, AH);
    sg.drawImage(hc, 0, 0);
    const o = eio(seg(t, T.openQ, T.openQ + .32)), ix = ICX[1], iy = ICY0 + 500;          // the app opens out of its icon
    const sw = lerp(ICS, AW, o), sh = lerp(ICS, AH, o), sx = lerp(ix - ICS / 2, 0, o), sy = lerp(iy - ICS / 2, 0, o);
    sg.save(); rr(sg, sx, sy, sw, sh, lerp(44, 0, o)); sg.clip();
    const sl = eio(seg(t, T.toB, T.toB + .35));
    if (sl < 1) sg.drawImage(ac, sx - sl * AW * .3, sy, sw, sh);
    if (sl > 0) { bitacora(bg2, t); sg.shadowColor = 'rgba(0,0,0,.6)'; sg.shadowBlur = 60; sg.drawImage(bc, AW * (1 - sl), 0); }
    sg.restore();
    screen = sc;
  }
  // camera: the phone expands out of the flame, a gentle push on the apps, a dive into the streak
  const ex = back(seg(t, T.phone, T.phone + .45), 1.4);
  let z = ex * lerp(1, 1.05, ss(seg(t, T.openQ, T.dive))), cx = W / 2, cy = H / 2 + lerp(-80, 0, ex);
  if (t >= T.dive && t < T.back) { const d = ei(seg(t, T.dive, T.orange)); z = Math.exp(lerp(Math.log(1.05), Math.log(7), d)); const tx = SX + (50 + 44 + 40) / AS, ty = SY + (390 + 80) / AS; cx = lerp(W / 2, tx, ss(d * 1.6)); cy = lerp(H / 2, ty, ss(d * 1.6)); }
  if (t >= T.back) { z = Math.exp(monotone([[T.back, 0], [18.5, Math.log(1.02)], [19.4, Math.log(1.2)], [T.off, Math.log(1.22)]])(t)); cy = monotone([[T.back, H / 2], [18.5, H / 2], [19.4, 760], [T.off, 760]])(t); }
  const cam = E.camera({ x: cx, y: cy, zoom: Math.max(.01, z) }); cam.apply(g);
  const off = seg(t, T.off, T.logo);
  if (off > 0) { const sy = off < .6 ? lerp(1, .006, ei(off / .6)) : .006, sx = off < .6 ? 1 : lerp(1, 0, ei((off - .6) / .4)); g.translate(W / 2, PY + PH / 2); g.scale(Math.max(sx, .001), sy); g.translate(-W / 2, -(PY + PH / 2)); }
  E.panel(g, PX, PY, PW, PH, { r: 96, fill: '#0B0C10', shadow: 1.2 });
  g.save(); rr(g, SX, SY, SW, SH, 84); g.clip(); g.drawImage(screen, SX, SY, SW, SH); g.restore();
  g.fillStyle = '#000'; g.beginPath(); g.arc(W / 2, SY + 34, 11, 0, TAU); g.fill();
  g.setTransform(1, 0, 0, 1, 0, 0);
  if (off > 0) E.glow(g, W / 2, H / 2, 300 * (1 - off * .5), PAL.accent, .5 * off);
  // the orange screen collapses back into the widget
  const k = seg(t, T.back, T.back + .4);
  if (t >= T.back && k < 1) { const e = eio(k), p0 = cam.proj(SX + 50 / AS, SY + 140 / AS), p1 = cam.proj(SX + (AW - 50) / AS, SY + 540 / AS); g.fillStyle = rgba(PAL.accent, 1 - ss(seg(k, .7, 1))); rr(g, lerp(0, p0.x, e), lerp(0, p0.y, e), lerp(W, p1.x - p0.x, e), lerp(H, p1.y - p0.y, e), lerp(0, 50, e)); g.fill(); }
  E.vignette(g, .5);
}
function sceneNumber(g, t) {
  g.setTransform(1, 0, 0, 1, 0, 0); g.fillStyle = PAL.accent; g.fillRect(0, 0, W, H);
  const gr = g.createRadialGradient(W / 2, H * .4, 0, W / 2, H * .4, H * .7); gr.addColorStop(0, 'rgba(255,255,255,.18)'); gr.addColorStop(1, 'rgba(0,0,0,.18)'); g.fillStyle = gr; g.fillRect(0, 0, W, H);
  const k = T.orange;
  E.scramble(g, '21', W / 2, 1060, t, [k + .3, k + .45], { px: 600, wt: 700, fam: 'disp', color: '#0A0B0F', align: 'center', tracking: -.05, start: k });
  text(g, 'DÍAS SEGUIDOS', W / 2, 1190, 44, { fam: 'mono', wt: 600, color: 'rgba(10,11,15,.82)', align: 'center', track: .2, a: 1, id: 'dias' });
  if (t > k + .45) TX.push({ id: 'n21', text: '21', x0: 240, y0: 620, x1: 840, y1: 1070 });
}
function sceneLogo(g, t) {
  E.bg(g); E.grid(g, { ox: W / 2, oy: H / 2, alpha: .5 });
  const p = seg(t, T.logo, T.logo + .35); if (p > 0) mark(g, W / 2, 640, Math.max(1, 230 * back(p, 1.8)));
  const a = ss(seg(t, T.logo + .1, T.logo + .3));
  g.save(); g.translate(0, (1 - eo(seg(t, T.logo + .1, T.logo + .4))) * 18);
  text(g, 'Fogata', W / 2, 950, 170, { fam: 'disp', wt: 700, align: 'center', track: -.035, a, id: 'title' });
  text(g, 'Mantén vivo tu fuego.', W / 2, 1040, 52, { color: PAL.dim, align: 'center', a: ss(seg(t, T.logo + .2, T.logo + .35)), id: 'tag' });
  g.restore();
  // call to action
  const c = back(seg(t, 22.05, 22.35), 1.6);
  if (c > 0) {
    g.save(); g.translate(W / 2, 1210); g.scale(c, c);
    g.shadowColor = rgba(PAL.accent, .7); g.shadowBlur = 40; g.fillStyle = PAL.accent; rr(g, -330, -58, 660, 116, 58); g.fill(); g.shadowColor = 'transparent';
    text(g, 'Gratis en Android', 0, 16, 50, { wt: 700, color: '#120A04', align: 'center', id: 'cta' });
    g.restore();
    text(g, 'LINK EN LA BIO', W / 2, 1330, 28, { fam: 'mono', wt: 500, color: PAL.dim, align: 'center', track: .25, a: ss(seg(t, 22.2, 22.4)), id: 'bio' });
  }
  // the two apps it lights up
  const q = ss(seg(t, T.logo + .45, T.logo + .75));
  if (q > 0) { g.save(); g.globalAlpha = q; iconValQ(g, W / 2 - 100, 1520, 120); iconBit(g, W / 2 + 100, 1520, 120); g.restore();
    text(g, 'Val Q', W / 2 - 100, 1625, 30, { align: 'center', color: PAL.dim, a: q, id: 'iq' }); text(g, 'Bitácora', W / 2 + 100, 1625, 30, { align: 'center', color: PAL.dim, a: q, id: 'ib' }); }
  E.vignette(g, .5);
}
function captions(g, t) {
  g.setTransform(1, 0, 0, 1, 0, 0);
  for (const c of CAPS) { if (t < c.t0 || t > c.t1) continue; const a = Math.min(seg(t, c.t0, c.t0 + .14), 1 - seg(t, c.t1 - .1, c.t1)); E.toast(g, c.text, a, { y: 1850, px: c.text.length > 44 ? 32 : 36, dense: 1 }); }
}
export function renderFilm(g, t) {
  MAIN = g; TX = [];
  g.setTransform(1, 0, 0, 1, 0, 0); g.globalAlpha = 1; g.globalCompositeOperation = 'source-over';
  if (t < T.press) sceneHook(g, t);
  else if (t < T.phone) sceneFlame(g, t);
  else if (t < T.orange || (t >= T.back && t < T.logo)) phoneScene(g, t);
  else if (t < T.back) sceneNumber(g, t);
  else sceneLogo(g, t);
  captions(g, t);
}
export const texts = () => TX;

export function buildEvents() {
  const ev = [];
  CARDS.forEach(c => ev.push({ t: c.t, type: 'card', pan: clamp((c.x - W / 2) / 900, -1, 1) }));
  stampT.forEach(t => ev.push({ t, type: 'stamp' })); ev.push({ t: T.jueves, type: 'stamp', red: 1 });
  ev.push({ t: T.press, type: 'press' }, { t: T.press + .4, type: 'name' });
  CARDS.forEach((c, i) => { const d = Math.hypot(c.x - W / 2, c.y - H / 2) / 1800; if (i % 4 === 0) ev.push({ t: T.press + .7 + d * .5, type: 'land', k: i }); });
  ev.push({ t: T.phone, type: 'expand' }, { t: T.hiQ, type: 'hi', n: 0 }, { t: T.hiB, type: 'hi', n: 1 }, { t: T.tapQ, type: 'tap' }, { t: T.openQ, type: 'open' });
  [0, .1, .2, .3].forEach(d => ev.push({ t: T.openQ + .2 + d, type: 'tick', i: d * 10 }));
  ev.push({ t: T.toB, type: 'swipe' }); [0, .12, .24].forEach(d => ev.push({ t: T.toB + .25 + d, type: 'tick', i: 4 + d * 10 }));
  ev.push({ t: T.dive, type: 'dive' }, { t: T.orange, type: 'digits', d: .45 }, { t: T.orange + .45, type: 'number' });
  ev.push({ t: T.back, type: 'sweep' }, { t: T.notif, type: 'notif' }, { t: T.tap, type: 'tap' }, { t: T.tap + .3, type: 'ring' }, { t: T.tap + .55, type: 'digits', d: .2 }, { t: T.tap + .78, type: 'lock' });
  ev.push({ t: T.off, type: 'crt' }, { t: T.logo, type: 'logo' }, { t: 22.05, type: 'cta' });
  return ev.sort((a, b) => a.t - b.t);
}
