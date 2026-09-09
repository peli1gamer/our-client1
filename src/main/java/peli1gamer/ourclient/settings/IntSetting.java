package peli1gamer.ourclient.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class IntSetting extends Setting<Integer> {
    private final int min, max, step;
    private IntSetting(Builder builder) {
        super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged);
        min = builder.min; max = builder.max; step = Math.max(1, builder.step);
    }
    public int min() { return min; }
    public int max() { return max; }
    public int step() { return step; }
    public void increment() { set(get() + step); }
    public void decrement() { set(get() - step); }
    @Override protected Integer sanitize(Integer value) { int v = value == null ? min : value; return Math.max(min, Math.min(max, v)); }
    @Override public boolean parse(String text) { try { set(Integer.parseInt(text.trim())); return true; } catch (RuntimeException ignored) { return false; } }

    public static final class Builder {
        private String name, title, description = "";
        private int defaultValue, min = 0, max = 100, step = 1;
        private Supplier<Boolean> visible;
        private Consumer<Integer> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(int v) { defaultValue = v; return this; }
        public Builder range(int min, int max) { this.min = Math.min(min, max); this.max = Math.max(min, max); return this; }
        public Builder min(int v) { min = v; if (max < min) max = min; return this; }
        public Builder max(int v) { max = v; if (min > max) min = max; return this; }
        public Builder sliderRange(int min, int max) { return range(min, max); }
        public Builder sliderMin(int v) { return min(v); }
        public Builder sliderMax(int v) { return max(v); }
        public Builder step(int v) { step = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<Integer> v) { onChanged = v; return this; }
        public IntSetting build() { defaultValue = Math.max(min, Math.min(max, defaultValue)); return new IntSetting(this); }
    }
}
