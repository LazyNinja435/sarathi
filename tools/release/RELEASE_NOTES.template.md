# Sarathi v0.1.0 — first public GitHub release

## Highlights

- First installable **GitHub Release** build of Sarathi.
- **Offline Gemma** via LiteRT-LM when the on-device model is present.
- **Settings → Download offline model** fetches release assets (chunked) into **app-private storage** (not public Downloads).

## Model download size

The Gemma LiteRT-LM bundle is about **2.5 GB** compressed on disk as a single `.litertlm`. Download uses multiple release assets; use **Wi‑Fi**, keep the app in the foreground, and ensure free space (model + temporary chunks).

## Android install

1. Open the release page on GitHub from a browser on your phone (or sideload the APK from your computer).
2. Download **`sarathi-v0.1.0.apk`** (or the APK named in `sarathi-latest.json`).
3. Android will prompt to install; confirm only if you trust this release.

## Updates

Use **Settings → Update Sarathi → Check for updates** to compare with `sarathi-latest.json` on the latest GitHub release. New APKs are verified with **SHA-256** before install; Sarathi does **not** install updates silently.

## Source

Repository: https://github.com/LazyNinja435/sarathi
