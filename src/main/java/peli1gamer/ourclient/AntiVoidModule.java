package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/** Attempts to recover from an imminent void fall without adding a dependency on Meteor. */
public final class AntiVoidModule implements ToggleableModule {
    public enum Mode { JUMP, STOP }

    private boolean enabled;
    private Mode mode = Mode.JUMP;
    private boolean triggered;

    @Override public String id() { return "anti-void"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) triggered = false;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !client.player.isAlive() || client.player.isSpectator()) return;

        int minY = client.level.getMinY();
        double y = client.player.getY();

        // Only intervene in the narrow band immediately below the world's floor.
        if (y > minY || y < minY - 12) {
            triggered = false;
            return;
        }

        if (triggered) return;
        triggered = true;

        if (mode == Mode.JUMP && client.player.onGround()) {
            client.player.jumpFromGround();
        } else if (mode == Mode.STOP) {
            var velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
        }
    }

    public Mode mode() { return mode; }
    public void cycleMode() { mode = mode == Mode.JUMP ? Mode.STOP : Mode.JUMP; }
}
