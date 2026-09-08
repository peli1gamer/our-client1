package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.lang.reflect.Field;

/** Native step-height adjustment without any external client dependency. */
public final class StepModule implements ToggleableModule {
    private static final float VANILLA_STEP_HEIGHT = 0.6f;
    private static final Field STEP_HEIGHT_FIELD = findStepHeightField();

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
        try {
            STEP_HEIGHT_FIELD.setFloat(client.player, enabled ? height : VANILLA_STEP_HEIGHT);
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Keep the module non-fatal if a future mapping changes the backing field.
        }
    }

    private static Field findStepHeightField() {
        try {
            Field field = Entity.class.getDeclaredField("field_6013");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException first) {
            try {
                Field field = Entity.class.getDeclaredField("maxUpStep");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException second) {
                throw new ExceptionInInitializerError(second);
            }
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
