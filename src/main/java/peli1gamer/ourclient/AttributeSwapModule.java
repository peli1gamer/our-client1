package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/** Temporarily selects the most suitable melee item for the entity under the crosshair. */
public final class AttributeSwapModule implements ToggleableModule {
    private boolean enabled;
    private boolean onlyWhenAttacking = true;
    private int previousSlot = -1;

    @Override public String id() { return "attribute-swap"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft mc = Minecraft.getInstance();
        if (enabled) previousSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
        else restore(mc.player);
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.screen != null || !mc.player.isAlive()) return;
        if (onlyWhenAttacking && !mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;

        int preferred = findBestWeapon(mc.player);
        if (preferred >= 0 && preferred != mc.player.getInventory().getSelectedSlot())
            mc.player.getInventory().setSelectedSlot(preferred);
    }

    private int findBestWeapon(LocalPlayer player) {
        int best = -1;
        float bestDamage = -1.0f;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof SwordItem) && !(stack.getItem() instanceof AxeItem)) continue;
            float damage = stack.getItem() instanceof SwordItem ? 4.0f : 5.0f;
            if (damage > bestDamage) { bestDamage = damage; best = slot; }
        }
        return best;
    }

    private void restore(LocalPlayer player) {
        if (player != null && previousSlot >= 0 && previousSlot < 9) player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }
}
