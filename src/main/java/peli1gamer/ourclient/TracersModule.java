package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TracersModule implements ToggleableModule {
    private boolean enabled;

    public TracersModule() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    @Override public String id() { return "tracers"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    private void render(WorldRenderContext context) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        PoseStack matrices = context.matrices();
        if (matrices == null || context.consumers() == null) return;

        Camera camera = context.camera();
        Vec3 cameraPos = camera.position();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());

        matrices.pushPose();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == client.player || !target.isAlive()) continue;
            if (client.player.distanceToSqr(target) > 64.0 * 64.0) continue;

            double x = target.getX() - cameraPos.x;
            double y = target.getEyeY() - cameraPos.y;
            double z = target.getZ() - cameraPos.z;

            matrices.pushPose();
            PoseStack.Pose pose = matrices.last();
            lines.addVertex(pose, 0.0f, 0.0f, 0.0f)
                    .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f);
            lines.addVertex(pose, (float) x, (float) y, (float) z)
                    .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f);
            matrices.popPose();
        }
        matrices.popPose();
    }
}
