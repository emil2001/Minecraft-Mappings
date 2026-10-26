package org.mtr.mapping;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.tool.BlockLootTable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class BlockLootTableTest {
    @Test void allPinnedLootResourcesHaveSupportedConditionsAndFunctions() throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get(System.getProperty("mtr.lootResources")))) {
            List<Path> tables = paths.filter(path -> path.toString().contains("/loot_tables/blocks/") && path.toString().endsWith(".json")).collect(Collectors.toList());
            assertFalse(tables.isEmpty());
            for (Path path : tables) try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                assertDoesNotThrow(() -> new BlockLootTable(reader), path.toString());
            }
        }
    }
    @Test void unsupportedLootConditionIsRejectedInsteadOfDroppingUnconditionally() {
        String json = "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"mtr:escalator\",\"conditions\":[{\"condition\":\"minecraft:unknown\"}]}]}]}";
        assertThrows(IllegalArgumentException.class, () -> new BlockLootTable(new StringReader(json)));
    }
}
