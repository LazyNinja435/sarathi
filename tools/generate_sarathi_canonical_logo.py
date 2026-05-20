from pathlib import Path
from PIL import Image


REPO_ROOT = Path(__file__).resolve().parents[1]
DRAWABLES = REPO_ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
OUT = REPO_ROOT / "shared" / "brand" / "sarathi-logo.png"


def resize_width(image: Image.Image, width: int) -> Image.Image:
    ratio = width / image.width
    return image.resize((width, round(image.height * ratio)), Image.Resampling.LANCZOS)


def resize_exact(image: Image.Image, width: int, height: int) -> Image.Image:
    return image.resize((width, height), Image.Resampling.LANCZOS)


def alpha_tint(image: Image.Image, opacity: float) -> Image.Image:
    source = image.convert("RGBA")
    tinted = Image.new("RGBA", source.size, (0, 0, 0, 0))
    alpha = source.getchannel("A").point(lambda value: int(value * opacity))
    tinted.putalpha(alpha)
    return tinted


def paste_centered(canvas: Image.Image, image: Image.Image, center_x: float, center_y: float) -> None:
    x = round(center_x - image.width / 2)
    y = round(center_y - image.height / 2)
    canvas.alpha_composite(image, (x, y))


def main() -> None:
    flute = Image.open(DRAWABLES / "splash_hero_flute.png").convert("RGBA")
    feather = Image.open(DRAWABLES / "splash_hero_feather.png").convert("RGBA")

    # Mirrors SplashCenterLogo: a wide box whose base is the height.
    base = 900
    wide_width = round(base * 1.9)
    wide_height = base
    cx = wide_width / 2
    cy = wide_height / 2
    wide = Image.new("RGBA", (wide_width, wide_height), (0, 0, 0, 0))

    flute_img = resize_width(flute, round(base * 1.6))
    paste_centered(wide, flute_img, cx, cy + base * 0.08)

    shadow = resize_exact(feather, round(base * 0.93), round(base * 1.33))
    shadow = alpha_tint(shadow, 0.28)
    shadow = shadow.rotate(15, resample=Image.Resampling.BICUBIC, expand=True)
    paste_centered(wide, shadow, cx + base * -0.21, cy + base * -0.11)

    feather_img = resize_exact(feather, round(base * 0.93), round(base * 1.33))
    feather_img = feather_img.rotate(15, resample=Image.Resampling.BICUBIC, expand=True)
    paste_centered(wide, feather_img, cx + base * -0.26, cy + base * -0.17)

    bbox = wide.getbbox()
    if bbox is None:
        raise RuntimeError("Generated logo is empty")
    cropped = wide.crop(bbox)

    square_size = 1024
    margin = 96
    scale = min((square_size - margin * 2) / cropped.width, (square_size - margin * 2) / cropped.height)
    logo = cropped.resize((round(cropped.width * scale), round(cropped.height * scale)), Image.Resampling.LANCZOS)
    square = Image.new("RGBA", (square_size, square_size), (0, 0, 0, 0))
    square.alpha_composite(logo, ((square_size - logo.width) // 2, (square_size - logo.height) // 2))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    square.save(OUT)
    print(f"Generated {OUT}")


if __name__ == "__main__":
    main()
