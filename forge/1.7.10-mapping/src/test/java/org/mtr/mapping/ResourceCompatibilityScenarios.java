package org.mtr.mapping;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

public class ResourceCompatibilityScenarios {
    public void nativePackStackRetainsPriorityAndClosesEveryStreamOnConsumerFailure() throws Exception {
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            SimpleReloadableResourceManager manager = new SimpleReloadableResourceManager(new IMetadataSerializer());
            List<TrackedStream> streams = new ArrayList<>();
            LanguageResourceScenarios.MemoryPack base = new LanguageResourceScenarios.MemoryPack() {
                @Override public InputStream getInputStream(ResourceLocation id) throws IOException {
                    TrackedStream stream = new TrackedStream(super.getInputStream(id)); streams.add(stream); return stream;
                }
            };
            LanguageResourceScenarios.MemoryPack override = new LanguageResourceScenarios.MemoryPack();
            base.files.put("custom_resources.json", "Базовый");
            base.files.put("custom_resources.json.mcmeta", "{}");
            override.files.put("custom_resources.json", "Новый");
            manager.reloadResources(Arrays.asList(base, override));
            ReflectionHelper.setPrivateValue(Minecraft.class, fixture.client, manager, "mcResourceManager", "field_110451_am");
            Identifier id = new Identifier("mtr:custom_resources.json");
            assertEquals("Новый", ResourceManagerHelper.readResource(id));
            List<String> values = new ArrayList<>();
            ResourceManagerHelper.readAllResources(id, stream -> {
                String value = text(stream); values.add(value);
                if (value.equals("Базовый")) throw new IllegalArgumentException("broken custom resource");
            });
            assertEquals(Arrays.asList("Базовый", "Новый"), values);
            assertEquals(2, streams.size());
            assertTrue(streams.stream().allMatch(stream -> stream.closed));
            assertEquals("", ResourceManagerHelper.readResource(new Identifier("mtr:missing.json")));
            assertEquals(1, ResourceManagerHelper.getResourcePackVersion());
        }
    }

    public void directorySearchFindsFolderAndZipResourcesOnceAndRespectsReload() throws Exception {
        Path root = Files.createTempDirectory("mtr-resource-stack");
        Path folder = root.resolve("folder");
        Path source = folder.resolve("assets/mtr/rails/nested/track.json");
        Files.createDirectories(source.getParent());
        Files.write(source, "folder".getBytes(StandardCharsets.UTF_8));
        Files.write(source.resolveSibling("track.json.mcmeta"), "{}".getBytes(StandardCharsets.UTF_8));
        Path zip = root.resolve("pack.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            put(output, "assets/mtr/rails/nested/track.json", "zip");
            put(output, "assets/mtr/rails/second.json", "second");
            put(output, "assets/mtr/rails_extra/wrong.json", "wrong");
        }
        FileResourcePack packed = new FileResourcePack(zip.toFile());
        try (NativeClientFixture fixture = new NativeClientFixture()) {
            SimpleReloadableResourceManager manager = new SimpleReloadableResourceManager(new IMetadataSerializer());
            manager.reloadResources(Arrays.asList(new FolderResourcePack(folder.toFile()), packed));
            ReflectionHelper.setPrivateValue(Minecraft.class, fixture.client, manager, "mcResourceManager", "field_110451_am");
            List<String> values = new ArrayList<>();
            ResourceManagerHelper.readDirectory("rails", (id, stream) -> values.add(id + "=" + text(stream)));
            assertEquals(Arrays.asList("mtr:rails/nested/track.json=folder", "mtr:rails/nested/track.json=zip",
                "mtr:rails/second.json=second"), values);
            manager.reloadResources(Collections.singletonList(new FolderResourcePack(folder.toFile())));
            values.clear();
            ResourceManagerHelper.readDirectory("rails", (id, stream) -> values.add(id + "=" + text(stream)));
            assertEquals(Collections.singletonList("mtr:rails/nested/track.json=folder"), values);
            assertThrows(IllegalArgumentException.class, () -> ResourceManagerHelper.readDirectory("../rails", (id, stream) -> {}));
        } finally {
            packed.close();
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.delete(path); } catch (IOException e) { throw new UncheckedIOException(e); } });
            }
        }
    }
    private static String text(InputStream stream) {
        try { return org.apache.commons.io.IOUtils.toString(stream, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
    private static void put(ZipOutputStream output, String name, String value) throws IOException {
        output.putNextEntry(new ZipEntry(name)); output.write(value.getBytes(StandardCharsets.UTF_8)); output.closeEntry();
    }
    private static final class TrackedStream extends FilterInputStream {
        boolean closed;
        TrackedStream(InputStream input) { super(input); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
