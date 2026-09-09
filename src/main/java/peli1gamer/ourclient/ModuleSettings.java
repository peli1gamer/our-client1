package peli1gamer.ourclient;

import peli1gamer.ourclient.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Compatibility facade for older Arson modules, backed by the native settings architecture. */
public class ModuleSettings extends Settings {
    public enum Type { TOGGLE, NUMBER, CHOICE }
    public record Entry(String id, String label, Type type, Supplier<String> value,
                        Runnable increment, Runnable decrement, BooleanSupplier visibility) {
        public boolean visible() { return visibility == null || visibility.getAsBoolean(); }
    }
    private final List<Entry> entries = new ArrayList<>();

    public ModuleSettings toggle(String id, String label, Supplier<Boolean> getter, Runnable toggle) {
        entries.add(new Entry(id, label, Type.TOGGLE, () -> getter.get() ? "ON" : "OFF", toggle, toggle, null)); return this;
    }
    public ModuleSettings number(String id, String label, Supplier<String> getter, Runnable increment, Runnable decrement) {
        entries.add(new Entry(id, label, Type.NUMBER, getter, increment, decrement, null)); return this;
    }
    public ModuleSettings choice(String id, String label, Supplier<String> getter, Runnable next, Runnable previous) {
        entries.add(new Entry(id, label, Type.CHOICE, getter, next, previous, null)); return this;
    }
    public ModuleSettings conditionalToggle(String id, String label, Supplier<Boolean> getter, Runnable toggle, BooleanSupplier visible) {
        entries.add(new Entry(id, label, Type.TOGGLE, () -> getter.get() ? "ON" : "OFF", toggle, toggle, visible)); return this;
    }
    public ModuleSettings conditionalNumber(String id, String label, Supplier<String> getter, Runnable increment, Runnable decrement, BooleanSupplier visible) {
        entries.add(new Entry(id, label, Type.NUMBER, getter, increment, decrement, visible)); return this;
    }
    public ModuleSettings conditionalChoice(String id, String label, Supplier<String> getter, Runnable next, Runnable previous, BooleanSupplier visible) {
        entries.add(new Entry(id, label, Type.CHOICE, getter, next, previous, visible)); return this;
    }
    public List<Entry> entries() { return List.copyOf(entries); }
}
