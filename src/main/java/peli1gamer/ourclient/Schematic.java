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
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lightweight JSON schematic format used by the client builder. */
public record Schematic(String name, List<BlockEntry> blocks) {
    private static final Gson GSON = new Gson();
    private static final int MAX_BLOCKS = 100_000;
    private static final int MAX_COORDINATE = 30_000_000;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_BLOCK_ID_LENGTH = 256;

    public static Schematic parse(String json, String fallbackName) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Schematic is empty");
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) throw new IllegalArgumentException("Schematic root must be an object");

        String name = fallbackName;
        JsonElement nameElement = root.get("name");
        if (nameElement != null && !nameElement.isJsonNull()) {
            if (!nameElement.isJsonPrimitive() || !nameElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Schematic field must be a string: name");
            }
            name = nameElement.getAsString();
        }
        if (name == null || name.isBlank()) name = "schematic";
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Schematic name is too long");

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
            if (x < -MAX_COORDINATE || x > MAX_COORDINATE
                    || y < -MAX_COORDINATE || y > MAX_COORDINATE
                    || z < -MAX_COORDINATE || z > MAX_COORDINATE) {
                throw new IllegalArgumentException("Schematic coordinate is out of bounds");
            }
            String id = requiredString(block, "block");
            if (id.length() > MAX_BLOCK_ID_LENGTH) throw new IllegalArgumentException("Block id is too long");
            Identifier key = Identifier.parse(id);
            Holder.Reference<Block> registered = BuiltInRegistries.BLOCK.get(key).orElse(null);
            if (registered == null) throw new IllegalArgumentException("Unknown block: " + id);
            if (!positions.add(new Position(x, y, z))) {
                throw new IllegalArgumentException("Duplicate schematic position: " + x + "," + y + "," + z);
            }

            BlockState state = registered.value().defaultBlockState();
            JsonElement propertiesElement = block.get("properties");
            if (propertiesElement != null && !propertiesElement.isJsonNull()) {
                if (!propertiesElement.isJsonObject()) {
                    throw new IllegalArgumentException("Schematic block properties must be an object");
                }
                JsonObject properties = propertiesElement.getAsJsonObject();
                for (var entry : properties.entrySet()) {
                    if (!entry.getValue().isJsonPrimitive()
                            || !entry.getValue().getAsJsonPrimitive().isString()) {
                        throw new IllegalArgumentException("Schematic block property must be a string: " + entry.getKey());
                    }
                    Property<?> property = null;
                    for (Property<?> candidate : state.getProperties()) {
                        if (candidate.getName().equals(entry.getKey())) {
                            property = candidate;
                            break;
                        }
                    }
                    if (property == null) {
                        throw new IllegalArgumentException("Unknown property for " + id + ": " + entry.getKey());
                    }
                    state = setProperty(state, property, entry.getValue().getAsString());
                }
            }
            entries.add(new BlockEntry(x, y, z, state));
        }
        return new Schematic(name, List.copyOf(entries));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setProperty(BlockState state, Property<?> property, String value) {
        Property rawProperty = property;
        Comparable parsed = (Comparable) rawProperty.getValue(value)
                .orElse(null);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Invalid value for property " + property.getName() + ": " + value);
        }
        return state.setValue(rawProperty, parsed);
    }

    private static int requiredInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or invalid schematic field: " + key);
        }
        Number number = value.getAsNumber();
        double numeric = number.doubleValue();
        int result = number.intValue();
        if (!Double.isFinite(numeric) || numeric != result) {
            throw new IllegalArgumentException("Schematic field must be an integer: " + key);
        }
        return result;
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing or invalid schematic field: " + key);
        }
        String result = value.getAsString();
        if (result.isBlank()) throw new IllegalArgumentException("Schematic field is blank: " + key);
        return result;
    }

    private record Position(int x, int y, int z) { }

    public record BlockEntry(int x, int y, int z, BlockState state) { }
}
