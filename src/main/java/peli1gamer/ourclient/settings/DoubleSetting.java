package peli1gamer.ourclient.settings;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DoubleSetting extends Setting<Double> {
    private final double min, max, step;
    private DoubleSetting(Builder builder) {
        super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged);
        min = Math.min(builder.min, builder.max); max = Math.max(builder.min, builder.max); step = Math.max(0.000001D, builder.step);
    }
    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
    public void increment() { set(get() + step); }
    public void decrement() { set(get() - step); }
    @Override protected Double sanitize(Double value) { double v = value == null || !Double.isFinite(value) ? min : value; return Math.max(min, Math.min(max, v)); }
    @Override public String displayValue() { return String.format(Locale.ROOT, "%.3f", get()).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    @Override public boolean parse(String text) { try { set(Double.parseDouble(text.trim())); return true; } catch (RuntimeException ignored) { return false; } }

    public static final class Builder {
        private String name, title, description = "";
        private double defaultValue, min = 0, max = 100, step = 0.1;
        private Supplier<Boolean> visible;
        private Consumer<Double> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(double v) { defaultValue = v; return this; }
        public Builder.range(double min, double max) { this.min = Math.min(min, max); this.max = Math.max(min, max); return this; }
        public Builder min(double v) { min = v; if (max < min) max = min; return this; }
        public Builder max(double v) { max = v; if (min > max) min = max; return this; }
        public Builder sliderRange(double min, double max) { return range(min, max); }
        public Builder sliderMin(double v) { return min(v); }
        public Builder sliderMax(double v) { return max(v); }
        public Builder step(double v) { step = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<Double> v) { onChanged = v; return this; }
        public DoubleSetting build() { defaultValue = Math.max(min, Math.min(max, defaultValue)); return new DoubleSetting(this); }
    }
}
