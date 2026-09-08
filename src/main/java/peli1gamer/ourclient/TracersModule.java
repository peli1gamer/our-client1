package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Lightweight player tracers using Fabric's supported world consumer path. */
public final class TracersModule implements ToggleableModule {
    private static final double MAX_RANGE = 96.0D;
    private static boolean renderHookInstalled;
    private static TracersModule activeInstance;
    private boolean enabled;

    public TracersModule() { installRenderHook(); }

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
        if (!enabled || context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null) return;

        try {
            Vec3 camera = mc.getCameraEntity().getPosition(1.0F);
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();

            for (Player target : mc.level.players()) {
                if (target == mc.player || !target.isAlive()) continue;
                if (mc.player.distanceToSqr(target) > maxRangeSquared) continue;

                // The pose stack is camera-relative, so convert the target into
                // that same coordinate space exactly once.
                float x = (float) (target.getX() - camera.x);
                float y = (float) (target.getEyeY() - camera.y);
                float z = (float) (target.getZ() - camera.z);
                vertex(pose, buffer, 0, 0, 0);
                vertex(pose, buffer, x, y, z);
            }
        } catch (RuntimeException exception) {
            enabled = false;
            if (activeInstance == this) activeInstance = null;
            OurClient.LOGGER.error("Disabling tracers after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.tracers = false;
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z) {
        consumer.addVertex(pose, x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
