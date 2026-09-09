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
            // Existing implemented modules.
            register(new AimAssistModule());
            register(new TriggerBotModule());
            register(new CrystalMacroModule());
            register(new AttributeSwapModule());
            register(new TracersModule());
            register(new ESPModule());
            register(new XrayModule());
            register(new FreecamModule());
            register(new AutoSchematicBuilderModule());
            register(new SavedBasesModule());
            register(new ScaffoldModule());

            // Small, stable movement/render batch.
            register(new AutoWalkModule());
            register(new AutoJumpModule());
            register(new AirJumpModule());
            register(new SprintModule());
            register(new FullbrightModule());
            register(new HighJumpModule());
            register(new BunnyHopModule());

            // Expanded catalog remains isolated so unfinished features cannot
            // destabilize the already implemented modules.
            registerCatalog();
            defaultsRegistered = true;
        } catch (RuntimeException exception) {
            modules.clear();
            modules.putAll(previous);
            OurClient.LOGGER.error("Could not register all default client modules", exception);
            throw exception;
        }
    }

    private void registerCatalog() {
        String[] movement = {
            "auto-sprint", "speed", "step", "long-jump",
            "no-slow", "no-fall", "jesus", "spider", "fast-climb", "flight", "safe-walk"
        };
        String[] render = {
            "player-esp", "mob-esp", "item-esp", "chest-esp", "nametags", "storage-esp",
            "search-block", "overlay", "hitboxes", "trajectories", "damage-indicators"
        };
        String[] world = {
            "nuker", "fast-mine", "auto-mine", "auto-bridge", "auto-build", "tower",
            "bed-breaker", "chest-aura", "auto-farm", "auto-fish", "auto-tool", "liquid-interact"
        };
        String[] player = {
            "auto-eat", "auto-armor", "inventory-manager", "chest-stealer", "auto-drop",
            "auto-respawn", "auto-reconnect", "fast-use", "no-swing", "anti-afk", "inventory-move"
        };
        String[] utility = {
            "waypoints", "coordinates", "compass", "radar", "fps-counter", "cps-counter",
            "keystrokes", "ping-display", "server-info", "potion-effects", "armor-hud", "item-counter", "timer"
        };
        addCatalog(movement, CatalogModule.Category.MOVEMENT);
        addCatalog(render, CatalogModule.Category.RENDER);
        addCatalog(world, CatalogModule.Category.WORLD);
        addCatalog(player, CatalogModule.Category.PLAYER);
        addCatalog(utility, CatalogModule.Category.UTILITY);
    }

    private void addCatalog(String[] ids, CatalogModule.Category category) {
        for (String id : ids) register(new CatalogModule(id, category));
    }

    /**
     * Applies a module state through one guarded path so GUI, keybinds and
     * future integrations do not each need their own failure recovery.
     */
    public boolean setEnabled(String id, boolean enabled) {
        ClientModule module = modules.get(id);
        if (!(module instanceof ToggleableModule toggleable)) return false;
        try {
            toggleable.setEnabled(enabled);
            return toggleable.enabled() == enabled;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Failed to set client module '{}' to {}", id, enabled, exception);
            if (enabled) {
                try { toggleable.setEnabled(false); }
                catch (RuntimeException disableException) {
                    OurClient.LOGGER.error("Could not roll back failed client module '{}'", id, disableException);
                }
            }
            return false;
        }
    }

    public boolean toggle(String id) {
        ClientModule module = modules.get(id);
        if (!(module instanceof ToggleableModule toggleable)) return false;
        return setEnabled(id, !toggleable.enabled());
    }

    public void tick(Minecraft client) {
        if (client == null) return;
        for (Map.Entry<String, ClientModule> entry : List.copyOf(modules.entrySet())) {
            String id = entry.getKey();
            ClientModule module = entry.getValue();
            try {
                module.onClientTick(client);
            } catch (RuntimeException exception) {
                OurClient.LOGGER.error("Client module '{}' failed during tick", id, exception);
                if (module instanceof ToggleableModule) {
                    setEnabled(id, false);
                }
                try { OurClient.syncAndSaveConfigFromModules(); }
                catch (RuntimeException saveException) { OurClient.LOGGER.error("Could not persist failed client module '{}' state", id, saveException); }
            }
        }
    }

    public ClientModule get(String id) { return modules.get(id); }
    public Collection<ClientModule> all() { return List.copyOf(modules.values()); }
    public int size() { return modules.size(); }
}
