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
    public boolean savedBases;
    public float aimSmoothing = 0.18f;
    public float aimRange = 12.0f;

    public static ClientConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                ClientConfig value = GSON.fromJson(Files.readString(path), ClientConfig.class);
                return value == null ? new ClientConfig() : value;
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return new ClientConfig();
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
