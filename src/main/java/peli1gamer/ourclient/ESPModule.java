package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stable player-box ESP using a native Arson see-through render pipeline. */
public final class ESPModule implements ToggleableModule {
    private static final double MAX_RANGE = 96.0D;
    private static boolean renderHookInstalled;
    private boolean enabled;

    public ESPModule() { installRenderHook(); }
    @Override public String id() { return "esp"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private static void installRenderHook() {
        if (renderHookInstalled) return;
        renderHookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ClientModule module = OurClient.modules().get("esp");
            if (module instanceof ESPModule instance && instance.enabled()) instance.render(context);
        });
    }

    private void render(WorldRenderContext context) {
        if (!enabled || context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null
                || mc.getCameraEntity().level() != mc.level) return;

        try {
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(ArsonRenderTypes.SEE_THROUGH_LINES);
            PoseStack.Pose pose = context.matrices().last();
            Vec3 camera = mc.getCameraEntity().position();

            for (Player target : mc.level.players()) {
                if (target == mc.player || !target.isAlive()) continue;
                if (camera.distanceToSqr(target.position()) > maxRangeSquared) continue;
                emitBox(pose, buffer, target.getBoundingBox());
            }
        } catch (RuntimeException exception) {
            disableAfterRenderFailure("ESP", exception);
        }
    }

    private void disableAfterRenderFailure(String name, RuntimeException exception) {
        ClientModuleManager manager = OurClient.modules();
        if (!manager.setEnabled(id(), false)) setEnabled(false);
        OurClient.LOGGER.error("Disabling {} after a render failure", name, exception);
        try {
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException saveException) {
            OurClient.LOGGER.error("Could not persist disabled {} state", name, saveException);
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b) {
        float minX = (float) b.minX, minY = (float) b.minY, minZ = (float) b.minZ;
        float maxX = (float) b.maxX, maxY = (float) b.maxY, maxZ = (float) b.maxZ;
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

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1).setColor(1.0f, 1.0f, 1.0f, 0.9f).setLineWidth(1.5f).setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, x2, y2, z2).setColor(1.0f, 1.0f, 1.0f, 0.9f).setLineWidth(1.5f).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
