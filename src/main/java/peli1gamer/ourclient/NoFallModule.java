package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Native fall-distance guard. */
public final class NoFallModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "no-fall"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !client.player.isAlive()) return;

        // resetFallDistance() is available in the current mappings; avoid relying on
        // a getter whose mapped name differs between Minecraft versions.
        client.player.resetFallDistance();
    }
}
