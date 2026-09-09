package peli1gamer.ourclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Keyboard binding stored as a GLFW-style integer key code. */
public final class KeybindSetting extends Setting<Integer> {
    private KeybindSetting(Builder builder) { super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged); }
    @Override protected Integer sanitize(Integer value) { return value == null ? -1 : value; }
    @Override public String displayValue() { return get() < 0 ? "NONE" : Integer.toString(get()); }
    @Override public boolean parse(String text) {
        try { set(Integer.parseInt(text.trim())); return true; } catch (RuntimeException ignored) { return false; }
    }
    public static final class Builder {
        private String name, title, description = "";
        private int defaultValue = -1;
        private Supplier<Boolean> visible;
        private Consumer<Integer> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(int v) { defaultValue = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<Integer> v) { onChanged = v; return this; }
        public KeybindSetting build() { return new KeybindSetting(this); }
    }
}
