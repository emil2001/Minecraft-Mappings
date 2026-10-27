package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.gtnh.DashboardMapView;
import org.mtr.mod.screen.WorldMap;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DashboardMapViewportTest {
    @Test void fractionalEdgesIncludeTheVisibleRightAndBottomRegions() throws Exception {
        WorldMap map = new WorldMap();
        map.setViewBounds(-.25, -.25, 256.5, 256.5);
        Field field = WorldMap.class.getDeclaredField("visibleRegions");
        field.setAccessible(true);
        List<?> regions = (List<?>) field.get(map);
        assertEquals(9, regions.size());
        assertTrue(regions.contains(DashboardMapView.key(1, 1)));
        assertTrue(regions.contains(DashboardMapView.key(-1, -1)));
    }
}
