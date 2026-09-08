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
import net.minecraft.world.phys.Vec3;

/** Stable player-box ESP using Fabric's supported world consumer path. */
public final class ESPModule implements ToggleableModule {
    private static final double MAX_RANGE = 96.0D;
    private static final double MAX_RANGE_SQUARED = MAX_RANGE * MAX_RANGE;
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
            Vec3 camera = mc.getCameraEntity().getPosition();
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();

            for (Player target : mc.level.players()) {
                if (target == mc.player || !target.isAlive()) continue;
                if (mc.player.distanceToSqr(target) > MAX_RANGE_SQUARED) continue;
                AABB worldBox = target.getBoundingBox();
                AABB box = worldBox.move(-camera.x, -camera.y, -camera.z);
                emitBox(pose, buffer, (float) box.minX, (float) box.minY, (float) box.minZ,
                        (float) box.maxX, (float) box.maxY, (float) box.maxZ);
            }
        } catch (RuntimeException exception) {
            enabled = false;
            if (activeInstance == this) activeInstance = null;
            OurClient.LOGGER.error("Disabling ESP after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.esp = false;
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer,
                                float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        line(pose, consumer, minX, minY, minZ, maxX, minY, minZ);
        line(pose, consumer, maxX, minY, minZ, maxX, minY, maxZ);
        line(pose, consumer, maxX, minY, maxZ, minX, minY, maxZ);
        line(pose, consumer, minX, minY, maxZ, minX, minY, minZ);
        line(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ);
        line(pose, consumer, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(pose, consumer, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(pose, consumer, minX, maxY, maxZ, minX, maxY, minZ);
        line(pose, consumer, minX, minY, minZ, minX, maxY, minZ);
        line(pose, consumer, maxX, minY, minZ, maxX, maxY, minZ);
        line(pose, consumer, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(pose, consumer, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
