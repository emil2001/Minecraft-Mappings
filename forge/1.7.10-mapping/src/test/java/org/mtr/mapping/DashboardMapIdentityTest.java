package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.tool.DashboardMapIdentity;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DashboardMapIdentityTest {
    private final Path cacheBase = Paths.get("map-cache");

    @Test void reopeningTheSameSaveUsesItsNormalizedAbsoluteIdentity() {
        Path save = Paths.get("worlds", "My Save");
        Path first = DashboardMapIdentity.singleplayer(cacheBase, save, Long.MIN_VALUE, 0);
        assertEquals(first, DashboardMapIdentity.singleplayer(cacheBase,
            save.toAbsolutePath().resolve("..").resolve("My Save"), Long.MIN_VALUE, 0));
        assertEquals(first, DashboardMapIdentity.singleplayer(cacheBase, save, Long.MIN_VALUE, 0));
    }

    @Test void saveLocationSeedAndDimensionKeepTerrainSeparate() {
        Path save = Paths.get("worlds", "one");
        Path first = DashboardMapIdentity.singleplayer(cacheBase, save, 42, 0);
        assertNotEquals(first, DashboardMapIdentity.singleplayer(cacheBase, Paths.get("worlds", "two"), 42, 0));
        assertNotEquals(first, DashboardMapIdentity.singleplayer(cacheBase, save, 43, 0));
        Path nether = DashboardMapIdentity.singleplayer(cacheBase, save, 42, -1);
        assertNotEquals(first, nether);
        assertEquals(first.getParent(), nether.getParent());
        assertEquals("dim_-1", nether.getFileName().toString());
    }

    @Test void serverEndpointNormalizationSurvivesReconnects() {
        Path first = DashboardMapIdentity.multiplayer(cacheBase, "EXAMPLE.ORG:25565", 0);
        assertEquals(first, DashboardMapIdentity.multiplayer(cacheBase, "  example.org:25565  ", 0));
        assertNotEquals(first, DashboardMapIdentity.multiplayer(cacheBase, "other.org:25565", 0));
        assertNotEquals(first, DashboardMapIdentity.multiplayer(cacheBase, "example.org:25566", 0));
        assertNotEquals(first, DashboardMapIdentity.multiplayer(cacheBase, "example.org:25565", 1));
    }

    @Test void singleplayerAndMultiplayerUseSeparateNamespaces() {
        Path save = Paths.get("worlds", "one");
        assertNotEquals(DashboardMapIdentity.singleplayer(cacheBase, save, 42, 0),
            DashboardMapIdentity.multiplayer(cacheBase, save.toAbsolutePath().normalize().toString(), 0));
    }

    @Test void rawServerAddressesCannotEscapeTheCacheDirectory() {
        Path result = DashboardMapIdentity.multiplayer(cacheBase, "../../elsewhere/[::1]:25565", -7);
        assertEquals(cacheBase, result.getParent().getParent());
        assertTrue(result.getParent().getFileName().toString().matches("world-[0-9a-f]{64}"));
        assertEquals("dim_-7", result.getFileName().toString());
    }

    @Test void blankServerEndpointsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DashboardMapIdentity.multiplayer(cacheBase, "", 0));
        assertThrows(IllegalArgumentException.class, () -> DashboardMapIdentity.multiplayer(cacheBase, " \t\n", 0));
    }
}
