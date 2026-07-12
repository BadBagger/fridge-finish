from __future__ import annotations

import csv
import hashlib
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
MARKETING = ROOT / "marketing"


REQUIRED = {
    "marketing/brand/fridge_finish_icon_master.svg": None,
    "marketing/brand/fridge_finish_icon_transparent.png": (1024, 1024, "alpha"),
    "marketing/brand/fridge_finish_icon_one_color.svg": None,
    "marketing/brand/fridge_finish_icon_one_color.png": (1024, 1024, "alpha"),
    "marketing/brand/fridge_finish_wordmark_horizontal.svg": None,
    "marketing/brand/fridge_finish_wordmark_horizontal.png": (1200, 320, "alpha"),
    "marketing/brand/fridge_finish_logo_stacked.svg": None,
    "marketing/brand/fridge_finish_logo_stacked.png": (900, 900, "alpha"),
    "marketing/brand/fridge_finish_logo_light.svg": None,
    "marketing/brand/fridge_finish_logo_dark.svg": None,
    "marketing/brand/smithware_studios_lockup.svg": None,
    "marketing/brand/brand_guide.md": None,
    "marketing/brand/color_palette.json": None,
    "marketing/brand/color_palette.ase-equivalent.txt": None,
    "marketing/play-store/icon/fridge_finish_play_icon_512.png": (512, 512, "alpha"),
    "marketing/play-store/feature-graphic/fridge_finish_feature_graphic_1024x500.png": (1024, 500, "opaque"),
    "marketing/play-store/feature-graphic/fridge_finish_feature_graphic_no_text_1024x500.png": (1024, 500, "opaque"),
    "marketing/play-store/screenshots/alt_text.csv": None,
    "marketing/copy/google_play_listing.md": None,
    "marketing/copy/google_play_listing.txt": None,
    "marketing/copy/store_metadata.json": None,
    "marketing/copy/screenshot_copy.csv": None,
    "marketing/copy/social_copy.md": None,
    "marketing/copy/press_kit.md": None,
    "marketing/play-store/play_console_checklist.md": None,
    "marketing/play-store/asset_manifest.json": None,
    "marketing/play-store/upload_order.md": None,
    "marketing/asset_manifest.json": None,
    "marketing/README.md": None,
}

PHONE_SCREENSHOTS = [
    "01_know_what_to_use_next.png",
    "02_expiring_food_priority.png",
    "03_add_food_quickly.png",
    "04_fridge_freezer_pantry.png",
    "05_reminders_before_expiry.png",
    "06_leftovers_and_food_tracking.png",
    "07_progress_or_insights.png",
    "08_simple_private_organized.png",
]

SOCIAL = {
    "marketing/social/fridge_finish_launch_square_1080x1080.png": (1080, 1080, "opaque"),
    "marketing/social/fridge_finish_portrait_post_1080x1350.png": (1080, 1350, "opaque"),
    "marketing/social/fridge_finish_story_1080x1920.png": (1080, 1920, "opaque"),
    "marketing/social/fridge_finish_social_preview_1200x628.png": (1200, 628, "opaque"),
    "marketing/social/fridge_finish_app_suite_tile_1200x1200.png": (1200, 1200, "opaque"),
    "marketing/social/fridge_finish_web_hero_1600x900.png": (1600, 900, "opaque"),
    "marketing/social/fridge_finish_beta_announcement_1080x1080.png": (1080, 1080, "opaque"),
    "marketing/social/fridge_finish_feature_card_expiry_1080x1080.png": (1080, 1080, "opaque"),
    "marketing/social/fridge_finish_feature_card_inventory_1080x1080.png": (1080, 1080, "opaque"),
    "marketing/social/fridge_finish_feature_card_leftovers_1080x1080.png": (1080, 1080, "opaque"),
}

