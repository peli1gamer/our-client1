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
    private static boolean initialized;

    @Override
    public void onInitializeClient() {
        if (initialized) return;
        Minecraft client = Minecraft.getInstance();
        config = ClientConfig.load(configPath(client));
        MODULES.registerDefaults();
        applyConfig();

        registerKey("toggle_aim", GLFW.GLFW_KEY_R);
        registerKey("toggle_tracers", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_freecam", GLFW.GLFW_KEY_F6);
        registerKey("toggle_schematic", GLFW.GLFW_KEY_F7);
        registerKey("toggle_scaffold", GLFW.GLFW_KEY_G);
        registerKey("save_base", GLFW.GLFW_KEY_V);
        registerKey("open_clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT);

        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
            handleKeybinds(tickClient);
            MODULES.tick(tickClient);
        });
        initialized = true;
        LOGGER.info("Our Client initialized with {} modules", MODULES.size());
    }

    private static void registerKey(String id, int key) {
        if (KEYBINDS.containsKey(id)) return;
        KeyMapping mapping = new KeyMapping("key.ourclient1." + id, key, KeyMapping.Category.MISC);
        KEYBINDS.put(id, KeyBindingHelper.registerKeyBinding(mapping));
    }

    private static void handleKeybinds(Minecraft client) {
        if (client.screen != null) { discardPendingKeybinds(); return; }
        while (KEYBINDS.get("open_clickgui").consumeClick()) client.setScreen(new OurClientClickGui());
        while (KEYBINDS.get("toggle_aim").consumeClick()) toggle("aim-assist");
        while (KEYBINDS.get("toggle_tracers").consumeClick()) toggle("tracers");
        while (KEYBINDS.get("toggle_freecam").consumeClick()) toggle("freecam");
        while (KEYBINDS.get("toggle_schematic").consumeClick()) toggle("auto-schematic-builder");
        while (KEYBINDS.get("toggle_scaffold").consumeClick()) toggle("scaffold");
        while (KEYBINDS.get("save_base").consumeClick()) {
            ClientModule module = MODULES.get("saved-bases");
            if (module instanceof SavedBasesModule bases) {
                try { bases.saveCurrentBase(client, bases.nextAutomaticName()); }
                catch (RuntimeException exception) { LOGGER.error("Failed to save base from keybind", exception); }
            }
        }
    }

    private static void discardPendingKeybinds() {
        for (KeyMapping mapping : KEYBINDS.values()) while (mapping.consumeClick()) { }
    }

    private static void toggle(String id) {
        ClientModule module = MODULES.get(id);
        if (!(module instanceof ToggleableModule toggleable)) return;
        try {
            toggleable.setEnabled(!toggleable.enabled());
            syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to toggle client module '{}'", id, exception);
            try { toggleable.setEnabled(false); } catch (RuntimeException ignored) { }
            try { syncAndSaveConfigFromModules(); } catch (RuntimeException ignored) { }
        }
    }

    private static void applyConfig() {
        setEnabled("aim-assist", config.aimAssist);
        setEnabled("tracers", config.tracers);
        setEnabled("freecam", false);
        setEnabled("auto-schematic-builder", config.schematicBuilder);
        setEnabled("saved-bases", config.savedBases);
        setEnabled("scaffold", config.scaffold);
    }

    private static void setEnabled(String id, boolean enabled) {
        ClientModule module = MODULES.get(id);
        if (module instanceof ToggleableModule toggleable) toggleable.setEnabled(enabled);
    }

    static void syncAndSaveConfigFromModules() {
        if (config == null) return;
        syncConfigFromModules();
        config.save(configPath(Minecraft.getInstance()));
    }

    private static void syncConfigFromModules() {
        config.aimAssist = enabled("aim-assist");
        config.tracers = enabled("tracers");
        config.freecam = enabled("freecam");
        config.schematicBuilder = enabled("auto-schematic-builder");
        config.savedBases = enabled("saved-bases");
        config.scaffold = enabled("scaffold");
    }

    private static boolean enabled(String id) {
        ClientModule module = MODULES.get(id);
        return module instanceof ToggleableModule toggleable && toggleable.enabled();
    }

    public static ClientModuleManager modules() { return MODULES; }
    public static ClientConfig config() { return config; }
    public static Path configPath(Minecraft client) { return client.gameDirectory.toPath().resolve("config/our-client1.json"); }
}
