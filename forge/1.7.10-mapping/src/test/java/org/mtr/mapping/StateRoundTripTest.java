package org.mtr.mapping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompressedStreamTools;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.tool.BlockStateStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StateRoundTripTest {
    @Test
    void metadataSchemaDoesNotDependOnMapIterationOrder() {
        Map<String, List<String>> forward = new LinkedHashMap<>();
        forward.put("a", Arrays.asList("0", "1"));
        forward.put("b", Arrays.asList("0", "1"));
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        reverse.put("b", forward.get("b"));
        reverse.put("a", forward.get("a"));
        Map<String, String> state = new LinkedHashMap<>();
        state.put("a", "1");
        state.put("b", "0");
        int metadata = new BlockStateStorage(forward).write(state, new NBTTagCompound());
        assertEquals(state, new BlockStateStorage(reverse).read(metadata, new NBTTagCompound()));
    }

    @Test
    void all192EscalatorStatesSurviveBeyondFourBitMetadata() throws java.io.IOException {
        Map<String, List<String>> domains = new LinkedHashMap<>();
        domains.put("facing", Arrays.asList("north", "east", "south", "west"));
        domains.put("direction", Arrays.asList("false", "true"));
        domains.put("orientation", Arrays.asList("landing_bottom", "landing_top", "flat", "slope", "transition_bottom", "transition_top"));
        domains.put("side", Arrays.asList("left", "right"));
        domains.put("status", Arrays.asList("false", "true"));
        BlockStateStorage storage = new BlockStateStorage(domains);
        assertTrue(storage.requiresTileEntity());
        int checked = 0;
        for (String facing : domains.get("facing")) for (String direction : domains.get("direction"))
        for (String orientation : domains.get("orientation")) for (String side : domains.get("side"))
        for (String status : domains.get("status")) {
            Map<String, String> state = new LinkedHashMap<>();
            state.put("facing", facing); state.put("direction", direction);
            state.put("orientation", orientation); state.put("side", side); state.put("status", status);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("floor_number", "Подвал");
            int metadata = storage.write(state, tag);
            assertTrue(metadata >= 0 && metadata < 16);
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, bytes);
            NBTTagCompound loaded = CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(state, storage.read(metadata, loaded));
            assertEquals("Подвал", tag.getString("floor_number"));
            checked++;
        }
        assertEquals(192, checked);
    }

    @Test
    void simpleStatesNeedOnlyMetadataAndUnknownValuesCannotBeLostSilently() {
        BlockStateStorage storage = new BlockStateStorage(Collections.singletonMap(
            "facing", Arrays.asList("north", "east", "south", "west")));
        assertFalse(storage.requiresTileEntity());
        Map<String, String> state = Collections.singletonMap("facing", "west");
        NBTTagCompound tag = new NBTTagCompound();
        int metadata = storage.write(state, tag);
        assertEquals(state, storage.read(metadata, new NBTTagCompound()));
        assertTrue(tag.hasNoTags());
        assertThrows(IllegalArgumentException.class, () -> storage.write(Collections.singletonMap("facing", "diagonal"), tag));
        assertThrows(IllegalArgumentException.class, () -> storage.read(16, tag));
    }
}
