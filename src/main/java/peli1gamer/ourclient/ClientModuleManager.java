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
        if (module == null || module.id() == null || module.id().isBlank()) {
            throw new IllegalArgumentException("Module and module id must be non-null");
        }
        if (modules.containsKey(module.id())) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
        }
        modules.put(module.id(), module);
    }

    public void registerDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;

        register(new AimAssistModule());
        register(new TracersModule());
        register(new FreecamModule());
        register(new AutoSchematicBuilderModule());
        register(new SavedBasesModule());
    }

    public void tick(Minecraft client) {
        for (ClientModule module : List.copyOf(modules.values())) {
            try {
                module.onClientTick(client);
            } catch (RuntimeException exception) {
                OurClient.LOGGER.error("Client module '{}' failed during tick", module.id(), exception);
                if (module instanceof ToggleableModule toggleable) {
                    toggleable.setEnabled(false);
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
