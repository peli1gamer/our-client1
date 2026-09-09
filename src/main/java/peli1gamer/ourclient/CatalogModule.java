package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/**
 * Stable registry entry for a planned Arson module. Behavior is intentionally
 * isolated from registration so unfinished features cannot destabilize the client.
 *
 * Planned entries are metadata only and deliberately do not implement
 * ToggleableModule; they cannot accidentally become enabled or persisted as
 * functional modules before their implementation exists.
 */
public final class CatalogModule implements ClientModule {
    public enum Category { COMBAT, MOVEMENT, RENDER, WORLD, PLAYER, UTILITY }

    private final String id;
    private final Category category;

    public CatalogModule(String id, Category category) {
        this.id = id;
        this.category = category;
    }

    @Override public String id() { return id; }
    public Category category() { return category; }
    @Override public void onClientTick(Minecraft client) { }
}