WEB = {
    "marketing/web/fridge_finish_icon_256.png": (256, 256, "alpha"),
    "marketing/web/fridge_finish_icon_512.png": (512, 512, "alpha"),
    "marketing/web/fridge_finish_logo_horizontal.svg": None,
    "marketing/web/fridge_finish_logo_horizontal.png": (1200, 320, "alpha"),
    "marketing/web/fridge_finish_hero_1600x900.png": (1600, 900, "opaque"),
    "marketing/web/fridge_finish_card_1200x675.png": (1200, 675, "opaque"),
    "marketing/web/fridge_finish_thumbnail_800x450.png": (800, 450, "opaque"),
    "marketing/web/fridge_finish_open_graph_1200x630.png": (1200, 630, "opaque"),
    "marketing/web/fridge_finish_favicon.svg": None,
    "marketing/web/fridge_finish_favicon_32.png": (32, 32, "alpha"),
    "marketing/web/fridge_finish_favicon_48.png": (48, 48, "alpha"),
    "marketing/web/fridge_finish_app_badge.svg": None,
}


def alpha_status(path: Path) -> str:
    image = Image.open(path)
    if image.mode not in ("RGBA", "LA"):
        return "opaque"
    extrema = image.getextrema()[-1]
    return "alpha" if extrema[0] < 255 else "opaque"


def image_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check_file(rel: str, rule) -> tuple[bool, str]:
    path = ROOT / rel
    if not path.exists():
        return False, "missing"
    if path.stat().st_size == 0:
        return False, "zero-byte"
    if rule is None:
        return True, "exists"
    expected_w, expected_h, expected_alpha = rule
    image = Image.open(path)
    if image.size != (expected_w, expected_h):
        return False, f"expected {expected_w}x{expected_h}, got {image.size[0]}x{image.size[1]}"
    actual_alpha = alpha_status(path)
    if expected_alpha == "alpha" and actual_alpha != "alpha":
        return False, "expected transparency"
    if expected_alpha == "opaque" and actual_alpha != "opaque":
        return False, "expected opaque image"
    return True, f"{expected_w}x{expected_h} {actual_alpha}"


def main() -> None:
    checks: list[tuple[str, bool, str]] = []
    expected = dict(REQUIRED)
    expected.update(SOCIAL)
    expected.update(WEB)
    for name in PHONE_SCREENSHOTS:
        expected[f"marketing/play-store/screenshots/phone/{name}"] = (1080, 1920, "opaque")
        expected[f"marketing/play-store/screenshots/raw/{name}"] = (1080, 1920, "opaque")

    for rel, rule in sorted(expected.items()):
        ok, detail = check_file(rel, rule)
        checks.append((rel, ok, detail))

    play_icon = ROOT / "marketing/play-store/icon/fridge_finish_play_icon_512.png"
    if play_icon.exists():
        checks.append(("play icon under 1 MB", play_icon.stat().st_size < 1_000_000, f"{play_icon.stat().st_size} bytes"))

    alt_path = ROOT / "marketing/play-store/screenshots/alt_text.csv"
    if alt_path.exists():
        with alt_path.open(newline="", encoding="utf-8") as f:
            rows = list(csv.DictReader(f))
        covered = {row["filename"] for row in rows}
        missing = sorted(set(PHONE_SCREENSHOTS) - covered)
        checks.append(("alt text covers every screenshot", not missing, f"missing={missing}"))
        long_alt = [row["filename"] for row in rows if len(row["alt_text"]) > 140]
        checks.append(("alt text <= 140 chars", not long_alt, f"too_long={long_alt}"))

    hashes: dict[str, list[str]] = {}
    for path in MARKETING.rglob("*.png"):
        hashes.setdefault(image_hash(path), []).append(path.relative_to(ROOT).as_posix())
    duplicates = [paths for paths in hashes.values() if len(paths) > 1]
    checks.append(("duplicate PNG hashes detected", True, f"{len(duplicates)} duplicate groups; expected for reused logo exports"))

    report = ["# Marketing Asset Validation Report", ""]
    all_ok = True
    for rel, ok, detail in checks:
        all_ok = all_ok and ok
        report.append(f"- [{'PASS' if ok else 'FAIL'}] {rel}: {detail}")
    report.append("")
    report.append(f"Overall: {'PASS' if all_ok else 'FAIL'}")
    (MARKETING / "VALIDATION_REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"Marketing validation {'passed' if all_ok else 'failed'}: {MARKETING / 'VALIDATION_REPORT.md'}")
    if not all_ok:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
