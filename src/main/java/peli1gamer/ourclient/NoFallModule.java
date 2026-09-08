package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

public final class NoFallModule implements ToggleableModule {
    private boolean enabled;
    @Override public String id() { return "no-fall"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return;
        if (client.player.getAbilities().flying || client.player.isFallFlying()) return;
        if (client.player.fallDistance > 2.5F && client.player.getDeltaMovement().y < 0) {
            client.player.fallDistance = 0;
            client.player.setOnGround(true);
        }
    }
}
