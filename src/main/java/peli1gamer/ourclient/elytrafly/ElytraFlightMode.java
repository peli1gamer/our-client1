package peli1gamer.ourclient.elytrafly;

import net.minecraft.client.player.LocalPlayer;

/** Small native strategy contract for Elytra flight modes. */
public interface ElytraFlightMode {
    void apply(LocalPlayer player, double speed, double verticalSpeed);
}
