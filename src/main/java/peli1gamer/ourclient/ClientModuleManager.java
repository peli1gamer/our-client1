package peli1gamer.ourclient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

public final class ClientModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    private boolean defaultsRegistered;

    public void register(ClientModule module) {
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
        for (ClientModule module : modules.values()) {
            module.onClientTick(client);
        }
    }

    public ClientModule get(String id) {
        return modules.get(id);
    }

    public Collection<ClientModule> all() {
        return modules.values();
    }

    public int size() {
        return modules.size();
    }
}
