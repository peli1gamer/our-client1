package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Holds the normal forward movement key while enabled without stealing user input permanently. */
public final class AutoWalkModule implements ToggleableModule {
    private boolean enabled;
    private boolean forcedForward;
    private boolean previousForward;

    @Override public String id() { return "auto-walk"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (enabled) {
            previousForward = client.options.keyUp.isDown();
            forcedForward = true;
        } else {
            restoreForward(client);
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        if (client.player == null || !client.player.isAlive() || client.screen != null
                || client.player.isSpectator() || client.player.isPassenger()) {
            restoreForward(client);
            return;
        }
        if (!forcedForward) {
            previousForward = client.options.keyUp.isDown();
            forcedForward = true;
        }
        client.options.keyUp.setDown(true);
    }

    private void restoreForward(Minecraft client) {
        if (!forcedForward) return;
        client.options.keyUp.setDown(previousForward);
        forcedForward = false;
    }
}
