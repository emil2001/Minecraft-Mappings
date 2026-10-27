package org.mtr.mapping.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class DashboardMapIdentity {
    private DashboardMapIdentity() {}

    public static Path singleplayer(Path cacheBase, Path saveDirectory, long seed, int dimension) {
        return directory(cacheBase, "singleplayer\0" + saveDirectory.toAbsolutePath().normalize() + "\0" + seed, dimension);
    }

    public static Path multiplayer(Path cacheBase, String endpoint, int dimension) {
        if (endpoint == null || endpoint.trim().isEmpty()) throw new IllegalArgumentException("Server endpoint is empty");
        return directory(cacheBase, "multiplayer\0" + endpoint.trim().toLowerCase(Locale.ROOT), dimension);
    }

    private static Path directory(Path cacheBase, String identity, int dimension) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder(64);
            for (byte value : digest) {
                key.append(Character.forDigit((value & 255) >>> 4, 16));
                key.append(Character.forDigit(value & 15, 16));
            }
            return cacheBase.resolve("world-" + key).resolve("dim_" + dimension);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
