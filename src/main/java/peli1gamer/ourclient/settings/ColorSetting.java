package peli1gamer.ourclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** ARGB color setting. */
public final class ColorSetting extends Setting<Integer> {
    private ColorSetting(Builder builder) { super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged); }
    @Override protected Integer sanitize(Integer value) { return value == null ? 0xFFFFFFFF : value; }
    @Override public String displayValue() { return String.format("#%08X", get()); }
    @Override public boolean parse(String text) {
        if (text == null) return false;
        String value = text.trim().replace("#", "");
        try {
            long parsed = Long.parseLong(value, 16);
            if (value.length() <= 6) parsed |= 0xFF000000L;
            if (parsed > 0xFFFFFFFFL) return false;
            set((int) parsed); return true;
        } catch (RuntimeException ignored) { return false; }
    }
    public static final class Builder {
        private String name, title, description = "";
        private int defaultValue = 0xFFFFFFFF;
        private Supplier<Boolean> visible;
        private Consumer<Integer> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(int v) { defaultValue = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<Integer> v) { onChanged = v; return this; }
        public ColorSetting build() { return new ColorSetting(this); }
    }
}
