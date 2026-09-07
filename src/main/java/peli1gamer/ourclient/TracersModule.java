package peli1gamer.ourclient;

public final class TracersModule implements ToggleableModule {
    private boolean enabled;
    @Override public String id() { return "tracers"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
