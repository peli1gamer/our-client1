package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Lightweight ore-finder overlay. It highlights valuable ore blocks around the player. */
public final class XrayModule implements ToggleableModule {
    private static final int RADIUS = 24;
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
        if (--scanTimer > 0) return;
        scanTimer = 10;
        matches.clear();
        BlockPos origin = mc.player.blockPosition();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (isOre(mc.level.getBlockState(pos).getBlock())) matches.add(pos.immutable());
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
        if (!enabled || matches.isEmpty() || context.commandQueue() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            Vec3 camera = mc.getCameraEntity() != null ? mc.getCameraEntity().getPosition(1.0F) : mc.player.getPosition(1.0F);
            List<Box> boxes = new ArrayList<>(matches.size());
            for (BlockPos pos : matches) {
                if (mc.player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > RADIUS * RADIUS) continue;
                AABB box = new AABB(pos).move(-camera.x, -camera.y, -camera.z).inflate(.02);
                boxes.add(new Box((float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ));
            }
            if (boxes.isEmpty()) return;
            context.commandQueue().submitCustomGeometry(context.matrices(), RenderTypes.lines(), (pose, consumer) -> emitBoxes(pose, consumer, boxes));
        } catch (RuntimeException exception) {
            enabled = false;
            if (active == this) active = null;
            OurClient.LOGGER.error("Disabling Xray after a render failure", exception);
        }
    }

    private static void emitBoxes(PoseStack.Pose pose, VertexConsumer consumer, List<Box> boxes) {
        for (Box b : boxes) {
            line(pose, consumer, b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ);
            line(pose, consumer, b.maxX, b.minY, b.maxZ, b.minX, b.minY, b.maxZ);
            line(pose, consumer, b.minX, b.minY, b.maxZ, b.minX, b.minY, b.minZ);
            line(pose, consumer, b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ);
            line(pose, consumer, b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ);
            line(pose, consumer, b.minX, b.maxY, b.maxZ, b.minX, b.maxY, b.minZ);
            line(pose, consumer, b.minX, b.minY, b.minZ, b.minX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ);
            line(pose, consumer, b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ);
            line(pose, consumer, b.minX, b.minY, b.maxZ, b.minX, b.maxY, b.maxZ);
        }
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        consumer.addVertex(pose, x1, y1, z1).setColor(1.0f, .55f, .05f, .95f).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, x2, y2, z2).setColor(1.0f, .55f, .05f, .95f).setNormal(pose, 0, 1, 0);
    }

    private record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
}
