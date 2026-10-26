package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

public final class WorldSavePath extends HolderBase<String> {
    public WorldSavePath(String path) { super(path); }
    public String getRelativePath() { return data; }
    public static WorldSavePath getRootMapped() { return new WorldSavePath(""); }
}
