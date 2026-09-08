package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Increases vertical movement while using vanilla climbable blocks. */
public final class FastClimbModule implements ToggleableModule {
    private boolean enabled;
    private double multiplier = 1.5D;

    @Override public String id() { return "fast-climb"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.screen != null) return;
        LocalPlayer player = client.player;
        if (!player.onClimbable()) return;

        var velocity = player.getDeltaMovement();
        double y = velocity.y;
        if (y > 0.0D) y = Math.min(0.2873D, y * multiplier);
        else if (y < 0.0D) y = Math.max(-0.2873D, y * multiplier);
        if (y != velocity.y) player.setDeltaMovement(velocity.x, y, velocity.z);
    }

    @Override
    public ModuleSettings settings() {
        return new ModuleSettings().number(
                "multiplier", "Speed multiplier",
                () -> String.format(java.util.Locale.ROOT, "%.1fx", multiplier),
                () -> multiplier = Math.min(3.0D, multiplier + 0.1D),
                () -> multiplier = Math.max(1.0D, multiplier - 0.1D));
    }
}
