package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

/** Stable native implementation inspired by Meteor's fast-climb behavior. */
public final class FastClimbModule implements ToggleableModule {
    private double speed = 0.2872D;
    private boolean enabled;

    @Override public String id() { return "fast-climb"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || !client.player.isAlive()) return;
        if (!climbing(client)) return;

        Vec3 velocity = client.player.getDeltaMovement();
        client.player.setDeltaMovement(velocity.x, speed, velocity.z);
    }

    private boolean climbing(Minecraft client) {
        return client.player.horizontalCollision
                && (client.player.onClimbable()
                || (client.player.getInBlockState().is(Blocks.POWDER_SNOW)
                && PowderSnowBlock.canEntityWalkOnPowderSnow(client.player)));
    }
}
