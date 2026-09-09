package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Native fall-damage guard: prevents normal fall distance from accumulating while active. */
public final class NoFallModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "no-fall"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !client.player.isAlive()) return;

        if (client.player.getFallDistance() > 0.0F) {
            client.player.resetFallDistance();
        }
    }
}
