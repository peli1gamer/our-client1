package peli1gamer.ourclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Tracks the last living entity recorded by Minecraft as having damaged the player. */
public final class TargetTrackerModule implements ToggleableModule, ConfigurableModule {
    private static final int DEFAULT_TIMEOUT_TICKS = 100;
    private static boolean hudHookInstalled;

    private boolean enabled;
    private int timeoutTicks = DEFAULT_TIMEOUT_TICKS;
    private LivingEntity target;
    private int targetAge;

    public TargetTrackerModule() {
        installHudHook();
    }

    @Override public String id() { return "target-tracker"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) clearTarget();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) {
            clearTarget();
            return;
        }

        timeoutTicks = Math.max(1, Math.min(600, timeoutTicks));
        LivingEntity attacker = client.player.getLastHurtByMob();
        if (isValidAttacker(client, attacker)) {
            if (target != attacker) {
                target = attacker;
                targetAge = 0;
            } else {
                targetAge = 0;
            }
        } else if (target != null) {
            targetAge++;
            if (targetAge >= timeoutTicks || !isValidTarget(client, target)) clearTarget();
        }
    }

    @Override
    public ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings();
        settings.number("timeout", "Target timeout", () -> Integer.toString(timeoutTicks),
                () -> timeoutTicks = Math.min(600, timeoutTicks + 10),
                () -> timeoutTicks = Math.max(10, timeoutTicks - 10));
        return settings;
    }

    public LivingEntity target() { return target; }
    public int timeoutTicks() { return timeoutTicks; }
    public void setTimeoutTicks(int ticks) { timeoutTicks = Math.max(1, Math.min(600, ticks)); }
    public boolean hasTarget() { return target != null; }

    private void clearTarget() {
        target = null;
        targetAge = 0;
    }

    private static boolean isValidAttacker(Minecraft client, LivingEntity entity) {
        return entity != null && entity != client.player && entity.level() == client.level
                && entity.isAlive() && !entity.isSpectator();
    }

    private boolean isValidTarget(Minecraft client, Entity entity) {
        return entity instanceof LivingEntity living && isValidAttacker(client, living);
    }

    private void installHudHook() {
        if (hudHookInstalled) return;
        hudHookInstalled = true;
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            ClientModule module = OurClient.modules().get("target-tracker");
            if (module instanceof TargetTrackerModule tracker && tracker.enabled()) tracker.renderHud(graphics);
        });
    }

    private void renderHud(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || target == null) return;
        try {
            String name = target.getDisplayName().getString();
            String text = "Target: " + name;
            int x = 8;
            int y = 8;
            graphics.drawString(client.font, Component.literal(text), x, y, 0xFFFF8A00, true);
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Target tracker HUD render failed", exception);
        }
    }
}
