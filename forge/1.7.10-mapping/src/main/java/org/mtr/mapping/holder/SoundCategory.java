package org.mtr.mapping.holder;

// Kept independent of client-only native SoundCategory so server sound packets can use it.
public enum SoundCategory {
    BLOCKS;
    public static SoundCategory getBlocksMapped() { return BLOCKS; }
}
