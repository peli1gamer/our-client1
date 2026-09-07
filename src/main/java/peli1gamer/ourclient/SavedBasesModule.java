package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SavedBasesModule implements ToggleableModule {
    private static final int MAX_BASES = 500;
    private static final int MAX_NAME_LENGTH = 64;

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
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Base name is too long."), true);
            return;
        }
        if (bases.size() >= MAX_BASES && !bases.containsKey(normalizedName)) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Maximum saved bases reached."), true);
            return;
        }

        BlockPos pos = client.player.blockPosition();
        String dimension = client.level.dimension().identifier().toString();
        bases.put(normalizedName, new SavedBase(dimension, pos.getX(), pos.getY(), pos.getZ()));
        save(client);
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Saved base: " + normalizedName), true);
    }

    public Map<String, SavedBase> bases() { return Map.copyOf(bases); }

    private Path path(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config/our-client1-bases.json");
    }

    private void save(Minecraft client) {
        Path path = path(client);
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(temp, gson.toJson(bases));
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            OurClient.LOGGER.warn("Could not save saved bases to {}", path, exception);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException cleanupException) {
                OurClient.LOGGER.debug("Could not clean up temporary bases file {}", temp, cleanupException);
            }
        }
    }

    private void load(Minecraft client) {
        Path path = path(client);
        try {
            if (!Files.exists(path)) return;
            Map<String, SavedBase> loadedBases = gson.fromJson(
                    Files.readString(path),
                    new com.google.gson.reflect.TypeToken<Map<String, SavedBase>>() {}.getType());
            if (loadedBases == null) return;

            for (Map.Entry<String, SavedBase> entry : loadedBases.entrySet()) {
                String name = entry.getKey();
                SavedBase base = entry.getValue();
                if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH || base == null
                        || base.dimension() == null || base.dimension().isBlank()) continue;
                if (bases.size() >= MAX_BASES) break;
                bases.put(name, base);
            }
        } catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not load saved bases from {}", path, exception);
        }
    }

    public record SavedBase(String dimension, int x, int y, int z) { }
}
