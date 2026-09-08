package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Prevents vanilla fall-distance accumulation while enabled. */
public final class NoFallModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "no-fall"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null) return;
        client.player.resetFallDistance();
    }
}
