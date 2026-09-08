package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class AimAssistModule implements ToggleableModule {
    private boolean enabled;
    private LivingEntity target;

    @Override public String id() { return "aim-assist"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) target = null;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        LocalPlayer player = client.player;
        if (!player.isAlive()) {
            target = null;
            return;
        }

        ClientConfig config = OurClient.config();
        if (config == null) return;

        double range = Math.max(1.0, config.aimRange);
        if (!isValidTarget(player, target, range)) target = findTarget(client, player, range);
        if (target == null) return;

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target.getEyeY() - player.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        float smoothing = Math.max(0.01f, Math.min(1.0f, config.aimSmoothing));
        float newYaw = approachAngle(player.getYRot(), wantedYaw, smoothing);
        float newPitch = approachAngle(player.getXRot(), wantedPitch, smoothing);
        player.setYRot(newYaw);
        player.setXRot(newPitch);
        player.setYHeadRot(newYaw);
    }

    private LivingEntity findTarget(Minecraft client, LocalPlayer player, double range) {
        LivingEntity best = null;
        double bestDistance = range * range;
        AABB searchBox = player.getBoundingBox().inflate(range);
        for (Entity entity : client.level.getEntities(player, searchBox,
                candidate -> candidate instanceof LivingEntity living && isValidTarget(player, living, range))) {
            if (!(entity instanceof LivingEntity living)) continue;
            double distance = player.distanceToSqr(living);
            if (distance >= bestDistance) continue;
            best = living;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean isValidTarget(LocalPlayer player, LivingEntity candidate, double range) {
        return candidate != null && candidate != player && candidate.level() == player.level()
                && candidate.isAlive() && !candidate.isSpectator()
                && player.distanceToSqr(candidate) <= range * range;
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
