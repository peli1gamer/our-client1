package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class EntityEspModule implements ToggleableModule {
    private static boolean hookInstalled;
    private static EntityEspModule playerInstance;
    private static EntityEspModule mobInstance;
    private static EntityEspModule itemInstance;
    private final String id;
    private final Mode mode;
    private boolean enabled;

    public enum Mode { PLAYER, MOB, ITEM }

    public EntityEspModule(String id, Mode mode) {
        this.id = id;
        this.mode = mode;
        installHook();
    }

    @Override public String id() { return id; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (mode == Mode.PLAYER) playerInstance = enabled ? this : (playerInstance == this ? null : playerInstance);
        if (mode == Mode.MOB) mobInstance = enabled ? this : (mobInstance == this ? null : mobInstance);
        if (mode == Mode.ITEM) itemInstance = enabled ? this : (itemInstance == this ? null : itemInstance);
    }

    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            renderOne(playerInstance, context);
            renderOne(mobInstance, context);
            renderOne(itemInstance, context);
        });
    }

    private static void renderOne(EntityEspModule module, WorldRenderContext context) {
        if (module == null || !module.enabled || context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null) return;
        try {
            VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();
            double max = 96.0D * 96.0D;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == null || e == mc.player || !e.isAlive() || mc.player.distanceToSqr(e) > max) continue;
                if (!matches(module.mode, e)) continue;
                emitBox(pose, buffer, e.getBoundingBox());
            }
        } catch (RuntimeException ex) {
            module.enabled = false;
            OurClient.LOGGER.error("Disabling {} after ESP render failure", module.id, ex);
        }
    }

    private static boolean matches(Mode mode, Entity e) {
        return switch (mode) {
            case PLAYER -> e instanceof Player;
            case MOB -> e instanceof LivingEntity && !(e instanceof Player);
            case ITEM -> e instanceof ItemEntity;
        };
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer c, AABB b) {
        line(pose,c,b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ); line(pose,c,b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ);
        line(pose,c,b.maxX,b.minY,b.maxZ,b.minX,b.minY,b.maxZ); line(pose,c,b.minX,b.minY,b.maxZ,b.minX,b.minY,b.minZ);
        line(pose,c,b.minX,b.maxY,b.minZ,b.maxX,b.maxY,b.minZ); line(pose,c,b.maxX,b.maxY,b.minZ,b.maxX,b.maxY,b.maxZ);
        line(pose,c,b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ); line(pose,c,b.minX,b.maxY,b.maxZ,b.minX,b.maxY,b.minZ);
        line(pose,c,b.minX,b.minY,b.minZ,b.minX,b.maxY,b.minZ); line(pose,c,b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ);
        line(pose,c,b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ); line(pose,c,b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ);
    }

    private static void line(PoseStack.Pose p, VertexConsumer c, double x1,double y1,double z1,double x2,double y2,double z2) {
        c.addVertex(p,(float)x1,(float)y1,(float)z1).setColor(1,1,1,0.9f).setLineWidth(1.5f).setNormal(p,0,1,0);
        c.addVertex(p,(float)x2,(float)y2,(float)z2).setColor(1,1,1,0.9f).setLineWidth(1.5f).setNormal(p,0,1,0);
    }
}
