# Fridge Finish Marketing Pack

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
tools\generate_marketing_pack.ps1
```

## Validate
```powershell
python tools\validate_marketing_assets.py
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
