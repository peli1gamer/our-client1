package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/** Selects a useful weapon from the hotbar for the current combat target. */
public final class AutoWeaponModule implements ToggleableModule {
    private boolean enabled;
    private int previousSlot = -1;

    @Override public String id() { return "auto-weapon"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (!enabled) restore(client);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        if (!(client.crosshairTarget instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target) || !target.isAlive() || target == client.player) return;

        int current = client.player.getInventory().getSelectedSlot();
        int best = chooseWeapon(client.player.getInventory().getItems(), target);
        if (best < 0) return;

        if (previousSlot < 0 && current != best) previousSlot = current;
        client.player.getInventory().setSelectedSlot(best);
    }

    private int chooseWeapon(java.util.List<ItemStack> hotbar, LivingEntity target) {
        int sword = -1;
        int axe = -1;
        int bestDurabilitySword = -1;
        int bestDurabilityAxe = -1;

        for (int i = 0; i < 9 && i < hotbar.size(); i++) {
            ItemStack stack = hotbar.get(i);
            if (stack.isEmpty()) continue;
            int durability = stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
            if (stack.getItem() instanceof SwordItem && durability > bestDurabilitySword) {
                sword = i;
                bestDurabilitySword = durability;
            }
            if (stack.getItem() instanceof AxeItem && durability > bestDurabilityAxe) {
                axe = i;
                bestDurabilityAxe = durability;
            }
        }

        // Axes are preferred while a player is actively blocking; otherwise use a sword.
        if (target instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isBlocking() && axe >= 0) return axe;
        if (sword >= 0) return sword;
        return axe;
    }

    private void restore(Minecraft client) {
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            int current = client.player.getInventory().getSelectedSlot();
            if (current == previousSlot || current >= 0) client.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }
}
