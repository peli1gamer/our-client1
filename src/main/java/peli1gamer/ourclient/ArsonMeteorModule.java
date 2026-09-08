package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

/**
 * Base class for modules ported from Meteor's module concepts into Arson.
 *
 * The implementation deliberately uses Arson's lifecycle and settings API rather
 * than importing Meteor's runtime. This keeps ports native to Arson and lets the
 * ClickGUI, config system and fault isolation own module state.
 */
public abstract class ArsonMeteorModule implements ToggleableModule {
    private final String id;
    private final String title;
    private final String description;
    private final ClientModuleManager.Category category;
    private final ModuleSettings settings = new ModuleSettings();
    private boolean enabled;

    protected ArsonMeteorModule(ClientModuleManager.Category category, String id, String title, String description) {
        this.category = category;
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

    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (enabled) onEnable(client);
        else onDisable(client);
    }

    /** Alias matching Meteor's Module#isActive(). */
    public final boolean isActive() { return enabled; }

    /** Alias matching Meteor's Module#enable(). */
    public final void enable() { setEnabled(true); }

    /** Alias matching Meteor's Module#disable(). */
    public final void disable() { setEnabled(false); }

    /** Alias matching Meteor's Module#toggle(). */
    public final void toggle() { setEnabled(!enabled); }
}
