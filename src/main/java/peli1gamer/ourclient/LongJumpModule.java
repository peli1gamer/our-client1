package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class LongJumpModule implements ToggleableModule {
    private boolean enabled;
    private boolean previousJump;

    @Override public String id() { return "long-jump"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; previousJump = false; }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        boolean jump = client.options.keyJump.isDown();
        if (jump && !previousJump && client.player.onGround()) {
            Vec3 v = client.player.getDeltaMovement();
            double yaw = Math.toRadians(client.player.getYRot());
            double forward = client.options.keyUp.isDown() ? 1 : client.options.keyDown.isDown() ? -1 : 0;
            double strafe = client.options.keyRight.isDown() ? 1 : client.options.keyLeft.isDown() ? -1 : 0;
            if (forward == 0 && strafe == 0) { forward = 1; }
            double len = Math.sqrt(forward * forward + strafe * strafe);
            forward /= len; strafe /= len;
            double x = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            double z = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
            client.player.setDeltaMovement(x * 1.25D, 0.62D, z * 1.25D);
        }
        previousJump = jump;
    }
}
