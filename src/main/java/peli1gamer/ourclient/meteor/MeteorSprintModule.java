package peli1gamer.ourclient.meteor;

import net.minecraft.client.Minecraft;
import peli1gamer.ourclient.ArsonMeteorModule;
import peli1gamer.ourclient.ClientModuleManager;

/** Native Arson implementation of Meteor's Sprint concept. */
public final class MeteorSprintModule extends ArsonMeteorModule {
    private boolean onlyWhenMoving = true;

    public MeteorSprintModule() {
        super(ClientModuleManager.Category.MOVEMENT, "sprint", "Sprint", "Automatically keeps sprinting when movement allows it.");
        settings().toggle("only-when-moving", "Only When Moving", () -> onlyWhenMoving,
                () -> onlyWhenMoving = !onlyWhenMoving);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.screen != null || client.player.isSpectator()) return;
        if (onlyWhenMoving && !client.player.input.hasForwardImpulse() && !client.player.input.hasLeftImpulse()) {
            client.player.setSprinting(false);
            return;
        }
        client.player.setSprinting(true);
    }

    @Override
    public void onDisable(Minecraft client) {
        if (client.player != null) client.player.setSprinting(false);
    }
}
