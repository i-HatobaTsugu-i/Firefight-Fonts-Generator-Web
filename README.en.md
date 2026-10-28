# Firefight Font Generator

[中文](README.md) · [English](README.en.md) · [Русский](README.ru.md)

Turn any `.ttf` / `.otf` font into a **PNG font atlas that Firefight can read directly**, and use it to replace the in-game typeface.

The web version is a single `index.html`: no install, no external assets — just double-click it or drop it on any static server.

## Download / install (Android APK)

If you'd rather not use a browser: grab the latest `Firefight-Font-Generator-*.apk` from [Releases](https://github.com/i-HatobaTsugu-i/Firefight-Fonts-Generator-Web/releases) and install it.

- The APK is a WebView shell that embeds the same `index.html`; UI, features, three languages and auto-save behave exactly like the web version.
- Android will warn about "unknown sources" — allow it to install.
- The APK is signed with a fixed key stored in the repo (`android/signing/keystore.p12`), so **later versions install over earlier ones**. Upgrading from the early debug-signed builds requires uninstalling once.
- That key and its password are public in this repo (anyone can sign an update the system treats as the same app). If that matters to you, replace it with your own key or inject it through repository Secrets.
- On launch the app compares its version with the latest release and asks whether to update (choosing "Later" remembers that version and won't nag again).

> Version numbers follow "date.nth-change-of-the-day", e.g. `2026.10.28.4`, stored in `APP_VERSION` inside `index.html` and shown in the page footer. Every push to `main` makes GitHub Actions build an APK and publish it as a Release tagged with that version (README-only changes are skipped).

> Each Release ships with an auto-generated changelog built from the commit history — see the Release body.

## Permissions and directories

The APK requests exactly one permission and only touches the following locations:

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Only used to query GitHub's `releases/latest` endpoint for the update check. Nothing is uploaded; the page itself works fully offline. |

| Directory | Purpose |
| --- | --- |
| System **Download** folder | Where exported PNG / zip files go; on Android 10+ this is written through MediaStore |
| `Android/data/<package>/files/Download` | Fallback location on Android 8 / 9, where the system download collection is missing |
| The font file you pick | Read once through the system file picker (SAF); only the file you selected |
| App-private storage (WebView data) | Settings (localStorage) and the font copy (IndexedDB); removed when you uninstall |

No storage permission, no access to photos, contacts or other folders, no analytics or ad SDKs. Footer links open in your system browser.

## Features

- **Atlas generation**: 16 × 16 cells, 64px each in the 1024 layout; export at 512 (32px cells) or 1024 (64px cells), or both at once.
- **Two layout versions**
  - New: matches the original atlas, byte order from `U+0020` to `U+00FE`, with `0x80–0x9F` taken from Windows-1252 and undefined slots left blank.
  - Legacy: ASCII, then 34 padding spaces, then Latin-1 (`¡`–`þ`).
- **Per-glyph tweaks**: click any cell to select it, then widen / heighten (by ink size, ±px) or offset horizontally / vertically (±px). Number fields and sliders stay in sync, and clipping to the cell is optional.
- **Weight**: regular / bold toggle, and "export both regular + bold" at export time.
- **Colors**: the page background follows the text color (dark or light) so you can always see the glyph; exported images **always have a transparent background**.
- **Font size**: slider plus input, accepting px (e.g. `48`) or a scale factor (e.g. `1.5` or `1.5x`).
- **Built-in manual**: the book button next to the title opens a step-by-step guide for every control, each with a live preview of the actual UI element.
- **Custom controls & motion**: layout dropdown, weight slider pill, checkboxes, language menu and color panel are all hand-built with open / close / select animations; the color picker offers presets, a hue slider, a saturation-value area and linked RGB / HSV / CMYK fields.
- **Export without a font**: exporting with no font loaded renders the device system font (the export dialog says so).
- **Clear font**: the "Clear font" chip next to the file name unloads the current font, deletes the stored copy and falls back to the system font immediately.
- **Auto-save**: font data, size, color, layout, weight and every glyph transform are stored in the browser and restored on reload.
- **Startup notice**: on every launch a copyright / disclaimer dialog appears first, and it reads the matching section of this README at runtime (falling back to built-in text when offline).
- **Update check**: after launch, the latest release is fetched and an update prompt is shown if a newer version exists.
- **Three languages**: 中文 / English / Русский, switchable from the round button in the top-right corner.

## Usage

1. Open `index.html` (double-click locally, or host it on GitHub Pages / any static server).
2. Click "Load font" and pick your `.ttf` / `.otf` (or skip it and export with the system font). A "Clear font" chip appears next to the file name.
3. Adjust size, weight and color; click a cell on the canvas to select one glyph and fine-tune it with the sliders.
4. Click "Download PNG" and choose 512 / 1024 / both:
   - a single file downloads directly as a PNG, e.g. `font_image_512.png`;
   - multiple files are packed into one `font_image.zip` (browsers block the second automatic download otherwise).
5. Replace the in-game font files with the exported PNG, following your mod's requirements.

## Atlas layout

| Item | 512 version | 1024 version |
| --- | --- | --- |
| Canvas | 512 × 512 | 1024 × 1024 |
| Cell size | 32 × 32 | 64 × 64 |
| Grid | 16 columns × 16 rows | 16 columns × 16 rows |
| Used | first 14 rows (223 character slots) | first 14 rows (223 character slots) |

- Index mapping: `index = byte value − 0x20`, so cell 1 is the space `0x20` and cell 224 corresponds to `0xFF` (blank in the original atlas, and left blank here too).
- The five slots missing glyphs in the original atlas (`€`, `…`, `Ž`, `•`, `ž`) stay blank to match it.
- Background is fully transparent — only glyph pixels exist, so the atlas drops straight into the game.

## Known limitations

- **Bold is synthetic**: with a single font file loaded, the browser fakes bold, which differs from the font's own Bold weight. Load the real Bold file if you need the exact look.
- **Missing glyph fallback**: glyphs absent from your font are drawn by a system fallback font — correct position, wrong typeface. With no font loaded at all, the whole atlas uses the system font.
- **Storage**: settings live in `localStorage`, the font copy in `IndexedDB`. Some browsers block `IndexedDB` on `file://`, which disables auto-restore; serve over http (local server or GitHub Pages) to get it back.
- **Old engines**: a few older mobile WebViews don't dispatch the `<dialog>` `close` event, so export runs directly from the option buttons instead.

## Files

```
index.html          # The tool: HTML + CSS + JS in one file, no external dependencies
README.md           # 中文说明
README.en.md        # English readme (this file)
README.ru.md        # Русская документация
android/            # Android shell (WebView; index.html is copied into assets at build time)
android/signing/    # APK signing keystore (generated and committed by CI on first build)
.github/workflows/  # GitHub Actions: push to main → build APK → publish Release
```

## Building the APK from source

```bash
cp index.html android/app/src/main/assets/index.html   # put the page into assets
cd android && gradle assembleRelease                   # output: app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17 + Android SDK 34 and `android/signing/keystore.p12` (without it the release build is unsigned). CI does all of this automatically; locally you usually only need the web version.

## Author

- Made by **DeepSeek 大肥鱼**.

## Disclaimer

- This is an unofficial third-party tool. It is not affiliated with, endorsed by, or authorized by the developers or publishers of Firefight.
- The tool only renders the font file you provide into a PNG atlas. It contains and distributes no original game assets.
- Replacing in-game fonts may crash the game, corrupt saves, or be flagged by anti-cheat / online services. You use it at your own risk.
- Font files remain the property of their authors — make sure you have the right to use and redistribute them.
- The APK signing key and password are public in this repository. Do not download the APK from anywhere other than this repository's Releases — a copy from a third-party channel may be signed with a forged key. Users who obtained the app from a third-party channel bear any losses caused by tampering, and whoever tampered with it bears the corresponding legal responsibility. This is an explicit warning: the author is not responsible for any distribution channel other than this repository.

## Terms of use

- Free to redistribute and modify; keep the author credit and a link to this project. **Absolutely no** redistribution after tampering.
- Commercial use is not allowed. The author promises that everything in this repository is **free of charge** — if you paid for a so-called premium service through a third-party channel, you were scammed.
- Copyright, licensing and attribution of the font files you generate are your responsibility, not the author's.
- These terms may change with new versions; the current content of this file is authoritative.

## Contact

Bug reports, feature ideas, or updates to the Firefight font layout, are welcome through any of these:

- Open an Issue in the GitHub repository
- Email: pigeon233333@outlook.com
- QQ: 168478141
