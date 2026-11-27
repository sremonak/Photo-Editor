# LumaCraft Photo Studio

**Professional, privacy-first photo editing for Android — 100% native Kotlin + Jetpack Compose Material 3, zero web code, zero cloud calls.**

Every push is built by GitHub Actions into a **signed Release APK** and published automatically to [GitHub Releases](../../releases). Because every release is signed with the same permanent key and `versionCode` increases monotonically, **you can install each new APK directly over the old one — no uninstalling, your saved presets and sessions survive updates.**

---

## Feature report (all features below are real, pixel-processing implementations)

### Editing engine (non-destructive, MVVM + undo/redo)
- **20 pro adjustments**: Exposure, Brightness, Contrast, Highlights, Shadows, Whites, Blacks, Saturation, Vibrance, Temperature, Tint, Hue shift, Sharpness, Clarity, Texture, Dehaze, Fade, Vignette, Grain, Noise Reduction — every slider is a real pixel operation with numeric value + per-slider reset.
- **Color grading**: 4-channel draggable **tone-curve editor** (Master/R/G/B Catmull-Rom splines), **8-zone HSL color mixer**, **split toning** (shadow/highlight hue+strength+balance), **3-way color balance pads** (shadows/midtones/highlights).
- **40 original filters** in 10 categories (Cinematic, Portrait, Vintage, Film, B&W, Landscape, Moody, Warm, Cool, Social) — each processes actual pixels via its adjustment/split-tone/curve recipe, with a global **intensity** control and live-filtered thumbnails.
- **Geometry**: aspect-ratio crops (9 presets), 90° rotate, flip H/V, straighten, **architectural perspective keystone** (V/H homography), reset.
- **Live RGB histogram** with shadow/highlight clipping indicators; press-and-hold **before/after compare**; pinch-zoom canvas.

### AI Studio — on-device (ML Kit, bundled model, offline)
| Tool | Status |
|---|---|
| AI Enhancement (auto tone/color analysis) | ✅ implemented |
| AI Background Remover → transparent PNG cutout | ✅ implemented |
| AI Background Blur (portrait lens) | ✅ implemented |
| AI Background Replacement (solid color) | ✅ implemented |
| Smart Subject Selection (mask overlay + invert) | ✅ implemented |
| AI Object Removal (object eraser brush) | ✅ implemented |
| AI Noise Reduction / Restoration / Old-Photo Restore | ✅ implemented |
| AI Portrait Enhancement (edge-aware skin smoothing) | ✅ implemented |
| 2× AI Upscale (bicubic + smart sharpen) | ✅ implemented (software upscale) |
| AI Colorization | ⛔ **honestly disabled** — requires a cloud model; no fake responses |

### Retouching & creative tools
- **Spot-heal** blemish tool (ring-sampled patch + feathered blend), **red-eye fix**, **teeth whitening** (tap tools).
- **Selective blur brush**, **pixelate brush**, **object eraser**, **freehand drawing** (colors + width).
- **Text overlays** (7 fonts, size/color/bold/italic/align/opacity, drag-to-position), **emoji stickers**, **watermark**, **frames** (White / Dark / Polaroid).

### Studio features
- **Collage Studio**: 24 layouts, spacing + corner radius, background colors, 7 aspect ratios, per-cell drag repositioning, caption, high-res export.
- **Batch Editing**: apply the current look to up to 9 photos, format/quality/resolution options, live progress + cancel.
- **Preset Manager**: save/rename/delete custom presets, 8 built-in pro presets, **JSON import & export**.
- **Projects**: Recent Projects, **auto-saved editing sessions** (state restored after app restart via the home screen), non-destructive pipeline — originals are never modified.
- **Export**: JPEG/PNG/WebP, quality 1–100%, Original/2048/1080/Square-1080, EXIF privacy strip toggle, 2× upscale, save to `Pictures/LumaCraft` + system share sheet.
- **Settings**: System/Light/Dark theme, haptics, export defaults, cache management, privacy info, open-source licenses.

### Privacy & security
- **No INTERNET permission** — the app physically cannot upload anything.
- No login, no analytics, no tracking, no API keys in the APK.
- ML Kit segmentation model is bundled inside the APK (Apache 2.0) and runs fully offline.

---

## Build instructions

```bash
./gradlew clean          # (or: gradle clean)
./gradlew assembleDebug
./gradlew assembleRelease
```

- `app/keystore/lumacraft-release.p12` is the permanent release signing key (PKCS12, alias `lumacraft_release_key`, password `LumaCraftRelease2026`, RSA-2048). CI signs with it automatically; override via `LUMA_KEYSTORE_PATH`, `LUMA_STORE_PASSWORD`, `LUMA_KEY_ALIAS`, `LUMA_KEY_PASSWORD` env vars if you rotate keys.
- CI (`.github/workflows/build-apk.yml`): builds `assembleRelease` + `assembleDebug`, verifies the signature with `apksigner verify`, computes SHA-256 checksums, and publishes a GitHub Release `v1.0.<run_number>`.

## Update without uninstalling

`applicationId` is always `com.lumacraft.studio` (no `.debug` suffix), signing identity never changes, and `versionCode = 100 + CI run number` always increases. Install any newer release APK and Android updates the app in place — presets, settings and saved sessions persist.

## Technical architecture

- **UI**: Jetpack Compose Material 3, light + dark schemes, MVVM (`StudioViewModel`, `StateFlow<StudioUiState>`), Coroutines for all image work (UI thread never blocks).
- **Engine** (`ImageEditEngine.kt`): single-pass IntArray pixel pipeline — WB/tone/curves/HSL/split-tone/wheels → clarity/texture via blurred luma grids → grain/vignette → sharpen unsharp → ML Kit bokeh/cutout passes → Canvas overlay baking (text/emoji/draw/frames/watermark).
- **Storage**: lightweight SharedPreferences + JSON vaults (presets, sessions, recents, settings) — no heavyweight database for tiny config data.
- **Memory safety**: sampled bitmap decoding, preview pipeline capped at 760 px, export pipeline at ≤4096 px, OOM-guarded upscale (skips beyond 36 MP).

## Known limitations (documented, not hidden)

1. **AI Colorization is disabled** — real colorization needs a cloud/onnx model we deliberately don't ship (privacy + APK size). Clearly labeled in the UI.
2. **Camera capture is not included** — the spec marked it optional; use your camera app and open the photo from the gallery (ACTION_EDIT/SEND are supported).
3. **Camera-RAW decode** uses the platform decoder (JPEG/PNG/WebP/HEIC where the device supports it); proprietary RAW (.CR3/.NEF…) demosaicing is out of scope — import them via a converter or use DNG if your device supports it.
4. **Batch exports skip overlay layers** (text/emoji/drawing are per-photo creative tools).
5. **Collage** uses auto center-crop framing with drag-to-reposition pan (no per-cell zoom UI yet).
6. **Skin smoothing** is a heuristic skin-tone algorithm — excellent for portraits, not a face-mesh retoucher (no face landmark detection).
7. Presets/sessions are stored on-device (SharedPreferences JSON) — clearing app data clears them.

## Third-party components (all Apache 2.0)

Jetpack Compose / AndroidX, Coil, Kotlin Coroutines, androidx.exifinterface, Google ML Kit Selfie Segmentation (bundled on-device model).
