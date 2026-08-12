// Generate placeholder APP icons (zakka vinyl motif) via sharp.
// Replace with 小南 mascot art later.
'use strict';
const sharp = require('sharp');
const path = require('path');
const fs = require('fs');

const OUT = path.join(__dirname, '..', 'icons');
fs.mkdirSync(OUT, { recursive: true });

const CREAM = [250, 243, 224];
const TERRACOTTA = [199, 125, 94];
const BROWN = [88, 60, 40];

function svgIcon(size, maskable) {
  const s = size;
  const pad = maskable ? s * 0.1 : 0;
  const r = s * 0.34;
  const cx = s / 2, cy = s / 2;
  const bg = maskable
    ? `<rect width="${s}" height="${s}" fill="rgb(${CREAM.join(',')})"/>`
    : `<rect width="${s}" height="${s}" rx="${s * 0.18}" fill="rgb(${CREAM.join(',')})"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${s}" height="${s}" viewBox="0 0 ${s} ${s}">
    ${bg}
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="rgb(${TERRACOTTA.join(',')})"/>
    <circle cx="${cx}" cy="${cy}" r="${r - s * 0.05}" fill="none" stroke="rgb(${CREAM.join(',')})" stroke-width="${s * 0.02}"/>
    <circle cx="${cx}" cy="${cy}" r="${s * 0.1}" fill="rgb(${BROWN.join(',')})"/>
    <ellipse cx="${cx + s * 0.08}" cy="${cy - r + s * 0.22}" rx="${s * 0.045}" ry="${s * 0.035}" fill="rgb(${CREAM.join(',')})"/>
    <rect x="${cx + s * 0.125}" y="${cy - r + s * 0.02}" width="${s * 0.045}" height="${s * 0.24}" rx="${s * 0.02}" fill="rgb(${CREAM.join(',')})"/>
    <rect x="${cx + s * 0.125}" y="${cy - r}" width="${s * 0.16}" height="${s * 0.05}" rx="${s * 0.02}" fill="rgb(${CREAM.join(',')})"/>
  </svg>`;
}

async function main() {
  const jobs = [
    ['icon-192.png', 192, false],
    ['icon-512.png', 512, false],
    ['icon-maskable-512.png', 512, true],
    ['apple-touch-icon.png', 180, false]
  ];
  for (const [name, size, maskable] of jobs) {
    await sharp(Buffer.from(svgIcon(size, maskable))).png().toFile(path.join(OUT, name));
    console.log('generated', name);
  }
}
main().catch((e) => { console.error(e); process.exit(1); });
