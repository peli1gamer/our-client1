package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/**
 * Stable registry entry for a planned Arson module. Behavior is intentionally
 * isolated from registration so unfinished features cannot destabilize the client.
 */
public final class CatalogModule implements ToggleableModule {
    public enum Category { COMBAT, MOVEMENT, RENDER, WORLD, PLAYER, UTILITY }

    private final String id;
    private final Category category;
    private boolean enabled;

    public CatalogModule(String id, Category category) {
        this.id = id;
        this.category = category;
    }

    @Override public String id() { return id; }
    public Category category() { return category; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public void onClientTick(Minecraft client) { }
}
