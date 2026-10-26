package org.mtr.mapping.tool;

import com.google.gson.*;
import org.mtr.mapping.holder.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** The item pools and conditions used by the pinned MTR resources. */
public final class BlockLootTable {
    private static final Map<Identifier, Optional<BlockLootTable>> TABLES = new ConcurrentHashMap<>();
    private final JsonArray pools;
    private boolean explosionDecay;
    private boolean itemsAvailable = true;

    public BlockLootTable(Reader reader) {
        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
        require(root.get("type").getAsString().equals("minecraft:block"), "Unsupported loot table type");
        pools = root.getAsJsonArray("pools");
        for (JsonElement value : pools) {
            JsonObject pool = value.getAsJsonObject();
            require(pool.get("rolls").getAsInt() == 1, "Unsupported loot pool rolls");
            validateConditions(pool);
            JsonArray entries = pool.getAsJsonArray("entries");
            require(entries.size() == 1, "Weighted loot pools are not used by MTR 4.0.5");
            JsonObject entry = entries.get(0).getAsJsonObject();
            require(entry.get("type").getAsString().equals("minecraft:item"), "Unsupported loot entry type");
            new Identifier(entry.get("name").getAsString());
            validateConditions(entry);
            if (entry.has("functions")) for (JsonElement functionValue : entry.getAsJsonArray("functions")) {
                JsonObject function = functionValue.getAsJsonObject();
                validateConditions(function);
                switch (function.get("function").getAsString()) {
                    case "minecraft:set_count":
                        int count = function.get("count").getAsInt();
                        require(count >= 0 && count <= 64, "Invalid loot stack count");
                        break;
                    case "minecraft:explosion_decay": explosionDecay = true; break;
                    default: throw new IllegalArgumentException("Unsupported loot function " + function);
                }
            }
        }
    }

    public static BlockLootTable forBlock(Block block) {
        Identifier id = new Identifier(net.minecraft.block.Block.blockRegistry.getNameForObject(block.data));
        return TABLES.computeIfAbsent(id, key -> {
            String path = "data/" + key.getNamespace() + "/loot_tables/blocks/" + key.getPath() + ".json";
            InputStream stream = BlockLootTable.class.getClassLoader().getResourceAsStream(path);
            if (stream == null) return Optional.empty();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return Optional.of(new BlockLootTable(reader));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalArgumentException("Invalid loot table " + id, exception);
            }
        }).orElse(null);
    }

    public void validateItems() {
        try {
            for (JsonElement pool : pools) for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                item(entry.getAsJsonObject().get("name").getAsString());
            }
        } catch (IllegalArgumentException exception) {
            // Modern loot loading discards unknown item references without aborting mod initialization.
            itemsAvailable = false;
            org.apache.logging.log4j.LogManager.getLogger("MTRLoot").warn("Ignoring invalid loot table: " + exception.getMessage());
        }
    }
    public boolean hasExplosionDecay() { return explosionDecay; }

    public ArrayList<net.minecraft.item.ItemStack> drops(BlockState state) {
        ArrayList<net.minecraft.item.ItemStack> result = new ArrayList<>();
        if (!itemsAvailable) return result;
        for (JsonElement value : pools) {
            JsonObject pool = value.getAsJsonObject();
            if (!matches(pool, state)) continue;
            for (JsonElement entryValue : pool.getAsJsonArray("entries")) {
                JsonObject entry = entryValue.getAsJsonObject();
                if (!matches(entry, state)) continue;
                int count = 1;
                if (entry.has("functions")) for (JsonElement functionValue : entry.getAsJsonArray("functions")) {
                    JsonObject function = functionValue.getAsJsonObject();
                    if (function.get("function").getAsString().equals("minecraft:set_count") && matches(function, state)) count = function.get("count").getAsInt();
                }
                if (count > 0) result.add(new net.minecraft.item.ItemStack(item(entry.get("name").getAsString()), count));
            }
        }
        return result;
    }

    private static net.minecraft.item.Item item(String id) {
        net.minecraft.item.Item item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry.getObject(id);
        require(item != null, "Unregistered loot item " + id);
        return item;
    }

    private static void validateConditions(JsonObject node) {
        if (node.has("conditions")) for (JsonElement condition : node.getAsJsonArray("conditions")) validateCondition(condition.getAsJsonObject());
    }
    private static void validateCondition(JsonObject condition) {
        switch (condition.get("condition").getAsString()) {
            case "minecraft:survives_explosion": break;
            case "minecraft:block_state_property":
                new Identifier(condition.get("block").getAsString());
                condition.getAsJsonObject("properties");
                break;
            case "minecraft:any_of":
                for (JsonElement term : condition.getAsJsonArray("terms")) validateCondition(term.getAsJsonObject());
                break;
            default: throw new IllegalArgumentException("Unsupported loot condition " + condition);
        }
    }

    private static boolean matches(JsonObject node, BlockState state) {
        if (node.has("conditions")) for (JsonElement condition : node.getAsJsonArray("conditions")) {
            if (!matchesCondition(condition.getAsJsonObject(), state)) return false;
        }
        return true;
    }
    private static boolean matchesCondition(JsonObject condition, BlockState state) {
        switch (condition.get("condition").getAsString()) {
            // Forge supplies explosion probability through dropBlockAsItemWithChance.
            case "minecraft:survives_explosion": return true;
            case "minecraft:any_of":
                for (JsonElement term : condition.getAsJsonArray("terms")) if (matchesCondition(term.getAsJsonObject(), state)) return true;
                return false;
            case "minecraft:block_state_property":
                if (!condition.get("block").getAsString().equals(net.minecraft.block.Block.blockRegistry.getNameForObject(state.getBlock().data))) return false;
                for (Map.Entry<String, JsonElement> property : condition.getAsJsonObject("properties").entrySet()) {
                    if (!property.getValue().getAsString().equals(state.data.getSerializedProperties().get(property.getKey()))) return false;
                }
                return true;
            default: throw new IllegalArgumentException("Unsupported loot condition");
        }
    }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
