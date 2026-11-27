# VELOX — Optical Atelier & RAW Darkroom (Native Android)

**VELOX** is an industrial-crafted **Native Android** professional photo editor engineered with **Kotlin 2.0**, **Jetpack Compose (Material 3 Expressive)**, and a multi-threaded real-bitmap analog & digital color pipeline.

## Design & Color Philosophy
- **Industrial Hardware Aesthetic**: Inspired by physical rangefinder lens barrels and darkroom contact sheets — **Obsidian Stage (`#08080A`)**, **Anodized Titanium (`#101115`)**, **International Vermilion (`#FF4D2D`)**, and **Champagne Brass (`#E8C58F`)**.
- **Material 3 Expressive Ergonomics**: Floating morphing pill navigation docks, spring-physics selection transitions, cylindrical 3D rotary lens-barrel dials (`KnurledRotaryDial.kt`) with haptic detents, and edge-to-edge darkroom immersion.
- **Bespoke Adaptive Launcher Icon**: 6-blade geometric brass aperture inside a multi-coated dark optical element with a Vermilion shutter release dot (`ic_launcher_foreground.xml` + Android 13–16 monochrome support).

## Core Darkroom Engine (`BitmapGradeEngine.kt`)
- **Real Pixel Processing on `Dispatchers.Default`**:
  - **Dehancer-Class Analog Emulsion**: Separable box-blur **Red-Orange Film Halation**, **Optical Highlight Bloom**, **35mm Silver Halide Grain**, and **Matte Paper Lift**.
  - **Capture One-Class 3-Way Color Balance Wheels**: Interactive circular chromatic trackballs for **Shadows**, **Midtones**, and **Highlights** (`ColorGradingWheels.kt`).
  - **4-Channel Spline Tone Curves**: `Master`, `Red`, `Green`, `Blue` Catmull-Rom LUTs backed by a live **48-bin RGB Parade & Waveform Scope** (`ProSplineCurve.kt`, `RgbParadeScope.kt`).
  - **8-Zone Selective HSL Equalizer**: Independent saturation and luminance sculpting across 8 chromatic zones.
  - **Selective Masking Studio**: Draggable *Radial Spot*, *Linear Sky*, and *Subject Rim* masks with **Ruby Mask Isolation Overlay**.
  - **Gallery Print Rebates & Real EXIF Extraction**: Reads real camera body, lens, ISO, focal length, aperture, and shutter speed via `ExifInterface`, stamping *Museum Matte*, *Obsidian Rebate*, or *35mm Negative* borders.
  - **Native `MediaStore` Export & Share**: Writes full-resolution `JPEG`, `PNG`, or `WebP Lossless` masters straight to `/Pictures/VeloxAtelier` and supports Android `ACTION_EDIT` / `ACTION_SEND` intents.
