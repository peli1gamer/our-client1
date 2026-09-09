package peli1gamer.ourclient.settings;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EnumSetting<T extends Enum<T>> extends Setting<T> {
    private final List<T> values;
    private EnumSetting(Builder<T> builder) {
        super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged);
        values = List.copyOf(Arrays.asList(builder.values));
        if (values.isEmpty()) throw new IllegalArgumentException("Enum setting needs at least one value");
    }
    public List<T> values() { return values; }
    public void next() { set(values.get((values.indexOf(get()) + 1 + values.size()) % values.size())); }
    public void previous() { set(values.get((values.indexOf(get()) - 1 + values.size()) % values.size())); }
    @Override protected T sanitize(T value) { return values.contains(value) ? value : values.get(0); }
    @Override public boolean parse(String text) {
        if (text == null) return false;
        for (T value : values) if (value.name().equalsIgnoreCase(text.trim())) { set(value); return true; }
        return false;
    }
    @Override public String displayValue() { return get().name(); }

    public static final class Builder<T extends Enum<T>> {
        private String name, title, description = "";
        private T defaultValue;
        private T[] values;
        private Supplier<Boolean> visible;
        private Consumer<T> onChanged;
        public Builder<T> name(String v) { name = v; return this; }
        public Builder<T> title(String v) { title = v; return this; }
        public Builder<T> description(String v) { description = v; return this; }
        public Builder<T> defaultValue(T v) { defaultValue = v; return this; }
        public Builder<T> values(T[] v) { values = v; return this; }
        public Builder<T> visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder<T> onChanged(Consumer<T> v) { onChanged = v; return this; }
        public EnumSetting<T> build() {
            if (values == null || values.length == 0) throw new IllegalArgumentException("Enum setting needs values");
            if (defaultValue == null) defaultValue = values[0];
            return new EnumSetting<>(this);
        }
    }
}
