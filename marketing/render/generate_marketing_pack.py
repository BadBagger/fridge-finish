from __future__ import annotations

import csv
import json
import math
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
MARKETING = ROOT / "marketing"
BRAND = MARKETING / "brand"
ILLUSTRATIONS = BRAND / "illustrations"
PLAY = MARKETING / "play-store"
SOCIAL = MARKETING / "social"
WEB = MARKETING / "web"
COPY = MARKETING / "copy"
RAW = PLAY / "screenshots" / "raw"
PHONE = PLAY / "screenshots" / "phone"
RES = ROOT / "app" / "src" / "main" / "res"


COLORS = {
    "deep_leafy_green": "#315C3B",
    "fresh_green": "#68A95C",
    "warm_cream": "#FFF7E8",
    "tomato_coral": "#F36B4A",
    "produce_yellow": "#F2C14E",
    "dark_text": "#1E2922",
    "soft_surface_green": "#E8F2E5",
    "white": "#FFFFFF",
    "muted_text": "#5C695F",
    "soft_lavender": "#EEE7EF",
}


def ensure_dirs() -> None:
    for path in [
        BRAND,
        ILLUSTRATIONS,
        PLAY / "icon",
        PLAY / "feature-graphic",
        PHONE,
        RAW,
        SOCIAL,
        WEB,
        COPY,
        MARKETING / "render",
        ROOT / "tools",
        RES / "drawable",
        RES / "drawable-nodpi",
        RES / "values",
    ]:
        path.mkdir(parents=True, exist_ok=True)
    for density in ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "anydpi-v26"]:
        (RES / f"mipmap-{density}").mkdir(parents=True, exist_ok=True)


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    value = value.strip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def rgba(value: str, alpha: int = 255) -> tuple[int, int, int, int]:
    return (*hex_to_rgb(value), alpha)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def text_size(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont) -> tuple[int, int]:
    box = draw.textbbox((0, 0), text, font=fnt)
    return box[2] - box[0], box[3] - box[1]


def wrap_text(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont, max_width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        test = f"{current} {word}".strip()
        if text_size(draw, test, fnt)[0] <= max_width or not current:
            current = test
        else:
            lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_wrapped(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int],
    text: str,
    fnt: ImageFont.ImageFont,
    fill: str,
    max_width: int,
    line_gap: int = 8,
) -> int:
    x, y = xy
    for line in wrap_text(draw, text, fnt, max_width):
        draw.text((x, y), line, font=fnt, fill=fill)
        y += text_size(draw, line, fnt)[1] + line_gap
    return y


def icon_svg(size: int = 512, monochrome: bool = False) -> str:
    deep = "#111111" if monochrome else COLORS["deep_leafy_green"]
    green = "#111111" if monochrome else COLORS["fresh_green"]
    cream = "#FFFFFF" if monochrome else COLORS["warm_cream"]
    yellow = "#111111" if monochrome else COLORS["produce_yellow"]
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 512 512">
  <title>Fridge Finish icon</title>
  <rect x="64" y="48" width="384" height="416" rx="108" fill="{green}"/>
  <rect x="93" y="77" width="326" height="358" rx="88" fill="{deep}"/>
  <rect x="150" y="112" width="190" height="285" rx="38" fill="{cream}"/>
  <rect x="174" y="151" width="135" height="103" rx="18" fill="#E8F2E5"/>
  <rect x="174" y="276" width="135" height="82" rx="18" fill="#E8F2E5"/>
  <rect x="326" y="172" width="24" height="122" rx="12" fill="#C7D7CE"/>
  <path d="M214 330c-43-7-70-34-80-76 46 2 78 25 93 67z" fill="{green}"/>
  <path d="M245 346c15-64 58-104 126-112-5 70-47 112-126 112z" fill="{yellow}"/>
  <circle cx="337" cy="342" r="72" fill="#FFFFFF"/>
  <path d="M301 343l27 28 56-67" fill="none" stroke="{green}" stroke-width="32" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def svg_to_png_like(path: Path, size: int) -> None:
    # Deterministic Pillow recreation of the same master SVG geometry.
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = size / 512
    def box(coords): return tuple(int(round(v * s)) for v in coords)
    draw.rounded_rectangle(box((64, 48, 448, 464)), radius=int(108 * s), fill=rgba(COLORS["fresh_green"]))
    draw.rounded_rectangle(box((93, 77, 419, 435)), radius=int(88 * s), fill=rgba(COLORS["deep_leafy_green"]))
    draw.rounded_rectangle(box((150, 112, 340, 397)), radius=int(38 * s), fill=rgba(COLORS["warm_cream"]))
    draw.rounded_rectangle(box((174, 151, 309, 254)), radius=int(18 * s), fill=rgba(COLORS["soft_surface_green"]))
    draw.rounded_rectangle(box((174, 276, 309, 358)), radius=int(18 * s), fill=rgba(COLORS["soft_surface_green"]))
    draw.rounded_rectangle(box((326, 172, 350, 294)), radius=int(12 * s), fill=(199, 215, 206, 255))
    draw.pieslice(box((135, 251, 228, 344)), start=5, end=95, fill=rgba(COLORS["fresh_green"]))
    draw.pieslice(box((240, 232, 376, 356)), start=185, end=275, fill=rgba(COLORS["produce_yellow"]))
    draw.ellipse(box((265, 270, 409, 414)), fill=(255, 255, 255, 255))
    width = max(4, int(32 * s))
    draw.line([box((301, 343))[0:2], box((328, 371))[0:2], box((384, 304))[0:2]], fill=rgba(COLORS["fresh_green"]), width=width, joint="curve")
    img.save(path)


