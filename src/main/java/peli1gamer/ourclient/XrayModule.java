package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Lightweight ore overlay using Fabric's world extraction/buffer path. */
public final class XrayModule implements ToggleableModule {
    private static final int RADIUS = 20;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_RENDERED_BLOCKS = 512;
    private static boolean hookInstalled;
    private static XrayModule active;
    private boolean enabled;
    private int scanTimer;
    private ClientLevel trackedLevel;
    private final List<BlockPos> matches = new ArrayList<>();

    public XrayModule() { installHook(); }

    @Override public String id() { return "xray"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            active = this;
            scanTimer = 0;
            trackedLevel = null;
            matches.clear();
        } else if (active == this) {
            active = null;
            matches.clear();
            scanTimer = 0;
            trackedLevel = null;
        }
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || !mc.player.isAlive() || mc.screen != null) return;

        if (trackedLevel != mc.level) {
            // Never carry ore positions from a previous world/dimension into the new one.
            trackedLevel = mc.level;
            matches.clear();
            scanTimer = 0;
        }

        if (scanTimer > 0) {
            scanTimer--;
            return;
        }
        scanTimer = SCAN_INTERVAL_TICKS;

        try {
            List<BlockPos> next = new ArrayList<>();
            Vec3 camera = mc.getCameraEntity() == null ? mc.player.position() : mc.getCameraEntity().position();
            BlockPos origin = BlockPos.containing(camera);
            int minY = Math.max(mc.level.getMinY(), origin.getY() - RADIUS);
            int maxY = Math.min(mc.level.getMaxY() - 1, origin.getY() + RADIUS);
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = -RADIUS; z <= RADIUS; z++) {
                        BlockPos pos = origin.offset(x, y - origin.getY(), z);
                        if (isOre(mc.level.getBlockState(pos).getBlock())) next.add(pos.immutable());
                    }
                }
            }
            matches.clear();
            matches.addAll(next);
        } catch (RuntimeException exception) {
            matches.clear();
            setEnabled(false);
            OurClient.LOGGER.error("Disabling Xray after a scan failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) {
                config.xray = false;
                try {
                    OurClient.syncAndSaveConfigFromModules();
                } catch (RuntimeException saveException) {
                    OurClient.LOGGER.error("Failed to persist Xray scan-failure state", saveException);
                }
            }
        }
    }

    private static boolean isOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            XrayModule module = active;
            if (module != null) module.render(context);
        });
    }

    private void render(WorldRenderContext context) {
        if (!enabled || matches.isEmpty() || context == null || context.matrices() == null || context.consumers() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.screen != null || mc.level == null
                || mc.getCameraEntity() == null || mc.getCameraEntity().level() != mc.level) return;

        try {
            List<BlockPos> snapshot = List.copyOf(matches);
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();
            Vec3 camera = mc.getCameraEntity().position();
            int rendered = 0;

            for (BlockPos pos : snapshot) {
                if (camera.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > RADIUS * RADIUS) continue;
                AABB box = new AABB(pos).inflate(.01);
                emitBox(pose, buffer, box, camera);
                if (++rendered >= MAX_RENDERED_BLOCKS) break;
            }
        } catch (RuntimeException exception) {
            setEnabled(false);
            OurClient.LOGGER.error("Disabling Xray after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) {
                config.xray = false;
                try {
                    OurClient.syncAndSaveConfigFromModules();
                } catch (RuntimeException saveException) {
                    OurClient.LOGGER.error("Failed to persist Xray render-failure state", saveException);
                }
            }
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b, Vec3 camera) {
        float minX = (float) (b.minX - camera.x), minY = (float) (b.minY - camera.y), minZ = (float) (b.minZ - camera.z);
        float maxX = (float) (b.maxX - camera.x), maxY = (float) (b.maxY - camera.y), maxZ = (float) (b.maxZ - camera.z);
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

    private static void line(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2) {
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(1.0f, .55f, .05f, .95f)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.5f);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(1.0f, .55f, .05f, .95f)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.5f);
    }
}
