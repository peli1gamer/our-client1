package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Keeps sprint active while the player is moving normally. */
public final class SprintModule implements ToggleableModule {
    private boolean enabled;

    @Override public String id() { return "sprint"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) Minecraft.getInstance().options.keySprint.setDown(false);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.screen != null) return;
        boolean moving = client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        client.options.keySprint.setDown(moving && !client.player.isShiftKeyDown());
    }
}
