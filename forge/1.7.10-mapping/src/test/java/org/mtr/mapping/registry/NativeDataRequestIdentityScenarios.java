package org.mtr.mapping.registry;

import org.mtr.core.data.ClientData;
import org.mtr.core.data.Position;
import org.mtr.core.operation.DataRequest;
import org.mtr.core.servlet.OperationProcessor;
import org.mtr.core.servlet.QueueObject;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;
import org.mtr.mod.packet.PacketRequestData;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

public final class NativeDataRequestIdentityScenarios {
    public void remoteRequestUsesAuthenticatedPlayerIdentityWhilePreservingPayload() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture()) {
            UUID nativeId = fixture.player.getUniqueID();
            UUID claimedId = new UUID(nativeId.getMostSignificantBits(), nativeId.getLeastSignificantBits() ^ 1);
            assertNotEquals(nativeId, claimedId);
            DataRequest request = requestWithExistingIds(claimedId);
            JsonObject expected = Utilities.getJsonObjectFromData(request);
            expected.addProperty("clientId", nativeId.toString());

            DataRequest forwarded = forward(fixture, request);
            assertEquals(nativeId, coreClientId(forwarded),
                "Core subscriptions must identify the authenticated server player, not a launcher-supplied UUID");
            assertEquals(expected, Utilities.getJsonObjectFromData(forwarded),
                "Identity binding must preserve the requested position, radius and all six existing-ID lists");
        }
    }

    public void matchingIdentityKeepsRequestDataUnchanged() throws Exception {
        try (ServerLifecycleScenarios.ServerFixture fixture = new ServerLifecycleScenarios.ServerFixture()) {
            DataRequest request = requestWithExistingIds(fixture.player.getUniqueID());
            DataRequest forwarded = forward(fixture, request);
            assertEquals(fixture.player.getUniqueID(), coreClientId(forwarded));
            assertEquals(Utilities.getJsonObjectFromData(request), Utilities.getJsonObjectFromData(forwarded));
        }
    }

    private static DataRequest requestWithExistingIds(UUID clientId) {
        ClientData existing = new ClientData();
        existing.stationIdMap.put(11L, null); existing.stationIdMap.put(12L, null);
        existing.platformIdMap.put(21L, null); existing.platformIdMap.put(22L, null);
        existing.sidingIdMap.put(31L, null); existing.sidingIdMap.put(32L, null);
        existing.simplifiedRouteIds.add(41L); existing.simplifiedRouteIds.add(42L);
        existing.depotIdMap.put(51L, null); existing.depotIdMap.put(52L, null);
        existing.railIdMap.put("rail-negative-coordinates", null);
        existing.railIdMap.put("rail-upper-floor", null);
        DataRequest request = new DataRequest(clientId, new Position(-1001, 97, 2003), 432);
        request.writeExistingIds(existing);
        JsonObject json = Utilities.getJsonObjectFromData(request);
        for (String key : new String[]{"existingStationIds", "existingPlatformIds", "existingSidingIds",
                "existingSimplifiedRouteIds", "existingDepotIds", "existingRailIds"}) {
            assertEquals(2, json.getAsJsonArray(key).size(), "The preservation regression needs populated " + key);
        }
        return request;
    }

    private static DataRequest forward(ServerLifecycleScenarios.ServerFixture fixture, DataRequest request) {
        AtomicReference<QueueObject> captured = new AtomicReference<>();
        BiConsumer<World, QueueObject> previous = Init.forward;
        Init.forward = (world, queued) -> {
            assertSame(fixture.world, world.data, "The request must use its authenticated sender's world");
            assertEquals(OperationProcessor.GET_DATA, queued.key);
            assertNull(captured.getAndSet(queued), "Exactly one Core request must be forwarded");
        };
        try {
            new PacketRequestData(request).runServer(new MinecraftServer(fixture.server), new ServerPlayerEntity(fixture.player));
            assertNotNull(captured.get());
            assertTrue(captured.get().data instanceof DataRequest);
            return (DataRequest) captured.get().data;
        } finally {
            Init.forward = previous;
        }
    }

    private static UUID coreClientId(DataRequest request) throws Exception {
        // Core uses this parsed UUID for its live subscription, independently of serialized clientId.
        Field field = DataRequest.class.getDeclaredField("uuid");
        field.setAccessible(true);
        return (UUID) field.get(request);
    }
}
