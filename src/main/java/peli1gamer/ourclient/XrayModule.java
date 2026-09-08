package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Lightweight ore overlay using Fabric's world extraction/buffer path. */
public final class XrayModule implements ToggleableModule {
    private static final int RADIUS = 20;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static boolean hookInstalled;
    private static XrayModule active;
    private boolean enabled;
    private int scanTimer;
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
        } else if (active == this) {
            active = null;
            matches.clear();
        }
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || !mc.player.isAlive()) return;
        if (scanTimer > 0) {
            scanTimer--;
            return;
        }
        scanTimer = SCAN_INTERVAL_TICKS;

        try {
            matches.clear();
            BlockPos origin = mc.player.blockPosition();
            int minY = Math.max(mc.level.getMinY(), origin.getY() - RADIUS);
            int maxY = Math.min(mc.level.getMaxY(), origin.getY() + RADIUS);
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = -RADIUS; z <= RADIUS; z++) {
                        BlockPos pos = origin.offset(x, y - origin.getY(), z);
                        if (isOre(mc.level.getBlockState(pos).getBlock())) matches.add(pos.immutable());
                    }
                }
            }
        } catch (RuntimeException exception) {
            matches.clear();
            enabled = false;
            if (active == this) active = null;
            OurClient.LOGGER.error("Disabling Xray after a scan failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.xray = false;
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
        if (mc.player == null || mc.getCameraEntity() == null) return;

        try {
            List<BlockPos> snapshot = List.copyOf(matches);
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();
            int rendered = 0;

            // The world render pose is already camera-relative; use world-space
            // block coordinates rather than translating by the camera twice.
            for (BlockPos pos : snapshot) {
                if (mc.player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > RADIUS * RADIUS) continue;
                AABB box = new AABB(pos).inflate(.01);
                emitBox(pose, buffer, box);
                if (++rendered >= 512) break;
            }
        } catch (RuntimeException exception) {
            enabled = false;
            if (active == this) active = null;
            OurClient.LOGGER.error("Disabling Xray after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.xray = false;
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b) {
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.minY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.minY, (float)b.maxZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.minY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.minY, (float)b.minZ);
        line(pose, consumer, (float)b.minX, (float)b.maxY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.maxY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.maxX, (float)b.maxY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.maxY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.minZ, (float)b.minX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.minZ, (float)b.maxX, (float)b.maxY, (float)b.minZ);
        line(pose, consumer, (float)b.maxX, (float)b.minY, (float)b.maxZ, (float)b.maxX, (float)b.maxY, (float)b.maxZ);
        line(pose, consumer, (float)b.minX, (float)b.minY, (float)b.maxZ, (float)b.minX, (float)b.maxY, (float)b.maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1).setColor(1.0f, .55f, .05f, .95f).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x2, y2, z2).setColor(1.0f, .55f, .05f, .95f).setNormal(pose, 0, 1, 0);
    }
}
