package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class SpiderModule implements ToggleableModule {
    private boolean enabled;
    @Override public String id() { return "spider"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        if (client.player.horizontalCollision && !client.player.isShiftKeyDown()) {
            Vec3 v = client.player.getDeltaMovement();
            client.player.setDeltaMovement(v.x, Math.max(v.y, 0.42D), v.z);
        }
    }
}
