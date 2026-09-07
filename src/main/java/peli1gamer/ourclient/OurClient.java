package peli1gamer.ourclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OurClient implements ClientModInitializer {
    public static final String MOD_ID = "our-client1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ClientModuleManager MODULES = new ClientModuleManager();

    @Override
    public void onInitializeClient() {
        MODULES.registerDefaults();

        ClientTickEvents.END_CLIENT_TICK.register(client -> MODULES.tick(client));
        LOGGER.info("Our Client initialized with {} modules", MODULES.size());
    }

    public static ClientModuleManager modules() {
        return MODULES;
    }
}
