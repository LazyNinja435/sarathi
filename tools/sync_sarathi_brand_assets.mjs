import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const sharedBrand = path.join(repoRoot, "shared", "brand");
const webBrand = path.join(repoRoot, "web", "apps", "frontend", "public", "brand");
const androidDrawables = path.join(repoRoot, "android", "app", "src", "main", "res", "drawable-nodpi");

fs.mkdirSync(sharedBrand, { recursive: true });
fs.mkdirSync(webBrand, { recursive: true });

const copies = [
  {
    from: path.join(sharedBrand, "sarathi-logo.png"),
    to: path.join(webBrand, "sarathi-logo.png")
  },
  {
    from: path.join(sharedBrand, "sarathi-logo.png"),
    to: path.join(androidDrawables, "ic_launcher_logo_foreground.png")
  },
  {
    from: path.join(repoRoot, "android", "app", "src", "main", "res", "drawable-nodpi", "splash_lotus_icon.png"),
    to: path.join(webBrand, "splash-lotus-icon.png")
  },
  {
    from: path.join(repoRoot, "android", "app", "src", "main", "res", "drawable-nodpi", "splash_divider_lotus.png"),
    to: path.join(webBrand, "splash-divider-lotus.png")
  }
];

for (const copy of copies) {
  fs.copyFileSync(copy.from, copy.to);
}

console.log("Synced Sarathi brand assets.");
