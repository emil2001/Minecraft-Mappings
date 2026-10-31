package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.LegacyBlockModels;
import org.mtr.mod.block.BlockCeiling;
import org.mtr.mod.block.BlockCeilingAuto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CeilingAttachmentTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }

    @Test void automaticCeilingMeetsBlockAbove() throws Throwable { run("automaticCeilingMeetsBlockAbove"); }
    @Test void litCeilingMeetsBlockAbove() throws Throwable { run("litCeilingMeetsBlockAbove"); }
    @Test void unlitCeilingMeetsBlockAbove() throws Throwable { run("unlitCeilingMeetsBlockAbove"); }

    private static void run(String method) throws Throwable {
        Object scenarios = BlockPlacementIntegrationTest.loader.loadClass(Scenarios.class.getName()).getConstructor().newInstance();
        BlockPlacementIntegrationTest.invoke(scenarios, method);
    }

    public static final class Scenarios {
        public void automaticCeilingMeetsBlockAbove() {
            verify("ceiling", new BlockCeilingAuto(settings()), true, false);
        }

        public void litCeilingMeetsBlockAbove() {
            verify("ceiling_light", new BlockCeiling(settings()), false, true);
        }

        public void unlitCeilingMeetsBlockAbove() {
            verify("ceiling_no_light", new BlockCeiling(settings()), false, false);
        }

        private static BlockSettings settings() {
            return new BlockSettings(net.minecraft.block.material.Material.iron).nonOpaque();
        }

        private static void verify(String id, BlockCeiling block, boolean automatic, boolean alwaysLit) {
            Path assets = Paths.get(System.getProperty("mtr.modelResources")).getParent();
            LegacyBlockModels models = new LegacyBlockModels(resource -> {
                if (!resource.getNamespace().equals("mtr")) return null;
                try { return new String(Files.readAllBytes(assets.resolve(resource.getPath())), StandardCharsets.UTF_8); }
                catch (Exception error) { throw new IllegalArgumentException(resource.toString(), error); }
            });
            BlockView world = new BlockView(new BlockPlacementScenarios.MemoryWorld());
            BlockPos pos = new BlockPos(8, 64, 8);
            List<org.junit.jupiter.api.function.Executable> cases = new ArrayList<>();
            for (boolean facing : new boolean[] {false, true}) {
                for (boolean light : automatic ? new boolean[] {false, true} : new boolean[] {alwaysLit}) {
                    BlockState state = block.getDefaultState2().with(new Property<>(BlockCeiling.FACING.data), facing);
                    if (automatic) state = state.with(new Property<>(BlockCeilingAuto.LIGHT.data), light);
                    final BlockState actualState = state;
                    Map<String, String> properties = new HashMap<>();
                    properties.put("facing", Boolean.toString(facing));
                    if (automatic) properties.put("light", Boolean.toString(light));
                    String label = id + " facing=" + facing + " light=" + light;
                    cases.add(() -> {
                        Box shape = block.getOutlineShape2(actualState, world, pos, new ShapeContext(null)).getBoundingBox();
                        List<LegacyBlockModels.Quad> faces = models.resolve(new Identifier("mtr:" + id), properties, 0);
                        assertAll(label,
                            () -> assertEquals(1, shape.data.maxY, 0.000001, "Selection and collision must meet the underside of the block above"),
                            () -> assertEquals(13.0 / 16, shape.data.minY, 0.000001, "Keep the ceiling three pixels thick"),
                            () -> assertEquals(13.0 / 16, minY(faces, false), 0.000001, "Rendered bars must agree with the real block shape"),
                            () -> assertEquals(1, maxY(faces), 0.000001, "The rendered ceiling must not leave an air gap"),
                            () -> assertEquals(light ? 18 : 12, faces.size(), "Keep both bars and the configured light strip"),
                            () -> assertVerticalTextureSampling(faces),
                            () -> { if (light) assertEquals(15.0 / 16, minY(faces, true), 0.000001, "The light strip must move with the bars"); }
                        );
                    });
                }
            }
            assertAll(cases);
        }

        private static double minY(List<LegacyBlockModels.Quad> faces, boolean onlyLight) {
            double value = Double.POSITIVE_INFINITY;
            for (LegacyBlockModels.Quad face : faces) {
                if (onlyLight && !face.texture.equals(new Identifier("mtr:block/white"))) continue;
                for (int offset = 1; offset < face.vertices.length; offset += 8) value = Math.min(value, face.vertices[offset]);
            }
            return value;
        }

        private static double maxY(List<LegacyBlockModels.Quad> faces) {
            double value = Double.NEGATIVE_INFINITY;
            for (LegacyBlockModels.Quad face : faces)
                for (int offset = 1; offset < face.vertices.length; offset += 8) value = Math.max(value, face.vertices[offset]);
            return value;
        }

        private static void assertVerticalTextureSampling(List<LegacyBlockModels.Quad> faces) {
            for (LegacyBlockModels.Quad face : faces) {
                if (Math.abs(face.vertices[6]) > 0.5) continue;
                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                for (int offset = 4; offset < face.vertices.length; offset += 8) {
                    min = Math.min(min, face.vertices[offset]);
                    max = Math.max(max, face.vertices[offset]);
                }
                assertEquals(6.0 / 16, min, 0.000001, "Moving the ceiling must preserve its original texture coordinates");
                assertEquals((face.texture.equals(new Identifier("mtr:block/white")) ? 7.0 : 9.0) / 16, max, 0.000001);
            }
        }
    }
}
