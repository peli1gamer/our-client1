package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Automatically attacks a valid living entity under the crosshair when the module is enabled. */
public final class TriggerBotModule implements ToggleableModule {
    private static final float MIN_ATTACK_STRENGTH = 0.9f;

    private boolean enabled;
    private int cooldown;

    @Override public String id() { return "trigger-bot"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        cooldown = 0;
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null
                || mc.screen != null || !mc.player.isAlive()) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        if (target == mc.player || !target.isAlive() || target.isSpectator()) return;
        if (!mc.player.hasLineOfSight(target)) return;
        if (mc.player.getAttackStrengthScale(0.0f) < MIN_ATTACK_STRENGTH) return;

        mc.gameMode.attack(mc.player, target);
        mc.player.resetAttackStrengthTicker();
        mc.player.swing(InteractionHand.MAIN_HAND);
        cooldown = 1;
    }
}
