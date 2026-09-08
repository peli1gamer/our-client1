package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Increases normal jump height without changing horizontal movement. */
public final class HighJumpModule implements ToggleableModule {
    private boolean enabled;
    private boolean previousJump;

    @Override public String id() { return "high-jump"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        // Enabling while jump is already held must not synthesize a new press.
        previousJump = enabled && client.options != null && client.options.keyJump.isDown();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.options == null) return;

        boolean jump = client.options.keyJump.isDown();
        if (client.screen != null || client.player == null) {
            previousJump = jump;
            return;
        }

        if (jump && !previousJump && client.player.onGround()) {
            Vec3 velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, 0.75D, velocity.z);
        }
        previousJump = jump;
    }
}
