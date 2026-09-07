package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Temporarily selects the strongest standard sword or axe in the hotbar. */
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
        if (preferred >= 0 && preferred != mc.player.getInventory().getSelectedSlot()) mc.player.getInventory().setSelectedSlot(preferred);
    }

    private int findBestWeapon(LocalPlayer player) {
        int best = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            int score = weaponScore(stack);
            if (score > bestScore) { bestScore = score; best = score >= 0 ? slot : -1; }
        }
        return best;
    }

    private int weaponScore(ItemStack stack) {
        if (stack.is(Items.NETHERITE_AXE)) return 100;
        if (stack.is(Items.DIAMOND_AXE)) return 90;
        if (stack.is(Items.IRON_AXE)) return 80;
        if (stack.is(Items.STONE_AXE)) return 70;
        if (stack.is(Items.GOLDEN_AXE)) return 60;
        if (stack.is(Items.WOODEN_AXE)) return 50;
        if (stack.is(Items.NETHERITE_SWORD)) return 95;
        if (stack.is(Items.DIAMOND_SWORD)) return 85;
        if (stack.is(Items.IRON_SWORD)) return 75;
        if (stack.is(Items.STONE_SWORD)) return 65;
        if (stack.is(Items.GOLDEN_SWORD)) return 55;
        if (stack.is(Items.WOODEN_SWORD)) return 45;
        return -1;
    }

    private void restore(LocalPlayer player) {
        if (player != null && previousSlot >= 0 && previousSlot < 9) player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }
}
