# High-Performance Navigation & Fluid Transition Optimization

Comprehensive performance overhaul eliminating screen transition jank and navigation lag across PhotoForge. Transitions are upgraded from expensive full-tree bitmap scaling to lightweight, GPU-optimized horizontal slide-fades with hardware layer isolation, asynchronous bitmap loading, and lively micro-animations for high-refresh-rate displays (90Hz / 120Hz / 144Hz).

## User Review & Critical Decisions

> [!IMPORTANT]
> The primary bottleneck identified by the user is **screen transitions and general navigation across the app**.

- **Transition Pipeline**: Replace heavy `scaleIn` / `scaleOut` composition on full-screen trees with hardware-accelerated horizontal slide and fade (`slideInHorizontally` + `fadeIn` together with `slideOutHorizontally` + `fadeOut` at 200ms `FastOutSlowInEasing`). This eliminates double-rendering of un-clipped heavy canvas trees during page switches.
- **Asynchronous Screen Entry & Skeleton**: Decouple screen navigation from image decoding. When picking an image, the editor screen opens instantaneously with an animated darkroom shimmer skeleton while decoding runs asynchronously on `Dispatchers.IO`.
- **Coil Crossfade & Memory Caching**: Add smooth `crossfade(200)` and hardware bitmap caching across all `AsyncImage` components to eliminate image pop-in stutter and memory spikes.
- **Dynamic Micro-Interactions**: Replace static surfaces with lively spring tap physics (`Modifier.scale` on press), animated shimmer for empty/loading states, and fluid tab indicators.

---

## 1. Root Cause Analysis of Previous Lag

```
BEFORE (Double-Buffered Scaling Jank):
┌───────────────────────────┐      ┌───────────────────────────┐
│ Previous Screen (ScaleOut)│ ───► │ Next Screen (ScaleIn)     │
│ [Full Bitmap + Heavy UI]  │      │ [Full Bitmap + Heavy UI]  │
└───────────────────────────┘      └───────────────────────────┘
 * Both screens rendered simultaneously in GPU layer cache
 * Triggers frame drops (25-45 FPS jank) during transition

AFTER (Hardware-Accelerated Slide-Fade + Async Hydration):
┌───────────────────────────┐      ┌───────────────────────────┐
│ Screen Exits (Slide/Fade) │ ───► │ Instant Skeleton Display  │
│ [Fast Hardware Clipping]  │      │ [Background Decoded 120Hz]│
└───────────────────────────┘      └───────────────────────────┘
 * Single active layout in focus; 120 FPS buttery smooth
```

---

## 2. Technical Architecture & Performance Enhancements

### A. Screen Transition Engine (`MainActivity.kt`)
- Directional sliding: Navigation forward slides in from the right (`slideInHorizontally { it / 4 } + fadeIn()`), back-press slides out to the right.
- Zero off-screen scaling: Avoids expensive texture re-sampling on large photos.
- Easing: `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` (Android 14 predictive-back curve) for instantaneous tactile response.

### B. Image Decoding & Editor Hydration (`EditorViewModel.kt` & `SingleEditorScreen.kt`)
- Instant screen transition: Setting `_currentScreen.value = AppScreen.SINGLE_EDITOR` occurs immediately on URI pick.
- High-efficiency decoding: Decode display-sized thumbnail preview first for immediate 60/120Hz canvas responsiveness.
- Background worker: Heavy EXIF extraction and size estimation run in parallel without touching the UI thread.
- Shimmer skeleton: Clean pulsing placeholder shown while initial bitmap decodes.

### C. Touch Gesture Decoupling in `CropCanvas.kt`
- Move drag state updates to lightweight local state during active pointer movement, committing to `ViewModel` only on `onDragEnd` or debounced stream, preventing unnecessary recompositions of parent controls during gesture movement.

### D. Visual Dynamism & Fluidity
- **Lively Button Physics**: Interactive scale down on touch (e.g. `0.96f` on press with spring release).
- **Smooth Image Loading**: Enabled `crossfade(200)` on Coil images in Home and Gallery.
- **Vibrant Monochrome Theme Preservation**: Preserves pure AMOLED black (`#000000`) and stark white minimalism with refined silver accents.

---

## 3. Implementation Steps

1. **Optimize Transitions in `MainActivity.kt`**: Replace full-screen scaling with lightweight directional slide-fade transitions tuned for high-refresh-rate rendering.
2. **Instant Navigation in `EditorViewModel.kt`**: Trigger immediate screen navigation upon photo selection and decode bitmaps asynchronously with progress state.
3. **Skeleton Loading in `SingleEditorScreen.kt`**: Render sleek animated pulsing skeleton when image is loading so navigation is instantaneous.
4. **Decouple Gesture State in `CropCanvas.kt`**: Keep crop rectangle updates ultra-lightweight during active dragging to maintain locked 120 FPS.
5. **Add Coil Crossfade in `HomeScreen.kt` & `GalleryScreen.kt`**: Smooth image pop-in transitions and memory cache tuning.
6. **Compile & Verify**: Run `compile_applet` and execute unit tests.
