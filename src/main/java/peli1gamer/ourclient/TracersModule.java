package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class TracersModule implements ToggleableModule {
    private static final double MAX_RANGE = 64.0D;
    private static boolean renderHookInstalled;
    private static TracersModule activeInstance;
    private boolean enabled;

    public TracersModule() {
        installRenderHook();
    }

    @Override public String id() { return "tracers"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) activeInstance = this;
        else if (activeInstance == this) activeInstance = null;
    }

    private static void installRenderHook() {
        if (renderHookInstalled) return;
        renderHookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            TracersModule instance = activeInstance;
            if (instance != null) instance.render(context);
        });
    }

    private void render(WorldRenderContext context) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        PoseStack matrices = context.matrices();
        if (matrices == null || context.commandQueue() == null) return;

        try {
            Vec3 cameraPos = client.getCameraEntity() != null
                    ? client.getCameraEntity().getEyePosition(1.0F)
                    : client.player.getEyePosition(1.0F);
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            List<TracerLine> lines = new ArrayList<>();

            for (Player target : client.level.players()) {
                if (target == client.player || !target.isAlive()) continue;
                if (client.player.distanceToSqr(target) > maxRangeSquared) continue;

                lines.add(new TracerLine(
                        (float) (target.getX() - cameraPos.x),
                        (float) (target.getEyeY() - cameraPos.y),
                        (float) (target.getZ() - cameraPos.z)));
            }

            if (lines.isEmpty()) return;

            // Minecraft 1.21.11 moved world feature rendering onto SubmitNodeCollector.
            // Submit immutable geometry instead of writing directly to the old buffer path.
            context.commandQueue().submitCustomGeometry(
                    matrices,
                    RenderTypes.lines(),
                    (pose, consumer) -> emitLines(pose, consumer, lines));
        } catch (RuntimeException exception) {
            enabled = false;
            if (activeInstance == this) activeInstance = null;
            OurClient.LOGGER.error("Disabling tracers after a render submission failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.tracers = false;
        }
    }

    private static void emitLines(PoseStack.Pose pose, VertexConsumer consumer, List<TracerLine> lines) {
        for (TracerLine line : lines) {
            consumer.addVertex(pose, 0.0f, 0.0f, 0.0f)
                    .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f);
            consumer.addVertex(pose, line.x(), line.y(), line.z())
                    .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f);
        }
    }

    private record TracerLine(float x, float y, float z) {}
}
