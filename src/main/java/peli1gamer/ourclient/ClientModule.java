package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

public interface ClientModule {
    String id();

    default void onClientTick(Minecraft client) {
    }

    /** Runtime settings exposed by the native Arson ClickGUI. */
    default ModuleSettings settings() {
        return new ModuleSettings();
    }
}
