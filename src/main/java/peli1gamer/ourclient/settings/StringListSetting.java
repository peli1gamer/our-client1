package peli1gamer.ourclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StringListSetting extends Setting<List<String>> {
    private final int maxEntries;
    private StringListSetting(Builder builder) {
        super(builder.name, builder.title, builder.description, List.copyOf(builder.defaultValue), builder.visible, builder.onChanged);
        maxEntries = Math.max(1, builder.maxEntries);
    }
    public int maxEntries() { return maxEntries; }
    public void add(String value) { if (value != null && !value.isBlank()) { List<String> copy = new ArrayList<>(get()); if (!copy.contains(value) && copy.size() < maxEntries) { copy.add(value); set(copy); } } }
    public void remove(String value) { List<String> copy = new ArrayList<>(get()); if (copy.remove(value)) set(copy); }
    @Override protected List<String> sanitize(List<String> value) {
        if (value == null) return List.of();
        return value.stream().filter(v -> v != null && !v.isBlank()).distinct().limit(maxEntries).toList();
    }
    @Override public String displayValue() { return String.join(", ", get()); }
    @Override public boolean parse(String text) { set(Arrays.stream(text.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList()); return true; }
    public static final class Builder {
        private String name, title, description = "";
        private List<String> defaultValue = List.of();
        private int maxEntries = 64;
        private Supplier<Boolean> visible;
        private Consumer<List<String>> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(List<String> v) { defaultValue = v == null ? List.of() : List.copyOf(v); return this; }
        public Builder maxEntries(int v) { maxEntries = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<List<String>> v) { onChanged = v; return this; }
        public StringListSetting build() { return new StringListSetting(this); }
    }
}
