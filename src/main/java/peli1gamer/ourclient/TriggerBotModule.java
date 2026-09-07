package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Attacks a valid entity when the player's crosshair is directly over it. */
public final class TriggerBotModule implements ToggleableModule, ConfigurableModule {
    private boolean enabled;
    private int delayTicks;
    private int cooldown;

    private final ModuleSettings settings = new ModuleSettings()
            .number("delay", "Attack Delay", () -> delayTicks + " ticks",
                    () -> delayTicks = Math.min(20, delayTicks + 1),
                    () -> delayTicks = Math.max(0, delayTicks - 1));

    @Override public String id() { return "trigger-bot"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public ModuleSettings settings() { return settings; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!(client.hitResult instanceof EntityHitResult hit)) return;

        Entity entity = hit.getEntity();
        LocalPlayer player = client.player;
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || entity == player) return;

        client.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND);
        cooldown = delayTicks;
    }
}
