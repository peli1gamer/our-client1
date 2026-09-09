package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Native void protection: detects empty space below the player's footprint. */
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
            client.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        } else {
            client.player.setDeltaMovement(velocity.x, 0.42D, velocity.z);
        }
    }

    private boolean voidBelow(Minecraft client) {
        AABB box = client.player.getBoundingBox();
        int minX = (int) Math.floor(box.minX + 1.0E-4D);
        int maxX = (int) Math.floor(box.maxX - 1.0E-4D);
        int minZ = (int) Math.floor(box.minZ + 1.0E-4D);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-4D);
        int baseY = (int) Math.floor(box.minY - 1.0E-4D);
        int minY = client.level.getMinY();

        for (int depth = 1; depth <= scanDepth; depth++) {
            int y = baseY - depth + 1;
            if (y < minY) return true;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (!state.isAir() && !state.getCollisionShape(client.level, pos).isEmpty()) return false;
                }
            }
        }
        return true;
    }
}
