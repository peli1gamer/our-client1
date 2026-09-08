package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/**
 * Native Arson base for modules adapted from Meteor functionality.
 * Meteor is the behavioral reference; Arson owns the runtime, settings, GUI and config.
 */
public abstract class ArsonMeteorModule implements ToggleableModule {
    private final String id;
    private final String title;
    private final String description;
    private final ClientModuleManager.Category category;
    private final ModuleSettings settings = new ModuleSettings();
    private boolean enabled;

    protected ArsonMeteorModule(ClientModuleManager.Category category, String id, String title, String description) {
        this.category = category == null ? ClientModuleManager.Category.MISC : category;
        this.id = id;
        this.title = title;
        this.description = description;
    }

    @Override public final String id() { return id; }
    @Override public final String title() { return title; }
    @Override public final String description() { return description; }
    @Override public final ClientModuleManager.Category category() { return category; }
    @Override public final ModuleSettings settings() { return settings; }
    @Override public final boolean enabled() { return enabled; }

    /** Single lifecycle boundary used by the manager, GUI and keybinds. */
    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        try {
            if (enabled) onEnable(client);
            else onDisable(client);
        } catch (RuntimeException exception) {
            this.enabled = false;
            throw exception;
        }
    }

    /** Meteor-compatible convenience aliases. */
    public final boolean isActive() { return enabled; }
    public final void enable() { setEnabled(true); }
    public final void disable() { setEnabled(false); }
    public final void toggle() { setEnabled(!enabled); }

    /** Safe default for ports that need the client each tick. */
    protected final Minecraft client() { return Minecraft.getInstance(); }
}
