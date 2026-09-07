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

        try {
            register(new AimAssistModule());
            register(new TracersModule());
            register(new FreecamModule());
            register(new AutoSchematicBuilderModule());
            register(new SavedBasesModule());
            defaultsRegistered = true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Could not register all default client modules", exception);
            modules.clear();
            throw exception;
        }
    }

    public void tick(Minecraft client) {
        if (client == null) return;
        for (ClientModule module : List.copyOf(modules.values())) {
            try {
                module.onClientTick(client);
            } catch (RuntimeException exception) {
                OurClient.LOGGER.error("Client module '{}' failed during tick", module.id(), exception);
                try {
                    if (module instanceof ToggleableModule toggleable) toggleable.setEnabled(false);
                    OurClient.syncAndSaveConfigFromModules();
                } catch (RuntimeException disableException) {
                    OurClient.LOGGER.error("Could not safely disable failed client module '{}'", module.id(), disableException);
                }
            }
        }
    }

    public ClientModule get(String id) {
        return modules.get(id);
    }

    public Collection<ClientModule> all() {
        return List.copyOf(modules.values());
    }

    public int size() {
        return modules.size();
    }
}
