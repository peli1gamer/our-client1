package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Native Arson Elytra flight using safe client-side movement controls. */
public final class ElytraFlyModule implements ToggleableModule {
    public enum Mode { VANILLA, BOUNCE, PITCH40, PACKET }

    private boolean enabled;
    private Mode mode = Mode.VANILLA;
    private double speed = 1.5D;
    private double verticalSpeed = 0.45D;
    private float previousPitch;
    private boolean pitchForced;

    @Override public String id() { return "elytra-fly"; }
    @Override public boolean enabled() { return enabled; }
    public Mode mode() { return mode; }
    public void setMode(Mode mode) {
        if (this.mode == Mode.PITCH40 && mode != Mode.PITCH40) restorePitch();
        this.mode = mode == null ? Mode.VANILLA : mode;
    }
    public double speed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.05D, Math.min(6.0D, speed)); }
    public double verticalSpeed() { return verticalSpeed; }
    public void setVerticalSpeed(double speed) { verticalSpeed = Math.max(0.05D, Math.min(3.0D, speed)); }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) { restorePitch(); this.enabled = false; return; }
        // Configuration is applied during client startup before a world/player exists.
        // Keep the requested state until a player is available instead of silently losing it.
        this.enabled = true;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        if (!player.isAlive() || player.isSpectator()) { setEnabled(false); return; }
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) { setEnabled(false); return; }
        if (client.screen != null) return;

        if (!player.isFallFlying()) {
            if (!player.onGround() && player.getDeltaMovement().y < 0.0D) player.startFallFlying();
            else return;
        }

        if (mode == Mode.PITCH40) {
            if (!pitchForced) { previousPitch = player.getXRot(); pitchForced = true; }
            player.setXRot(-40.0F);
        } else restorePitch();

        switch (mode) {
            case VANILLA, PITCH40, PACKET -> applyFlight(player);
            case BOUNCE -> applyBounce(player);
        }
    }

    private void applyFlight(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        double x = 0.0D, z = 0.0D;
        if (client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown()) {
            if (horizontal.lengthSqr() > 1.0E-6D) { horizontal = horizontal.normalize().scale(speed); x = horizontal.x; z = horizontal.z; }
        }
        double y = verticalInput(client) * verticalSpeed;
        player.setDeltaMovement(x, y, z);
    }

    private void applyBounce(LocalPlayer player) {
        applyFlight(player);
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < -0.35D) player.setDeltaMovement(velocity.x, 0.35D, velocity.z);
    }

    private double verticalInput(Minecraft client) {
        return (client.options.keyJump.isDown() ? 1.0D : 0.0D)
                - (client.options.keyShift.isDown() ? 1.0D : 0.0D);
    }

    private void restorePitch() {
        if (!pitchForced) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.setXRot(previousPitch);
        pitchForced = false;
    }
}
