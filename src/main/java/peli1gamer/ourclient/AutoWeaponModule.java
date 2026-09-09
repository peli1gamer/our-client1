package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Selects a useful weapon from the hotbar for the current combat target. */
public final class AutoWeaponModule implements ToggleableModule {
    private boolean enabled;
    private int previousSlot = -1;
    private int activeWeaponSlot = -1;

    @Override public String id() { return "auto-weapon"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (!enabled) restore(client);
        else resetState();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        if (client.player == null || client.level == null || client.screen != null) {
            restore(client);
            return;
        }

        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive()
                || target == client.player
                || target.level() != client.player.level()) {
            restore(client);
            return;
        }

        int current = client.player.getInventory().getSelectedSlot();
        int best = chooseWeapon(client.player.getInventory(), target);
        if (best < 0) {
            restore(client);
            return;
        }

        if (best == current) {
            // If the user changed slots away from our active slot, stop owning the slot state.
            if (activeWeaponSlot >= 0 && current != activeWeaponSlot) resetState();
            return;
        }

        if (previousSlot < 0) previousSlot = current;
        activeWeaponSlot = best;
        client.player.getInventory().setSelectedSlot(best);
    }

    private int chooseWeapon(net.minecraft.world.entity.player.Inventory inventory, LivingEntity target) {
        int sword = -1;
        int axe = -1;
        int bestDurabilitySword = -1;
        int bestDurabilityAxe = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            int durability = stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
            if (stack.is(ItemTags.SWORDS) && durability > bestDurabilitySword) {
                sword = i;
                bestDurabilitySword = durability;
            }
            if (stack.getItem() instanceof AxeItem && durability > bestDurabilityAxe) {
                axe = i;
                bestDurabilityAxe = durability;
            }
        }

        if (target instanceof Player targetPlayer && targetPlayer.isBlocking() && axe >= 0) return axe;
        if (sword >= 0) return sword;
        return axe;
    }

    private void restore(Minecraft client) {
        if (client.player != null && previousSlot >= 0 && activeWeaponSlot >= 0
            && client.player.getInventory().getSelectedSlot() == activeWeaponSlot) {
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
        resetState();
    }

    private void resetState() {
        previousSlot = -1;
        activeWeaponSlot = -1;
    }
}
