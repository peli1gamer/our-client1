package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Native Arson Elytra flight. Designed around vanilla fall-flying rather than Meteor internals. */
public final class ElytraFlyModule implements ToggleableModule {
    public enum Mode { VANILLA, BOUNCE, PITCH40, PACKET }

    private boolean enabled;
    private Mode mode = Mode.VANILLA;
    private double speed = 1.5D;
    private double verticalSpeed = 0.45D;
    private float pitch40 = 40.0F;

    @Override public String id() { return "elytra-fly"; }
    @Override public boolean enabled() { return enabled; }

    public Mode mode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode == null ? Mode.VANILLA : mode; }
    public double speed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.05D, Math.min(6.0D, speed)); }
    public double verticalSpeed() { return verticalSpeed; }
    public void setVerticalSpeed(double speed) { verticalSpeed = Math.max(0.05D, Math.min(3.0D, speed)); }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) this.enabled = false;
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !player.isAlive()) {
            enabled = false;
            return;
        }

        // Do not force the module on when the player is not wearing an Elytra.
        if (!player.isFallFlying()) {
            if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(net.minecraft.world.item.Items.ELYTRA)
                    && !player.onGround() && player.getDeltaMovement().y < 0.0D) {
                player.startFallFlying();
            } else {
                return;
            }
        }

        switch (mode) {
            case VANILLA -> applyVanilla(player);
            case BOUNCE -> applyBounce(player);
            case PITCH40 -> applyPitch40(player);
            case PACKET -> applyPacketSafe(player);
        }
    }

    private void applyVanilla(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-6D) horizontal = horizontal.normalize().scale(speed);
        double y = verticalInput(player) * verticalSpeed;
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
    }

    private void applyBounce(LocalPlayer player) {
        applyVanilla(player);
        if (player.getDeltaMovement().y < -0.35D) player.setDeltaMovement(player.getDeltaMovement().x, 0.35D, player.getDeltaMovement().z);
    }

    private void applyPitch40(LocalPlayer player) {
        player.setXRot(-pitch40);
        applyVanilla(player);
    }

    private void applyPacketSafe(LocalPlayer player) {
        // Packet mode deliberately stays client-side here. A native implementation must
        // not fabricate protocol packets or depend on Meteor's networking abstractions.
        applyVanilla(player);
    }

    private double verticalInput(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        return (client.options.keyJump.isDown() ? 1.0D : 0.0D)
                - (client.options.keyShift.isDown() ? 1.0D : 0.0D);
    }
}
