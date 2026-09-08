package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

/** Places an end crystal on the crosshair block and breaks a crosshair crystal while enabled. */
public final class CrystalMacroModule implements ToggleableModule {
    private static final double MAX_ACTION_DISTANCE_SQUARED = 36.0D;
    private final Random random = new Random();
    private boolean enabled;
    private int placeDelay;
    private int breakDelay;
    private int nextBreakDelay = 2;
    private int previousSlot = -1;

    @Override public String id() { return "crystal-macro"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft mc = Minecraft.getInstance();
        if (enabled) {
            previousSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
            reset();
            rollBreakDelay();
        } else {
            restorePreviousSlot(mc.player);
            reset();
        }
    }

    private void reset() {
        placeDelay = 0;
        breakDelay = 0;
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null
                || mc.screen != null || !mc.player.isAlive() || mc.player.isUsingItem()) return;

        if (placeDelay > 0) placeDelay--;
        if (breakDelay > 0) breakDelay--;

        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            tryBreak(mc, entityHit);
        } else if (hit instanceof BlockHitResult blockHit) {
            tryPlace(mc, blockHit);
        }
    }

    private void tryPlace(Minecraft mc, BlockHitResult hit) {
        if (placeDelay > 0) return;
        BlockPos base = hit.getBlockPos();
        if (mc.player.distanceToSqr(base.getX() + .5, base.getY() + .5, base.getZ() + .5) > MAX_ACTION_DISTANCE_SQUARED) return;
        if (!mc.level.getBlockState(base).is(Blocks.OBSIDIAN)
                && !mc.level.getBlockState(base).is(Blocks.BEDROCK)) return;

        BlockPos crystalPos = base.above();
        if (!mc.level.isEmptyBlock(crystalPos) || !mc.level.isEmptyBlock(crystalPos.above())) return;
        AABB space = new AABB(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ(),
                crystalPos.getX() + 1, crystalPos.getY() + 2, crystalPos.getZ() + 1);
        if (!mc.level.getEntities((Entity) null, space).isEmpty()) return;

        int slot = findHotbar(mc.player);
        if (slot < 0) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        try {
            if (oldSlot != slot) mc.player.getInventory().setSelectedSlot(slot);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            mc.player.swing(InteractionHand.MAIN_HAND);
            placeDelay = 1;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.warn("Crystal Macro skipped an invalid placement", exception);
            placeDelay = 2;
        } finally {
            if (mc.player != null && oldSlot >= 0 && oldSlot < 9) {
                mc.player.getInventory().setSelectedSlot(oldSlot);
            }
        }
    }

    private void tryBreak(Minecraft mc, EntityHitResult hit) {
        if (breakDelay > 0) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof EndCrystal) || !entity.isAlive()) return;
        if (mc.player.distanceToSqr(entity) > MAX_ACTION_DISTANCE_SQUARED) return;

        try {
            mc.gameMode.attack(mc.player, entity);
            mc.player.resetAttackStrengthTicker();
            mc.player.swing(InteractionHand.MAIN_HAND);
            breakDelay = nextBreakDelay;
            rollBreakDelay();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.warn("Crystal Macro skipped an invalid break", exception);
            breakDelay = 2;
        }
    }

    private void rollBreakDelay() { nextBreakDelay = 1 + random.nextInt(2); }

    private int findHotbar(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private void restorePreviousSlot(LocalPlayer player) {
        if (player != null && previousSlot >= 0 && previousSlot < 9) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }
}
