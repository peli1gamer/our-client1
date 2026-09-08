package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Automatically jumps while the player is moving on the ground. */
public final class AutoJumpModule implements ToggleableModule {
    private boolean enabled;
    private int cooldown;

    @Override public String id() { return "auto-jump"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        cooldown = 0;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.screen != null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (client.player.onGround() && (client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown())) {
            client.player.jumpFromGround();
            cooldown = 2;
        }
    }
}
