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

/** Arson Client bootstrap and integration point. */
public final class OurClient implements ClientModInitializer {
    public static final String MOD_ID = "our-client1";
    public static final String CLIENT_NAME = "Arson Client";
    public static final String CLIENT_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);
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
        applyConfig(client);

        registerKey("toggle_aim", GLFW.GLFW_KEY_R);
        registerKey("toggle_trigger", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_crystal", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_attribute_swap", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_tracers", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_esp", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_xray", GLFW.GLFW_KEY_F8);
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
        LOGGER.info("{} {} initialized with {} modules", CLIENT_NAME, CLIENT_VERSION, MODULES.size());
    }

    private static void registerKey(String id, int key) {
        if (KEYBINDS.containsKey(id)) return;
        KeyMapping mapping = new KeyMapping("key.ourclient1." + id, key, KeyMapping.Category.MISC);
        KEYBINDS.put(id, KeyBindingHelper.registerKeyBinding(mapping));
    }

    private static void handleKeybinds(Minecraft client) {
        if (client.screen != null) { discardPendingKeybinds(); return; }
        while (KEYBINDS.get("open_clickgui").consumeClick()) client.setScreen(new OurClientClickGui());
        while (KEYBINDS.get("toggle_aim").consumeClick()) toggle("aim-assist", client);
        while (KEYBINDS.get("toggle_trigger").consumeClick()) toggle("trigger-bot", client);
        while (KEYBINDS.get("toggle_crystal").consumeClick()) toggle("crystal-macro", client);
        while (KEYBINDS.get("toggle_attribute_swap").consumeClick()) toggle("attribute-swap", client);
        while (KEYBINDS.get("toggle_tracers").consumeClick()) toggle("tracers", client);
        while (KEYBINDS.get("toggle_esp").consumeClick()) toggle("esp", client);
        while (KEYBINDS.get("toggle_xray").consumeClick()) toggle("xray", client);
        while (KEYBINDS.get("toggle_freecam").consumeClick()) toggle("freecam", client);
        while (KEYBINDS.get("toggle_schematic").consumeClick()) toggle("auto-schematic-builder", client);
        while (KEYBINDS.get("toggle_scaffold").consumeClick()) toggle("scaffold", client);
        while (KEYBINDS.get("save_base").consumeClick()) {
            ClientModule module = MODULES.get("saved-bases");
            if (module instanceof SavedBasesModule bases) {
                try { bases.saveCurrentBase(client, bases.nextAutomaticName()); }
                catch (RuntimeException exception) { LOGGER.error("Failed to save base from keybind", exception); }
            }
        }
    }

    private static void discardPendingKeybinds() { for (KeyMapping mapping : KEYBINDS.values()) while (mapping.consumeClick()) { } }

    private static void toggle(String id, Minecraft client) {
        MODULES.setEnabled(id, !(MODULES.get(id) instanceof ToggleableModule t && t.enabled()), client);
        syncAndSaveConfigFromModules();
    }

    private static void applyConfig(Minecraft client) {
        setEnabled("aim-assist", config.aimAssist, client);
        setEnabled("trigger-bot", config.triggerBot, client);
        setEnabled("crystal-macro", config.crystalMacro, client);
        setEnabled("attribute-swap", config.attributeSwap, client);
        setEnabled("tracers", config.tracers, client);
        setEnabled("esp", config.esp, client);
        setEnabled("xray", config.xray, client);
        setEnabled("freecam", false, client);
        setEnabled("auto-schematic-builder", config.schematicBuilder, client);
        setEnabled("saved-bases", config.savedBases, client);
        setEnabled("scaffold", config.scaffold, client);
    }

    private static void setEnabled(String id, boolean enabled, Minecraft client) { MODULES.setEnabled(id, enabled, client); }

    static void syncAndSaveConfigFromModules() {
        if (config == null) return;
        syncConfigFromModules();
        config.save(configPath(Minecraft.getInstance()));
    }

    private static void syncConfigFromModules() {
        config.aimAssist = enabled("aim-assist");
        config.triggerBot = enabled("trigger-bot");
        config.crystalMacro = enabled("crystal-macro");
        config.attributeSwap = enabled("attribute-swap");
        config.tracers = enabled("tracers");
        config.esp = enabled("esp");
        config.xray = enabled("xray");
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
