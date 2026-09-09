package peli1gamer.ourclient.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Generic multi-value setting with immutable snapshots exposed to callers. */
public final class ListSetting<T> extends Setting<List<T>> {
    private final List<T> allowed;
    private ListSetting(Builder<T> builder) {
        super(builder.name, builder.title, builder.description, List.copyOf(builder.defaultValue), builder.visible, builder.onChanged);
        allowed = List.copyOf(builder.allowed);
    }
    public List<T> allowed() { return allowed; }
    public void add(T value) { if (allowed.isEmpty() || allowed.contains(value)) { List<T> copy = new ArrayList<>(get()); if (!copy.contains(value)) { copy.add(value); set(copy); } } }
    public void remove(T value) { List<T> copy = new ArrayList<>(get()); if (copy.remove(value)) set(copy); }
    @Override protected List<T> sanitize(List<T> value) {
        if (value == null) return List.of();
        if (allowed.isEmpty()) return List.copyOf(value);
        return value.stream().filter(allowed::contains).distinct().toList();
    }
    @Override public String displayValue() { return get().toString(); }
    @Override public boolean parse(String text) { return false; }

    public static final class Builder<T> {
        private String name, title, description = "";
        private List<T> defaultValue = List.of(), allowed = List.of();
        private Supplier<Boolean> visible;
        private Consumer<List<T>> onChanged;
        public Builder<T> name(String v) { name = v; return this; }
        public Builder<T> title(String v) { title = v; return this; }
        public Builder<T> description(String v) { description = v; return this; }
        public Builder<T> defaultValue(Collection<T> v) { defaultValue = v == null ? List.of() : List.copyOf(v); return this; }
        public Builder<T> allowedValues(Collection<T> v) { allowed = v == null ? List.of() : List.copyOf(v); return this; }
        public Builder<T> visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder<T> onChanged(Consumer<List<T>> v) { onChanged = v; return this; }
        public ListSetting<T> build() { return new ListSetting<>(this); }
    }
}