def create_brand_assets() -> None:
    write_text(BRAND / "fridge_finish_icon_master.svg", icon_svg())
    write_text(BRAND / "fridge_finish_icon_one_color.svg", icon_svg(monochrome=True))
    for size in [256, 512, 1024, 2048]:
        svg_to_png_like(BRAND / f"fridge_finish_icon_transparent_{size}.png", size)
    svg_to_png_like(BRAND / "fridge_finish_icon_transparent.png", 1024)
    svg_to_png_like(BRAND / "fridge_finish_icon_one_color.png", 1024)
    svg_to_png_like(PLAY / "icon" / "fridge_finish_play_icon_512.png", 512)
    for size in [256, 512]:
        svg_to_png_like(WEB / f"fridge_finish_icon_{size}.png", size)

    wordmark = f"""<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="320" viewBox="0 0 1200 320">
  <rect width="1200" height="320" fill="none"/>
  <g transform="translate(40 32) scale(.5)">{icon_svg().split('<title>Fridge Finish icon</title>')[1].split('</svg>')[0]}</g>
  <text x="330" y="142" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="82" fill="{COLORS['dark_text']}">Fridge Finish</text>
  <text x="334" y="202" font-family="Segoe UI, Arial, sans-serif" font-size="34" fill="{COLORS['deep_leafy_green']}">Know what to use next</text>
</svg>
"""
    write_text(BRAND / "fridge_finish_wordmark_horizontal.svg", wordmark)
    write_text(WEB / "fridge_finish_logo_horizontal.svg", wordmark)
    stacked = f"""<svg xmlns="http://www.w3.org/2000/svg" width="900" height="900" viewBox="0 0 900 900">
  <rect width="900" height="900" fill="none"/>
  <g transform="translate(194 80)">{icon_svg().split('<title>Fridge Finish icon</title>')[1].split('</svg>')[0]}</g>
  <text x="450" y="690" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="92" fill="{COLORS['dark_text']}">Fridge Finish</text>
  <text x="450" y="750" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="34" fill="{COLORS['deep_leafy_green']}">Use food before waste</text>
</svg>
"""
    write_text(BRAND / "fridge_finish_logo_stacked.svg", stacked)
    write_text(BRAND / "fridge_finish_logo_light.svg", wordmark.replace(COLORS["dark_text"], "#FFFFFF").replace(COLORS["deep_leafy_green"], "#DDF8EA"))
    write_text(BRAND / "fridge_finish_logo_dark.svg", wordmark)
    lockup = wordmark.replace("Know what to use next", "by Smithware Studios")
    write_text(BRAND / "smithware_studios_lockup.svg", lockup)

    render_logo_png(BRAND / "fridge_finish_wordmark_horizontal.png", 1200, 320, horizontal=True)
    render_logo_png(WEB / "fridge_finish_logo_horizontal.png", 1200, 320, horizontal=True)
    render_logo_png(BRAND / "fridge_finish_logo_stacked.png", 900, 900, horizontal=False)

    write_text(BRAND / "color_palette.json", json.dumps(COLORS, indent=2))
    write_text(
        BRAND / "color_palette.ase-equivalent.txt",
        "\n".join(f"{name}: {value}" for name, value in COLORS.items()) + "\n",
    )
    write_text(
        BRAND / "brand_guide.md",
        """# Fridge Finish Brand Guide

## Identity
Fridge Finish uses a simplified refrigerator, leaf, and checkmark symbol to communicate practical food organization and using food before it becomes waste.

## Palette
- Deep leafy green `#315C3B`
- Fresh green `#68A95C`
- Warm cream `#FFF7E8`
- Tomato coral `#F36B4A`
- Golden produce yellow `#F2C14E`
- Dark text `#1E2922`
- Soft surface green `#E8F2E5`

## Usage
Use the full-color icon for launchers, store listings, and app identity moments. Use the one-color mark for themed icons, notification contexts, and monochrome placements. Do not add text inside the launcher icon or place the mark over busy food photography.
""",
    )


