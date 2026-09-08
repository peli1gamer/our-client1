package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Automatically enables vanilla sprint while the player is moving. */
public final class AutoSprintModule implements ToggleableModule {
    private boolean enabled;
    private boolean allDirections;
    private boolean requireFood = true;

    @Override public String id() { return "auto-sprint"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stopSprinting();
    }

    @Override
    public void onClientTick(Minecraft client) {
        // Always clean up our state when the module cannot safely act. This prevents
        // sprint remaining stuck when opening a screen, dying, or leaving a world.
        if (!enabled) return;
        if (client.player == null || client.level == null || client.screen != null) {
            stopSprinting();
            return;
        }

        LocalPlayer player = client.player;
        if (!player.isAlive()) {
            stopSprinting();
            return;
        }

        boolean moving = allDirections
                ? player.xxa != 0.0F || player.zza != 0.0F
                : player.zza > 0.0F;
        boolean foodOk = !requireFood || player.getFoodData().getFoodLevel() > 6;
        boolean canSprint = moving && foodOk
                && !player.isCrouching()
                && !player.isPassenger()
                && !player.horizontalCollision
                && !player.isSpectator();
        player.setSprinting(canSprint);
    }

    private void stopSprinting() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.setSprinting(false);
    }

    @Override
    public ModuleSettings settings() {
        return new ModuleSettings()
                .toggle("all-directions", "All directions", () -> allDirections,
                        () -> allDirections = !allDirections)
                .toggle("require-food", "Require food", () -> requireFood,
                        () -> requireFood = !requireFood);
    }
}
