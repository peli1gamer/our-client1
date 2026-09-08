package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Increases normal jump height without changing horizontal movement. */
public final class HighJumpModule implements ToggleableModule {
    private boolean enabled;
    private boolean previousJump;
    private LocalPlayer activePlayer;

    @Override public String id() { return "high-jump"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        previousJump = enabled && client.options != null && client.options.keyJump.isDown();
        activePlayer = enabled ? client.player : null;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.options == null) return;

        boolean jump = client.options.keyJump.isDown();
        if (client.screen != null || client.player == null) {
            previousJump = jump;
            return;
        }
        if (activePlayer != null && activePlayer != client.player) {
            activePlayer = client.player;
            previousJump = jump;
            return;
        }
        if (!client.player.isAlive() || client.player.isSpectator() || client.player.isPassenger()) {
            previousJump = jump;
            return;
        }

        if (jump && !previousJump && client.player.onGround()) {
            Vec3 velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, 0.75D, velocity.z);
        }
        previousJump = jump;
    }
}
