package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Core Arson module contract. Modules stay independent of the ClickGUI. */
public interface ClientModule {
    String id();

    default String title() { return id(); }
    default String description() { return "No description available."; }
    default ClientModuleManager.Category category() { return ClientModuleManager.Category.MISC; }
    default ModuleSettings settings() { return new ModuleSettings(); }
    default void onEnable(Minecraft client) { }
    default void onDisable(Minecraft client) { }
    default void onClientTick(Minecraft client) { }
}
