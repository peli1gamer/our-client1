package peli1gamer.ourclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Native Arson implementation of a hold-use respawn-anchor charge/detonate macro. */
public final class AnchorMacroModule implements ToggleableModule {
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private final Set<BlockPos> knownAnchors = new HashSet<>();
    private final Random random = new Random();
    private boolean enabled;
    private boolean oneGlowstone = true, whileUse, lootProtect, onlyOwn, onlyCharge, switchBack;
    private int explodeSlot = 9, switchDelay = 1, glowstoneDelay, explodeDelay = 1;
    private int placeChance = 100, switchChance = 100, glowstoneChance = 100, explodeChance = 100;
    private int switchClock, glowstoneClock, explodeClock;
    private LocalPlayer activePlayer;

    @Override public String id() { return "anchor-macro"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled; resetClocks(); activePlayer = Minecraft.getInstance().player;
        knownAnchors.clear(); ownedAnchors.clear();
    }

    @Override public void onClientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!enabled || player == null || mc.level == null || mc.gameMode == null || mc.screen != null || !player.isAlive()) return;
        if (activePlayer != player) { activePlayer = player; resetClocks(); knownAnchors.clear(); ownedAnchors.clear(); }
        tickClocks();
        trackOwnAnchorCandidate(mc);
        if (!isUseHeld(mc) || (!whileUse && (player.isUsingItem() || isGoodTool(player.getOffhandItem())))
                || (lootProtect && (bodyNearby(mc) || valuablesNearby(mc)))) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR) || (onlyOwn && !ownedAnchors.contains(pos))) return;
        if (charges(mc, pos) < (oneGlowstone ? 1 : 4)) charge(mc, hit); else detonate(mc, pos, hit);
    }

    private void trackOwnAnchorCandidate(Minecraft mc) {
        if (!isUseHeld(mc) || !mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR) || !(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockPos clicked = hit.getBlockPos();
        BlockPos target = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        if (mc.level.getBlockState(target).is(Blocks.RESPAWN_ANCHOR) && knownAnchors.add(target)) ownedAnchors.add(target);
    }

    private void charge(Minecraft mc, BlockHitResult hit) {
        if (!roll(placeChance)) return;
        if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
            if (switchClock < switchDelay) { switchClock++; return; }
            if (roll(switchChance)) { switchClock = 0; useHotbarItem(mc, Items.GLOWSTONE, hit); }
            return;
        }
        if (glowstoneClock < glowstoneDelay) { glowstoneClock++; return; }
        if (roll(glowstoneChance)) { glowstoneClock = 0; rightClick(mc, hit); }
    }

    private void detonate(Minecraft mc, BlockPos pos, BlockHitResult hit) {
        int slot = Math.max(0, Math.min(8, explodeSlot - 1));
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            if (switchClock < switchDelay) { switchClock++; return; }
            if (roll(switchChance)) { switchClock = 0; mc.player.getInventory().setSelectedSlot(slot); }
            return;
        }
        if (explodeClock < explodeDelay) { explodeClock++; return; }
        if (!roll(explodeChance)) return;
        explodeClock = 0;
        if (onlyCharge) return;
        rightClick(mc, hit); ownedAnchors.remove(pos); knownAnchors.remove(pos);
        if (switchBack) switchTo(mc, Items.RESPAWN_ANCHOR);
    }

    private void useHotbarItem(Minecraft mc, Item item, BlockHitResult hit) {
        int slot = findHotbar(mc.player, item); if (slot < 0) return;
        int oldSlot = mc.player.getInventory().getSelectedSlot(); mc.player.getInventory().setSelectedSlot(slot);
        try { rightClick(mc, hit); } finally { if (oldSlot != slot) mc.player.getInventory().setSelectedSlot(oldSlot); }
    }
    private void switchTo(Minecraft mc, Item item) { int slot = findHotbar(mc.player, item); if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot); }
    private void rightClick(Minecraft mc, BlockHitResult hit) {
        if (mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction()) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    private int charges(Minecraft mc, BlockPos pos) { BlockState state = mc.level.getBlockState(pos); return state.is(Blocks.RESPAWN_ANCHOR) ? state.getValue(RespawnAnchorBlock.CHARGE) : 0; }
    private int findHotbar(LocalPlayer player, Item item) { for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).is(item)) return i; return -1; }
    private boolean isUseHeld(Minecraft mc) { return mc.options.keyUse.isDown(); }
    private boolean isGoodTool(ItemStack stack) {
        if (stack.isEmpty() || !(stack.is(ItemTags.SWORDS) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES))) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(); return id.contains("diamond") || id.contains("netherite");
    }
    private boolean bodyNearby(Minecraft mc) { for (Entity e : mc.level.players()) if (e != mc.player && e.distanceToSqr(mc.player) <= 36.0 && e instanceof LivingEntity l && l.isDeadOrDying()) return true; return false; }
    private boolean valuablesNearby(Minecraft mc) {
        AABB area = new AABB(mc.player.getX()-10, mc.player.getY()-5, mc.player.getZ()-10, mc.player.getX()+10, mc.player.getY()+5, mc.player.getZ()+10); int goodArmor = 0;
        for (Entity e : mc.level.getEntities(null, area)) { if (!(e instanceof ItemEntity item)) continue; ItemStack s = item.getItem(); if (s.isEmpty()) continue;
            if (s.is(ItemTags.HEAD_ARMOR) || s.is(ItemTags.CHEST_ARMOR) || s.is(ItemTags.LEG_ARMOR) || s.is(ItemTags.FOOT_ARMOR)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); if (id.startsWith("netherite_") || id.startsWith("diamond_")) goodArmor++;
            } else if (s.getCount() > 32 && (s.is(Items.END_CRYSTAL) || s.is(Items.OBSIDIAN) || s.is(Items.ENCHANTED_GOLDEN_APPLE) || s.is(Items.EXPERIENCE_BOTTLE))) return true;
        } return goodArmor >= 2;
    }
    private boolean roll(int percent) { return random.nextInt(100) < Math.max(0, Math.min(100, percent)); }
    private void tickClocks() { if (switchClock > 0) switchClock--; if (glowstoneClock > 0) glowstoneClock--; if (explodeClock > 0) explodeClock--; }
    private void resetClocks() { switchClock = glowstoneClock = explodeClock = 0; }
    public boolean oneGlowstone() { return oneGlowstone; } public void setOneGlowstone(boolean v) { oneGlowstone = v; }
    public boolean whileUse() { return whileUse; } public void setWhileUse(boolean v) { whileUse = v; }
    public boolean lootProtect() { return lootProtect; } public void setLootProtect(boolean v) { lootProtect = v; }
    public int explodeSlot() { return explodeSlot; } public void setExplodeSlot(int v) { explodeSlot = clamp(v,1,9); }
    public boolean onlyOwn() { return onlyOwn; } public void setOnlyOwn(boolean v) { onlyOwn = v; }
    public boolean onlyCharge() { return onlyCharge; } public void setOnlyCharge(boolean v) { onlyCharge = v; }
    public boolean switchBack() { return switchBack; } public void setSwitchBack(boolean v) { switchBack = v; }
    public int switchDelay() { return switchDelay; } public void setSwitchDelay(int v) { switchDelay = clamp(v,0,20); }
    public int glowstoneDelay() { return glowstoneDelay; } public void setGlowstoneDelay(int v) { glowstoneDelay = clamp(v,0,20); }
    public int explodeDelay() { return explodeDelay; } public void setExplodeDelay(int v) { explodeDelay = clamp(v,0,20); }
    public int placeChance() { return placeChance; } public void setPlaceChance(int v) { placeChance = clamp(v,0,100); }
    public int switchChance() { return switchChance; } public void setSwitchChance(int v) { switchChance = clamp(v,0,100); }
    public int glowstoneChance() { return glowstoneChance; } public void setGlowstoneChance(int v) { glowstoneChance = clamp(v,0,100); }
    public int explodeChance() { return explodeChance; } public void setExplodeChance(int v) { explodeChance = clamp(v,0,100); }
    private static int clamp(int v,int min,int max) { return Math.max(min, Math.min(max,v)); }
}
