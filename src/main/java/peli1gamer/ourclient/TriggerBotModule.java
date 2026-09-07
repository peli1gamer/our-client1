package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Attacks a living entity currently under the crosshair while attack is held. */
public final class TriggerBotModule implements ToggleableModule {
    private boolean enabled;
    private int cooldown;

    @Override public String id() { return "trigger-bot"; }
    @Override public boolean enabled() { return enabled; }

    @Override public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) cooldown = 0;
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null || !mc.player.isAlive()) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target) || !target.isAlive() || target == mc.player) return;
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        cooldown = 1;
    }
}
