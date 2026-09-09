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
import net.minecraft.world.entity.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Native Arson ESP family inspired by Glazed feature coverage. */
public final class GlazedESPModule implements ToggleableModule {
    public enum Mode {
        PLAYERS, MOBS, ITEMS, VILLAGERS, PILLAGERS, WANDERING_TRADERS,
        AMETHYST, BEEHIVES, DEEPSLATE, DRIPSTONE, KELP, VINES, LIGHT_BLOCKS
    }

    private static final double DEFAULT_RANGE = 96.0D;
    private static final int BLOCK_SCAN_RADIUS = 20;
    private static final int BLOCK_LIMIT = 384;
    private static final int BLOCK_REFRESH_TICKS = 10;
    private static final int ENTITY_REFRESH_DISTANCE_SQR = 4;
    private static final List<GlazedESPModule> INSTANCES = new ArrayList<>();
    private static final EnumMap<Mode, List<BlockPos>> BLOCK_CACHE = new EnumMap<>(Mode.class);
    private static boolean hookInstalled;
    private static Object cachedLevel;
    private static BlockPos cachedCenter;
    private static int sharedRefreshTicks;

    private final String id;
    private final Mode mode;
    private boolean enabled;
    private double range = DEFAULT_RANGE;
    private int refreshTicks;
    private final List<BlockPos> matches = new ArrayList<>();

    public GlazedESPModule(String id, Mode mode) {
        this.id = id;
        this.mode = mode;
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        installHook();
    }

