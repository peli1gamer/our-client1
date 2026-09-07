package peli1gamer.ourclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OurClient implements ClientModInitializer {
    public static final String MOD_ID = "our-client1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ClientModuleManager MODULES = new ClientModuleManager();
    private static final Map<String, KeyMapping> KEYBINDS = new LinkedHashMap<>();
    private static ClientConfig config;

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load(Minecraft.getInstance().gameDirectory.toPath().resolve("config/our-client1.json"));
        MODULES.registerDefaults();
        registerKey("toggle_aim", GLFW.GLFW_KEY_R);
        registerKey("toggle_tracers", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_freecam", GLFW.GLFW_KEY_F6);
        registerKey("toggle_schematic", GLFW.GLFW_KEY_F7);
        registerKey("save_base", GLFW.GLFW_KEY_V);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds(client);
            MODULES.tick(client);
        });
        LOGGER.info("Our Client initialized with {} modules", MODULES.size());
    }

    private static void registerKey(String id, int key) {
        KeyMapping mapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ourclient1." + id,
                key,
                KeyMapping.Category.MISC));
        KEYBINDS.put(id, mapping);
    }

    private static void handleKeybinds(Minecraft client) {
        while (KEYBINDS.get("toggle_aim").consumeClick()) toggle("aim-assist");
        while (KEYBINDS.get("toggle_tracers").consumeClick()) toggle("tracers");
        while (KEYBINDS.get("toggle_freecam").consumeClick()) toggle("freecam");
        while (KEYBINDS.get("toggle_schematic").consumeClick()) toggle("auto-schematic-builder");
        while (KEYBINDS.get("save_base").consumeClick()) {
            ClientModule module = MODULES.get("saved-bases");
            if (module instanceof SavedBasesModule bases) bases.saveCurrentBase(client, "base-" + System.currentTimeMillis());
        }
    }

    private static void toggle(String id) {
        ClientModule module = MODULES.get(id);
        if (module instanceof ToggleableModule toggleable) toggleable.setEnabled(!toggleable.enabled());
    }

    public static ClientModuleManager modules() { return MODULES; }
    public static ClientConfig config() { return config; }

    public static Path configPath(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config/our-client1.json");
    }
}
