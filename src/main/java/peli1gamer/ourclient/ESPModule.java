package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Stable player-box ESP using Fabric's world extraction/buffer path. */
public final class ESPModule implements ToggleableModule {
    private static final double MAX_RANGE = 96.0D;
    private static boolean renderHookInstalled;
    private static ESPModule activeInstance;
    private boolean enabled;

    public ESPModule() { installRenderHook(); }

    @Override public String id() { return "esp"; }
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
            ESPModule instance = activeInstance;
            if (instance != null) instance.render(context);
        });
    }

    private void render(WorldRenderContext context) {
        if (!enabled || context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null) return;

        try {
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();

            for (Player target : mc.level.players()) {
                if (target == mc.player || !target.isAlive()) continue;
                if (mc.player.distanceToSqr(target) > maxRangeSquared) continue;
                emitBox(pose, buffer, target.getBoundingBox());
            }
        } catch (RuntimeException exception) {
            disableAfterRenderFailure("ESP", exception);
        }
    }

    private void disableAfterRenderFailure(String name, RuntimeException exception) {
        enabled = false;
        if (activeInstance == this) activeInstance = null;
        OurClient.LOGGER.error("Disabling {} after a render failure", name, exception);
        ClientConfig config = OurClient.config();
        if (config != null) config.esp = false;
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b) {
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.minY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.minY, (float)b.maxZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.minY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.minY, (float)b.minZ);
        line(pose, consumer, (float)b.minX, (float)b.maxY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.maxY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.maxX, (float)b.maxY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.maxY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.minZ, (float)b.minX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.maxZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setLineWidth(1.5f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setLineWidth(1.5f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