    @Override public String id() { return id; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            matches.clear();
            refreshTicks = 0;
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) {
            matches.clear();
            return;
        }
        if (isBlockMode()) {
            refreshTicks++;
            refreshSharedBlockCache(client, refreshTicks >= BLOCK_REFRESH_TICKS);
            matches.clear();
            List<BlockPos> cached = BLOCK_CACHE.get(mode);
            if (cached != null) matches.addAll(cached);
        }
    }

    public ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings();
        settings.number("range", "Range", () -> String.format("%.0f", range),
                () -> range = Math.min(128.0D, range + 8.0D),
                () -> range = Math.max(16.0D, range - 8.0D));
        return settings;
    }

    private boolean isBlockMode() {
        return switch (mode) {
            case AMETHYST, BEEHIVES, DEEPSLATE, DRIPSTONE, KELP, VINES, LIGHT_BLOCKS -> true;
            default -> false;
        };
    }

    private static synchronized void refreshSharedBlockCache(Minecraft mc, boolean requested) {
        BlockPos center = mc.player.blockPosition();
        boolean worldChanged = cachedLevel != mc.level;
        boolean moved = cachedCenter == null || cachedCenter.distSqr(center) >= ENTITY_REFRESH_DISTANCE_SQR;
        if (!requested && !worldChanged && !moved && !BLOCK_CACHE.isEmpty()) return;
        if (sharedRefreshTicks++ < BLOCK_REFRESH_TICKS && !worldChanged && !moved && !BLOCK_CACHE.isEmpty()) return;
        sharedRefreshTicks = 0;
        cachedLevel = mc.level;
        cachedCenter = center;
        BLOCK_CACHE.clear();
        for (Mode blockMode : new Mode[]{Mode.AMETHYST, Mode.BEEHIVES, Mode.DEEPSLATE, Mode.DRIPSTONE, Mode.KELP, Mode.VINES, Mode.LIGHT_BLOCKS}) {
            BLOCK_CACHE.put(blockMode, new ArrayList<>());
        }
        BlockPos min = center.offset(-BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS);
        BlockPos max = center.offset(BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            for (Mode blockMode : BLOCK_CACHE.keySet()) {
                List<BlockPos> result = BLOCK_CACHE.get(blockMode);
                if (result.size() < BLOCK_LIMIT && matchesBlock(blockMode, mc.level.getBlockState(pos).getBlock())) {
                    result.add(pos.immutable());
                }
            }
        }
    }

    private static boolean matchesBlock(Mode mode, Block block) {
        return switch (mode) {
            case AMETHYST -> block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST
                    || block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
                    || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD;
            case BEEHIVES -> block == Blocks.BEE_NEST || block == Blocks.BEEHIVE;
            case DEEPSLATE -> block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE
                    || block == Blocks.DEEPSLATE_BRICKS || block == Blocks.DEEPSLATE_TILES
                    || block == Blocks.POLISHED_DEEPSLATE;
            case DRIPSTONE -> block == Blocks.DRIPSTONE_BLOCK || block == Blocks.POINTED_DRIPSTONE;
            case KELP -> block == Blocks.KELP || block == Blocks.KELP_PLANT;
            case VINES -> block == Blocks.VINE || block == Blocks.WEEPING_VINES
                    || block == Blocks.WEEPING_VINES_PLANT || block == Blocks.TWISTING_VINES
                    || block == Blocks.TWISTING_VINES_PLANT;
            case LIGHT_BLOCKS -> BuiltInRegistries.BLOCK.getKey(block).getPath().contains("light");
            default -> false;
        };
    }

    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            synchronized (INSTANCES) {
                for (GlazedESPModule module : INSTANCES) {
                    if (!module.enabled) continue;
                    try {
                        module.render(context);
                    } catch (RuntimeException exception) {
                        module.enabled = false;
                        module.matches.clear();
                        OurClient.LOGGER.error("Disabling render module '{}' after render failure", module.id, exception);
                    }
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
        if (isBlockMode()) {
            for (BlockPos pos : matches) emitBox(pose, buffer, new AABB(pos), camera);
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!entity.isAlive() || entity == mc.player || outOfRange(entity, camera)) continue;
            if (matchesEntity(entity)) emitBox(pose, buffer, entity.getBoundingBox().inflate(entity instanceof ItemEntity ? 0.05D : 0.02D), camera);
        }
    }

    private boolean matchesEntity(Entity entity) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return switch (mode) {
            case PLAYERS -> entity instanceof Player;
            case MOBS -> entity instanceof net.minecraft.world.entity.Mob && !(entity instanceof Player);
            case ITEMS -> entity instanceof ItemEntity;
            case VILLAGERS -> path.equals("villager") || path.equals("zombie_villager");
            case PILLAGERS -> path.equals("pillager") || path.equals("vindicator") || path.equals("evoker") || path.equals("illusioner");
            case WANDERING_TRADERS -> path.equals("wandering_trader");
            default -> false;
        };
    }

    private boolean outOfRange(Entity entity, Vec3 camera) {
        return camera.distanceToSqr(entity.position()) > range * range;
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB box, Vec3 camera) {
        float minX = (float) (box.minX - camera.x), minY = (float) (box.minY - camera.y), minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x), maxY = (float) (box.maxY - camera.y), maxZ = (float) (box.maxZ - camera.z);
        line(pose, consumer, minX, minY, minZ, maxX, minY, minZ); line(pose, consumer, maxX, minY, minZ, maxX, minY, maxZ);
        line(pose, consumer, maxX, minY, maxZ, minX, minY, maxZ); line(pose, consumer, minX, minY, maxZ, minX, minY, minZ);
        line(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ); line(pose, consumer, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(pose, consumer, maxX, maxY, maxZ, minX, maxY, maxZ); line(pose, consumer, minX, maxY, maxZ, minX, maxY, minZ);
        line(pose, consumer, minX, minY, minZ, minX, maxY, minZ); line(pose, consumer, maxX, minY, minZ, maxX, maxY, minZ);
        line(pose, consumer, maxX, minY, maxZ, maxX, maxY, maxZ); line(pose, consumer, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1).setColor(1.0f, 1.0f, 1.0f, 0.9f).setLineWidth(1.5f).setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, x2, y2, z2).setColor(1.0f, 1.0f, 1.0f, 0.9f).setLineWidth(1.5f).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
