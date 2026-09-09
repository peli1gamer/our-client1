package peli1gamer.ourclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OurClient implements ClientModInitializer {
    public static final String MOD_ID = "our-client1";
    public static final Logger LOGGER = LoggerFactory.getLogger("Arson Client");
    private static final ClientModuleManager MODULES = new ClientModuleManager();
    private static final Map<String, KeyMapping> KEYBINDS = new LinkedHashMap<>();
    private static ClientConfig config;
    private static boolean initialized;

    @Override public void onInitializeClient() {
        if (initialized) return;
        Minecraft client = Minecraft.getInstance(); config = ClientConfig.load(configPath(client)); MODULES.registerDefaults(); applyConfig(); syncAndSaveConfigFromModules();
        registerKey("toggle_aim", GLFW.GLFW_KEY_R); registerKey("toggle_trigger", GLFW.GLFW_KEY_UNKNOWN); registerKey("toggle_crystal", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_attribute_swap", GLFW.GLFW_KEY_UNKNOWN); registerKey("toggle_tracers", GLFW.GLFW_KEY_UNKNOWN); registerKey("toggle_esp", GLFW.GLFW_KEY_UNKNOWN);
        registerKey("toggle_xray", GLFW.GLFW_KEY_F8); registerKey("toggle_freecam", GLFW.GLFW_KEY_F6); registerKey("toggle_schematic", GLFW.GLFW_KEY_F7);
        registerKey("toggle_scaffold", GLFW.GLFW_KEY_G); registerKey("save_base", GLFW.GLFW_KEY_V); registerKey("open_clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ScreenEvents.AFTER_INIT.register((minecraft, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) return;
            try { int width = 120, x = scaledWidth - width - 10, y = scaledHeight - 32; Screens.getButtons(screen).add(Button.builder(Component.literal("Arson Client"), b -> minecraft.setScreen(new OurClientClickGui())).bounds(x, y, width, 20).build()); }
            catch (RuntimeException exception) { LOGGER.error("Could not add Arson Client button to title screen", exception); }
        });
        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> { handleKeybinds(tickClient); MODULES.tick(tickClient); });
        initialized = true; LOGGER.info("Arson Client initialized with {} modules", MODULES.size());
    }

    private static void registerKey(String id, int key) { if (!KEYBINDS.containsKey(id)) KEYBINDS.put(id, KeyBindingHelper.registerKeyBinding(new KeyMapping("key.ourclient1." + id, key, KeyMapping.Category.MISC))); }
    private static void handleKeybinds(Minecraft client) {
        if (client.screen != null) { discardPendingKeybinds(); return; }
        while (KEYBINDS.get("open_clickgui").consumeClick()) { client.setScreen(new OurClientClickGui()); discardPendingKeybinds(); return; }
        while (KEYBINDS.get("toggle_aim").consumeClick()) toggle("aim-assist"); while (KEYBINDS.get("toggle_trigger").consumeClick()) toggle("trigger-bot");
        while (KEYBINDS.get("toggle_crystal").consumeClick()) toggle("crystal-macro"); while (KEYBINDS.get("toggle_attribute_swap").consumeClick()) toggle("attribute-swap");
        while (KEYBINDS.get("toggle_tracers").consumeClick()) toggle("tracers"); while (KEYBINDS.get("toggle_esp").consumeClick()) toggle("esp"); while (KEYBINDS.get("toggle_xray").consumeClick()) toggle("xray");
        while (KEYBINDS.get("toggle_freecam").consumeClick()) toggle("freecam"); while (KEYBINDS.get("toggle_schematic").consumeClick()) toggle("auto-schematic-builder"); while (KEYBINDS.get("toggle_scaffold").consumeClick()) toggle("scaffold");
        while (KEYBINDS.get("save_base").consumeClick()) { ClientModule module = MODULES.get("saved-bases"); if (module instanceof SavedBasesModule bases) try { bases.saveCurrentBase(client, bases.nextAutomaticName()); } catch (RuntimeException exception) { LOGGER.error("Failed to save base from keybind", exception); } }
    }
    private static void discardPendingKeybinds() { for (KeyMapping mapping : KEYBINDS.values()) while (mapping.consumeClick()) { } }
    private static void toggle(String id) { if (MODULES.toggle(id)) syncAndSaveConfigFromModules(); }

    private static void applyConfig() {
        setEnabled("aim-assist", config.aimAssist); setEnabled("trigger-bot", config.triggerBot); setEnabled("crystal-macro", config.crystalMacro); setEnabled("attribute-swap", config.attributeSwap); setEnabled("auto-weapon", config.autoWeapon);
        setEnabled("tracers", config.tracers); setEnabled("esp", config.esp); setEnabled("xray", config.xray); setEnabled("freecam", false); setEnabled("auto-schematic-builder", config.schematicBuilder); setEnabled("saved-bases", config.savedBases); setEnabled("scaffold", config.scaffold);
        setEnabled("auto-walk", config.autoWalk); setEnabled("auto-jump", config.autoJump); setEnabled("air-jump", config.airJump); setEnabled("sprint", config.sprint); setEnabled("auto-sprint", config.autoSprint); setEnabled("fullbright", config.fullbright); setEnabled("high-jump", config.highJump); setEnabled("bunny-hop", config.bunnyHop);
        setEnabled("fast-climb", config.fastClimb); setEnabled("anti-void", config.antiVoid); setEnabled("no-fall", config.noFall); setEnabled("elytra-fly", config.elytraFly); setEnabled("block-esp", config.blockEsp);
    }
    private static void setEnabled(String id, boolean enabled) { MODULES.setEnabled(id, enabled); }
    static void syncAndSaveConfigFromModules() { if (config == null) return; syncConfigFromModules(); config.save(configPath(Minecraft.getInstance())); }
    private static void syncConfigFromModules() {
        config.aimAssist = enabled("aim-assist"); config.triggerBot = enabled("trigger-bot"); config.crystalMacro = enabled("crystal-macro"); config.attributeSwap = enabled("attribute-swap"); config.autoWeapon = enabled("auto-weapon"); config.tracers = enabled("tracers"); config.esp = enabled("esp"); config.xray = enabled("xray");
        config.freecam = enabled("freecam"); config.schematicBuilder = enabled("auto-schematic-builder"); config.savedBases = enabled("saved-bases"); config.scaffold = enabled("scaffold"); config.autoWalk = enabled("auto-walk"); config.autoJump = enabled("auto-jump"); config.airJump = enabled("air-jump"); config.sprint = enabled("sprint"); config.autoSprint = enabled("auto-sprint"); config.fullbright = enabled("fullbright"); config.highJump = enabled("high-jump"); config.bunnyHop = enabled("bunny-hop");
        config.fastClimb = enabled("fast-climb"); config.antiVoid = enabled("anti-void"); config.noFall = enabled("no-fall"); config.elytraFly = enabled("elytra-fly"); config.blockEsp = enabled("block-esp");
    }
    private static boolean enabled(String id) { ClientModule module = MODULES.get(id); return module instanceof ToggleableModule toggleable && toggleable.enabled(); }
    public static ClientModuleManager modules() { return MODULES; } public static ClientConfig config() { return config; } public static Path configPath(Minecraft client) { return client.gameDirectory.toPath().resolve("config/our-client1.json"); }
}
