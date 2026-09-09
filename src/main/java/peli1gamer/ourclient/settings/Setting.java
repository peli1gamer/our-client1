package peli1gamer.ourclient.settings;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Native Arson setting primitive. Independent of Meteor. */
public abstract class Setting<T> {
    private final String name;
    private final String title;
    private final String description;
    private final T defaultValue;
    private final Supplier<Boolean> visible;
    private final Consumer<T> onChanged;
    private T value;

    protected Setting(String name, String title, String description, T defaultValue,
                      Supplier<Boolean> visible, Consumer<T> onChanged) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Setting name must not be blank");
        this.name = name;
        this.title = title == null || title.isBlank() ? name : title;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visible = visible == null ? () -> true : visible;
        this.onChanged = onChanged;
    }

    public final String name() { return name; }
    public final String title() { return title; }
    public final String description() { return description; }
    public final T get() { return value; }
    public final T defaultValue() { return defaultValue; }
    public final boolean visible() { try { return visible.get(); } catch (RuntimeException ignored) { return false; } }

    public final boolean set(T newValue) {
        T sanitized;
        try { sanitized = sanitize(newValue); }
        catch (RuntimeException exception) { return false; }
        if (Objects.equals(value, sanitized)) return false;
        T previous = value;
        value = sanitized;
        if (onChanged != null) {
            try { onChanged.accept(value); }
            catch (RuntimeException exception) { value = previous; throw exception; }
        }
        return true;
    }

    public final void reset() { set(defaultValue); }
    protected T sanitize(T value) { return value; }
    public String displayValue() { return String.valueOf(value); }
    public boolean parse(String text) { return false; }
}
