package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Native void protection: detects empty space below the player before the world floor. */
public final class AntiVoidModule implements ToggleableModule {
    public enum Mode { JUMP, STOP }
    private boolean enabled;
    private Mode mode = Mode.JUMP;
    private boolean triggered;
    private int scanDepth = 6;

    @Override public String id() { return "anti-void"; }
    @Override public boolean enabled() { return enabled; }
    public Mode mode() { return mode; }
    public void cycleMode() { mode = mode == Mode.JUMP ? Mode.STOP : Mode.JUMP; }
    public int scanDepth() { return scanDepth; }
    public void setScanDepth(int depth) { scanDepth = Math.max(2, Math.min(12, depth)); }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; if (!enabled) triggered = false; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !client.player.isAlive() || client.player.isSpectator() || client.player.onGround()) {
            triggered = false;
            return;
        }

        double fallSpeed = client.player.getDeltaMovement().y;
        if (fallSpeed >= 0.0D || !voidBelow(client)) { triggered = false; return; }
        if (triggered) return;
        triggered = true;

        var velocity = client.player.getDeltaMovement();
        if (mode == Mode.STOP) {
            client.player.setDeltaMovement(0.0D, Math.max(0.0D, velocity.y), 0.0D);
        } else {
            client.player.setDeltaMovement(velocity.x, 0.42D, velocity.z);
        }
    }

    private boolean voidBelow(Minecraft client) {
        BlockPos base = client.player.blockPosition();
        int minY = client.level.getMinY();
        for (int depth = 1; depth <= scanDepth; depth++) {
            int y = base.getY() - depth;
            if (y < minY) return true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = base.offset(dx, -depth, dz);
                BlockState state = client.level.getBlockState(pos);
                if (!state.isAir() && state.isCollisionShapeFullBlock(client.level, pos)) return false;
            }
        }
        return true;
    }
}
