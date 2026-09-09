package peli1gamer.ourclient.settings;

import java.util.ArrayList;
import java.util.List;

/** A named, collapsible group of settings. */
public final class SettingGroup {
    private final String name;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean expanded = true;

    public SettingGroup(String name) {
        this.name = name == null || name.isBlank() ? "General" : name;
    }

    public String name() { return name; }
    public boolean expanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public <S extends Setting<?>> S add(S setting) {
        if (setting == null) throw new IllegalArgumentException("Setting must not be null");
        if (settings.stream().anyMatch(existing -> existing.name().equals(setting.name()))) {
            throw new IllegalArgumentException("Duplicate setting: " + setting.name());
        }
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> all() { return List.copyOf(settings); }
    public Setting<?> get(String name) {
        if (name == null) return null;
        for (Setting<?> setting : settings) if (setting.name().equals(name)) return setting;
        return null;
    }
}
