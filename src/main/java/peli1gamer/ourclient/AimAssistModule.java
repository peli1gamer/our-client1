package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AimAssistModule implements ToggleableModule, ConfigurableModule {
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
    public ModuleSettings settings() {
        ClientConfig c = OurClient.config();
        ModuleSettings settings = new ModuleSettings();
        if (c == null) return settings;
        settings.number("range", "Targeting Distance", () -> String.format(java.util.Locale.ROOT, "%.1f", c.aimRange),
                () -> { c.aimRange = Math.min(64.0f, c.aimRange + 1.0f); OurClient.syncAndSaveConfigFromModules(); },
                () -> { c.aimRange = Math.max(1.0f, c.aimRange - 1.0f); OurClient.syncAndSaveConfigFromModules(); });
        settings.number("smoothness", "Smoothness", () -> String.format(java.util.Locale.ROOT, "%.2f", c.aimSmoothing),
                () -> { c.aimSmoothing = Math.min(1.0f, c.aimSmoothing + 0.01f); OurClient.syncAndSaveConfigFromModules(); },
                () -> { c.aimSmoothing = Math.max(0.01f, c.aimSmoothing - 0.01f); OurClient.syncAndSaveConfigFromModules(); });
        settings.choice("target-part", "Target Part", () -> currentTargetPart(c).displayName(),
                () -> changeTargetPart(c, 1), () -> changeTargetPart(c, -1));
        return settings;
    }

    private static AimTargetPart currentTargetPart(ClientConfig c) {
        try { return AimTargetPart.valueOf(c.aimTargetPart); }
        catch (IllegalArgumentException exception) { c.aimTargetPart = AimTargetPart.HEAD.name(); return AimTargetPart.HEAD; }
    }

    private static void changeTargetPart(ClientConfig c, int direction) {
        AimTargetPart[] values = AimTargetPart.values();
        AimTargetPart current = currentTargetPart(c);
        int next = (current.ordinal() + direction + values.length) % values.length;
        c.aimTargetPart = values[next].name();
        OurClient.syncAndSaveConfigFromModules();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        LocalPlayer player = client.player;
        if (!player.isAlive()) { target = null; return; }

        ClientConfig config = OurClient.config();
        if (config == null) return;
        double range = Math.max(1.0, config.aimRange);
        if (!isValidTarget(player, target, range)) target = findTarget(client, player, range);
        if (target == null) return;

        Vec3 aimPoint = getAimPoint(player, target, currentTargetPart(config));
        double dx = aimPoint.x - player.getX();
        double dz = aimPoint.z - player.getZ();
        double dy = aimPoint.y - player.getEyeY();
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

    private static Vec3 getAimPoint(LocalPlayer player, LivingEntity target, AimTargetPart part) {
        AABB box = target.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        Vec3 head = new Vec3(centerX, target.getEyeY(), centerZ);
        Vec3 torso = new Vec3(centerX, box.minY + box.getYsize() * 0.55, centerZ);
        Vec3 feet = new Vec3(centerX, box.minY, centerZ);
        return switch (part) {
            case HEAD -> head;
            case TORSO -> torso;
            case FEET -> feet;
            case CLOSEST_BODY_PART -> closest(player.getEyePosition(), head, torso, feet);
        };
    }

    private static Vec3 closest(Vec3 origin, Vec3... points) {
        Vec3 best = points[0];
        double distance = origin.distanceToSqr(best);
        for (int i = 1; i < points.length; i++) {
            double candidate = origin.distanceToSqr(points[i]);
            if (candidate < distance) { distance = candidate; best = points[i]; }
        }
        return best;
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
