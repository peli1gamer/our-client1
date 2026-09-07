package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lightweight JSON schematic format used by the client builder. */
public record Schematic(String name, List<BlockEntry> blocks) {
    private static final Gson GSON = new Gson();
    private static final int MAX_BLOCKS = 100_000;
    private static final int MAX_COORDINATE = 30_000_000;

    public static Schematic parse(String json, String fallbackName) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Schematic is empty");
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) throw new IllegalArgumentException("Schematic root must be an object");

        String name = fallbackName;
        JsonElement nameElement = root.get("name");
        if (nameElement != null && !nameElement.isJsonNull()) {
            name = nameElement.getAsString();
        }
        if (name == null || name.isBlank()) name = "schematic";

        JsonArray array = root.getAsJsonArray("blocks");
        if (array == null) throw new IllegalArgumentException("Schematic has no blocks array");
        if (array.size() > MAX_BLOCKS) {
            throw new IllegalArgumentException("Schematic contains too many blocks (max " + MAX_BLOCKS + ")");
        }

        List<BlockEntry> entries = new ArrayList<>(array.size());
        Set<Position> positions = new HashSet<>(Math.max(16, array.size() * 2));
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException("Each schematic block must be an object");
            }
            JsonObject block = element.getAsJsonObject();
            int x = requiredInt(block, "x");
            int y = requiredInt(block, "y");
            int z = requiredInt(block, "z");
            if (Math.abs(x) > MAX_COORDINATE || Math.abs(y) > MAX_COORDINATE || Math.abs(z) > MAX_COORDINATE) {
                throw new IllegalArgumentException("Schematic coordinate is out of bounds");
            }
            String id = requiredString(block, "block");
            Identifier key = Identifier.parse(id);
            Holder.Reference<Block> registered = BuiltInRegistries.BLOCK.get(key)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown block: " + id));
            if (!positions.add(new Position(x, y, z))) {
                throw new IllegalArgumentException("Duplicate schematic position: " + x + "," + y + "," + z);
            }
            BlockState state = registered.value().defaultBlockState();
            entries.add(new BlockEntry(x, y, z, state));
        }
        return new Schematic(name, List.copyOf(entries));
    }

    private static int requiredInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing or invalid schematic field: " + key);
        }
        return value.getAsInt();
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing or invalid schematic field: " + key);
        }
        String result = value.getAsString();
        if (result.isBlank()) throw new IllegalArgumentException("Schematic field is blank: " + key);
        return result;
    }

    private record Position(int x, int y, int z) { }

    public record BlockEntry(int x, int y, int z, BlockState state) { }
}
