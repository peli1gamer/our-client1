package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class JesusModule implements ToggleableModule {
    private boolean enabled;
    @Override public String id() { return "jesus"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        if (client.player.isInWater() && !client.player.isShiftKeyDown() && !client.player.isSwimming()) {
            Vec3 v = client.player.getDeltaMovement();
            if (v.y < 0.0D) client.player.setDeltaMovement(v.x, 0.0D, v.z);
            if (client.player.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > 0.85D && v.y <= 0.05D) {
                client.player.setDeltaMovement(v.x, 0.03D, v.z);
            }
        }
    }
}
