package peli1gamer.ourclient;

public final class AutoSchematicBuilderModule implements ToggleableModule {
    private boolean enabled;
    @Override public String id() { return "auto-schematic-builder"; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
