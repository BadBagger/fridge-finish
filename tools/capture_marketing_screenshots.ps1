$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "marketing/play-store/screenshots/raw"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$screens = @(
  "01_know_what_to_use_next",
  "02_expiring_food_priority",
  "03_add_food_quickly",
  "04_fridge_freezer_pantry",
  "05_reminders_before_expiry",
  "06_leftovers_and_food_tracking",
  "07_progress_or_insights",
  "08_simple_private_organized"
)

Write-Host "Make sure the emulator is on the intended Fridge Finish screen before each capture."
foreach ($screen in $screens) {
  Read-Host "Navigate to $screen, then press Enter"
  $target = Join-Path $out "$screen.png"
  adb exec-out screencap -p > $target
  Write-Host "Saved $target"
}
