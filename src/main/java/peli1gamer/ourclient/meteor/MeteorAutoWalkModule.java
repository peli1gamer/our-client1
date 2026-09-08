package peli1gamer.ourclient.meteor;

import peli1gamer.ourclient.ArsonMeteorModule;
import peli1gamer.ourclient.ClientModuleManager;

import net.minecraft.client.Minecraft;

/** Native Arson port of Meteor's AutoWalk concept. */
public final class MeteorAutoWalkModule extends ArsonMeteorModule {
    private enum Direction { FORWARDS, BACKWARDS, LEFT, RIGHT }

    private Direction direction = Direction.FORWARDS;

    public MeteorAutoWalkModule() {
        super(ClientModuleManager.Category.MOVEMENT, "auto-walk", "Auto Walk", "Automatically holds a movement direction.");
        settings().choice("direction", "Direction", () -> direction.name(),
                () -> direction = Direction.values()[(direction.ordinal() + 1) % Direction.values().length],
                () -> direction = Direction.values()[(direction.ordinal() + Direction.values().length - 1) % Direction.values().length]);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.screen != null) return;
        switch (direction) {
            case FORWARDS -> client.options.keyUp.setDown(true);
            case BACKWARDS -> client.options.keyDown.setDown(true);
            case LEFT -> client.options.keyLeft.setDown(true);
            case RIGHT -> client.options.keyRight.setDown(true);
        }
    }

    @Override
    public void onDisable(Minecraft client) {
        if (client.player == null) return;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
    }
}
