package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Lightweight block ESP with cached scanning and see-through rendering. */
public final class BlockESPModule implements ToggleableModule {
    private static final int DEFAULT_RADIUS = 24;
    private static final int MAX_RESULTS = 256;
    private static boolean hookInstalled;
    private static BlockESPModule active;

    private final List<BlockPos> matches = new ArrayList<>();
    private boolean enabled;
    private int radius = DEFAULT_RADIUS;

    public BlockESPModule() { installHook(); }
    @Override public String id() { return "block-esp"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) active = this;
        else if (active == this) active = null;
        if (!enabled) matches.clear();
    }

    @Override
    public void onClientTick(Minecraft client) {
        radius = Math.max(4, Math.min(48, radius));
        if (!enabled || client.player == null || client.level == null || client.player.isSpectator()) {
            matches.clear();
            if (active == this && !enabled) active = null;
            return;
        }
        matches.clear();
        BlockPos center = client.player.blockPosition();
        int r = radius;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (matches.size() >= MAX_RESULTS) break;
            if (isInteresting(client.level.getBlockState(pos).getBlock())) matches.add(pos.immutable());
        }
    }

    public int radius() { return radius; }
    public void increaseRadius() { radius = Math.min(48, radius + 4); }
    public void decreaseRadius() { radius = Math.max(4, radius - 4); }

    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(BlockESPModule::render);
    }

    private static void render(WorldRenderContext context) {
        BlockESPModule module = active;
        if (module == null || !module.enabled || context == null || context.matrices() == null
                || context.consumers() == null || module.matches.isEmpty()) return;
        try {
            VertexConsumer consumer = context.consumers().getBuffer(ArsonRenderTypes.SEE_THROUGH_LINES);
            PoseStack.Pose pose = context.matrices().last();
            for (BlockPos pos : module.matches) emitBox(pose, consumer, new AABB(pos));
        } catch (RuntimeException exception) {
            module.enabled = false;
            module.matches.clear();
            if (active == module) active = null;
            OurClient.LOGGER.error("Disabling Block ESP after render failure", exception);
        }
    }

    private static boolean isInteresting(Block block) {
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer c, AABB b) {
        line(pose,c,b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ); line(pose,c,b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ);
        line(pose,c,b.maxX,b.minY,b.maxZ,b.minX,b.minY,b.maxZ); line(pose,c,b.minX,b.minY,b.maxZ,b.minX,b.minY,b.minZ);
        line(pose,c,b.minX,b.maxY,b.minZ,b.maxX,b.maxY,b.minZ); line(pose,c,b.maxX,b.maxY,b.minZ,b.maxX,b.maxY,b.maxZ);
        line(pose,c,b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ); line(pose,c,b.minX,b.maxY,b.maxZ,b.minX,b.maxY,b.minZ);
        line(pose,c,b.minX,b.minY,b.minZ,b.minX,b.maxY,b.minZ); line(pose,c,b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ);
        line(pose,c,b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ); line(pose,c,b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer c,double x1,double y1,double z1,double x2,double y2,double z2) {
        c.addVertex(pose,(float)x1,(float)y1,(float)z1).setColor(1,0.42f,0,0.9f).setLineWidth(1.5f).setNormal(pose,0,1,0);
        c.addVertex(pose,(float)x2,(float)y2,(float)z2).setColor(1,0.42f,0,0.9f).setLineWidth(1.5f).setNormal(pose,0,1,0);
    }
}
