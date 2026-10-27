package org.mtr.mapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeDataRequestIdentityTest {
    @BeforeAll static void setup() throws Throwable { BlockPlacementIntegrationTest.setupForgeLoader(); }
    @AfterAll static void close() throws Exception { BlockPlacementIntegrationTest.closeForgeLoader(); }

    @Test void remoteRequestUsesAuthenticatedPlayerIdentityWhilePreservingPayload() throws Throwable {
        run("remoteRequestUsesAuthenticatedPlayerIdentityWhilePreservingPayload");
    }

    @Test void matchingIdentityKeepsRequestDataUnchanged() throws Throwable {
        run("matchingIdentityKeepsRequestDataUnchanged");
    }

    private static void run(String name) throws Throwable {
        BlockPlacementIntegrationTest.invoke(BlockPlacementIntegrationTest.loader
            .loadClass("org.mtr.mapping.registry.NativeDataRequestIdentityScenarios").getConstructor().newInstance(), name);
    }
}
