package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Locale;

public final class AimAssistModule implements ToggleableModule {
    private boolean enabled;
    private LivingEntity target;

    @Override public String id() { return "aim-assist"; }
    @Override public String description() { return "Smoothly turns toward the nearest valid living entity within range."; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) target = null;
    }

    @Override
    public ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings();
        settings.number("range", "Range", () -> String.format(Locale.ROOT, "%.1f", range()),
                () -> setRange(range() + 0.5f), () -> setRange(range() - 0.5f));
        settings.number("smoothness", "Smoothness", () -> String.format(Locale.ROOT, "%.2f", smoothing()),
                () -> setSmoothing(smoothing() + 0.02f), () -> setSmoothing(smoothing() - 0.02f));
        return settings;
    }

    private float range() {
        ClientConfig config = OurClient.config();
        return config == null ? 12.0f : config.aimRange;
    }

    private void setRange(float value) {
        ClientConfig config = OurClient.config();
        if (config != null) config.aimRange = Math.max(1.0f, Math.min(64.0f, value));
    }

    private float smoothing() {
        ClientConfig config = OurClient.config();
        return config == null ? 0.18f : config.aimSmoothing;
    }

    private void setSmoothing(float value) {
        ClientConfig config = OurClient.config();
        if (config != null) config.aimSmoothing = Math.max(0.01f, Math.min(1.0f, value));
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        LocalPlayer player = client.player;
        if (!player.isAlive()) {
            target = null;
            return;
        }

        double range = Math.max(1.0, range());
        if (!isValidTarget(player, target, range)) target = findTarget(client, player, range);
        if (target == null) return;

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target.getEyeY() - player.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        float smoothing = Math.max(0.01f, Math.min(1.0f, smoothing()));
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
        return candidate != null && candidate != player && candidate.isAlive() && !candidate.isSpectator()
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
