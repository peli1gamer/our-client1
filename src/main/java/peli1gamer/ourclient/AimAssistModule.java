package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class AimAssistModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "aim-assist"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        LocalPlayer player = client.player;
        LivingEntity target = findTarget(client, player);
        if (target == null) return;

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target.getEyeY() - player.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        float smoothing = Math.max(0.01f, Math.min(1.0f, OurClient.config().aimSmoothing));
        player.setYRot(approachAngle(player.getYRot(), wantedYaw, smoothing));
        player.setXRot(approachAngle(player.getXRot(), wantedPitch, smoothing));
    }

    private LivingEntity findTarget(Minecraft client, LocalPlayer player) {
        double range = Math.max(1.0, OurClient.config().aimRange);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive() || living.isSpectator()) continue;
            double distance = player.distanceToSqr(living);
            if (distance >= bestDistance) continue;
            best = living;
            bestDistance = distance;
        }
        return best;
    }

    private static float approachAngle(float current, float target, float factor) {
        float delta = wrapDegrees(target - current);
        return current + delta * factor;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }
}
