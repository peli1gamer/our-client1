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
            // Combat
            register(new AimAssistModule());

            // Visual
            register(new TracersModule());
            register(new ESPModule());

            // Movement
            register(new FreecamModule());

            // World
            register(new AutoSchematicBuilderModule());
            register(new SavedBasesModule());
            register(new ScaffoldModule());
            defaultsRegistered = true;
        } catch (RuntimeException exception) {
            modules.clear();
            modules.putAll(previous);
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
                    catch (RuntimeException disableException) { OurClient.LOGGER.error("Could not disable failed client module '{}'", id, disableException); }
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
