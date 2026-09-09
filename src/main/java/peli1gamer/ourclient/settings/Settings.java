package peli1gamer.ourclient.settings;

import java.util.ArrayList;
import java.util.List;

/** Collection of setting groups owned by one module or feature. */
public class Settings implements Iterable<SettingGroup> {
    private final List<SettingGroup> groups = new ArrayList<>();
    private SettingGroup defaultGroup;

    public SettingGroup createGroup(String name) {
        SettingGroup group = new SettingGroup(name);
        groups.add(group);
        if (defaultGroup == null) defaultGroup = group;
        return group;
    }

    public SettingGroup getDefaultGroup() {
        if (defaultGroup == null) defaultGroup = createGroup("General");
        return defaultGroup;
    }
    public List<SettingGroup> groups() { return List.copyOf(groups); }
    public Setting<?> get(String groupName, String settingName) {
        for (SettingGroup group : groups) if (group.name().equals(groupName)) return group.get(settingName);
        return null;
    }
    public Setting<?> get(String settingName) {
        for (SettingGroup group : groups) {
            Setting<?> setting = group.get(settingName);
            if (setting != null) return setting;
        }
        return null;
    }
    public List<Setting<?>> visibleSettings() {
        List<Setting<?>> result = new ArrayList<>();
        for (SettingGroup group : groups) if (group.expanded()) for (Setting<?> setting : group.all()) if (setting.visible()) result.add(setting);
        return List.copyOf(result);
    }
    @Override public java.util.Iterator<SettingGroup> iterator() { return groups().iterator(); }
}
