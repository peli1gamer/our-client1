package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Lightweight player tracers using Fabric's supported world consumer path. */
public final class TracersModule implements ToggleableModule {
    private static final double MAX_RANGE = 96.0D;
    private static boolean renderHookInstalled;
    private boolean enabled;

    public TracersModule() { installRenderHook(); }
    @Override public String id() { return "tracers"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private static void installRenderHook() {
        if (renderHookInstalled) return;
        renderHookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ClientModule module = OurClient.modules().get("tracers");
            if (module instanceof TracersModule instance && instance.enabled()) instance.render(context);
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
                vertex(pose, buffer, camera.x, camera.y, camera.z);
                vertex(pose, buffer, target.getX(), target.getEyeY(), target.getZ());
            }
        } catch (RuntimeException exception) {
            disableAfterRenderFailure(exception);
        }
    }

    private void disableAfterRenderFailure(RuntimeException exception) {
        ClientModuleManager manager = OurClient.modules();
        if (!manager.setEnabled(id(), false)) setEnabled(false);
        OurClient.LOGGER.error("Disabling tracers after a render failure", exception);
        try {
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException saveException) {
            OurClient.LOGGER.error("Could not persist disabled tracers state", saveException);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                .setLineWidth(1.5f)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
