# Lumina RAW Pro — Native Android Photo Editor

A flagship **Native Android** professional photo editor built with **Kotlin**, **Jetpack Compose (Material 3)**, and hardware-accelerated darkroom UI architecture.

## Tech Stack
- **100% Native Android**: Kotlin `2.0.21` + Android SDK `35` (`minSdk 26`)
- **UI Toolkit**: Jetpack Compose + Material 3 Calibrated Darkroom Theme (`#090A0D`)
- **CI / APK Build**: GitHub Actions (`.github/workflows/build-apk.yml`) compiling native `app-debug.apk`

## UI Screens & Studio Modules (Phase 1 — UI Design)
1. **Studio Home Hub (`StudioHomeScreen.kt`)**:
   - Native Android Photo Picker (`PickVisualMedia`) RAW/Gallery import CTA
   - Quick Studio Launch Tiles (*RAW Cam*, *AI Cutout*, *HSL Lab*, *Batch*)
   - AI Neural Studio Carousel (*3D Relight*, *Bokeh f/1.2*, *Magic Eraser*, *4K Super-Res*)
   - Non-Destructive Recent RAW Sessions Grid + Bottom Studio Navigation (`Studio`, `Drafts`, `LUT Lab`, `Engine`)
2. **Pro Darkroom Editor (`ProEditorScreen.kt`)**:
   - Precision Top Action Bar (*Back*, *Undo/Redo* with step counter, *Live RGB Waveform Histogram* toggle, *Split-Screen Before/After Wipe* toggle, *Export* CTA)
   - Interactive Darkroom Viewport (`StudioViewportCanvas.kt`) with real-time `ColorMatrix` preview, draggable Before/After wipe divider, 3D Key-Light sun handle, and Rule-of-Thirds crop overlay
   - Custom Hardware Tick-Mark Dial (`PrecisionDial.kt`) with Android haptic tick feedback
   - Interactive 4-Channel (`RGB`, `Red`, `Green`, `Blue`) Spline Tone Curve Graph (`ToneCurveEditor.kt`)
   - 8 Studio Dock Modules: **LUTs**, **Light**, **Curves**, **Color (WB + 8-Band HSL)**, **Optics**, **AI Lab**, **Crop**, **Layers**
   - Material 3 **Export Master Render Bottom Sheet** (`JPG`, `PNG 16-bit`, `WEBP`, `DNG RAW`, `sRGB / Display P3 / Adobe RGB`)
