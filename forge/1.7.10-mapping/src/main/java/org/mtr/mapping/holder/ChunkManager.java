package org.mtr.mapping.holder;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.mtr.mapping.tool.NativeHolder;

public final class ChunkManager extends NativeHolder<IChunkProvider> {
    public ChunkManager(IChunkProvider data) { super(data); }
    public WorldChunk getWorldChunk(int x, int z) {
        if (!data.chunkExists(x, z)) return null;
        Chunk chunk = data.provideChunk(x, z);
        // Client chunkExists always returns true; its absent chunks are represented by EmptyChunk.
        return chunk == null || chunk instanceof EmptyChunk ? null : new WorldChunk(chunk);
    }
    public boolean isChunkLoaded(int x, int z) { return getWorldChunk(x, z) != null; }
}
