package peli1gamer.ourclient.meteor;

import peli1gamer.ourclient.ArsonMeteorModule;
import peli1gamer.ourclient.ClientModuleManager;
import net.minecraft.client.Minecraft;

/**
 * Native Arson port of Meteor's Fullbright gamma mode. The original has additional
 * luminance/potion integrations; Arson starts with the client-only gamma behavior
 * so it remains stable without Meteor mixins.
 */
public final class MeteorFullbrightModule extends ArsonMeteorModule {
    private double previousGamma;
    private double gamma = 16.0;

    public MeteorFullbrightModule() {
        super(ClientModuleManager.Category.RENDER, "fullbright", "Fullbright", "Lights up dark areas using the client's gamma setting.");
        settings().number("gamma", "Gamma", () -> String.format("%.1f", gamma),
            () -> gamma = Math.min(16.0, gamma + 0.5),
            () -> gamma = Math.max(1.0, gamma - 0.5));
    }

    @Override
    public void onEnable(Minecraft client) {
        previousGamma = client.options.gamma().get();
        client.options.gamma().set(gamma);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.options.gamma().get() != gamma) client.options.gamma().set(gamma);
    }

    @Override
    public void onDisable(Minecraft client) {
        client.options.gamma().set(previousGamma);
    }
}
