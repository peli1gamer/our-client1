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

/** Lightweight ore overlay with an incremental scan to avoid client-thread stalls. */
public final class XrayModule implements ToggleableModule {
    private static final int RADIUS = 20;
    private static final int BLOCKS_PER_TICK = 1200;
    private static boolean hookInstalled;
    private static XrayModule active;
    private boolean enabled;
    private int scanX, scanY, scanZ;
    private boolean scanning;
    private final List<BlockPos> matches = new ArrayList<>();

    public XrayModule() { installHook(); }
    @Override public String id() { return "xray"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            active = this;
            beginScan();
        } else {
            if (active == this) active = null;
            matches.clear();
            scanning = false;
        }
    }

    private void beginScan() {
        scanX = -RADIUS;
        scanY = -RADIUS;
        scanZ = -RADIUS;
        matches.clear();
        scanning = true;
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || !mc.player.isAlive()) return;
        try {
            if (!scanning) beginScan();
            BlockPos origin = mc.player.blockPosition();
            int minY = Math.max(mc.level.getMinY(), origin.getY() - RADIUS);
            int maxY = Math.min(mc.level.getMaxY(), origin.getY() + RADIUS);
            int processed = 0;
            while (scanning && processed++ < BLOCKS_PER_TICK) {
                int worldY = origin.getY() + scanY;
                if (worldY >= minY && worldY <= maxY) {
                    BlockPos pos = origin.offset(scanX, scanY, scanZ);
                    if (isOre(mc.level.getBlockState(pos).getBlock())) matches.add(pos.immutable());
                }
                advance();
            }
        } catch (RuntimeException exception) {
            matches.clear();
            scanning = false;
            enabled = false;
            if (active == this) active = null;
            OurClient.LOGGER.error("Disabling Xray after a scan failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.xray = false;
        }
    }

    private void advance() {
        scanZ++;
        if (scanZ > RADIUS) {
            scanZ = -RADIUS;
            scanY++;
            if (scanY > RADIUS) {
                scanY = -RADIUS;
                scanX++;
                if (scanX > RADIUS) scanning = false;
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
        if (mc.player == null || mc.getCameraEntity() == null) return;
        try {
            List<BlockPos> snapshot = List.copyOf(matches);
            MultiBufferSource consumers = context.consumers();
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = context.matrices().last();
            int rendered = 0;
            for (BlockPos pos : snapshot) {
                if (mc.player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > RADIUS * RADIUS) continue;
                emitBox(pose, buffer, new AABB(pos).inflate(.01));
                if (++rendered >= 512) break;
            }
        } catch (RuntimeException exception) {
            enabled = false;
            scanning = false;
            if (active == this) active = null;
            OurClient.LOGGER.error("Disabling Xray after a render failure", exception);
            ClientConfig config = OurClient.config();
            if (config != null) config.xray = false;
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, AABB b) {
        float minX = (float)b.minX, minY = (float)b.minY, minZ = (float)b.minZ;
        float maxX = (float)b.maxX, maxY = (float)b.maxY, maxZ = (float)b.maxZ;
        line(pose,consumer,minX,minY,minZ,maxX,minY,minZ); line(pose,consumer,maxX,minY,minZ,maxX,minY,maxZ);
        line(pose,consumer,maxX,minY,maxZ,minX,minY,maxZ); line(pose,consumer,minX,minY,maxZ,minX,minY,minZ);
        line(pose,consumer,minX,maxY,minZ,maxX,maxY,minZ); line(pose,consumer,maxX,maxY,minZ,maxX,maxY,maxZ);
        line(pose,consumer,maxX,maxY,maxZ,minX,maxY,maxZ); line(pose,consumer,minX,maxY,maxZ,minX,maxY,minZ);
        line(pose,consumer,minX,minY,minZ,minX,maxY,minZ); line(pose,consumer,maxX,minY,minZ,maxX,maxY,minZ);
        line(pose,consumer,maxX,minY,maxZ,maxX,maxY,maxZ); line(pose,consumer,minX,minY,maxZ,minX,maxY,maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer,float x1,float y1,float z1,float x2,float y2,float z2) {
        float nx=x2-x1, ny=y2-y1, nz=z2-z1;
        consumer.addVertex(pose,x1,y1,z1).setColor(1.0f,.55f,.05f,.95f).setNormal(pose,nx,ny,nz).setLineWidth(1.5f);
        consumer.addVertex(pose,x2,y2,z2).setColor(1.0f,.55f,.05f,.95f).setNormal(pose,nx,ny,nz).setLineWidth(1.5f);
    }
}
