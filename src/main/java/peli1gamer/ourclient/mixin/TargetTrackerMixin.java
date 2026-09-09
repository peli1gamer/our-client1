package peli1gamer.ourclient.mixin;

import peli1gamer.ourclient.ClientModule;
import peli1gamer.ourclient.OurClient;
import peli1gamer.ourclient.TargetTrackerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TargetTrackerMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void arson$targetTrackerCaptureDamage(DamageSource source, float amount, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || (Object) this != client.player) return;

        Entity attacker = source.getEntity();
        if (attacker == null || attacker == client.player) return;

        ClientModule module = OurClient.modules().get("target-tracker");
        if (module instanceof TargetTrackerModule tracker) tracker.recordDamage(attacker);
    }
}
