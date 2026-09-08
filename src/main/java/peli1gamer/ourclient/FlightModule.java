package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class FlightModule implements ToggleableModule {
    private static final double SPEED = 0.75D;
    private boolean enabled;
    @Override public String id() { return "flight"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        double forward = (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
        double strafe = (client.options.keyRight.isDown() ? 1 : 0) - (client.options.keyLeft.isDown() ? 1 : 0);
        double vertical = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 1) { forward /= len; strafe /= len; }
        double yaw = Math.toRadians(client.player.getYRot());
        double x = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double z = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        client.player.setDeltaMovement(x * SPEED, vertical * SPEED, z * SPEED);
        client.player.fallDistance = 0;
    }
}
