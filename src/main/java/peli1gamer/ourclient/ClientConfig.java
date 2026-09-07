package peli1gamer.ourclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small JSON-backed client configuration. */
public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean aimAssist;
    public boolean tracers;
    public boolean freecam;
    public boolean schematicBuilder;
    public boolean savedBases = true;
    public float aimSmoothing = 0.18f;
    public float aimRange = 12.0f;
    /** Maximum schematic placement attempts per client tick. Kept low by default for weak devices. */
    public int schematicPlacementsPerTick = 1;

    public static ClientConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                ClientConfig value = GSON.fromJson(Files.readString(path), ClientConfig.class);
                if (value != null) {
                    value.sanitize();
                    return value;
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return new ClientConfig();
    }

    public void save(Path path) {
        sanitize();
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException ignored) {
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
