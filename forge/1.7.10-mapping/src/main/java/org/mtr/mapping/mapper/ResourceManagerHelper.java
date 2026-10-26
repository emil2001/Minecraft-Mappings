package org.mtr.mapping.mapper;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.DummyClass;
import org.mtr.mapping.tool.LegacyResources;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.zip.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ResourceManagerHelper extends DummyClass {
    public static void readResource(Identifier id, Consumer<InputStream> consumer) {
        try { read(Minecraft.getMinecraft().getResourceManager().getResource(id.data), consumer); }
        catch (Exception exception) { logException(exception); }
    }
    public static String readResource(Identifier id) {
        String[] result = {""};
        readResource(id, input -> {
            try { result[0] = IOUtils.toString(input, StandardCharsets.UTF_8); }
            catch (IOException exception) { throw new UncheckedIOException(exception); }
        });
        return result[0];
    }
    public static void readAllResources(Identifier id, Consumer<InputStream> consumer) {
        try {
            for (Object resource : Minecraft.getMinecraft().getResourceManager().getAllResources(id.data)) read((IResource) resource, consumer);
        } catch (Exception exception) { logException(exception); }
    }
    private static void read(IResource resource, Consumer<InputStream> consumer) {
        try { consumer.accept(resource.getInputStream()); }
        catch (Exception exception) { logException(exception); }
        finally { LegacyResources.close(resource); }
    }
    public static int getResourcePackVersion() { return 1; }

    public static void readDirectory(String path, BiConsumer<Identifier, InputStream> consumer) {
        if (!path.matches("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*") || Arrays.asList(path.split("/")).contains(".."))
            throw new IllegalArgumentException("Invalid resource directory: " + path);
        try {
            SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
            Map<String, FallbackResourceManager> domains = ReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class,
                manager, "domainResourceManagers", "field_110548_a");
            Set<Identifier> ids = new TreeSet<>(Comparator.comparing(Identifier::toString));
            for (Map.Entry<String, FallbackResourceManager> domain : domains.entrySet()) {
                List<IResourcePack> packs = ReflectionHelper.getPrivateValue(FallbackResourceManager.class, domain.getValue(),
                    "resourcePacks", "field_110540_a");
                for (IResourcePack pack : packs) {
                    if (pack instanceof AbstractResourcePack) {
                        File root = ReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack) pack,
                            "resourcePackFile", "field_110597_b");
                        scan(root, domain.getKey(), path, ids);
                    } else if (pack instanceof DefaultResourcePack) {
                        Map<String, File> index = ReflectionHelper.getPrivateValue(DefaultResourcePack.class, (DefaultResourcePack) pack, "field_152781_b");
                        for (String key : index.keySet()) {
                            if (key.startsWith(domain.getKey() + ":" + path + "/") && !key.endsWith(".mcmeta")) ids.add(new Identifier(key));
                        }
                        java.security.CodeSource source = DefaultResourcePack.class.getProtectionDomain().getCodeSource();
                        if (source != null && source.getLocation().getProtocol().equals("file"))
                            scan(new File(source.getLocation().toURI()), domain.getKey(), path, ids);
                    } else {
                        org.apache.logging.log4j.LogManager.getLogger("MTRResources").warn("Cannot enumerate native resource pack: " + pack.getPackName());
                    }
                }
            }
            for (Identifier id : ids) readAllResources(id, input -> consumer.accept(id, input));
        } catch (Exception exception) { logException(exception); }
    }
    private static void scan(File root, String namespace, String path, Set<Identifier> ids) throws IOException {
        String prefix = "assets/" + namespace + "/";
        String directory = prefix + path + "/";
        if (root.isDirectory()) {
            Path start = root.toPath().resolve(directory);
            if (!Files.isDirectory(start)) return;
            try (Stream<Path> files = Files.walk(start)) {
                files.filter(Files::isRegularFile).forEach(file -> add(namespace,
                    root.toPath().relativize(file).toString().replace(File.separatorChar, '/').substring(prefix.length()), ids));
            }
        } else {
            try (ZipFile zip = new ZipFile(root)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().startsWith(directory)) add(namespace, entry.getName().substring(prefix.length()), ids);
                }
            }
        }
    }
    private static void add(String namespace, String path, Set<Identifier> ids) {
        if (path.endsWith(".mcmeta")) return;
        try { ids.add(new Identifier(namespace, path)); }
        catch (IllegalArgumentException exception) { logException(exception); }
    }
}
