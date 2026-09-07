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

    @Override public String id() { return "saved-bases"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void saveCurrentBase(Minecraft client, String name) {
        if (client.player == null || client.level == null) return;
        BlockPos pos = client.player.blockPosition();
        String dimension = client.level.dimension().location().toString();
        bases.put(name, new SavedBase(dimension, pos.getX(), pos.getY(), pos.getZ()));
        save(client);
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Saved base: " + name), true);
    }

    public Map<String, SavedBase> bases() { return Map.copyOf(bases); }

    private void save(Minecraft client) {
        try {
            Path path = client.gameDirectory.toPath().resolve("config/our-client1-bases.json");
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(bases));
        } catch (IOException ignored) { }
    }

    public record SavedBase(String dimension, int x, int y, int z) { }
}
