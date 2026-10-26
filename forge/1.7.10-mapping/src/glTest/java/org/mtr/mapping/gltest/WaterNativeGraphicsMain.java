package org.mtr.mapping.gltest;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import java.io.File;
import java.net.URL;
import java.util.*;

/** Runs real legacy rendering in an isolated hidden native GL window; never starts Minecraft. */
public final class WaterNativeGraphicsMain {
    public static void main(String[] args) throws Throwable {
        if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO)) throw new AssertionError("SDL video init failed");
        long window = 0, context = 0;
        try {
            SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, 2);
            SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION, 1);
            window = SDLVideo.SDL_CreateWindow("MTR graphics compatibility test", 64, 64,
                SDLVideo.SDL_WINDOW_OPENGL | SDLVideo.SDL_WINDOW_HIDDEN);
            if (window == 0) throw new AssertionError("Hidden window creation failed");
            context = SDLVideo.SDL_GL_CreateContext(window);
            if (context == 0 || !SDLVideo.SDL_GL_MakeCurrent(window, context)) throw new AssertionError("GL context creation failed");
            GL.create(SDLVideo::SDL_GL_GetProcAddress); GL.createCapabilities();
            System.out.println("Native GL: " + GL11.glGetString(GL11.GL_VERSION) + " / " + GL11.glGetString(GL11.GL_RENDERER));
            List<URL> urls = new ArrayList<>();
            for (String path : System.getProperty("mtr.glClasspath").split(File.pathSeparator)) urls.add(new File(path).toURI().toURL());
            try (LaunchClassLoader loader = new LaunchClassLoader(urls.toArray(new URL[0]))) {
                loader.addClassLoaderExclusion("org.lwjgl."); loader.addClassLoaderExclusion("org.lwjglx.");
                loader.addClassLoaderExclusion("org.junit."); loader.addClassLoaderExclusion("org.opentest4j.");
                loader.addClassLoaderExclusion("org.apiguardian."); loader.addClassLoaderExclusion("org.mtr.mapping.gltest.");
                loader.addTransformerExclusion("com.gtnewhorizons.angelica.client.font.");
                loader.addTransformerExclusion("jss.");
                Launch.classLoader = loader; Launch.blackboard = new HashMap<>();
                loader.registerTransformer(WaterLegacyGlRedirect.class.getName());
                loader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
                loader.registerTransformer("org.mtr.mapping.core.TileEntityFactoryTransformer");
                loader.registerTransformer("org.mtr.mapping.core.NeighborUpdateTransformer");
                loader.registerTransformer("org.mtr.mapping.core.PlayerRenderOffsetTransformer");
                loader.registerTransformer("org.mtr.mapping.core.PlayerPhysicsTransformer");
                loader.registerTransformer("org.mtr.mapping.core.WaterloggingTransformer");
                loader.registerTransformer("org.mtr.mapping.core.StyledTextTransformer");
                loader.registerTransformer("org.mtr.mapping.core.RandomBlockTickTransformer");
                loader.registerTransformer("org.mtr.mapping.core.EntityFactoryTransformer");
                loader.registerTransformer("org.mtr.mapping.core.EntitySpawnLookupTransformer");
                loader.registerTransformer("org.mtr.mapping.core.SoundResolverTransformer");
                try {
                    loader.loadClass("org.mtr.mapping.WaterGraphicsScenarios").getMethod("run").invoke(null);
                } catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
            }
        } finally {
            GL.setCapabilities(null);
            if (context != 0) SDLVideo.SDL_GL_DestroyContext(context);
            if (window != 0) SDLVideo.SDL_DestroyWindow(window);
            SDLInit.SDL_Quit();
        }
    }
}
