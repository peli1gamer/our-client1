package peli1gamer.ourclient.meteor;

import peli1gamer.ourclient.ArsonMeteorModule;
import peli1gamer.ourclient.ClientModuleManager;
import net.minecraft.client.Minecraft;

/** Native Arson port of Meteor's AutoJump concept. */
public final class MeteorAutoJumpModule extends ArsonMeteorModule {
    private enum JumpWhen { SPRINTING, WALKING, ALWAYS }
    private enum Mode { JUMP, LOW_HOP }

    private JumpWhen jumpWhen = JumpWhen.ALWAYS;
    private Mode mode = Mode.JUMP;
    private double velocityHeight = 0.25;

    public MeteorAutoJumpModule() {
        super(ClientModuleManager.Category.MOVEMENT, "auto-jump", "Auto Jump", "Automatically jumps while the player is moving.");
        settings()
            .choice("mode", "Mode", () -> mode.name(),
                () -> mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length],
                () -> mode = Mode.values()[(mode.ordinal() + Mode.values().length - 1) % Mode.values().length])
            .choice("jump-when", "Jump When", () -> jumpWhen.name(),
                () -> jumpWhen = JumpWhen.values()[(jumpWhen.ordinal() + 1) % JumpWhen.values().length],
                () -> jumpWhen = JumpWhen.values()[(jumpWhen.ordinal() + JumpWhen.values().length - 1) % JumpWhen.values().length])
            .number("velocity-height", "Velocity Height", () -> String.format("%.2f", velocityHeight),
                () -> velocityHeight = Math.min(2.0, velocityHeight + 0.05),
                () -> velocityHeight = Math.max(0.0, velocityHeight - 0.05));
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.player.isShiftKeyDown() || !client.player.onGround()) return;

        boolean moving = client.player.zza != 0 || client.player.xxa != 0;
        if (jumpWhen == JumpWhen.SPRINTING && !(client.player.isSprinting() && moving)) return;
        if (jumpWhen == JumpWhen.WALKING && !moving) return;

        if (mode == Mode.JUMP) {
            client.player.jumpFromGround();
        } else {
            var velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, velocityHeight, velocity.z);
        }
    }
}
