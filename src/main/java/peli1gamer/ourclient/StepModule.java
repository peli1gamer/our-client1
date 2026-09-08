package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Native step-height adjustment without any external client dependency. */
public final class StepModule implements ToggleableModule {
    private static final float VANILLA_STEP_HEIGHT = 0.6f;

    private boolean enabled;
    private float height = 1.0f;

    @Override public String id() { return "step"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) apply(client);
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null) return;
        apply(client);
    }

    private void apply(Minecraft client) {
        float target = enabled ? height : VANILLA_STEP_HEIGHT;
        try {
            Field field = findStepHeightField(client.player.getClass());
            if (field != null) {
                field.setAccessible(true);
                field.setFloat(client.player, target);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Mapping changes must never crash the client; Step simply becomes inert.
        }
    }

    private static Field findStepHeightField(Class<?> type) throws IllegalAccessException {
        float current = Float.NaN;
        try {
            current = ((Number) type.getMethod("getStepHeight").invoke(null)).floatValue();
        } catch (ReflectiveOperationException ignored) {
            // Fall through to field-name/type discovery.
        }

        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (field.getType() != float.class) continue;
                String name = field.getName();
                if (name.equals("maxUpStep") || name.equals("stepHeight") || name.equals("field_6013")) {
                    return field;
                }
                if (!Float.isNaN(current)) {
                    try {
                        field.setAccessible(true);
                        if (Math.abs(field.getFloat(type.cast(null)) - current) < 0.0001f) return field;
                    } catch (RuntimeException ignored) {
                        // Continue scanning fields.
                    }
                }
            }
        }
        return null;
    }

    public float height() { return height; }
    public void setHeight(float value) { height = Math.max(VANILLA_STEP_HEIGHT, Math.min(2.0f, value)); }

    @Override
    public ModuleSettings settings() {
        return new ModuleSettings().number("height", "Step height", () -> String.format(java.util.Locale.ROOT, "%.1f", height),
                () -> setHeight(height + 0.1f), () -> setHeight(height - 0.1f));
    }
}
