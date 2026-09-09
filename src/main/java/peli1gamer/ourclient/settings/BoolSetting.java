package peli1gamer.ourclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BoolSetting extends Setting<Boolean> {
    private BoolSetting(Builder builder) {
        super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged);
    }

    @Override public String displayValue() { return get() ? "ON" : "OFF"; }
    @Override public boolean parse(String text) {
        if (text == null) return false;
        if (text.equalsIgnoreCase("toggle")) { set(!get()); return true; }
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("on")) { set(true); return true; }
        if (text.equalsIgnoreCase("false") || text.equalsIgnoreCase("off")) { set(false); return true; }
        return false;
    }

    public static final class Builder {
        private String name;
        private String title;
        private String description = "";
        private boolean defaultValue;
        private Supplier<Boolean> visible;
        private Consumer<Boolean> onChanged;
        public Builder name(String value) { name = value; return this; }
        public Builder title(String value) { title = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder defaultValue(boolean value) { defaultValue = value; return this; }
        public Builder visible(Supplier<Boolean> value) { visible = value; return this; }
        public Builder onChanged(Consumer<Boolean> value) { onChanged = value; return this; }
        public BoolSetting build() { return new BoolSetting(this); }
    }
}
