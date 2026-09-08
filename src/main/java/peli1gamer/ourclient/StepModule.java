package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Native step-height adjustment without any external client dependency. */
public final class StepModule implements ToggleableModule {
    private boolean enabled;
    private float height = 1.0f;

    @Override public String id() { return "step"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) apply(client);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null) return;
        apply(client);
    }

    private void apply(Minecraft client) {
        client.player.setMaxUpStep(enabled ? height : 0.6f);
    }

    public float height() { return height; }
    public void setHeight(float value) { height = Math.max(0.6f, Math.min(2.0f, value)); }

    @Override
    public ModuleSettings settings() {
        return new ModuleSettings().number("height", "Step height", () -> String.format(java.util.Locale.ROOT, "%.1f", height),
                () -> setHeight(height + 0.1f), () -> setHeight(height - 0.1f));
    }
}
