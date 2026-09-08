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

            // RenderTypes.lines() in this Minecraft version requires a LineWidth
            // vertex element. VertexConsumer does not expose that element, so using
            // it directly leaves incomplete vertices and crashes during upload.
            // The debug line strip render type supplies its width through the render
            // state and is safe for this extraction path.
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.debugLineStrip(1.5));
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

    /** Emits one box as a single line-strip path, with duplicated transition vertices. */
    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b) {
        float minX = (float) b.minX, minY = (float) b.minY, minZ = (float) b.minZ;
        float maxX = (float) b.maxX, maxY = (float) b.maxY, maxZ = (float) b.maxZ;

        lineStrip(pose, consumer,
                minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, minX, minY, maxZ, minX, minY, minZ,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ,
                minX, minY, minZ, minX, maxY, minZ,
                maxX, maxY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, maxX, maxY, maxZ,
                minX, maxY, maxZ, minX, minY, maxZ);
    }

    private static void lineStrip(PoseStack.Pose pose, VertexConsumer consumer, float... p) {
        for (int i = 0; i < p.length; i += 3) {
            float x = p[i], y = p[i + 1], z = p[i + 2];
            float nx = 0.0f, ny = 1.0f, nz = 0.0f;
            if (i + 5 < p.length) {
                nx = p[i + 3] - x;
                ny = p[i + 4] - y;
                nz = p[i + 5] - z;
            }
            consumer.addVertex(pose, x, y, z)
                    .setColor(1.0f, .55f, .05f, .95f)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
