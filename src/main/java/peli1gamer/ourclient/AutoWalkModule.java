package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Holds the normal forward movement key while enabled. */
public final class AutoWalkModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "auto-walk"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) Minecraft.getInstance().options.keyUp.setDown(false);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.screen != null) return;
        client.options.keyUp.setDown(true);
    }
}
