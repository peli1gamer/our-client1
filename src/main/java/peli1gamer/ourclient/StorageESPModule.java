package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

public final class StorageESPModule implements ToggleableModule {
    private static StorageESPModule active;
    private static boolean hookInstalled;
    private final List<BlockPos> matches = new ArrayList<>();
    private boolean enabled;
    private int tick;

    public StorageESPModule() { installHook(); }
    @Override public String id() { return "storage-esp"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; if (enabled) active = this; else if (active == this) active = null; if (!enabled) matches.clear(); }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return;
        if (++tick < 10) return;
        tick = 0;
        try {
            matches.clear();
            BlockPos origin = client.player.blockPosition();
            int r = 24;
            for (int x = origin.getX() - r; x <= origin.getX() + r; x++) {
                for (int y = Math.max(client.level.getMinY(), origin.getY() - 12); y <= Math.min(client.level.getMaxY(), origin.getY() + 12); y++) {
                    for (int z = origin.getZ() - r; z <= origin.getZ() + r; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (isStorage(client.level.getBlockState(pos))) matches.add(pos.immutable());
                        if (matches.size() >= 512) return;
                    }
                }
            }
        } catch (RuntimeException ex) {
            enabled = false; if (active == this) active = null;
            OurClient.LOGGER.error("Disabling storage ESP after scan failure", ex);
        }
    }

    private static boolean isStorage(BlockState s) {
        return s.is(Blocks.CHEST) || s.is(Blocks.TRAPPED_CHEST) || s.is(Blocks.BARREL)
            || s.is(Blocks.SHULKER_BOX) || s.is(Blocks.HOPPER) || s.is(Blocks.DROPPER)
            || s.is(Blocks.DISPENSER) || s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE)
            || s.is(Blocks.SMOKER) || s.is(Blocks.BREWING_STAND);
    }

    private static void installHook() {
        if (hookInstalled) return;
        hookInstalled = true;
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            StorageESPModule m = active;
            if (m == null || !m.enabled || context.matrices() == null || context.consumers() == null) return;
            try {
                VertexConsumer c = context.consumers().getBuffer(RenderType.lines());
                PoseStack.Pose p = context.matrices().last();
                for (BlockPos pos : m.matches) draw(p, c, new AABB(pos));
            } catch (RuntimeException ex) {
                m.enabled = false; if (active == m) active = null;
                OurClient.LOGGER.error("Disabling storage ESP after render failure", ex);
            }
        });
    }

    private static void draw(PoseStack.Pose p, VertexConsumer c, AABB b) {
        line(p,c,b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ); line(p,c,b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ);
        line(p,c,b.maxX,b.minY,b.maxZ,b.minX,b.minY,b.maxZ); line(p,c,b.minX,b.minY,b.maxZ,b.minX,b.minY,b.minZ);
        line(p,c,b.minX,b.maxY,b.minZ,b.maxX,b.maxY,b.minZ); line(p,c,b.maxX,b.maxY,b.minZ,b.maxX,b.maxY,b.maxZ);
        line(p,c,b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ); line(p,c,b.minX,b.maxY,b.maxZ,b.minX,b.maxY,b.minZ);
        line(p,c,b.minX,b.minY,b.minZ,b.minX,b.maxY,b.minZ); line(p,c,b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ);
        line(p,c,b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ); line(p,c,b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ);
    }
    private static void line(PoseStack.Pose p, VertexConsumer c,double x1,double y1,double z1,double x2,double y2,double z2) {
        c.addVertex(p,(float)x1,(float)y1,(float)z1).setColor(1,0.6f,0,0.9f).setLineWidth(1.5f).setNormal(p,0,1,0);
        c.addVertex(p,(float)x2,(float)y2,(float)z2).setColor(1,0.6f,0,0.9f).setLineWidth(1.5f).setNormal(p,0,1,0);
    }
}
