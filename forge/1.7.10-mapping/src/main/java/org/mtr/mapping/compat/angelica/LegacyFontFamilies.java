package org.mtr.mapping.compat.angelica;

import com.google.gson.*;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.tool.LegacyResources;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads the same priority-ordered provider stack as modern font resources. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyFontFamilies implements IResourceManagerReloadListener, AutoCloseable {
    private IResourceManager resources;
    private final Map<ResourceLocation, Family> families = new HashMap<>();
    private final Set<LegacyTtfProvider> owned = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Choice NATIVE = new Choice(null);
    public LegacyFontFamilies(IResourceManager resources) { this.resources = Objects.requireNonNull(resources); }

    public FontProvider find(ResourceLocation font, char character) {
        try {
            Choice choice = family(font, new LinkedHashSet<>()).find(character);
            return choice == null ? null : choice.provider;
        } catch (IOException exception) { throw new IllegalStateException("Cannot load font " + font, exception); }
    }
    private Family family(ResourceLocation font, Set<ResourceLocation> visiting) throws IOException {
        Family cached = families.get(font);
        if (cached != null) return cached;
        if (!visiting.add(font)) throw new IOException("Cyclic font reference: " + visiting + " -> " + font);
        List<IResource> stack = resources.getAllResources(asset(font, ".json"));
        List<Entry> entries = new ArrayList<>();
        List<LegacyTtfProvider> created = new ArrayList<>();
        try {
            for (int i = stack.size() - 1; i >= 0; i--) {
                JsonArray providers = new JsonParser().parse(new InputStreamReader(stack.get(i).getInputStream(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("providers");
                if (providers == null) throw new IOException("Missing font providers: " + font);
                for (JsonElement value : providers) {
                    JsonObject provider = value.getAsJsonObject();
                    String type = provider.get("type").getAsString();
                    if (type.startsWith("minecraft:")) type = type.substring(10);
                    if (type.equals("ttf")) {
                        ResourceLocation file = new Identifier(provider.get("file").getAsString()).data;
                        float size = number(provider, "size", 11), oversample = number(provider, "oversample", 1);
                        JsonArray shift = provider.getAsJsonArray("shift");
                        if (shift != null && shift.size() != 2) throw new IOException("Invalid TTF shift: " + font);
                        IResource resource = resources.getResource(asset(file, ""));
                        LegacyTtfProvider ttf;
                        try {
                            ttf = new LegacyTtfProvider(Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream()), size, oversample,
                                shift == null ? 0 : shift.get(0).getAsFloat(), shift == null ? 0 : shift.get(1).getAsFloat(),
                                provider.has("skip") ? provider.get("skip").getAsString() : "");
                        } catch (FontFormatException exception) { throw new IOException("Invalid TTF font: " + file, exception); }
                        finally { LegacyResources.close(resource); }
                        created.add(ttf);
                        Choice choice = new Choice(ttf);
                        entries.add(character -> ttf.isGlyphAvailable(character) ? choice : null);
                    } else if (type.equals("reference")) {
                        ResourceLocation reference = new Identifier(provider.get("id").getAsString()).data;
                        String id = reference.toString();
                        if (id.equals("minecraft:include/space")) entries.add(character -> character == ' ' ? NATIVE : null);
                        else if (id.equals("minecraft:include/default") || id.equals("minecraft:include/unifont")) entries.add(character -> NATIVE);
                        else {
                            Family referenced = family(reference, visiting);
                            entries.add(referenced::find);
                        }
                    } else throw new IOException("Unsupported font provider " + type + " in " + font);
                }
            }
            Family family = new Family(entries);
            families.put(font, family);
            owned.addAll(created);
            return family;
        } catch (IOException | RuntimeException exception) {
            created.forEach(LegacyTtfProvider::close);
            throw exception;
        } finally {
            for (IResource resource : stack) LegacyResources.close(resource);
            visiting.remove(font);
        }
    }
    private static float number(JsonObject object, String key, float fallback) { return object.has(key) ? object.get(key).getAsFloat() : fallback; }
    private static ResourceLocation asset(ResourceLocation id, String suffix) throws IOException {
        String path = id.getResourcePath();
        if (Arrays.asList(path.split("/")).contains("..")) throw new IOException("Invalid font asset path: " + id);
        return new ResourceLocation(id.getResourceDomain(), "font/" + path + suffix);
    }
    @Override public void onResourceManagerReload(IResourceManager resources) { close(); this.resources = Objects.requireNonNull(resources); }
    @Override public void close() {
        owned.forEach(LegacyTtfProvider::close);
        owned.clear(); families.clear();
    }
    private interface Entry { Choice find(char character); }
    private static final class Choice {
        final FontProvider provider;
        Choice(FontProvider provider) { this.provider = provider; }
    }
    private static final class Family {
        final List<Entry> entries;
        Family(List<Entry> entries) { this.entries = entries; }
        Choice find(char character) {
            for (Entry entry : entries) { Choice choice = entry.find(character); if (choice != null) return choice; }
            return null;
        }
    }
}
