package peli1gamer.ourclient;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;

/** Central registry and fault boundary for client modules. */
public final class ClientModuleManager {
    public enum Category {
        COMBAT("Combat"),
        RENDER("Render"),
        MOVEMENT("Movement"),
        WORLD("World"),
        PLAYER("Player"),
        MISC("Misc");

        private final String title;

        Category(String title) { this.title = title; }
        public String title() { return title; }
    }

    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private boolean defaultsRegistered;

    public void register(ClientModule module) {
        register(Category.MISC, module, module == null ? "" : module.description());
    }

    public void register(Category category, ClientModule module, String description) {
        if (module == null) throw new IllegalArgumentException("Module must be non-null");
        String id = module.id();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Module id must be non-null and non-blank");
        if (modules.containsKey(id)) throw new IllegalArgumentException("Duplicate module id: " + id);
        modules.put(id, module);
        categories.put(id, category == null ? Category.MISC : category);
        descriptions.put(id, description == null || description.isBlank() ? module.description() : description);
    }

    public void registerDefaults() {
        if (defaultsRegistered) return;
        Map<String, ClientModule> previous = new LinkedHashMap<>(modules);
        Map<String, Category> previousCategories = new LinkedHashMap<>(categories);
        Map<String, String> previousDescriptions = new LinkedHashMap<>(descriptions);
        try {
            register(Category.COMBAT, new AimAssistModule(), "Smoothly assists your aim toward nearby living entities.");
            register(Category.COMBAT, new TriggerBotModule(), "Attacks a living entity when it is directly under your crosshair.");
            register(Category.COMBAT, new CrystalMacroModule(), "Places and breaks end crystals from the current crosshair target.");
            register(Category.COMBAT, new AttributeSwapModule(), "Selects the strongest sword or axe in your hotbar when attacking.");

            register(Category.RENDER, new TracersModule(), "Draws lines from the camera to nearby players.");
            register(Category.RENDER, new ESPModule(), "Draws boxes around nearby players.");
            register(Category.RENDER, new XrayModule(), "Highlights nearby valuable ores without changing world blocks.");

            register(Category.MOVEMENT, new FreecamModule(), "Moves a client-side camera independently from the player.");

            register(Category.WORLD, new AutoSchematicBuilderModule(), "Places blocks from the configured JSON schematic file.");
            register(Category.WORLD, new SavedBasesModule(), "Saves and restores named base locations.");
            register(Category.WORLD, new ScaffoldModule(), "Places available building blocks beneath the player.");
            defaultsRegistered = true;
        } catch (RuntimeException exception) {
            modules.clear();
            modules.putAll(previous);
            categories.clear();
            categories.putAll(previousCategories);
            descriptions.clear();
            descriptions.putAll(previousDescriptions);
            OurClient.LOGGER.error("Could not register all default client modules", exception);
            throw exception;
        }
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
                if (module instanceof ToggleableModule toggleable) {
                    try { toggleable.setEnabled(false); }
                    catch (RuntimeException disableException) {
                        OurClient.LOGGER.error("Could not disable failed client module '{}'", id, disableException);
                    }
                }
                try { OurClient.syncAndSaveConfigFromModules(); }
                catch (RuntimeException saveException) {
                    OurClient.LOGGER.error("Could not persist failed client module '{}' state", id, saveException);
                }
            }
        }
    }

    public ClientModule get(String id) { return modules.get(id); }
    public Collection<ClientModule> all() { return List.copyOf(modules.values()); }
    public Category categoryOf(String id) { return categories.getOrDefault(id, Category.MISC); }
    public String descriptionOf(String id) { return descriptions.getOrDefault(id, "No description available."); }
    public List<ClientModule> inCategory(Category category) {
        return modules.values().stream().filter(module -> categoryOf(module.id()) == category).toList();
    }
    public Map<Category, Integer> counts() {
        Map<Category, Integer> result = new EnumMap<>(Category.class);
        for (Category category : Category.values()) result.put(category, 0);
        for (ClientModule module : modules.values()) result.compute(categoryOf(module.id()), (k, value) -> value + 1);
        return Map.copyOf(result);
    }
    public int size() { return modules.size(); }
}
