package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_CONFIG_BYTES = 1024L * 1024L;

    public boolean aimAssist;
    public boolean triggerBot;
    public boolean crystalMacro;
    public boolean anchorMacro;
    public boolean attributeSwap;
    public boolean autoWeapon;
    public boolean tracers;
    public boolean esp;
    public boolean freecam;
    public boolean xray;
    public boolean schematicBuilder;
    public boolean savedBases = true;
    public boolean scaffold;
    public boolean autoWalk;
    public boolean autoJump;
    public boolean airJump;
    public boolean sprint;
    public boolean autoSprint;
    public boolean fullbright;
    public boolean highJump;
    public boolean bunnyHop;
    public boolean fastClimb;
    public boolean antiVoid;
    public boolean noFall;
    public boolean elytraFly;
    public boolean blockEsp;
    public float aimSmoothing = 0.18f;
    public float aimRange = 12.0f;
    public int schematicPlacementsPerTick = 1;

    public static ClientConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                if (Files.size(path) > MAX_CONFIG_BYTES) throw new IOException("Configuration file is too large");
                ClientConfig value = GSON.fromJson(Files.readString(path), ClientConfig.class);
                if (value != null) { value.sanitize(); return value; }
            }
        } catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not load client config from {}", path, exception);
        }
        return new ClientConfig();
    }

    public void save(Path path) {
        Path parent = path.getParent(); Path temp = null;
        try {
            sanitize();
            if (parent != null) Files.createDirectories(parent);
            Path tempDirectory = parent != null ? parent : Path.of(".");
            temp = Files.createTempFile(tempDirectory, path.getFileName().toString(), ".tmp");
            Files.writeString(temp, GSON.toJson(this));
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | RuntimeException exception) {
            OurClient.LOGGER.warn("Could not save client config to {}", path, exception);
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException | RuntimeException ignored) { }
        }
    }

    private void sanitize() {
        if (!Float.isFinite(aimSmoothing)) aimSmoothing = 0.18f;
        if (!Float.isFinite(aimRange)) aimRange = 12.0f;
        aimSmoothing = Math.max(0.01f, Math.min(1.0f, aimSmoothing));
        aimRange = Math.max(1.0f, Math.min(64.0f, aimRange));
        schematicPlacementsPerTick = Math.max(1, Math.min(20, schematicPlacementsPerTick));
    }
}
