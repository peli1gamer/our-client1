package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Keeps the world bright without relying on a gamma value that Minecraft may clamp. */
public final class FullbrightModule implements ToggleableModule {
    private static final double FULLBRIGHT_GAMMA = 16.0D;

    private boolean enabled;
    private Double previousGamma;
    private boolean addedNightVision;

    @Override public String id() { return "fullbright"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        Minecraft client = Minecraft.getInstance();
        this.enabled = enabled;
        if (enabled) {
            previousGamma = client.options.gamma().get();
            client.options.gamma().set(FULLBRIGHT_GAMMA);
            ensureNightVision(client);
        } else {
            restoreGamma(client);
            removeOwnedNightVision(client);
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return;

        // Modern Minecraft can clamp the gamma option, so use night vision as a
        // reliable fallback while still preserving the user's gamma setting.
        if (client.options.gamma().get() < FULLBRIGHT_GAMMA) {
            client.options.gamma().set(FULLBRIGHT_GAMMA);
        }
        ensureNightVision(client);
    }

    private void ensureNightVision(Minecraft client) {
        if (client.player == null) return;
        var effect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value());
        if (!client.player.hasEffect(effect)) {
            client.player.addEffect(new MobEffectInstance(effect, 420, 0, false, false, false));
            addedNightVision = true;
        }
    }

    private void removeOwnedNightVision(Minecraft client) {
        if (!addedNightVision || client.player == null) {
            addedNightVision = false;
            return;
        }
        var effect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value());
        // Only remove it if it is still the effect we supplied. If another source
        // replaced/reapplied it, leave that effect alone.
        MobEffectInstance instance = client.player.getEffect(effect);
        if (instance != null && instance.getAmplifier() == 0) client.player.removeEffect(effect);
        addedNightVision = false;
    }

    private void restoreGamma(Minecraft client) {
        if (previousGamma == null) return;
        if (Double.compare(client.options.gamma().get(), FULLBRIGHT_GAMMA) == 0) {
            client.options.gamma().set(previousGamma);
        }
        previousGamma = null;
    }
}
