package org.mtr.mapping.holder;

import org.mtr.mapping.tool.HolderBase;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class RenderLayer extends HolderBase<RenderLayer.State> {
    public static final class State {
        public final Identifier texture;
        public final boolean translucent, cull, lightmap, lighting, depthWrite, gui;
        public final int mode;
        private State(Identifier texture, boolean translucent, boolean cull, boolean lightmap, boolean lighting, boolean depthWrite, boolean gui, int mode) {
            this.texture=texture; this.translucent=translucent; this.cull=cull; this.lightmap=lightmap;
            this.lighting=lighting; this.depthWrite=depthWrite; this.gui=gui; this.mode=mode;
        }
    }
    private RenderLayer(Identifier texture, boolean translucent, boolean cull, boolean lightmap, boolean lighting, boolean depthWrite, boolean gui, int mode) {
        super(new State(texture, translucent, cull, lightmap, lighting, depthWrite, gui, mode));
    }
    public static RenderLayer getCutout() { return new RenderLayer(new Identifier("minecraft:textures/atlas/blocks.png"), false, true, true, false, true, false, 7); }
    public static RenderLayer getTranslucent() { return new RenderLayer(new Identifier("minecraft:textures/atlas/blocks.png"), true, true, true, false, true, false, 7); }
    public static RenderLayer getEntityCutout(Identifier texture) { return new RenderLayer(texture, false, true, true, true, true, false, 7); }
    public static RenderLayer getEntityTranslucentCull(Identifier texture) { return new RenderLayer(texture, true, true, true, true, true, false, 7); }
    public static RenderLayer getBeaconBeam(Identifier texture, boolean translucent) { return new RenderLayer(texture, translucent, false, false, false, !translucent, false, 7); }
    public static RenderLayer forCompiledModel(Identifier texture, boolean translucent, boolean glowing) {
        // Modern compiled glowing cutout blends alpha while retaining depth writes and culling.
        return new RenderLayer(texture, translucent || glowing, true, !glowing, !glowing, !translucent || !glowing, false, 7);
    }
    public static RenderLayer getText(Identifier texture) { return new RenderLayer(texture, true, false, true, false, true, false, 7); }
    public static RenderLayer getLines() { return new RenderLayer(null, true, false, false, false, false, false, 1); }
    public static RenderLayer forGui(Identifier texture) { return new RenderLayer(texture, true, false, false, false, false, true, 7); }
}
