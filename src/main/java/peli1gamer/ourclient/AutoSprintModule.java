package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Automatically sprints while the player is moving forward. */
public final class AutoSprintModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "auto-sprint"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (!enabled) stopSprinting(Minecraft.getInstance());
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;

        boolean canSprint = client.player.isAlive()
            && !client.player.isSpectator()
            && !client.player.isPassenger()
            && client.player.zza > 0.0F
            && !client.player.isUsingItem();

        if (canSprint) client.player.setSprinting(true);
        else stopSprinting(client);
    }

    private void stopSprinting(Minecraft client) {
        if (client.player != null) client.player.setSprinting(false);
    }
}