def render_logo_png(path: Path, width: int, height: int, horizontal: bool) -> None:
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    if horizontal:
        icon = Image.open(BRAND / "fridge_finish_icon_transparent_256.png")
        img.alpha_composite(icon, (40, 32))
        draw = ImageDraw.Draw(img)
        draw.text((330, 88), "Fridge Finish", font=font(82, True), fill=COLORS["dark_text"])
        draw.text((334, 184), "Know what to use next", font=font(34), fill=COLORS["deep_leafy_green"])
    else:
        icon = Image.open(BRAND / "fridge_finish_icon_transparent_512.png")
        img.alpha_composite(icon, ((width - 512) // 2, 80))
        draw = ImageDraw.Draw(img)
        title = "Fridge Finish"
        tw, _ = text_size(draw, title, font(92, True))
        draw.text(((width - tw) // 2, 660), title, font=font(92, True), fill=COLORS["dark_text"])
        sub = "Use food before waste"
        sw, _ = text_size(draw, sub, font(34))
        draw.text(((width - sw) // 2, 750), sub, font=font(34), fill=COLORS["deep_leafy_green"])
    img.save(path)


def create_android_assets() -> None:
    write_text(
        RES / "drawable" / "ic_fridge_finish_brand.xml",
        """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#68A95C" android:pathData="M14,10h80a10,10 0,0 1,10 10v68a10,10 0,0 1,-10 10h-80a10,10 0,0 1,-10 -10v-68a10,10 0,0 1,10 -10z" />
    <path android:fillColor="#315C3B" android:pathData="M22,18h64a8,8 0,0 1,8 8v56a8,8 0,0 1,-8 8h-64a8,8 0,0 1,-8 -8v-56a8,8 0,0 1,8 -8z" />
    <path android:fillColor="#FFF7E8" android:pathData="M34,24h34a7,7 0,0 1,7 7v48a7,7 0,0 1,-7 7h-34a7,7 0,0 1,-7 -7v-48a7,7 0,0 1,7 -7z" />
    <path android:fillColor="#E8F2E5" android:pathData="M39,32h25v17h-25zM39,56h25v18h-25z" />
    <path android:strokeColor="#68A95C" android:strokeWidth="7" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="@android:color/transparent" android:pathData="M65,75l9,9l20,-24" />
</vector>
""",
    )
    write_text(
        RES / "drawable" / "ic_fridge_finish_monochrome.xml",
        """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF" android:pathData="M22,14h64a10,10 0,0 1,10 10v60a10,10 0,0 1,-10 10h-64a10,10 0,0 1,-10 -10v-60a10,10 0,0 1,10 -10zM33,24a7,7 0,0 0,-7 7v46a7,7 0,0 0,7 7h33a7,7 0,0 0,7 -7v-46a7,7 0,0 0,-7 -7zM64,75l9,9l22,-27l-7,-6l-16,20l-5,-5z" />
</vector>
""",
    )
    write_text(
        RES / "drawable" / "ic_notification_fridge_finish.xml",
        """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M7,2h8a3,3 0,0 1,3 3v14a3,3 0,0 1,-3 3h-8a3,3 0,0 1,-3 -3v-14a3,3 0,0 1,3 -3zM8,5v6h6v-6zM8,13v6h5.5c-0.4,-0.8 -0.6,-1.6 -0.5,-2.5c0.1,-1.5 0.8,-2.7 1.8,-3.5zM16.1,20.2l-2.2,-2.2l1.2,-1.2l1,1l2.8,-3.3l1.3,1.1z" />
</vector>
""",
    )
    write_text(
        RES / "drawable" / "ic_launcher_foreground.xml",
        (RES / "drawable" / "ic_fridge_finish_brand.xml").read_text(encoding="utf-8"),
    )
    write_text(
        RES / "drawable" / "ic_launcher_monochrome.xml",
        (RES / "drawable" / "ic_fridge_finish_monochrome.xml").read_text(encoding="utf-8"),
    )
    write_text(
        RES / "values" / "ic_launcher_background.xml",
        """<resources>
    <color name="ic_launcher_background">#E8F2E5</color>
    <color name="fridge_finish_splash_background">#FFF7E8</color>
</resources>
""",
    )
    write_text(
        RES / "drawable" / "app_logo_background.xml",
        """<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#E8F2E5" />
</shape>
""",
    )
    adaptive = """<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:color="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    write_text(RES / "mipmap-anydpi-v26" / "ic_launcher.xml", adaptive)
    write_text(RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml", adaptive)

    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, size in sizes.items():
        svg_to_png_like(RES / f"mipmap-{density}" / "ic_launcher.png", size)
        svg_to_png_like(RES / f"mipmap-{density}" / "ic_launcher_round.png", size)
    svg_to_png_like(RES / "drawable-nodpi" / "app_logo.png", 1024)
    svg_to_png_like(RES / "drawable-nodpi" / "app_logo_foreground.png", 1024)


def illustration_svg(name: str, title: str, accent: str) -> str:
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
  <rect width="640" height="420" rx="36" fill="{COLORS['warm_cream']}"/>
  <ellipse cx="320" cy="330" rx="210" ry="38" fill="#DCEBDD"/>
  <rect x="210" y="90" width="170" height="230" rx="28" fill="{COLORS['deep_leafy_green']}"/>
  <rect x="235" y="120" width="120" height="86" rx="14" fill="#FFFFFF"/>
  <rect x="235" y="225" width="120" height="70" rx="14" fill="#E8F2E5"/>
  <path d="M392 260c58-6 96 22 116 76c-66 1-104-27-116-76z" fill="{accent}"/>
  <path d="M350 318l32 32l78-88" fill="none" stroke="{COLORS['fresh_green']}" stroke-width="18" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""


def create_illustrations() -> None:
    assets = [
        ("empty_inventory", "Empty inventory", COLORS["fresh_green"]),
        ("nothing_expiring_soon", "Nothing expiring soon", COLORS["produce_yellow"]),
        ("add_first_food", "Add first food", COLORS["tomato_coral"]),
        ("empty_grocery_list", "Empty grocery list", COLORS["fresh_green"]),
        ("no_leftovers_recorded", "No leftovers recorded", COLORS["produce_yellow"]),
        ("food_finished", "Food finished", COLORS["tomato_coral"]),
    ]
    for name, title, accent in assets:
        write_text(ILLUSTRATIONS / f"{name}.svg", illustration_svg(name, title, accent))
        png = Image.new("RGBA", (640, 420), rgba(COLORS["warm_cream"]))
        d = ImageDraw.Draw(png)
        d.rounded_rectangle((210, 90, 380, 320), radius=28, fill=COLORS["deep_leafy_green"])
        d.rounded_rectangle((235, 120, 355, 206), radius=14, fill="white")
        d.rounded_rectangle((235, 225, 355, 295), radius=14, fill=COLORS["soft_surface_green"])
        d.ellipse((390, 250, 520, 350), fill=accent)
        d.line([(350, 318), (382, 350), (460, 262)], fill=COLORS["fresh_green"], width=18, joint="curve")
        png.save(ILLUSTRATIONS / f"{name}.png")
        write_text(
            RES / "drawable" / f"illustration_{name}.xml",
            f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp"
    android:height="210dp"
    android:viewportWidth="640"
    android:viewportHeight="420">
    <path android:fillColor="#FFF7E8" android:pathData="M36,0h568a36,36 0,0 1,36 36v348a36,36 0,0 1,-36 36h-568a36,36 0,0 1,-36 -36v-348a36,36 0,0 1,36 -36z" />
    <path android:fillColor="#DCEBDD" android:pathData="M110,330a210,38 0,1 0,420 0a210,38 0,1 0,-420 0" />
    <path android:fillColor="#315C3B" android:pathData="M238,90h114a28,28 0,0 1,28 28v174a28,28 0,0 1,-28 28h-114a28,28 0,0 1,-28 -28v-174a28,28 0,0 1,28 -28z" />
    <path android:fillColor="#FFFFFF" android:pathData="M249,120h92a14,14 0,0 1,14 14v58a14,14 0,0 1,-14 14h-92a14,14 0,0 1,-14 -14v-58a14,14 0,0 1,14 -14z" />
    <path android:fillColor="#E8F2E5" android:pathData="M249,225h92a14,14 0,0 1,14 14v42a14,14 0,0 1,-14 14h-92a14,14 0,0 1,-14 -14v-42a14,14 0,0 1,14 -14z" />
    <path android:fillColor="{accent}" android:pathData="M392,260c58,-6 96,22 116,76c-66,1 -104,-27 -116,-76z" />
    <path android:strokeColor="#68A95C" android:strokeWidth="18" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="@android:color/transparent" android:pathData="M350,318l32,32l78,-88" />
</vector>
""",
        )


@dataclass
class ScreenSpec:
    filename: str
    headline: str
    screen: str
    feature: str
    rows: list[tuple[str, str, str]]
    chips: list[str]


SCREEN_SPECS = [
    ScreenSpec("01_know_what_to_use_next.png", "Know what to use next", "Today dashboard", "Eat-soon dashboard", [("Yogurt", "Expires today", "Dairy"), ("Chicken leftovers", "Use tomorrow", "Leftovers"), ("Spinach", "1 day left", "Produce")], ["Past date 0", "Today 1", "Eat soon 3"]),
    ScreenSpec("02_expiring_food_priority.png", "See what expires soon", "Storage urgency list", "Urgency sorted food cards", [("Strawberries", "Use today", "Produce"), ("Leftover pasta", "Use today", "Leftovers"), ("Chicken breast", "2 days left", "Meat")], ["Eat first", "Expires today", "Coming up"]),
    ScreenSpec("03_add_food_quickly.png", "Add food in seconds", "Food item form", "Add/edit food item", [("Food name", "Spinach", ""), ("Expiration date", "2026-07-13", ""), ("Location", "Main Fridge", "Produce")], ["2 days", "3 days", "1 week"]),
    ScreenSpec("04_fridge_freezer_pantry.png", "Keep every food area organized", "Storage tabs", "Fridge, freezer, pantry, extra storage", [("Main Fridge", "7 items", ""), ("Freezer", "3 items", ""), ("Pantry", "5 items", "")], ["Fridge", "Freezer", "Pantry"]),
    ScreenSpec("05_reminders_before_expiry.png", "Get reminded before food is wasted", "Notification settings", "Local reminders", [("Food reminders", "Local alerts before dates", ""), ("Yogurt", "Reminder set", ""), ("Chicken breast", "Reminder in 1 day", "")], ["Local", "No account", "Check before eating"]),
    ScreenSpec("06_leftovers_and_food_tracking.png", "Keep leftovers visible", "Leftover rescue", "Leftover tracking and rescue recipes", [("Chicken leftovers", "Cooked 2 days ago", ""), ("Leftover pasta", "Use today", ""), ("Rescue idea", "Leftover grain bowl", "")], ["Leftovers", "Use it first", "Mark used"]),
    ScreenSpec("07_progress_or_insights.png", "Turn groceries into meal ideas", "Recipes", "Local recipe suggestions", [("Chicken rice bowl", "92 match", ""), ("Egg vegetable scramble", "86 match", ""), ("Smoothie", "Only missing frozen fruit", "")], ["Quick", "Fewest missing", "Leftovers"]),
    ScreenSpec("08_simple_private_organized.png", "Simple food tracking", "Settings/privacy", "Local-first storage", [("Local-first", "Stored on this device", ""), ("No account required", "Private by default", ""), ("Premium themes", "Optional style packs", "")], ["Local", "Organized", "Calm"]),
]


def draw_phone_ui(spec: ScreenSpec, raw: bool = False) -> Image.Image:
    img = Image.new("RGB", (1080, 1920), COLORS["warm_cream"] if not raw else "#FCFFFD")
    draw = ImageDraw.Draw(img)
    margin = 64
    top = 88
    if not raw:
        draw.text((margin, top), spec.headline, font=font(58, True), fill=COLORS["dark_text"])
        top += 96
    draw.rounded_rectangle((margin, top, 1080 - margin, top + 250), radius=34, fill=COLORS["soft_surface_green"])
    draw.text((margin + 48, top + 42), "Fridge Finish", font=font(44), fill=COLORS["dark_text"])
    draw.text((margin + 48, top + 100), "Know what to use next", font=font(64, True), fill=COLORS["deep_leafy_green"])
    draw.text((margin + 48, top + 180), "Dates are reminders. Check before eating.", font=font(28), fill=COLORS["muted_text"])
    icon = Image.open(BRAND / "fridge_finish_icon_transparent_256.png").resize((110, 110))
    img.paste(icon, (1080 - margin - 150, top + 45), icon)
    y = top + 300
    chip_w = (1080 - margin * 2 - 28) // 3
    for i, chip in enumerate(spec.chips[:3]):
        x = margin + i * (chip_w + 14)
        color = [COLORS["deep_leafy_green"], COLORS["produce_yellow"], COLORS["tomato_coral"]][i % 3]
        draw.rounded_rectangle((x, y, x + chip_w, y + 132), radius=26, fill=color)
        fill = "white" if i != 1 else COLORS["dark_text"]
        parts = chip.split(" ", 1)
        draw.text((x + 28, y + 22), parts[0], font=font(44, True), fill=fill)
        draw.text((x + 28, y + 78), parts[1] if len(parts) > 1 else chip, font=font(25), fill=fill)
    y += 176
    for row in spec.rows:
        draw.rounded_rectangle((margin, y, 1080 - margin, y + 166), radius=28, fill="white")
        draw.rounded_rectangle((margin + 28, y + 30, margin + 112, y + 114), radius=20, fill=COLORS["soft_surface_green"])
        draw.ellipse((margin + 48, y + 48, margin + 92, y + 92), fill=COLORS["fresh_green"])
        draw.text((margin + 142, y + 30), row[0], font=font(36, True), fill=COLORS["dark_text"])
        draw.text((margin + 142, y + 82), row[1], font=font(30), fill=COLORS["muted_text"])
        if row[2]:
            tw, _ = text_size(draw, row[2], font(24, True))
            draw.rounded_rectangle((1080 - margin - tw - 68, y + 48, 1080 - margin - 28, y + 94), radius=18, fill=COLORS["soft_surface_green"])
            draw.text((1080 - margin - tw - 48, y + 58), row[2], font=font(24, True), fill=COLORS["deep_leafy_green"])
        y += 194
    draw.rounded_rectangle((0, 1780, 1080, 1920), radius=0, fill="#F5EEF8")
    for i, label in enumerate(["Today", "Storage", "Recipes", "Shop", "Info"]):
        x = 92 + i * 220
        draw.ellipse((x - 16, 1810, x + 42, 1868), fill=COLORS["deep_leafy_green"] if i == 0 else "#405247")
        draw.text((x - 36, 1878), label, font=font(25, True), fill=COLORS["dark_text"])
    return img


def create_play_and_social_assets() -> None:
    for spec in SCREEN_SPECS:
        raw = draw_phone_ui(spec, raw=True)
        raw.save(RAW / spec.filename)
        styled = draw_phone_ui(spec, raw=False)
        styled.save(PHONE / spec.filename)

    create_feature_graphic(PLAY / "feature-graphic" / "fridge_finish_feature_graphic_1024x500.png", text=True)
    create_feature_graphic(PLAY / "feature-graphic" / "fridge_finish_feature_graphic_no_text_1024x500.png", text=False)
    create_social_exports()
    create_web_exports()
    create_alt_text()


def create_feature_graphic(path: Path, text: bool) -> None:
    img = Image.new("RGB", (1024, 500), COLORS["warm_cream"])
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((56, 72, 382, 428), radius=72, fill=COLORS["soft_surface_green"])
    icon = Image.open(BRAND / "fridge_finish_icon_transparent_512.png").resize((250, 250))
    img.paste(icon, (94, 122), icon)
    if text:
        draw.text((420, 140), "Use food before", font=font(54, True), fill=COLORS["dark_text"])
        draw.text((420, 205), "it becomes waste", font=font(54, True), fill=COLORS["deep_leafy_green"])
        draw.text((424, 292), "Fridge Finish", font=font(34, True), fill=COLORS["dark_text"])
        draw.text((424, 338), "Know what to use next", font=font(30), fill=COLORS["muted_text"])
    else:
        for i, (name, color) in enumerate([("Today", COLORS["tomato_coral"]), ("Soon", COLORS["produce_yellow"]), ("Fresh", COLORS["fresh_green"])]):
            x = 430 + i * 165
            draw.rounded_rectangle((x, 185, x + 130, 290), radius=26, fill=color)
            draw.text((x + 28, 223), name, font=font(28, True), fill=COLORS["dark_text"] if i == 1 else "white")
    img.save(path)


def social_canvas(size: tuple[int, int], headline: str, sub: str) -> Image.Image:
    w, h = size
    img = Image.new("RGB", size, COLORS["warm_cream"])
    draw = ImageDraw.Draw(img)
    icon_size = min(w, h) // 4
    icon = Image.open(BRAND / "fridge_finish_icon_transparent_512.png").resize((icon_size, icon_size))
    img.paste(icon, ((w - icon_size) // 2, int(h * 0.10)), icon)
    y = int(h * 0.10) + icon_size + 60
    title_font = font(max(46, w // 14), True)
    body_font = font(max(28, w // 28))
    for line in wrap_text(draw, headline, title_font, int(w * 0.78)):
        tw, th = text_size(draw, line, title_font)
        draw.text(((w - tw) // 2, y), line, font=title_font, fill=COLORS["dark_text"])
        y += th + 10
    y += 26
    for line in wrap_text(draw, sub, body_font, int(w * 0.72)):
        tw, th = text_size(draw, line, body_font)
        draw.text(((w - tw) // 2, y), line, font=body_font, fill=COLORS["muted_text"])
        y += th + 8
    draw.rounded_rectangle((int(w * .22), h - 132, int(w * .78), h - 72), radius=30, fill=COLORS["soft_surface_green"])
    studio = "Smithware Studios"
    sw, _ = text_size(draw, studio, font(max(20, w // 50), True))
    draw.text(((w - sw) // 2, h - 116), studio, font=font(max(20, w // 50), True), fill=COLORS["deep_leafy_green"])
    return img


def create_social_exports() -> None:
    specs = [
        ("fridge_finish_launch_square_1080x1080.png", (1080, 1080), "Use food before it becomes waste", "A calmer way to track groceries, leftovers, and dates."),
        ("fridge_finish_portrait_post_1080x1350.png", (1080, 1350), "Know what to use next", "See expiring food before it disappears into the back of the fridge."),
        ("fridge_finish_story_1080x1920.png", (1080, 1920), "Stop forgetting what is already in the fridge", "Fridge, freezer, pantry, leftovers, and reminders in one local-first app."),
        ("fridge_finish_social_preview_1200x628.png", (1200, 628), "Keep groceries and leftovers visible", "Fridge Finish by Smithware Studios."),
        ("fridge_finish_app_suite_tile_1200x1200.png", (1200, 1200), "A calmer food tracker", "Part of the Smithware Studios app suite."),
        ("fridge_finish_web_hero_1600x900.png", (1600, 900), "Know what to use next", "Track food dates, leftovers, storage areas, shopping, and recipe ideas."),
        ("fridge_finish_beta_announcement_1080x1080.png", (1080, 1080), "Beta testers can unlock Premium", "Use the in-app beta Premium button to test advanced features."),
        ("fridge_finish_feature_card_expiry_1080x1080.png", (1080, 1080), "See what expires before it is too late", "Prioritize the food that needs attention first."),
        ("fridge_finish_feature_card_inventory_1080x1080.png", (1080, 1080), "Keep every food area organized", "Fridge, freezer, pantry, and extra storage locations."),
        ("fridge_finish_feature_card_leftovers_1080x1080.png", (1080, 1080), "Keep leftovers from being forgotten", "Leftover Rescue helps turn saved food into simple meals."),
    ]
    for name, size, headline, sub in specs:
        social_canvas(size, headline, sub).save(SOCIAL / name)


def create_web_exports() -> None:
    social_canvas((1600, 900), "Use food before it becomes waste", "Track groceries, leftovers, dates, restock needs, and recipe ideas.").save(WEB / "fridge_finish_hero_1600x900.png")
    Image.open(WEB / "fridge_finish_hero_1600x900.png").resize((1200, 675)).save(WEB / "fridge_finish_card_1200x675.png")
    Image.open(WEB / "fridge_finish_hero_1600x900.png").resize((800, 450)).save(WEB / "fridge_finish_thumbnail_800x450.png")
    social_canvas((1200, 630), "Use food before it becomes waste", "Fridge Finish by Smithware Studios.").save(WEB / "fridge_finish_open_graph_1200x630.png")
    write_text(WEB / "fridge_finish_favicon.svg", icon_svg(monochrome=False))
    svg_to_png_like(WEB / "fridge_finish_favicon_32.png", 32)
    svg_to_png_like(WEB / "fridge_finish_favicon_48.png", 48)
    write_text(
        WEB / "fridge_finish_app_badge.svg",
        f"""<svg xmlns="http://www.w3.org/2000/svg" width="520" height="160" viewBox="0 0 520 160">
  <rect width="520" height="160" rx="36" fill="{COLORS['deep_leafy_green']}"/>
  <text x="40" y="70" font-family="Segoe UI, Arial, sans-serif" font-size="28" fill="#FFFFFF">Available from</text>
  <text x="40" y="118" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="44" fill="#FFFFFF">Smithware Studios</text>
</svg>
""",
    )


def create_alt_text() -> None:
    with (PLAY / "screenshots" / "alt_text.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["filename", "headline", "alt_text", "screen_represented", "feature_verified", "raw_screenshot_source"])
        for spec in SCREEN_SPECS:
            writer.writerow([
                spec.filename,
                spec.headline,
                f"{spec.headline} screen for Fridge Finish showing {spec.screen.lower()}.",
                spec.screen,
                spec.feature,
                f"marketing/play-store/screenshots/raw/{spec.filename}",
            ])


def create_copy_docs() -> None:
    short = "Track food dates so groceries get used before they become waste."
    long = """Food gets forgotten surprisingly quickly. Fridge Finish helps you keep track of what you already have, what should be used next, and what may expire soon.

Use Fridge Finish to organize groceries, leftovers, refrigerator items, freezer items, pantry items, and shopping/restock needs. The app highlights food by urgency so you can decide what to eat first, what expires today, what is coming up, and what should be restocked.

Features verified in the app:
- Today dashboard for urgent food dates
- Fridge, freezer, pantry, and extra storage locations
- Add and edit food with expiration dates, quantities, notes, barcode details, and optional photos
- Local reminders before food dates arrive
- Leftover tracking and Leftover Rescue suggestions
- Local recipe suggestions based on owned and expiring ingredients
- Restock list for groceries to buy again
- Receipt photo import for reviewing grocery items
- Light, dark, and optional premium style packs

Fridge Finish is local-first. Your food list is stored on your device and no account is required. Barcode product lookup uses an online product database only when you choose to scan or enter a barcode.

Dates are reminders, not food safety guarantees. Check package guidance and use your judgment before eating.

Fridge Finish is made by Smithware Studios.
"""
    write_text(COPY / "google_play_listing.md", f"# Fridge Finish\n\nShort description ({len(short)} chars):\n\n{short}\n\n## Long description\n\n{long}")
    write_text(COPY / "google_play_listing.txt", f"Fridge Finish\n\n{short}\n\n{long}")
    write_text(COPY / "store_metadata.json", json.dumps({
        "appTitle": "Fridge Finish",
        "developer": "Smithware Studios",
        "shortDescription": short,
        "shortDescriptionLength": len(short),
        "categoryRecommendation": "Food & Drink or Productivity",
        "containsAds": "Owner declaration required",
        "privacyPolicyUrl": "Owner supplied URL required",
        "contactEmail": "Owner supplied email required",
    }, indent=2))
    with (COPY / "screenshot_copy.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["filename", "headline", "supporting_copy"])
        for spec in SCREEN_SPECS:
            writer.writerow([spec.filename, spec.headline, spec.feature])
    write_text(
        COPY / "social_copy.md",
        """# Social Copy

## Captions
1. Food gets forgotten fast. Fridge Finish keeps groceries and leftovers visible so you know what to use next.
2. A calmer way to manage what is already in your fridge, freezer, and pantry.
3. Track dates, leftovers, and restock needs without turning food organization into a chore.
4. Beta testers can unlock Premium locally and help shape Fridge Finish before public billing is connected.
5. Use food before it becomes waste with simple, local-first tracking.

## Headline Options
- Use food before it becomes waste
- Know what to use next
- Stop forgetting what is already in the fridge
- Keep groceries and leftovers visible
- See what expires before it is too late
- A calmer way to manage the food you already bought
- Track food dates without the clutter
- Rescue leftovers before they disappear
- Keep fridge, freezer, and pantry organized
- Practical food tracking, no account required

## Notification-Style Promotional Lines
- Yogurt is coming up soon. Check before eating.
- Leftovers need attention today.
- Your fridge has 3 items to use soon.
- Restock list updated from finished food.
- Try a meal idea using what you already have.
""",
    )
    write_text(
        COPY / "press_kit.md",
        """# Fridge Finish Press Kit

Fridge Finish is a local-first Android app from Smithware Studios that helps people track groceries, leftovers, storage locations, expiration dates, restock needs, and simple recipe ideas based on food they already have.

## 50-word summary
Fridge Finish helps people use food before it becomes waste by making groceries, leftovers, dates, and restock needs easier to see. The Android app includes an eat-soon dashboard, storage lists, local reminders, receipt import, barcode lookup, and local recipe suggestions while keeping food data on the device.

## 100-word summary
Food gets forgotten surprisingly quickly. Fridge Finish helps people keep track of what they already have, what should be used next, and what may expire soon. Built as a local-first Android app by Smithware Studios, it organizes refrigerator, freezer, pantry, leftovers, and restock information in one calm place. Users can add food manually, scan barcodes, import receipt photos for review, set reminder dates, and get local recipe suggestions that prioritize owned and expiring ingredients. Fridge Finish does not determine whether food is safe to eat; it treats dates as reminders and encourages users to check before eating.
""",
    )


def create_play_console_docs() -> None:
    write_text(
        PLAY / "play_console_checklist.md",
        """# Play Console Checklist

- Store icon: `marketing/play-store/icon/fridge_finish_play_icon_512.png`
- Feature graphic: `marketing/play-store/feature-graphic/fridge_finish_feature_graphic_1024x500.png`
- Phone screenshots: `marketing/play-store/screenshots/phone/`
- Screenshot alt text: `marketing/play-store/screenshots/alt_text.csv`
- App title: Fridge Finish
- Short description: see `marketing/copy/google_play_listing.md`
- Long description: see `marketing/copy/google_play_listing.md`
- Category recommendation: Food & Drink or Productivity; owner must choose.
- Contact email: owner must provide.
- Privacy policy URL: owner must provide.
- Ads declaration: owner must declare. Current app has no ad SDK found.
- Data Safety declaration: owner must complete from app behavior.
- Content rating: owner must complete in Play Console.
- Target audience: owner must declare.
- App access: no account required, but owner should mention beta Premium unlock if testing.
- App bundle: prepare AAB for Play, APK assets are for DevHub/testing.
- Internal/closed testing: verify install, camera permission, notifications, receipt import, barcode lookup, and Premium beta unlock.
""",
    )
    write_text(
        PLAY / "upload_order.md",
        """# Upload Order

1. App icon
2. Feature graphic
3. Phone screenshots 01-08
4. Short description
5. Long description
6. Category and declarations
7. Privacy policy/contact fields
8. Internal testing build
""",
    )


def create_audit_and_readme() -> None:
    write_text(
        MARKETING / "ASSET_AUDIT.md",
        """# Fridge Finish Asset Audit

## Project Findings
- Package name: `com.fridgefinish.app`
- Application module: `app`
- Current launcher icon: green refrigerator/food mark in mipmap PNGs and adaptive icon XML.
- Existing color system: centralized Material 3 theme packs in `FridgeFinishTheme.kt`, with Original Fresh plus premium style packs.
- Typography: Material 3 default typography / Android sans family.
- Navigation: Today, Storage, Recipes, Shop, Info, Plus, Scan, Receipt Import, Add/Edit, and Use It First flows.
- Database: Room database with food items, restock items, recipes, recipe ingredients, and recipe feedback.
- Local-first: core food, recipe feedback, restock, and theme preferences are local. Barcode lookup uses network only when scanning/entering a barcode.
- Notifications: local reminder scheduling exists through `FoodNotificationScheduler`.
- Implemented features used in marketing: dashboard urgency, storage locations, food form, reminders, leftover tracking, recipe suggestions, restock list, receipt import, barcode lookup, local-first settings, premium themes.

## Claims Excluded
- No guaranteed food safety claims.
- No savings amount or environmental impact statistics.
- No household sync, cloud backup, account login, or medical/nutrition claims.
- No claim that barcode scanning finds expiration dates.
- No claim that AI is used in the app.

## Brand Decision
The existing green refrigerator identity was preserved and refined into a refrigerator + leaf + checkmark system.
""",
    )
    write_text(
        MARKETING / "SCREENSHOT_WORKFLOW.md",
        """# Screenshot Workflow

## Build debug app
```powershell
.\\gradlew.bat assembleDebug
```

## Start demo mode
The marketing demo receiver exists only in the debug source set:
```powershell
adb shell am broadcast -a com.fridgefinish.app.DEBUG_MARKETING_DEMO
```

## Reset demo data
```powershell
adb shell pm clear com.fridgefinish.app
adb shell am broadcast -a com.fridgefinish.app.DEBUG_MARKETING_DEMO
```

## Capture manually from emulator
```powershell
tools\\capture_marketing_screenshots.ps1
```

## Generate styled screenshots and all marketing assets
```powershell
tools\\generate_marketing_pack.ps1
```

## Validate dimensions
```powershell
python tools\\validate_marketing_assets.py
```
""",
    )
    write_text(
        MARKETING / "README.md",
        """# Fridge Finish Marketing Pack

This pack contains Fridge Finish brand assets, Android icon resources, Google Play artwork, social media graphics, website assets, store listing copy, screenshot copy, Play Console prep files, and validation tooling.

## Key folders
- `marketing/brand/`: editable SVG logos, PNG exports, brand guide, palette, and illustrations.
- `marketing/play-store/`: Play icon, feature graphic, phone screenshots, alt text, checklist, upload order, and asset manifest.
- `marketing/social/`: social and launch graphics.
- `marketing/web/`: website, DevHub, favicon, and Open Graph assets.
- `marketing/copy/`: Google Play listing copy, press kit, social copy, and metadata.
- `marketing/render/`: deterministic local renderer.
- `tools/`: generation, capture, and validation scripts.

## Regenerate
```powershell
tools\\generate_marketing_pack.ps1
```

## Validate
```powershell
python tools\\validate_marketing_assets.py
```

## Google Play upload files
- `marketing/play-store/icon/fridge_finish_play_icon_512.png`
- `marketing/play-store/feature-graphic/fridge_finish_feature_graphic_1024x500.png`
- `marketing/play-store/screenshots/phone/*.png`
- Copy from `marketing/copy/google_play_listing.md`

## Owner-supplied fields still required
- Contact email
- Privacy policy URL
- Ads declaration
- Data Safety declaration
- Content rating
- Target audience selection
- Category selection

## Verified claims
The marketing copy is based on implemented local Room storage, Today dashboard, storage lists, add/edit food, local reminders, restock list, recipe suggestions, leftover tracking, barcode lookup, receipt import, and theme settings.
""",
    )
    write_text(
        ROOT / "MARKETING_PACK_CHANGELOG.md",
        f"""# Marketing Pack Changelog

## {date.today().isoformat()}
- Added Fridge Finish brand identity assets for Smithware Studios.
- Added Android launcher, monochrome, notification, splash/background, and in-app brand resources.
- Added Play Store icon, feature graphic, screenshots, alt text, social, web, and copy assets.
- Added deterministic renderer and validator.
- Added debug-only marketing demo receiver for screenshot setup.
- Preserved local-first app behavior and avoided unsupported marketing claims.
""",
    )


def manifest_entry(path: Path, asset_type: str, intended_use: str, feature: str, play_safe: bool) -> dict:
    rel = path.relative_to(ROOT).as_posix()
    width = height = None
    fmt = path.suffix.lstrip(".").upper()
    transparency = False
    if path.suffix.lower() == ".png":
        im = Image.open(path)
        width, height = im.size
        transparency = im.mode in ("RGBA", "LA") and im.getextrema()[-1][0] < 255
    return {
        "filename": path.name,
        "relativePath": rel,
        "assetType": asset_type,
        "width": width,
        "height": height,
        "fileFormat": fmt,
        "transparency": transparency,
        "intendedUse": intended_use,
        "sourceAsset": "marketing/render/generate_marketing_pack.py",
        "generationCommand": "tools/generate_marketing_pack.ps1",
        "lastGeneratedDate": date.today().isoformat(),
        "featureRepresented": feature,
        "safeForPlayStoreUse": play_safe,
    }


def create_manifest() -> None:
    entries = []
    for path in MARKETING.rglob("*"):
        if path.is_file() and path.suffix.lower() in {".png", ".svg", ".json", ".csv", ".md", ".txt"}:
            entries.append(manifest_entry(path, "marketing", "Fridge Finish marketing pack", "Brand and verified app features", "play-store" in path.as_posix()))
    write_text(MARKETING / "asset_manifest.json", json.dumps(entries, indent=2))
    write_text(PLAY / "asset_manifest.json", json.dumps([e for e in entries if "/play-store/" in e["relativePath"]], indent=2))


def main() -> None:
    ensure_dirs()
    create_brand_assets()
    create_android_assets()
    create_illustrations()
    create_play_and_social_assets()
    create_copy_docs()
    create_play_console_docs()
    create_audit_and_readme()
    create_manifest()


if __name__ == "__main__":
    main()
