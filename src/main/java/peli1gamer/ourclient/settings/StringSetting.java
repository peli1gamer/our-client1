package peli1gamer.ourclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StringSetting extends Setting<String> {
    private final int maxLength;
    private StringSetting(Builder builder) {
        super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged);
        maxLength = Math.max(1, builder.maxLength);
    }
    public int maxLength() { return maxLength; }
    @Override protected String sanitize(String value) {
        String result = value == null ? "" : value;
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }
    @Override public boolean parse(String text) { set(text); return true; }

    public static final class Builder {
        private String name, title, description = "", defaultValue = "";
        private int maxLength = 256;
        private Supplier<Boolean> visible;
        private Consumer<String> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(String v) { defaultValue = v; return this; }
        public Builder maxLength(int v) { maxLength = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<String> v) { onChanged = v; return this; }
        public StringSetting build() { return new StringSetting(this); }
    }
}
