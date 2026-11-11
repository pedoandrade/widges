const { Resvg } = require('@resvg/resvg-js');
const fs = require('fs');
const path = require('path');

const svgPath = path.join(__dirname, 'imgs', 'icone_app_widgets.svg');
const svgData = fs.readFileSync(svgPath);

// target dp size for launcher icon (48dp baseline)
const baseSize = 48; // mdpi
const densities = {
  mdpi: 1,
  hdpi: 1.5,
  xhdpi: 2,
  xxhdpi: 3,
  xxxhdpi: 4,
};

for (const [density, scale] of Object.entries(densities)) {
  const size = Math.round(baseSize * scale);
  const resvg = new Resvg(svgData, { fitTo: { mode: 'width', value: size } });
  const pngData = resvg.render().asPng();
  const outDir = path.join(__dirname, 'app', 'src', 'main', 'res', `mipmap-${density}`);
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  const outPath = path.join(outDir, 'ic_launcher.png');
  fs.writeFileSync(outPath, pngData);
  // also write round version (same image for simplicity)
  const outRound = path.join(outDir, 'ic_launcher_round.png');
  fs.writeFileSync(outRound, pngData);
  console.log(`Generated ${outPath}`);
}
