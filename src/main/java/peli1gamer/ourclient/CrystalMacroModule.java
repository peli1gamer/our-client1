package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

/** Hold-use crystal helper. It only acts on the player's current crosshair target. */
public final class CrystalMacroModule implements ToggleableModule {
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
            if (mc.player != null && previousSlot >= 0 && previousSlot < 9) mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            reset();
        }
    }

    private void reset() { placeDelay = 0; breakDelay = 0; }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;
        if (!mc.player.isAlive() || mc.player.isUsingItem()) return;
        if (placeDelay > 0) placeDelay--;
        if (breakDelay > 0) breakDelay--;
        if (!mc.options.keyUse.isDown()) return;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit) tryPlace(mc, blockHit);
        else if (hit instanceof EntityHitResult entityHit) tryBreak(mc, entityHit);
    }

    private void tryPlace(Minecraft mc, BlockHitResult hit) {
        if (placeDelay > 0) return;
        BlockPos pos = hit.getBlockPos();
        if (!mc.level.getBlockState(pos).is(Blocks.OBSIDIAN) && !mc.level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        BlockPos above = pos.above();
        if (!mc.level.isEmptyBlock(above)) return;
        AABB space = new AABB(above.getX(), above.getY(), above.getZ(), above.getX() + 1, above.getY() + 2, above.getZ() + 1);
        if (!mc.level.getEntities((Entity) null, space).isEmpty()) return;
        if (lootNearby(mc, above)) return;
        int slot = findHotbar(mc.player);
        if (slot < 0) return;
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        placeDelay = 1;
    }

    private void tryBreak(Minecraft mc, EntityHitResult hit) {
        if (breakDelay > 0) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof EndCrystal) || !entity.isAlive()) return;
        if (lootNearby(mc, entity.blockPosition())) return;
        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        breakDelay = nextBreakDelay;
        rollBreakDelay();
    }

    private void rollBreakDelay() { nextBreakDelay = 1 + random.nextInt(2); }

    private boolean lootNearby(Minecraft mc, BlockPos pos) {
        AABB area = new AABB(pos.getX() - 10, pos.getY() - 10, pos.getZ() - 10, pos.getX() + 10, pos.getY() + 10, pos.getZ() + 10);
        for (Entity entity : mc.level.getEntities((Entity) null, area)) if (entity instanceof ItemEntity item && !item.getItem().isEmpty()) return true;
        return false;
    }

    private int findHotbar(LocalPlayer player) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return i;
        return -1;
    }
}
