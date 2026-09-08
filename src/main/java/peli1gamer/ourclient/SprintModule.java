package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Keeps sprint active while the player is moving without permanently overriding user input. */
public final class SprintModule implements ToggleableModule {
    private boolean enabled;
    private boolean forcedSprint;
    private boolean previousSprint;

    @Override public String id() { return "sprint"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (enabled) {
            previousSprint = client.options.keySprint.isDown();
            forcedSprint = true;
        } else {
            restoreSprint(client);
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        if (client.player == null || !client.player.isAlive() || client.screen != null) {
            restoreSprint(client);
            return;
        }
        if (!forcedSprint) {
            previousSprint = client.options.keySprint.isDown();
            forcedSprint = true;
        }
        boolean moving = client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        client.options.keySprint.setDown(moving && !client.player.isShiftKeyDown());
    }

    private void restoreSprint(Minecraft client) {
        if (!forcedSprint) return;
        client.options.keySprint.setDown(previousSprint);
        forcedSprint = false;
    }
}
