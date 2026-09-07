package peli1gamer.ourclient;

public interface ToggleableModule extends ClientModule {
    boolean enabled();
    void setEnabled(boolean enabled);
}
