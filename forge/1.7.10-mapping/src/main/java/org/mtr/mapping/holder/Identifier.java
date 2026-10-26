package org.mtr.mapping.holder;

import net.minecraft.util.ResourceLocation;
import org.mtr.mapping.tool.HolderBase;

public final class Identifier extends HolderBase<ResourceLocation> {
    public Identifier(String namespace, String path) {
        super(new ResourceLocation(namespace, path));
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/._-]*")) throw new IllegalArgumentException("Invalid resource identifier");
    }
    public Identifier(String value) {
        this(value.contains(":") ? value.substring(0, value.indexOf(':')) : "minecraft",
            value.contains(":") ? value.substring(value.indexOf(':') + 1) : value);
    }
    public String getNamespace() { return data.getResourceDomain(); }
    public String getPath() { return data.getResourcePath(); }
    @Override public String toString() { return data.toString(); }
    @Override public boolean equals(Object other) { return other instanceof Identifier && data.equals(((Identifier) other).data); }
    @Override public int hashCode() { return data.hashCode(); }
}
