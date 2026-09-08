package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Automatically re-jumps while moving, using the normal jump key behavior. */
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
        if (!enabled || client.player == null || client.screen != null) return;
        if (client.player.onGround() && client.player.input != null
                && (client.player.input.forwardImpulse != 0.0F || client.player.input.leftImpulse != 0.0F)) {
            client.player.jumpFromGround();
        }
    }
}
