package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Raises the client gamma while enabled and restores the user's value on disable. */
public final class FullbrightModule implements ToggleableModule {
    private static final double FULLBRIGHT_GAMMA = 16.0D;

    private boolean enabled;
    private Double previousGamma;

    @Override public String id() { return "fullbright"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (enabled) {
            previousGamma = client.options.gamma().get();
            client.options.gamma().set(FULLBRIGHT_GAMMA);
        } else if (previousGamma != null) {
            // Only restore a value if Fullbright still owns the gamma setting.
            // This avoids overwriting a user/config change made after the last tick.
            if (client.options.gamma().get() >= FULLBRIGHT_GAMMA) {
                client.options.gamma().set(previousGamma);
            }
            previousGamma = null;
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (enabled && client.options.gamma().get() < FULLBRIGHT_GAMMA) {
            client.options.gamma().set(FULLBRIGHT_GAMMA);
        }
    }
}
