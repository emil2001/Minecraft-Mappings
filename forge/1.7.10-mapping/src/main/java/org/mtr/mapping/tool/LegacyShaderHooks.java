package org.mtr.mapping.tool;

import java.lang.reflect.Method;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyShaderHooks {
    private LegacyShaderHooks() {}
    private static final class Iris {
        static final Object API;
        static final Method SHADOW;
        static {
            Object api=null; Method shadow=null;
            try {
                Class<?> type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api=type.getMethod("getInstance").invoke(null); shadow=type.getMethod("isRenderingShadowPass");
            } catch (ClassNotFoundException absent) {
                // Iris is optional on legacy clients.
            } catch (ReflectiveOperationException exception) { throw new ExceptionInInitializerError(exception); }
            API=api; SHADOW=shadow;
        }
    }
    public static boolean isRenderingShadowPass() {
        if (!cpw.mods.fml.common.Loader.isModLoaded("angelica") && !cpw.mods.fml.common.Loader.isModLoaded("iris")) return false;
        if (Iris.SHADOW==null) return false;
        try { return (Boolean)Iris.SHADOW.invoke(Iris.API); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot read Iris shadow pass",exception); }
    }
}
