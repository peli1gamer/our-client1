package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Base contract for every Arson Client module. Keep this interface small so modules stay easy to extend. */
public interface ClientModule {
    String id();

    /** Human-readable help text shown by the ClickGUI. */
    default String description() {
        return "No description available.";
    }

    /** Optional settings exposed to the ClickGUI. */
    default ModuleSettings settings() {
        return new ModuleSettings();
    }

    default void onClientTick(Minecraft client) {
    }
}
