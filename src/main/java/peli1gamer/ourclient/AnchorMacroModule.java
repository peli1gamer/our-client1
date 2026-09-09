package peli1gamer.ourclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private boolean oneGlowstone = true;
    private boolean whileUse;
    private boolean lootProtect;
    private int explodeSlot = 9;
    private boolean onlyOwn;
    private boolean onlyCharge;
    private boolean switchBack;
    private int switchDelay = 1;
    private int glowstoneDelay;
    private int explodeDelay = 1;
    private int placeChance = 100;
    private int switchChance = 100;
    private int glowstoneChance = 100;
    private int explodeChance = 100;

    private int switchClock;
    private int glowstoneClock;
    private int explodeClock;
    private LocalPlayer activePlayer;

    @Override public String id() { return "anchor-macro"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            activePlayer = Minecraft.getInstance().player;
            resetClocks();
            knownAnchors.clear();
            ownedAnchors.clear();
        } else {
            resetClocks();
            activePlayer = null;
            knownAnchors.clear();
            ownedAnchors.clear();
        }
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null || !mc.player.isAlive()) return;
        if (activePlayer != mc.player) {
            activePlayer = mc.player;
            resetClocks();
            knownAnchors.clear();
            ownedAnchors.clear();
        }

        tickClocks();
        trackOwnAnchorCandidate(mc);

        if (!isUseHeld(mc)) {
            resetClocks();
            return;
        }
        if (!whileUse && (mc.player.isUsingItem() || isGoodTool(mc.player.getOffhandItem()))) return;
        if (lootProtect && (bodyNearby(mc) || valuablesNearby(mc))) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) return;
        if (onlyOwn && !ownedAnchors.contains(pos)) return;

        int wanted = oneGlowstone ? 1 : 4;
        if (charges(mc, pos) < wanted) charge(mc, hit);
        else detonate(mc, pos, hit);
    }

    private void trackOwnAnchorCandidate(Minecraft mc) {
        if (!isUseHeld(mc) || !mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockPos clicked = hit.getBlockPos();
        BlockPos target = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        if (!mc.level.getBlockState(target).is(Blocks.RESPAWN_ANCHOR)) return;
        if (knownAnchors.add(target)) ownedAnchors.add(target);
    }

    private void charge(Minecraft mc, BlockHitResult hit) {
        if (roll() > placeChance) return;
        if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
            if (switchClock < switchDelay) { switchClock++; return; }
            if (roll() <= switchChance) {
                switchClock = 0;
                useHotbarItem(mc, Items.GLOWSTONE, hit);
            }
            return;
        }
        if (glowstoneClock < glowstoneDelay) { glowstoneClock++; return; }
        if (roll() <= glowstoneChance) {
            glowstoneClock = 0;
            rightClick(mc, hit);
        }
    }

    private void detonate(Minecraft mc, BlockPos pos, BlockHitResult hit) {
        int slot = explodeSlot - 1;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            if (switchClock < switchDelay) { switchClock++; return; }
            if (roll() <= switchChance) {
                switchClock = 0;
                mc.player.getInventory().setSelectedSlot(slot);
            }
            return;
        }
        if (explodeClock < explodeDelay) { explodeClock++; return; }
        if (roll() > explodeChance) return;
        explodeClock = 0;
        if (onlyCharge) return;
        rightClick(mc, hit);
        ownedAnchors.remove(pos);
        knownAnchors.remove(pos);
        if (switchBack) switchTo(mc, Items.RESPAWN_ANCHOR);
    }

    private void useHotbarItem(Minecraft mc, Item item, BlockHitResult hit) {
        int slot = findHotbar(mc.player, item);
        if (slot < 0) return;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        try {
            if (item == Items.GLOWSTONE) rightClick(mc, hit);
        } finally {
            if (oldSlot != slot) mc.player.getInventory().setSelectedSlot(oldSlot);
        }
    }

    private void switchTo(Minecraft mc, Item item) {
        int slot = findHotbar(mc.player, item);
        if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot);
    }

    private void rightClick(Minecraft mc, BlockHitResult hit) {
        if (mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction()) {
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private int charges(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.RESPAWN_ANCHOR) ? state.getValue(RespawnAnchorBlock.CHARGE) : 0;
    }

    private int findHotbar(LocalPlayer player, Item item) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).is(item)) return i;
        return -1;
    }

    private boolean isUseHeld(Minecraft mc) {
        InputConstants.Key key = InputConstants.getKey(mc.options.keyUse.saveString());
        return key != null && !key.equals(InputConstants.UNKNOWN) && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
    }

    private boolean isGoodTool(ItemStack stack) {
        if (stack.isEmpty() || !(stack.is(ItemTags.SWORDS) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
            || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES))) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.contains("diamond") || id.contains("netherite");
    }

    private boolean bodyNearby(Minecraft mc) {
        for (Entity entity : mc.level.players()) {
            if (entity == mc.player || entity.distanceToSqr(mc.player) > 36.0) continue;
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) return true;
        }
        return false;
    }

    private boolean valuablesNearby(Minecraft mc) {
        AABB area = new AABB(mc.player.getX() - 10, mc.player.getY() - 5, mc.player.getZ() - 10,
            mc.player.getX() + 10, mc.player.getY() + 5, mc.player.getZ() + 10);
        int goodArmor = 0;
        for (Entity entity : mc.level.getEntities(null, area)) {
            if (!(entity instanceof ItemEntity item)) continue;
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) continue;
            if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (id.startsWith("netherite_") || id.startsWith("diamond_")) goodArmor++;
            } else if (stack.getCount() > 32 && (stack.is(Items.END_CRYSTAL) || stack.is(Items.OBSIDIAN)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.EXPERIENCE_BOTTLE))) return true;
        }
        return goodArmor >= 2;
    }

    private int roll() { return random.nextInt(100) + 1; }
    private void tickClocks() {
        if (switchClock > 0) switchClock--;
        if (glowstoneClock > 0) glowstoneClock--;
        if (explodeClock > 0) explodeClock--;
    }
    private void resetClocks() { switchClock = 0; glowstoneClock = 0; explodeClock = 0; }

    public boolean oneGlowstone() { return oneGlowstone; }
    public void setOneGlowstone(boolean value) { oneGlowstone = value; }
    public boolean whileUse() { return whileUse; }
    public void setWhileUse(boolean value) { whileUse = value; }
    public boolean lootProtect() { return lootProtect; }
    public void setLootProtect(boolean value) { lootProtect = value; }
    public int explodeSlot() { return explodeSlot; }
    public void setExplodeSlot(int value) { explodeSlot = clamp(value, 1, 9); }
    public boolean onlyOwn() { return onlyOwn; }
    public void setOnlyOwn(boolean value) { onlyOwn = value; }
    public boolean onlyCharge() { return onlyCharge; }
    public void setOnlyCharge(boolean value) { onlyCharge = value; }
    public boolean switchBack() { return switchBack; }
    public void setSwitchBack(boolean value) { switchBack = value; }
    public int switchDelay() { return switchDelay; }
    public void setSwitchDelay(int value) { switchDelay = clamp(value, 0, 20); }
    public int glowstoneDelay() { return glowstoneDelay; }
    public void setGlowstoneDelay(int value) { glowstoneDelay = clamp(value, 0, 20); }
    public int explodeDelay() { return explodeDelay; }
    public void setExplodeDelay(int value) { explodeDelay = clamp(value, 0, 20); }
    public int placeChance() { return placeChance; }
    public void setPlaceChance(int value) { placeChance = clamp(value, 0, 100); }
    public int switchChance() { return switchChance; }
    public void setSwitchChance(int value) { switchChance = clamp(value, 0, 100); }
    public int glowstoneChance() { return glowstoneChance; }
    public void setGlowstoneChance(int value) { glowstoneChance = clamp(value, 0, 100); }
    public int explodeChance() { return explodeChance; }
    public void setExplodeChance(int value) { explodeChance = clamp(value, 0, 100); }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
