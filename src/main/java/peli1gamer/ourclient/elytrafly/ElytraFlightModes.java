package peli1gamer.ourclient.elytrafly;

/** Central registry for the native Elytra mode implementations. */
public final class ElytraFlightModes {
    private ElytraFlightModes() {}

    public static final ElytraFlightMode VANILLA = new Vanilla();
    public static final ElytraFlightMode BOUNCE = new Bounce();
    public static final ElytraFlightMode PITCH40 = new Pitch40();
    public static final ElytraFlightMode PACKET = new Packet();
}
