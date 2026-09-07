package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Lightweight JSON schematic format used by the client builder. */
public record Schematic(String name, List<BlockEntry> blocks) {
    private static final Gson GSON = new Gson();

    public static Schematic parse(String json, String fallbackName) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        String name = root.has("name") ? root.get("name").getAsString() : fallbackName;
        JsonArray array = root.getAsJsonArray("blocks");
        if (array == null) throw new IllegalArgumentException("Schematic has no blocks array");

        List<BlockEntry> entries = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject block = element.getAsJsonObject();
            int x = block.get("x").getAsInt();
            int y = block.get("y").getAsInt();
            int z = block.get("z").getAsInt();
            String id = block.get("block").getAsString();
            Identifier key = Identifier.parse(id);
            Block registered = BuiltInRegistries.BLOCK.get(key);
            if (registered == null) throw new IllegalArgumentException("Unknown block: " + id);
            BlockState state = registered.defaultBlockState();
            entries.add(new BlockEntry(x, y, z, state));
        }
        return new Schematic(name, List.copyOf(entries));
    }

    public record BlockEntry(int x, int y, int z, BlockState state) { }
}
