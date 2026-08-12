// gen_xiaonan_icons.js — 用小南吉祥物生成安卓全套 launcher 图标
// 源图: mascot-v1_1-tele (1024x1024 暖色插画, 主体人物弹吉他居中偏左)
// 产物:
//   - mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png  (方形, 带圆角可选)
//   - 同上 ic_launcher_round.png  (圆形)
//   - drawable-nodpi/ic_launcher_foreground.png  (adaptive foreground, 108dp 安全区)
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const SRC = '/tmp/xiaonan.png';
const OUT_DIR = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'res');

// 各密度方形图标尺寸
const SQUARE_SIZES = {
  mdpi: 48,
  hdpi: 72,
  xhdpi: 96,
  xxhdpi: 144,
  xxxhdpi: 192,
};

// adaptive icon foreground: 108dp, 安全区是中心 66dp (约 61%)，四周留白给系统裁剪
const FOREGROUND_SIZE = 432; // 108dp * 4 (xxxhdpi 参考, 存 drawable-nodpi 用大图)

function ensureDir(p) {
  fs.mkdirSync(p, { recursive: true });
}

async function generate() {
  // 读取源图元信息
  const meta = await sharp(SRC).metadata();
  console.log('源图:', meta.width, 'x', meta.height);

  // 1. 方形图标: 直接等比缩放整图(背景是奶油色, 与品牌一致), 加轻微圆角更精致
  for (const [dpi, px] of Object.entries(SQUARE_SIZES)) {
    const dir = path.join(OUT_DIR, `mipmap-${dpi}`);
    ensureDir(dir);
    // 方形
    await sharp(SRC).resize(px, px, { fit: 'cover', position: 'centre' })
      .png().toFile(path.join(dir, 'ic_launcher.png'));
    // 圆形 (mask)
    const circleMask = Buffer.from(
      `<svg width="${px}" height="${px}"><circle cx="${px/2}" cy="${px/2}" r="${px/2}" fill="#fff"/></svg>`
    );
    await sharp(SRC).resize(px, px, { fit: 'cover', position: 'centre' })
      .composite([{ input: circleMask, blend: 'dest-in' }])
      .png().toFile(path.join(dir, 'ic_launcher_round.png'));
    console.log(`  ✓ mipmap-${dpi} (${px}px) 方形+圆形`);
  }

  // 2. adaptive icon foreground: 缩放主体到安全区中心
  //    安全区约 66/108 = 61%, 前景图主体放在中心 66% 区域内
  //    做法: 把源图缩放到 FOREGROUND_SIZE 的 61% 居中, 四周透明
  const fgDir = path.join(OUT_DIR, 'drawable-nodpi');
  ensureDir(fgDir);
  const inner = Math.round(FOREGROUND_SIZE * 0.72); // 主体占 72%, 略大于安全区以便圆形裁切后不露边
  const fg = await sharp(SRC)
    .resize(inner, inner, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer();
  // 放到 432x432 透明画布居中
  const pad = Math.round((FOREGROUND_SIZE - inner) / 2);
  await sharp({
    create: {
      width: FOREGROUND_SIZE,
      height: FOREGROUND_SIZE,
      channels: 4,
      background: { r: 0, g: 0, b: 0, alpha: 0 },
    },
  }).composite([{ input: fg, left: pad, top: pad }])
    .png().toFile(path.join(fgDir, 'ic_launcher_foreground.png'));
  console.log(`  ✓ drawable-nodpi/ic_launcher_foreground.png (${FOREGROUND_SIZE}px, 透明安全区)`);

  console.log('\n全部图标生成完成');
}

generate().catch(e => { console.error('失败:', e); process.exit(1); });
