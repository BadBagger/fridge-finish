#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/marketing/play-store/screenshots/raw"
mkdir -p "$OUT"
screens=(
  01_know_what_to_use_next
  02_expiring_food_priority
  03_add_food_quickly
  04_fridge_freezer_pantry
  05_reminders_before_expiry
  06_leftovers_and_food_tracking
  07_progress_or_insights
  08_simple_private_organized
)
for screen in "${screens[@]}"; do
  read -r -p "Navigate to $screen, then press Enter"
  adb exec-out screencap -p > "$OUT/$screen.png"
done
