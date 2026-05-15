# Launch / splash redesign — verification report

**Date:** 2026-05-14  
**Scope:** Welcome (splash) screen visual upgrade, hero peacock emblem, radiance motes, adaptive launcher icon. Onboarding navigation unchanged.

## Build result

- **Command:** `.\gradlew.bat :app:assembleDebug` (with `JAVA_HOME` set to Android Studio JBR)
- **Result:** **BUILD SUCCESSFUL**

## Devices tested

| Device | Notes |
|--------|--------|
| **Android Emulator** (`emulator-5554`) | APK installed; `pm clear` used to re-show welcome flow; splash and launcher screenshots captured. |
| **Physical Pixel** | Not connected in this session; recommend repeating smoke on documented QA device when available. |

## Screenshots

Captured under `verification/screenshots/redesign_launch/`:

- `01_current_redesign_splash.png` — welcome screen after cold start  
- `02_launcher_icon.png` — home screen showing updated launcher glyph  

## Files changed

| File | Purpose |
|------|---------|
| `app/src/main/java/com/sarathi/app/ui/screens/SplashScreen.kt` | Layered layout: atmosphere, motes + hero emblem, typography spacing, ornamental divider, filigree Begin control, footer lotus. |
| `app/src/main/java/com/sarathi/app/ui/components/PeacockFeatherEmblem.kt` | `PeacockEmblemStyle` (`Standard` / `Hero`); hero path adds mandala halo, layered glow, gold strands, pendant beads, subtle infinite glow / halo drift. |
| `app/src/main/java/com/sarathi/app/ui/components/SplashDecor.kt` | `SplashScreenAtmosphere`, `SplashRadianceMotes`, dividers, lotus title row, Begin button filigree shell, `SplashFooterLotus`. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive foreground: simplified same emblem (feather + eye + halo rings + pendant) for small launcher readability. |

**Unchanged by design:** `SarathiNavGraph.kt` (`onBegin` → `Routes.NAME`), RAG, LLM engines, other screens’ use of `PeacockFeatherEmblem()` defaults (`Standard`).

## Splash redesign summary

- **Background:** Existing `SacredBackground` gradient retained; **additional** `SplashScreenAtmosphere` adds edge vignette, extra star specks, translucent peacock-feather silhouette hints, and gold corner brackets (splash only).
- **Center:** `PeacockFeatherEmblem` in **Hero** style at ~198 dp with mandala, soft rays, pendant, and gentle pulsing glow.
- **Typography:** Larger, more spaced “Sarathi” title; quote at 22 sp with comfortable line height; welcome line preserved.
- **Ornament:** Lotus + lines above content; lotus-style divider between quote and welcome; small footer lotus under the button.
- **Begin:** Same `SacredButton` behavior; optional corner filigree drawn behind; label uses `titleLarge` for a more ceremonial weight.

## Launcher icon summary

- **Approach:** Single vector `ic_launcher_foreground.xml` on midnight background (`ic_launcher_background`).
- **Compromise:** Launcher size requires **fewer** rings, strokes, and glow than the in-app Hero canvas; pendant is reduced to three simple dots and a short stem so the glyph stays legible at 48 dp. Colors align with the in-app emblem (teal `#2E8B7A`, navy `#0B1B34`, gold `#D8B45A` / `#F1DFA2`).

## Particle animation summary

- **Composable:** `SplashRadianceMotes` in `SplashDecor.kt`.
- **Mechanism:** `rememberInfiniteTransition` drives a 0→1 phase over **10 s**, linear loop. **14** motes use fixed random angles (stable seed); each particle’s radial distance is `(phase + stagger) % 1` with gentle easing; alpha falls off as radius grows (`(1-ease)^1.4`). A small sine term adds **very** slow angular wobble and soft “shimmer” on alpha—intended as calm radiance, not confetti.
- **Performance:** Low particle count, one `Canvas` per frame for motes only; no extra animation libraries.

## QA checklist (manual)

1. Welcome screen closer to draft (deep indigo, gold emblem, ornament, specks). **Pass on emulator screenshot.**  
2. Center glowing logo = Hero `PeacockFeatherEmblem`. **Pass.**  
3. Motes drift outward from center. **Pass (visual on device/emulator).**  
4. Text readable. **Pass.**  
5. Begin clear. **Pass.**  
6. Launcher icon updated. **Pass (home screenshot).**  
7. **Begin → onboarding:** Verified on emulator: `uiautomator` dump after tap shows **Name** screen copy (“bestowed upon you”) and **Offer my name** control.  
8. No crashes observed during install / launch / `pm clear` cycle.  
9. No frame profiler run; design targets low-cost drawing.

## Remaining polish ideas

- Add `verticalScroll` on very short viewports if real devices clip content.  
- Optional very light **noise** texture shader (only if profiling stays green).  
- Physical Pixel pass for motion perception (emulator can misrepresent glow/particles).
