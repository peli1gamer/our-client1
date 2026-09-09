package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Temporarily selects the strongest standard sword or axe while attacking. */
public final class AttributeSwapModule implements ToggleableModule {
    private boolean enabled;
    private boolean onlyWhenAttacking = true;
    private int previousSlot = -1;
    private int activeWeaponSlot = -1;
    private ClientLevel activeLevel;
    private LocalPlayer activePlayer;

    @Override public String id() { return "attribute-swap"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft mc = Minecraft.getInstance();
        if (enabled) {
            previousSlot = -1;
            activeWeaponSlot = -1;
            activeLevel = mc.level;
            activePlayer = mc.player;
        } else {
            restore(mc.player);
            activeLevel = null;
            activePlayer = null;
        }
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.screen != null || !mc.player.isAlive()) {
            if (enabled && mc.player != null && (mc.level == null || mc.screen != null || !mc.player.isAlive())) {
                restore(mc.player);
            }
            return;
        }

        if (activePlayer != mc.player) {
            // A respawn/player replacement gives us a different inventory owner.
            // Never apply the old player's saved slot to the new player.
            previousSlot = -1;
            activeWeaponSlot = -1;
            activePlayer = mc.player;
        }

        if (activeLevel != null && activeLevel != mc.level) {
            // The same player object can survive a dimension transition. Clear
            // any slot ownership before continuing in the new level.
            restore(mc.player);
        }
        activeLevel = mc.level;

        boolean attacking = mc.options.keyAttack.isDown();
        if (onlyWhenAttacking && !attacking) {
            restore(mc.player);
            return;
        }
        if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)) {
            restore(mc.player);
            return;
        }
        Entity entity = hit.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            restore(mc.player);
            return;
        }

        int preferred = findBestWeapon(mc.player);
        if (preferred < 0) {
            restore(mc.player);
            return;
        }

        int current = mc.player.getInventory().getSelectedSlot();
        if (activeWeaponSlot >= 0 && current != activeWeaponSlot) {
            // The user changed slots while the module owned the swap. Do not
            // fight their input or restore over their new choice.
            previousSlot = -1;
            activeWeaponSlot = -1;
        }

        if (preferred != current) {
            if (activeWeaponSlot < 0) previousSlot = current;
            mc.player.getInventory().setSelectedSlot(preferred);
            activeWeaponSlot = preferred;
        }
    }

    private int findBestWeapon(LocalPlayer player) {
        int best = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            int score = weaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = score >= 0 ? slot : -1;
            }
        }
        return best;
    }

    private int weaponScore(ItemStack stack) {
        if (stack.is(Items.NETHERITE_AXE)) return 100;
        if (stack.is(Items.NETHERITE_SWORD)) return 99;
        if (stack.is(Items.DIAMOND_AXE)) return 90;
        if (stack.is(Items.DIAMOND_SWORD)) return 89;
        if (stack.is(Items.IRON_AXE)) return 80;
        if (stack.is(Items.IRON_SWORD)) return 79;
        if (stack.is(Items.STONE_AXE)) return 70;
        if (stack.is(Items.STONE_SWORD)) return 69;
        if (stack.is(Items.GOLDEN_AXE)) return 60;
        if (stack.is(Items.GOLDEN_SWORD)) return 59;
        if (stack.is(Items.WOODEN_AXE)) return 50;
        if (stack.is(Items.WOODEN_SWORD)) return 49;
        return -1;
    }

    private void restore(LocalPlayer player) {
        if (player != null && previousSlot >= 0 && previousSlot < 9
                && activeWeaponSlot >= 0 && player.getInventory().getSelectedSlot() == activeWeaponSlot) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        activeWeaponSlot = -1;
    }
}
