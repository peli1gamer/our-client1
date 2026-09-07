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
    private static final int MAX_DIMENSION_LENGTH = 256;
    private static final long MAX_FILE_BYTES = 1024L * 1024L;
    private static final int LOAD_RETRY_DELAY_TICKS = 200;

    private final Map<String, SavedBase> bases = new LinkedHashMap<>();
    private boolean enabled = true;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private boolean loaded;
    private int loadRetryCooldown;

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
        if (loaded) return;
        if (loadRetryCooldown > 0) {
            loadRetryCooldown--;
            return;
        }
        ClientConfig config = OurClient.config();
        if (config != null) enabled = config.savedBases;
        if (load(client)) loaded = true;
        else loadRetryCooldown = LOAD_RETRY_DELAY_TICKS;
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
        if (dimension.length() > MAX_DIMENSION_LENGTH) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Dimension identifier is too long."), true);
            return;
        }

        SavedBase previous = bases.put(normalizedName, new SavedBase(dimension, pos.getX(), pos.getY(), pos.getZ()));
        if (!save(client)) {
            if (previous == null) bases.remove(normalizedName);
            else bases.put(normalizedName, previous);
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Could not save base to disk."), true);
            return;
        }
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Saved base: " + normalizedName), true);
    }

    public String nextAutomaticName() {
        String base = "base-" + System.currentTimeMillis();
        if (!bases.containsKey(base)) return base;
        for (int suffix = 2; suffix <= MAX_BASES; suffix++) {
            String candidate = base + "-" + suffix;
            if (!bases.containsKey(candidate)) return candidate;
        }
        return base + "-" + System.nanoTime();
    }

    public Map<String, SavedBase> bases() { return Map.copyOf(bases); }

    private Path path(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config/our-client1-bases.json");
    }

    private boolean save(Minecraft client) {
        Path path = path(client);
        Path temp = null;
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tempDirectory = parent != null ? parent : Path.of(".");
            temp = Files.createTempFile(tempDirectory, path.getFileName().toString(), ".tmp");
            Files.writeString(temp, gson.toJson(bases));
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            OurClient.LOGGER.warn("Could not save saved bases to {}", path, exception);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException cleanupException) {
                    OurClient.LOGGER.debug("Could not clean up temporary bases file {}", temp, cleanupException);
                }
            }
            return false;
        }
    }

    private boolean load(Minecraft client) {
        Path path = path(client);
        try {
            if (!Files.exists(path)) return true;
            if (Files.size(path) > MAX_FILE_BYTES) {
                throw new IOException("Saved bases file is too large");
            }
            Map<String, SavedBase> loadedBases = gson.fromJson(
                    Files.readString(path),
                    new com.google.gson.reflect.TypeToken<Map<String, SavedBase>>() {}.getType());
            if (loadedBases == null) return true;

            for (Map.Entry<String, SavedBase> entry : loadedBases.entrySet()) {
                String name = entry.getKey();
                SavedBase base = entry.getValue();
                if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH || base == null
                        || base.dimension() == null || base.dimension().isBlank()
                        || base.dimension().length() > MAX_DIMENSION_LENGTH) continue;
                if (bases.size() >= MAX_BASES) break;
                bases.put(name, base);
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not load saved bases from {}", path, exception);
            return false;
        }
    }

    public record SavedBase(String dimension, int x, int y, int z) { }
}
