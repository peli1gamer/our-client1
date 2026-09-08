package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Automatically re-jumps while moving, using the normal movement keys. */
public final class BunnyHopModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "bunny-hop"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.options == null || client.screen != null) return;
        if (!client.player.isAlive() || client.player.isSpectator() || client.player.isPassenger()) return;

        boolean moving = client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown();

        if (client.player.onGround() && moving) {
            client.player.jumpFromGround();
        }
    }
}
