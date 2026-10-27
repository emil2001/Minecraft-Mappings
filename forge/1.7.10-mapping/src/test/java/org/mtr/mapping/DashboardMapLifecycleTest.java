package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mtr.gtnh.DashboardMapCache;
import org.mtr.gtnh.DashboardMapStore;
import org.mtr.mod.screen.DashboardScreen;
import org.mtr.mod.screen.WidgetMap;
import org.mtr.mod.screen.WorldMap;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DashboardMapLifecycleTest {
    @TempDir Path directory;

    @Test void nativeScreenRemovalReleasesTheMapAndFlushesAcceptedTerrain() throws Exception {
        WorldMap map = new WorldMap();
        DashboardMapCache cache = new DashboardMapCache(directory);
        try {
            put(map, "cache", cache);
            put(map, "cacheDirectory", directory);
            // Only GUI shells bypass constructors; lifecycle dispatch, map and worker are real.
            WidgetMap widget = allocate(WidgetMap.class);
            put(widget, "worldMap", map);
            DashboardScreen screen = allocate(DashboardScreen.class);
            put(screen, "widgetMap", widget);

            int color = 0xFF123456;
            int[] chunk = new int[256];
            Arrays.fill(chunk, color);
            assertTrue(cache.updateChunk(1, 2, chunk));

            screen.onGuiClosed();

            assertNull(get(map, "cache"), "Native screen removal must release its cache reference");
            assertNull(get(map, "cacheDirectory"));
            assertTrue(cache.awaitClosed(5, TimeUnit.SECONDS), "Removed Dashboard left its cache worker alive");
            int[] saved = new DashboardMapStore(directory).read(0, 0);
            assertNotNull(saved, "Accepted terrain must flush when the native GUI is removed");
            assertEquals(color, saved[32 * 256 + 16]);
            assertEquals(0, saved[0], "Closing must preserve unsampled pixels as transparent");
        } finally {
            cache.close();
            assertTrue(cache.awaitClosed(5, TimeUnit.SECONDS));
        }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static void put(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
