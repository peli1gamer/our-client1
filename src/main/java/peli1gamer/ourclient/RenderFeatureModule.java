package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
            if (isStorageBlock(mc, pos)) storageMatches.add(pos.immutable());
        }
    }

    private static boolean isStorageBlock(Minecraft mc, BlockPos pos) {
        String path = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).getPath();
        return path.equals("chest") || path.equals("trapped_chest") || path.equals("ender_chest")
                || path.equals("barrel") || path.contains("_chest")
                || path.endsWith("_shulker_box") || path.equals("shulker_box");
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
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null
                || mc.getCameraEntity().level() != mc.level) return;
        MultiBufferSource consumers = context.consumers();
        VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
        PoseStack.Pose pose = context.matrices().last();
        Vec3 camera = mc.getCameraEntity().position();
        switch (feature) {
            case PLAYER_ESP -> renderPlayers(mc, pose, buffer, camera);
            case MOB_ESP -> renderMobs(mc, pose, buffer, camera);
            case ITEM_ESP -> renderItems(mc, context, pose, buffer, camera);
            case STORAGE_ESP -> renderStorage(mc, pose, buffer, camera);
            case VOID_ESP -> renderVoid(mc, pose, buffer);
            case TRAIL -> renderTrail(pose, buffer);
        }
    }

    private static void renderPlayers(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (Player entity : mc.level.players()) {
            if (entity == mc.player || !entity.isAlive() || outOfRange(entity, camera)) continue;
            emitBox(pose, buffer, entity.getBoundingBox(), 0.3f, 0.75f, 1.0f);
        }
    }

    private static void renderMobs(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity instanceof Player || !entity.isAlive()
                    || !(entity instanceof net.minecraft.world.entity.Mob) || outOfRange(entity, camera)) continue;
            emitBox(pose, buffer, entity.getBoundingBox(), 1.0f, 0.25f, 0.25f);
        }
    }

    private static void renderItems(Minecraft mc, WorldRenderContext context, PoseStack.Pose pose,
                                    VertexConsumer buffer, Vec3 camera) {
        // Entity hitboxes are stored at the simulation position, while world rendering is
        // interpolated between ticks. Moving the box by the same interpolation delta keeps
        // the ESP attached to moving/dropped items instead of visibly lagging behind them.
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || outOfRange(item, camera)) continue;
            Vec3 current = item.position();
            Vec3 rendered = item.getPosition(tickDelta);
            AABB box = item.getBoundingBox().move(rendered.x - current.x, rendered.y - current.y, rendered.z - current.z);
            emitBox(pose, buffer, box.inflate(0.03D), 1.0f, 0.85f, 0.15f);
        }
    }

    private void renderStorage(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
        for (BlockPos pos : storageMatches) {
            if (camera.distanceToSqr(Vec3.atCenterOf(pos)) > (MAX_RANGE + 8.0D) * (MAX_RANGE + 8.0D)) continue;
            String path = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).getPath();
            float[] color = storageColor(path);
            emitBox(pose, buffer, new AABB(pos), color[0], color[1], color[2]);
        }
    }

    private static float[] storageColor(String path) {
        if (path.equals("ender_chest")) return new float[]{0.65f, 0.2f, 0.95f};
        if (path.equals("trapped_chest")) return new float[]{1.0f, 0.18f, 0.12f};
        if (path.contains("copper") && path.contains("oxidized")) return new float[]{0.2f, 0.7f, 0.62f};
        if (path.contains("copper") && path.contains("weathered")) return new float[]{0.2f, 0.62f, 0.55f};
        if (path.contains("copper") && path.contains("exposed")) return new float[]{0.55f, 0.68f, 0.58f};
        if (path.contains("copper")) return new float[]{0.85f, 0.45f, 0.2f};
        if (path.contains("shulker")) return shulkerColor(path);
        if (path.equals("barrel")) return new float[]{0.58f, 0.34f, 0.16f};
        return new float[]{0.72f, 0.45f, 0.2f};
    }

    private static float[] shulkerColor(String path) {
        if (path.contains("white")) return new float[]{0.95f, 0.95f, 0.95f};
        if (path.contains("orange")) return new float[]{1.0f, 0.45f, 0.08f};
        if (path.contains("magenta")) return new float[]{0.9f, 0.2f, 0.75f};
        if (path.contains("light_blue")) return new float[]{0.35f, 0.7f, 1.0f};
        if (path.contains("yellow")) return new float[]{1.0f, 0.85f, 0.1f};
        if (path.contains("lime")) return new float[]{0.45f, 0.9f, 0.2f};
        if (path.contains("pink")) return new float[]{1.0f, 0.5f, 0.7f};
        if (path.contains("gray")) return new float[]{0.35f, 0.35f, 0.4f};
        if (path.contains("light_gray")) return new float[]{0.7f, 0.7f, 0.7f};
        if (path.contains("cyan")) return new float[]{0.1f, 0.8f, 0.8f};
        if (path.contains("purple")) return new float[]{0.65f, 0.25f, 0.8f};
        if (path.contains("blue")) return new float[]{0.2f, 0.4f, 1.0f};
        if (path.contains("brown")) return new float[]{0.5f, 0.3f, 0.15f};
        if (path.contains("green")) return new float[]{0.2f, 0.7f, 0.25f};
        if (path.contains("red")) return new float[]{0.9f, 0.15f, 0.15f};
        if (path.contains("black")) return new float[]{0.12f, 0.12f, 0.12f};
        return new float[]{0.72f, 0.45f, 0.2f};
    }

    private static void renderVoid(Minecraft mc, PoseStack.Pose pose, VertexConsumer buffer) {
        int y = mc.level.getMinY();
        double x = mc.player.getX(), z = mc.player.getZ();
        emitBox(pose, buffer, new AABB(x - 1.5D, y, z - 1.5D, x + 1.5D, y + 0.1D, z + 1.5D), 1.0f, 0.15f, 0.15f);
    }

    private void renderTrail(PoseStack.Pose pose, VertexConsumer buffer) {
        Vec3 previous = null;
        for (Vec3 point : trail) {
            if (previous != null) line(pose, buffer,
                    (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) point.x, (float) point.y, (float) point.z,
                    1.0f, 0.55f, 0.05f);
            previous = point;
        }
    }

    private static boolean outOfRange(Entity entity, Vec3 camera) {
        return camera.distanceToSqr(entity.position()) > MAX_RANGE * MAX_RANGE;
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b, float r, float g, float blue) {
        float minX=(float)b.minX, minY=(float)b.minY, minZ=(float)b.minZ;
        float maxX=(float)b.maxX, maxY=(float)b.maxY, maxZ=(float)b.maxZ;
        line(pose,consumer,minX,minY,minZ,maxX,minY,minZ,r,g,blue); line(pose,consumer,maxX,minY,minZ,maxX,minY,maxZ,r,g,blue);
        line(pose,consumer,maxX,minY,maxZ,minX,minY,maxZ,r,g,blue); line(pose,consumer,minX,minY,maxZ,minX,minY,minZ,r,g,blue);
        line(pose,consumer,minX,maxY,minZ,maxX,maxY,minZ,r,g,blue); line(pose,consumer,maxX,maxY,minZ,maxX,maxY,maxZ,r,g,blue);
        line(pose,consumer,maxX,maxY,maxZ,minX,maxY,maxZ,r,g,blue); line(pose,consumer,minX,maxY,maxZ,minX,maxY,minZ,r,g,blue);
        line(pose,consumer,minX,minY,minZ,minX,maxY,minZ,r,g,blue); line(pose,consumer,maxX,minY,minZ,maxX,maxY,minZ,r,g,blue);
        line(pose,consumer,maxX,minY,maxZ,maxX,maxY,maxZ,r,g,blue); line(pose,consumer,minX,minY,maxZ,minX,maxY,maxZ,r,g,blue);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1,float y1,float z1,float x2,float y2,float z2,float r,float g,float blue) {
        consumer.addVertex(pose,x1,y1,z1).setColor(r,g,blue,0.9f).setLineWidth(1.5f).setNormal(pose,0.0f,1.0f,0.0f);
        consumer.addVertex(pose,x2,y2,z2).setColor(r,g,blue,0.9f).setLineWidth(1.5f).setNormal(pose,0.0f,1.0f,0.0f);
    }
}
