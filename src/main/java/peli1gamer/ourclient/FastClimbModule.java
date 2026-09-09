package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

/** Stable native fast-climb behavior. */
public final class FastClimbModule implements ToggleableModule {
    private double speed = 0.2872D;
    private boolean enabled;

    @Override public String id() { return "fast-climb"; }
    @Override public boolean enabled() { return enabled; }
    public double speed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.05D, Math.min(1.0D, speed)); }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null || !client.player.isAlive()) return;
        if (client.player.isFallFlying() || client.player.isSpectator() || client.player.isInWater() || client.player.isInLava()) return;
        if (!climbing(client)) return;

        Vec3 velocity = client.player.getDeltaMovement();
        double targetY = Math.max(velocity.y, speed);
        // Preserve existing upward motion and never turn a downward/idle input into
        // an aggressive launch when the player is not actually climbing.
        if (targetY > velocity.y) client.player.setDeltaMovement(velocity.x, targetY, velocity.z);
    }

    private boolean climbing(Minecraft client) {
        if (!client.player.horizontalCollision) return false;
        return client.player.onClimbable()
                || (client.player.getInBlockState().is(Blocks.POWDER_SNOW)
                && PowderSnowBlock.canEntityWalkOnPowderSnow(client.player));
    }
}
