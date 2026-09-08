package peli1gamer.ourclient.meteor;

import peli1gamer.ourclient.ArsonMeteorModule;
import peli1gamer.ourclient.ClientModuleManager;
import net.minecraft.client.Minecraft;

/** Native Arson port of Meteor's AirJump concept. */
public final class MeteorAirJumpModule extends ArsonMeteorModule {
    private boolean maintainLevel;
    private boolean previousJump;
    private int level;

    public MeteorAirJumpModule() {
        super(ClientModuleManager.Category.MOVEMENT, "air-jump", "Air Jump", "Allows jumping while airborne.");
        settings().toggle("maintain-level", "Maintain Level", () -> maintainLevel, () -> maintainLevel = !maintainLevel);
    }

    @Override
    public void onEnable(Minecraft client) {
        previousJump = false;
        level = client.player == null ? 0 : client.player.blockPosition().getY();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.screen != null || client.player.onGround()) {
            previousJump = client.options.keyJump.isDown();
            return;
        }

        boolean jump = client.options.keyJump.isDown();
        if (jump && !previousJump) {
            level = client.player.blockPosition().getY();
            client.player.jumpFromGround();
        } else if (maintainLevel && client.player.blockPosition().getY() == level && jump) {
            client.player.jumpFromGround();
        }
        previousJump = jump;
    }
}
