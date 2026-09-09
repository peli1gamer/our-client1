package peli1gamer.ourclient;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Tracks the last valid entity recorded as having damaged the local player. */
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
        if (target == null) return;

        if (!isValidTarget(client, target)) {
            clearTarget();
            return;
        }

        targetAge++;
        if (targetAge >= timeoutTicks) clearTarget();
    }

    /** Called by the damage-source mixin when the local player is actually hurt. */
    public void recordDamage(Entity attacker) {
        if (!enabled || attacker == null) return;
        Minecraft client = Minecraft.getInstance();
        if (!isValidAttacker(client, attacker)) return;
        target = (LivingEntity) attacker;
        targetAge = 0;
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

    private static boolean isValidAttacker(Minecraft client, Entity entity) {
        return client.player != null && client.level != null
                && entity instanceof LivingEntity living
                && entity != client.player
                && entity.level() == client.level
                && living.isAlive()
                && !living.isSpectator();
    }

    private boolean isValidTarget(Minecraft client, Entity entity) {
        return isValidAttacker(client, entity);
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
            String text = "Target: " + target.getDisplayName().getString();
            graphics.drawString(client.font, Component.literal(text), 8, 8, 0xFFFF8A00, true);
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Target tracker HUD render failed", exception);
        }
    }
}
