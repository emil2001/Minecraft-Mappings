# Native contained-water regression

From the `forge/1.7.10-mapping` directory, with the configured JDK and dependencies:

```sh
../../gradlew --offline -PmtrGlClient=/absolute/path/to/isolated/prism/data nativeWaterGraphicsTest
```

The optional task uses the same pinned native libraries as `nativeGraphicsTest` and currently targets macOS ARM64. It opens a hidden 64×64 OpenGL window, loads the native legacy renderer in an isolated LaunchClassLoader, and runs `WaterGraphicsScenarios`. It does not launch a Minecraft world. No staging init script or staged production classes are needed.

Coverage includes bottom/top/dry/double slabs, a fence-like post, a non-MODEL host, adjacent water/full cubes/partial panels/slabs, clipping boundaries, unchanged host-pass vertices, and comparison of native fluid positions/UV/color/brightness with an independent source-water view under the same lighting. Normals are excluded from that last comparison because the contained-water helper adds them.

The native ItemBlock fallback regression registers an ordinary item with no selected custom renderer. Real `RenderItem.renderItemIntoGUI` output must be nonempty and exactly equal to a render-type -1 control. The shared `renderItemIn3d` predicate must remain false for water-only hosts and true for MODEL hosts. Equipped/dropped callers use the same predicate, but their full drawing paths are not pixel-tested.

`WaterLegacyGlRedirect` is test-only. It avoids running the legacy ASM remapper over classes without LWJGL references; otherwise interface lambda handles can be rewritten incorrectly even when no type mapping is needed. The production transformer is not replaced.

## Evidence and limits

Before the renderer change, the native reference-water pixel passed and the wet-bottom-slab water pixel failed. Before the separate water-only render ID fix (WR-1), the native icon control passed, while all three fallback assertions failed: 3D classification, nonempty output, and image equality. After the fix, the focused harness passed (`BUILD SUCCESSFUL in 5s`). The complete original logs and reviewed diffs are retained in the task's `work/water-rendering-staging` evidence directory; this historical run used staged sources. Integrated verification is recorded separately.

This harness does not establish real GTNH/Angelica world acceptance, extended-state chunk rebuild behavior, arbitrary TESR mesh clipping, worker callback thread safety, shader water identity, or forced render-layer compatibility. Host geometry uses collision-box unions; native neighbor support covers full cubes/slabs and mapped shape callbacks. Neighboring native water may retain its own redundant interface.
