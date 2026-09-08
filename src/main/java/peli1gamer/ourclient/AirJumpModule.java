package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Allows an extra jump while airborne, with one activation per press. */
public final class AirJumpModule implements ToggleableModule {
    private boolean enabled;
    private boolean previousJump;

    @Override public String id() { return "air-jump"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        previousJump = false;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.options == null) return;

        boolean jump = client.options.keyJump.isDown();
        if (client.screen != null || client.player == null) {
            previousJump = jump;
            return;
        }
        if (client.player.isSpectator() || client.player.isPassenger()) {
            previousJump = jump;
            return;
        }

        if (jump && !previousJump && !client.player.onGround()) {
            Vec3 velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, 0.42D, velocity.z);
        }
        previousJump = jump;
    }
}
