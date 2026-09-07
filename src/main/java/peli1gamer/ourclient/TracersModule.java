package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TracersModule implements ToggleableModule {
    private static final double MAX_RANGE = 64.0D;
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

        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) return;
        Vec3 cameraPos = cameraEntity.getEyePosition(1.0F);
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());

        matrices.pushPose();
        try {
            double maxRangeSquared = MAX_RANGE * MAX_RANGE;
            for (Player target : client.level.players()) {
                if (target == client.player || !target.isAlive()) continue;
                if (client.player.distanceToSqr(target) > maxRangeSquared) continue;

                double x = target.getX() - cameraPos.x;
                double y = target.getEyeY() - cameraPos.y;
                double z = target.getZ() - cameraPos.z;

                PoseStack.Pose pose = matrices.last();
                lines.addVertex(pose, 0.0f, 0.0f, 0.0f)
                        .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                        .setNormal(pose, 0.0f, 1.0f, 0.0f);
                lines.addVertex(pose, (float) x, (float) y, (float) z)
                        .setColor(1.0f, 1.0f, 1.0f, 0.9f)
                        .setNormal(pose, 0.0f, 1.0f, 0.0f);
            }
        } finally {
            matrices.popPose();
        }
    }
}
