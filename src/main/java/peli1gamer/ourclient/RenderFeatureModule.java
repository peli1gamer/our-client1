package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Native Arson render features; behavior is independently implemented from reference clients. */
public final class RenderFeatureModule implements ToggleableModule {
    public enum Feature { PLAYER_ESP, MOB_ESP, ITEM_ESP, STORAGE_ESP, VOID_ESP, TRAIL }
    private static final double MAX_RANGE = 96.0D;
    private static final int STORAGE_RADIUS = 24;
    private static final int STORAGE_RESULT_LIMIT = 256;
    private static final int TRAIL_LIMIT = 160;
    private static final int STORAGE_REFRESH_TICKS = 5;
    private static boolean hookInstalled;
    private static final Map<Feature, RenderFeatureModule> INSTANCES = new ConcurrentHashMap<>();
    private final String id;
    private final Feature feature;
    private boolean enabled;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final List<BlockPos> storageMatches = new ArrayList<>();
    private int storageRefreshTicks;

    public RenderFeatureModule(String id, Feature feature) {
        this.id = id;
        this.feature = feature;
        INSTANCES.put(feature, this);
        installHook();
    }
    @Override public String id() { return id; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            if (feature == Feature.TRAIL) trail.clear();
            if (feature == Feature.STORAGE_ESP) storageMatches.clear();
        }
        if (enabled && feature == Feature.STORAGE_ESP) storageRefreshTicks = 0;
    }
    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) {
            if (feature == Feature.TRAIL) trail.clear();
            if (feature == Feature.STORAGE_ESP) storageMatches.clear();
            return;
        }
        if (feature == Feature.TRAIL) {
            Vec3 p = client.player.position();
            if (trail.isEmpty() || trail.peekLast().distanceToSqr(p) >= 0.04D) {
                trail.addLast(p);
                while (trail.size() > TRAIL_LIMIT) trail.removeFirst();
            }
        } else if (feature == Feature.STORAGE_ESP) {
            if (++storageRefreshTicks >= STORAGE_REFRESH_TICKS || storageMatches.isEmpty()) {
                storageRefreshTicks = 0;
                refreshStorage(client);
            }
        }
    }
    private void refreshStorage(Minecraft mc) {
        storageMatches.clear();
        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-STORAGE_RADIUS, -STORAGE_RADIUS, -STORAGE_RADIUS),
                center.offset(STORAGE_RADIUS, STORAGE_RADIUS, STORAGE_RADIUS))) {
            if (storageMatches.size() >= STORAGE_RESULT_LIMIT) break;
            var state = mc.level.getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.CHEST)
                    || state.is(net.minecraft.world.level.block.Blocks.TRAPPED_CHEST)
                    || state.is(net.minecraft.world.level.block.Blocks.BARREL)) {
                storageMatches.add(pos.immutable());
            }
        }
    }
    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            for (RenderFeatureModule module : INSTANCES.values()) {
                if (!module.enabled) continue;
                try { module.render(context); }
                catch (RuntimeException exception) {
                    module.enabled = false;
                    OurClient.LOGGER.error("Disabling render module '{}' after render failure", module.id, exception);
                }
            }
        });
    }
    private void render(WorldRenderContext context) {
        if (context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null) return;
        MultiBufferSource consumers = context.consumers();
        VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
        PoseStack.Pose pose = context.matrices().last();
        Vec3 camera = mc.getCameraEntity().position();
        switch (feature) {
            case PLAYER_ESP -> renderPlayers(mc, pose, buffer, camera);
            case MOB_ESP -> renderMobs(mc, pose, buffer, camera);
            case ITEM_ESP -> renderItems(mc, pose, buffer, camera);
            case STORAGE_ESP -> renderStorage(pose, buffer, camera);
            case VOID_ESP -> renderVoid(mc, pose, buffer, camera);
            case TRAIL -> renderTrail(pose, buffer, camera);
        }
    }
    private static void renderPlayers(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (Player entity : mc.level.players()) {
            if (entity == mc.player || !entity.isAlive() || outOfRange(entity, camera)) continue;
            emitBox(pose, buffer, entity.getBoundingBox(), camera);
        }
    }
    private static void renderMobs(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity instanceof Player || !entity.isAlive() || !(entity instanceof net.minecraft.world.entity.Mob) || outOfRange(entity, camera)) continue;
            emitBox(pose, buffer, entity.getBoundingBox(), camera);
        }
    }
    private static void renderItems(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || outOfRange(item, camera)) continue;
            emitBox(pose, buffer, item.getBoundingBox().inflate(0.05D), camera);
        }
    }
    private void renderStorage(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (BlockPos pos : storageMatches) emitBox(pose, buffer, new AABB(pos), camera);
    }
    private static void renderVoid(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        int y = mc.level.getMinY();
        double x = mc.player.getX(), z = mc.player.getZ();
        emitBox(pose, buffer, new AABB(x - 1.5D, y, z - 1.5D, x + 1.5D, y + 0.1D, z + 1.5D), camera);
    }
    private void renderTrail(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        Vec3 previous = null;
        for (Vec3 point : trail) {
            if (previous != null) line(pose, buffer,
                    (float) (previous.x - camera.x), (float) (previous.y - camera.y), (float) (previous.z - camera.z),
                    (float) (point.x - camera.x), (float) (point.y - camera.y), (float) (point.z - camera.z));
            previous = point;
        }
    }
    private static boolean outOfRange(Entity entity, Vec3 camera) {
        return camera.distanceToSqr(entity.position()) > MAX_RANGE * MAX_RANGE;
    }
    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b, Vec3 camera) {
        float minX=(float)(b.minX-camera.x), minY=(float)(b.minY-camera.y), minZ=(float)(b.minZ-camera.z);
        float maxX=(float)(b.maxX-camera.x), maxY=(float)(b.maxY-camera.y), maxZ=(float)(b.maxZ-camera.z);
        line(pose,consumer,minX,minY,minZ,maxX,minY,minZ); line(pose,consumer,maxX,minY,minZ,maxX,minY,maxZ);
        line(pose,consumer,maxX,minY,maxZ,minX,minY,maxZ); line(pose,consumer,minX,minY,maxZ,minX,minY,minZ);
        line(pose,consumer,minX,maxY,minZ,maxX,maxY,minZ); line(pose,consumer,maxX,maxY,minZ,maxX,maxY,maxZ);
        line(pose,consumer,maxX,maxY,maxZ,minX,maxY,maxZ); line(pose,consumer,minX,maxY,maxZ,minX,maxY,minZ);
        line(pose,consumer,minX,minY,minZ,minX,maxY,minZ); line(pose,consumer,maxX,minY,minZ,maxX,maxY,minZ);
        line(pose,consumer,maxX,minY,maxZ,maxX,maxY,maxZ); line(pose,consumer,minX,minY,maxZ,minX,maxY,maxZ);
    }
    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1,float y1,float z1,float x2,float y2,float z2) {
        consumer.addVertex(pose,x1,y1,z1).setColor(1.0f,1.0f,1.0f,0.9f).setLineWidth(1.5f).setNormal(pose,0.0f,1.0f,0.0f);
        consumer.addVertex(pose,x2,y2,z2).setColor(1.0f,1.0f,1.0f,0.9f).setLineWidth(1.5f).setNormal(pose,0.0f,1.0f,0.0f);
    }
}
