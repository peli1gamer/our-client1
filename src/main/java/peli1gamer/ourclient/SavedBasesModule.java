package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SavedBasesModule implements ToggleableModule {
    private final Map<String, SavedBase> bases = new LinkedHashMap<>();
    private boolean enabled = true;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private boolean loaded;

    @Override public String id() { return "saved-bases"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ClientConfig config = OurClient.config();
        if (config != null) config.savedBases = enabled;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!loaded) {
            loaded = true;
            ClientConfig config = OurClient.config();
            if (config != null) enabled = config.savedBases;
            load(client);
        }
    }

    public void saveCurrentBase(Minecraft client, String name) {
        if (!enabled || client.player == null || client.level == null || name == null || name.isBlank()) return;
        BlockPos pos = client.player.blockPosition();
        String dimension = client.level.dimension().identifier().toString();
        bases.put(name, new SavedBase(dimension, pos.getX(), pos.getY(), pos.getZ()));
        save(client);
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Saved base: " + name), true);
    }

    public Map<String, SavedBase> bases() { return Map.copyOf(bases); }

    private Path path(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config/our-client1-bases.json");
    }

    private void save(Minecraft client) {
        try {
            Path path = path(client);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, gson.toJson(bases));
        } catch (IOException ignored) {
        }
    }

    private void load(Minecraft client) {
        try {
            Path path = path(client);
            if (!Files.exists(path)) return;
            Map<String, SavedBase> loadedBases = gson.fromJson(
                    Files.readString(path),
                    new com.google.gson.reflect.TypeToken<Map<String, SavedBase>>() {}.getType());
            if (loadedBases != null) bases.putAll(loadedBases);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public record SavedBase(String dimension, int x, int y, int z) { }
}
