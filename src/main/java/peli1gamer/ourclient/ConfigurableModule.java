package peli1gamer.ourclient;

import peli1gamer.ourclient.settings.Settings;

/** Optional module capability exposing typed native settings to the GUI and config system. */
public interface ConfigurableModule {
    Settings settings();
}
