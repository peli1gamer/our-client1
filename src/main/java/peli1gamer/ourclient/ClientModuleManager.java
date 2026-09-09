package peli1gamer.ourclient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class ClientModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    private boolean defaultsRegistered;

    public void register(ClientModule module) {
        if (module == null) throw new IllegalArgumentException("Module must be non-null");
        String id = module.id();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Module id must be non-null and non-blank");
        if (modules.containsKey(id)) throw new IllegalArgumentException("Duplicate module id: " + id);
        modules.put(id, module);
    }

    public void registerDefaults() {
        if (defaultsRegistered) return;
        Map<String, ClientModule> previous = new LinkedHashMap<>(modules);
        try {
            register(new AimAssistModule()); register(new TriggerBotModule()); register(new CrystalMacroModule()); register(new AnchorMacroModule()); register(new AttributeSwapModule()); register(new AutoWeaponModule());
            register(new HoverTotemModule());
            register(new TracersModule()); register(new ESPModule()); register(new XrayModule()); register(new FreecamModule()); register(new AutoSchematicBuilderModule()); register(new SavedBasesModule()); register(new ScaffoldModule());
            register(new AutoWalkModule()); register(new AutoJumpModule()); register(new AirJumpModule()); register(new SprintModule()); register(new AutoSprintModule()); register(new FullbrightModule()); register(new HighJumpModule()); register(new BunnyHopModule()); register(new FastClimbModule()); register(new AntiVoidModule()); register(new NoFallModule()); register(new ElytraFlyModule()); register(new BlockESPModule());

            register(new RenderFeatureModule("player-esp", RenderFeatureModule.Feature.PLAYER_ESP));
            register(new RenderFeatureModule("mob-esp", RenderFeatureModule.Feature.MOB_ESP));
            register(new RenderFeatureModule("item-esp", RenderFeatureModule.Feature.ITEM_ESP));
            register(new RenderFeatureModule("storage-esp", RenderFeatureModule.Feature.STORAGE_ESP));
            register(new RenderFeatureModule("void-esp", RenderFeatureModule.Feature.VOID_ESP));
            register(new RenderFeatureModule("trail", RenderFeatureModule.Feature.TRAIL));

            // Extended native ESP coverage.
            register(new ExtendedESPModule("players-esp", ExtendedESPModule.Mode.PLAYERS));
            register(new ExtendedESPModule("mobs-esp", ExtendedESPModule.Mode.MOBS));
            register(new ExtendedESPModule("items-esp", ExtendedESPModule.Mode.ITEMS));
            register(new ExtendedESPModule("villager-esp", ExtendedESPModule.Mode.VILLAGERS));
            register(new ExtendedESPModule("pillager-esp", ExtendedESPModule.Mode.PILLAGERS));
            register(new ExtendedESPModule("wandering-esp", ExtendedESPModule.Mode.WANDERING_TRADERS));
            register(new ExtendedESPModule("amethyst-esp", ExtendedESPModule.Mode.AMETHYST));
            register(new ExtendedESPModule("beehive-esp", ExtendedESPModule.Mode.BEEHIVES));
            register(new ExtendedESPModule("deepslate-esp", ExtendedESPModule.Mode.DEEPSLATE));
            register(new ExtendedESPModule("dripstone-esp", ExtendedESPModule.Mode.DRIPSTONE));
            register(new ExtendedESPModule("kelp-esp", ExtendedESPModule.Mode.KELP));
            register(new ExtendedESPModule("vine-esp", ExtendedESPModule.Mode.VINES));
            register(new ExtendedESPModule("light-esp", ExtendedESPModule.Mode.LIGHT_BLOCKS));

            registerCatalog();
            defaultsRegistered = true;
        } catch (RuntimeException exception) {
            modules.clear(); modules.putAll(previous);
            OurClient.LOGGER.error("Could not register all default client modules", exception);
            throw exception;
        }
    }

    private void registerCatalog() {
        String[] movement = { "speed", "step", "long-jump", "no-slow", "jesus", "spider", "flight", "safe-walk" };
        String[] render = { "chest-esp", "nametags", "search-block", "overlay", "hitboxes", "trajectories", "damage-indicators", "better-tab", "better-tooltips", "block-selection", "blur", "boss-stack", "breadcrumbs", "break-indicators", "camera-tweaks", "chams", "city-esp", "entity-owner", "free-look", "hand-view", "hole-esp", "item-highlight", "item-physics", "light-overlay", "logout-spots", "no-render", "pop-chams", "time-changer", "tunnel-esp", "wall-hack", "weather-changer", "zoom" };
        String[] world = { "nuker", "fast-mine", "auto-mine", "auto-bridge", "auto-build", "tower", "bed-breaker", "chest-aura", "auto-farm", "auto-fish", "auto-tool", "liquid-interact" };
        String[] player = { "auto-eat", "auto-armor", "inventory-manager", "chest-stealer", "auto-drop", "auto-respawn", "auto-reconnect", "fast-use", "no-swing", "anti-afk", "inventory-move" };
        String[] utility = { "waypoints", "coordinates", "compass", "radar", "fps-counter", "cps-counter", "keystrokes", "ping-display", "server-info", "potion-effects", "armor-hud", "item-counter", "timer" };
        addCatalog(movement, CatalogModule.Category.MOVEMENT); addCatalog(render, CatalogModule.Category.RENDER); addCatalog(world, CatalogModule.Category.WORLD); addCatalog(player, CatalogModule.Category.PLAYER); addCatalog(utility, CatalogModule.Category.UTILITY);
    }
    private void addCatalog(String[] ids, CatalogModule.Category category) { for (String id : ids) register(new CatalogModule(id, category)); }

    public boolean setEnabled(String id, boolean enabled) {
        ClientModule module = modules.get(id);
        if (!(module instanceof ToggleableModule toggleable)) return false;
        if (toggleable.enabled() == enabled) return true;
        try {
            toggleable.setEnabled(enabled);
            if (toggleable.enabled() == enabled) return true;
            try { toggleable.setEnabled(!enabled); } catch (RuntimeException rollbackException) { OurClient.LOGGER.error("Could not roll back client module '{}' after incomplete state transition", id, rollbackException); }
            return false;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Failed to set client module '{}' to {}", id, enabled, exception);
            try { toggleable.setEnabled(!enabled); } catch (RuntimeException rollbackException) { OurClient.LOGGER.error("Could not roll back failed client module '{}' state", id, rollbackException); }
            return false;
        }
    }
    public boolean toggle(String id) { ClientModule module = modules.get(id); return module instanceof ToggleableModule toggleable && setEnabled(id, !toggleable.enabled()); }

    public void tick(Minecraft client) {
        if (client == null) return;
        for (Map.Entry<String, ClientModule> entry : List.copyOf(modules.entrySet())) {
            String id = entry.getKey(); ClientModule module = entry.getValue();
            try { module.onClientTick(client); }
            catch (RuntimeException exception) {
                OurClient.LOGGER.error("Client module '{}' failed during tick", id, exception);
                if (module instanceof ToggleableModule) setEnabled(id, false);
                try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException saveException) { OurClient.LOGGER.error("Could not persist failed client module '{}' state", id, saveException); }
            }
        }
    }
    public ClientModule get(String id) { return modules.get(id); }
    public Collection<ClientModule> all() { return List.copyOf(modules.values()); }
    public int size() { return modules.size(); }
}
