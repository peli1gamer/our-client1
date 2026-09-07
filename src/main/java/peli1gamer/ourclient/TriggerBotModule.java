package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Attacks a living entity only while the attack key is held and the crosshair is on it. */
public final class TriggerBotModule implements ToggleableModule {
    private boolean enabled;
    private int cooldown;
    private int attackDelay = 4;

    @Override public String id() { return "trigger-bot"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) cooldown = 0;
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;
        if (!mc.player.isAlive() || !mc.options.keyAttack.isDown()) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive() || living.isRemoved()) return;
        if (mc.player.distanceTo(living) > 6.0D) return;

        mc.gameMode.attack(mc.player, living);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        cooldown = Math.max(1, attackDelay);
    }

    public int attackDelay() { return attackDelay; }
    public void setAttackDelay(int ticks) { attackDelay = Math.max(1, Math.min(20, ticks)); }
}
