package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Native step-height adjustment without any external client dependency. */
public final class StepModule implements ToggleableModule {
    private static final float VANILLA_STEP_HEIGHT = 0.6f;

    private boolean enabled;
    private float height = 1.0f;
    private Field stepField;

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
            Field field = stepField;
            if (field == null || !field.getDeclaringClass().isAssignableFrom(client.player.getClass())) {
                field = findStepHeightField(client.player);
                stepField = field;
            }
            if (field != null) field.setFloat(client.player, target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            stepField = null;
        }
    }

    private static Field findStepHeightField(Object entity) throws ReflectiveOperationException {
        Class<?> type = entity.getClass();
        java.lang.reflect.Method getter;
        try {
            getter = type.getMethod("getStepHeight");
        } catch (NoSuchMethodException e) {
            return null;
        }
        float original = ((Number) getter.invoke(entity)).floatValue();

        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (field.getType() != float.class) continue;
                String name = field.getName();
                if (!name.equals("maxUpStep") && !name.equals("stepHeight") && !name.equals("field_6013")) continue;
                if (verifyStepField(entity, getter, field, original)) return field;
            }
        }

        // Last-resort compatibility path: verify the candidate by changing it and checking the getter.
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (field.getType() != float.class) continue;
                try {
                    field.setAccessible(true);
                    float previous = field.getFloat(entity);
                    if (Math.abs(previous - original) > 0.0001f) continue;
                    if (verifyStepField(entity, getter, field, original)) return field;
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // Continue scanning fields.
                }
            }
        }
        return null;
    }

    private static boolean verifyStepField(Object entity, java.lang.reflect.Method getter, Field field, float original)
            throws IllegalAccessException, ReflectiveOperationException {
        field.setAccessible(true);
        float previous = field.getFloat(entity);
        float probe = original + 0.1f;
        try {
            field.setFloat(entity, probe);
            float reported = ((Number) getter.invoke(entity)).floatValue();
            return Math.abs(reported - probe) < 0.0001f;
        } finally {
            // Never leave the player modified if reflection/getter invocation fails during probing.
            field.setFloat(entity, previous);
        }
    }

    public float height() { return height; }
    public void setHeight(float value) { height = Math.max(VANILLA_STEP_HEIGHT, Math.min(2.0f, value)); }

    @Override
    public ModuleSettings settings() {
        return new ModuleSettings().number("height", "Step height", () -> String.format(java.util.Locale.ROOT, "%.1f", height),
                () -> setHeight(height + 0.1f), () -> setHeight(height - 0.1f));
    }
}
