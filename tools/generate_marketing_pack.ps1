$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
python marketing/render/generate_marketing_pack.py
python tools/validate_marketing_assets.py
Write-Host "Fridge Finish marketing pack generated and validated."
