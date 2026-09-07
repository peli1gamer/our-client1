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

import java.util.ArrayList;
import java.util.List;

/** Stable player-box ESP using Fabric's supported world consumer path. */
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
            Vec3 camera = mc.getCameraEntity().getPosition(1.0F);
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            List<Box> boxes = new ArrayList<>();

            for (Player target : mc.level.players()) {
                if (target == mc.player || !target.isAlive()) continue;
                if (mc.player.distanceToSqr(target) > maxRangeSquared) continue;
                AABB worldBox = target.getBoundingBox();
                AABB box = worldBox.move(-camera.x, -camera.y, -camera.z);
                boxes.add(new Box((float) box.minX, (float) box.minY, (float) box.minZ,
                        (float) box.maxX, (float) box.maxY, (float) box.maxZ));
            }

            if (boxes.isEmpty()) return;
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();
            emitBoxes(pose, buffer, boxes);
        } catch (RuntimeException exception) {
            enabled = false;
            if (activeInstance == this) activeInstance = null;
            OurClient.LOGGER.error("Disabling ESP after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.esp = false;
        }
    }

    private static void emitBoxes(PoseStack.Pose pose, VertexConsumer consumer, List<Box> boxes) {
        for (Box b : boxes) {
            line(pose, consumer, b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ);
            line(pose, consumer, b.maxX, b.minY, b.maxZ, b.minX, b.minY, b.maxZ);
            line(pose, consumer, b.minX, b.minY, b.maxZ, b.minX, b.minY, b.minZ);
            line(pose, consumer, b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ);
            line(pose, consumer, b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ);
            line(pose, consumer, b.minX, b.maxY, b.maxZ, b.minX, b.maxY, b.minZ);
            line(pose, consumer, b.minX, b.minY, b.minZ, b.minX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ);
            line(pose, consumer, b.minX, b.minY, b.maxZ, b.minX, b.maxY, b.maxZ);
        }
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

    private record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
}
