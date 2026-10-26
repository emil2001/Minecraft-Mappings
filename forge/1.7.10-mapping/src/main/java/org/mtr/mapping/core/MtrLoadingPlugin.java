package org.mtr.mapping.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.Name("MTR GTNH factories")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"org.mtr.mapping.core"})
public final class MtrLoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[]{TileEntityFactoryTransformer.class.getName(), NeighborUpdateTransformer.class.getName(), EntityFactoryTransformer.class.getName(), EntitySpawnLookupTransformer.class.getName(), SoundResolverTransformer.class.getName(), PlayerRenderOffsetTransformer.class.getName(), StyledTextTransformer.class.getName(), RandomBlockTickTransformer.class.getName(), PlayerPhysicsTransformer.class.getName(), NativeArrivalTransformer.class.getName(), WaterloggingTransformer.class.getName()}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {}
    public String getAccessTransformerClass() { return null; }
}
