package peli1gamer.ourclient.elytrafly;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Bounce implements ElytraFlightMode {
    @Override public void apply(LocalPlayer player, double speed, double verticalSpeed) {
        Vec3 v = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-6D) horizontal = horizontal.normalize().scale(speed);
        player.setDeltaMovement(horizontal.x, v.y < -0.35D ? 0.35D : v.y, horizontal.z);
    }
}
