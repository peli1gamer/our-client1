package peli1gamer.ourclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Stable, self-contained Arson Click GUI.
 *
 * The layout deliberately avoids Minecraft's focus-navigation machinery and
 * keeps all scrolling state local to this screen. This makes the GUI safe to
 * open from both the title screen and an in-game screen.
 */
public final class OurClientClickGui extends Screen {
    private static final int BG = 0xF20A0A0D;
    private static final int PANEL = 0xF2181820;
    private static final int PANEL_2 = 0xF21D1D27;
    private static final int ROW = 0xFF22222C;
    private static final int HOVER = 0xFF2B2B37;
    private static final int SELECTED = 0xFF343440;
    private static final int ACCENT = 0xFFFF6A00;
    private static final int TEXT = 0xFFECE8F5;
    private static final int MUTED = 0xFFAAA5B7;
    private static final int OFF = 0xFF666272;

    private static final String[] CATEGORY_NAMES = {
        "COMBAT", "MOVEMENT", "RENDER", "WORLD", "PLAYER", "UTILITY"
    };

    private int category;
    private int selectedIndex;
    private int scroll;
    private String search = "";
    private boolean editingSearch;
    private String settingsId;
    private String editingSetting;

    public OurClientClickGui() {
        super(Component.literal("Arson Client"));
    }

    @Override
    protected void init() {
        // 1.21.11 Screen#setInitialFocus expects a non-null listener.
        // Explicitly clearing focus is safe and avoids the old focus crash.
        clearFocus();
        clampScroll();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int margin = 16;
        int gap = 12;
        int sidebar = Math.min(178, Math.max(148, width / 5));
        int contentX = margin + sidebar + gap;
        int contentW = Math.max(220, width - contentX - margin);
        int panelBottom = height - margin;

        drawSidebar(g, margin, sidebar, panelBottom, mouseX, mouseY);
        drawModulePanel(g, contentX, margin, contentW, panelBottom, mouseX, mouseY);

        if (settingsId != null) drawSettings(g);
    }

    private void drawSidebar(GuiGraphics g, int left, int sidebar, int bottom, int mouseX, int mouseY) {
        g.fill(left, 16, left + sidebar, bottom, PANEL);
        g.fill(left, 16, left + 4, bottom, ACCENT);

        g.drawString(font, "ARSON", left + 18, 28, TEXT, false);
        g.drawString(font, "CLIENT", left + 18, 42, ACCENT, false);
        g.drawString(font, "MODULES", left + 18, 61, MUTED, false);

        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            int y = 78 + i * 34;
            boolean active = i == category;
            boolean hover = inside(mouseX, mouseY, left + 8, y, sidebar - 16, 29);
            int fill = active ? SELECTED : (hover ? HOVER : ROW);
            g.fill(left + 8, y, left + sidebar - 8, y + 29, fill);
            if (active) g.fill(left + 8, y, left + 11, y + 29, ACCENT);
            g.drawString(font, CATEGORY_NAMES[i], left + 20, y + 10, active ? TEXT : MUTED, false);
        }

        int footerY = bottom - 48;
        g.drawString(font, "↑ ↓  select", left + 18, footerY, MUTED, false);
        g.drawString(font, "ENTER  toggle   O  settings", left + 18, footerY + 14, MUTED, false);
        g.drawString(font, "ESC  close", left + 18, footerY + 28, MUTED, false);
    }

    private void drawModulePanel(GuiGraphics g, int x, int top, int w, int bottom, int mouseX, int mouseY) {
        g.fill(x, top, x + w, bottom, PANEL);

        g.drawString(font, CATEGORY_NAMES[category], x + 18, top + 14, TEXT, false);
        List<ClientModule> modules = modulesForCategory();
        int enabled = 0;
        for (ClientModule module : modules) {
            if (module instanceof ToggleableModule t && t.enabled()) enabled++;
        }
        g.drawString(font, enabled + " enabled  •  " + modules.size() + " modules", x + 18, top + 30, MUTED, false);

        int searchY = top + 47;
        int searchH = 30;
        g.fill(x + 14, searchY, x + w - 14, searchY + searchH, PANEL_2);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, searchText + (editingSearch ? "_" : ""), x + 24, searchY + 10,
            search.isEmpty() ? MUTED : TEXT, false);

        int listTop = searchY + 40;
        int listBottom = bottom - 14;
        int rowH = 34;
        int visibleRows = Math.max(1, (listBottom - listTop) / rowH);
        int maxScroll = Math.max(0, modules.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (modules.isEmpty()) {
            g.drawString(font, "No matching modules", x + 24, listTop + 16, MUTED, false);
            return;
        }

        for (int i = 0; i < modules.size(); i++) {
            int y = listTop + (i - scroll) * rowH;
            if (y + 30 < listTop || y >= listBottom) continue;

            ClientModule module = modules.get(i);
            boolean active = module instanceof ToggleableModule t && t.enabled();
            boolean selected = i == selectedIndex;
            boolean hover = inside(mouseX, mouseY, x + 14, y, w - 28, 30);

            int fill = selected ? SELECTED : (hover ? HOVER : ROW);
            g.fill(x + 14, y, x + w - 14, y + 30, fill);
            if (active) g.fill(x + 14, y, x + 17, y + 30, ACCENT);

            g.drawString(font, pretty(module.id()), x + 26, y + 9, TEXT, false);
            g.drawString(font, active ? "ON" : "OFF", x + w - 54, y + 9, active ? ACCENT : OFF, false);
        }

        if (maxScroll > 0) {
            int trackX = x + w - 8;
            int trackTop = listTop;
            int trackBottom = listBottom;
            int trackH = Math.max(10, trackBottom - trackTop);
            int thumbH = Math.max(18, trackH * visibleRows / modules.size());
            int thumbTravel = Math.max(0, trackH - thumbH);
            int thumbY = trackTop + (maxScroll == 0 ? 0 : thumbTravel * scroll / maxScroll);
            g.fill(trackX, trackTop, trackX + 3, trackBottom, ROW);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
            g.drawString(font, "SCROLL", x + w - 55, listBottom - 9, MUTED, false);
        }
    }

    private void drawSettings(GuiGraphics g) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            editingSetting = null;
            return;
        }

        int w = Math.min(450, Math.max(300, width - 42));
        int h = settingsHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        g.fill(0, 0, width, height, 0x66000000);
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 3, ACCENT);

        g.drawString(font, pretty(settingsId), x + 18, y + 16, TEXT, false);
        g.drawString(font, "MODULE SETTINGS", x + 18, y + 32, MUTED, false);
        g.drawString(font, "ESC closes", x + w - 82, y + 20, MUTED, false);

        int row = y + 54;
        if (module instanceof ToggleableModule t) {
            setting(g, x, row, w, "Enabled", t.enabled() ? "ON" : "OFF");
            row += 38;
        }

        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, x, row, w, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange));
            row += 38;
            setting(g, x, row, w, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing));
            row += 38;
        } else if (c != null && settingsId.equals("auto-schematic-builder")) {
            setting(g, x, row, w, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick));
            row += 38;
        } else {
            g.drawString(font, "No adjustable settings yet.", x + 18, row + 9, MUTED, false);
            row += 38;
        }

        if (editingSetting != null) {
            g.drawString(font, "Editing: " + editingSetting + "  (wheel / ← →)", x + 18, y + h - 28, ACCENT, false);
        } else {
            g.drawString(font, "Click a setting value to edit it", x + 18, y + h - 28, MUTED, false);
        }
    }

    private int settingsHeight() {
        if (settingsId != null && settingsId.equals("aim-assist")) return 190;
        if (settingsId != null && settingsId.equals("auto-schematic-builder")) return 155;
        return 135;
    }

    private void setting(GuiGraphics g, int x, int y, int w, String label, String value) {
        g.drawString(font, label, x + 18, y + 9, TEXT, false);
        g.fill(x + w - 145, y, x + w - 18, y + 28, ROW);
        g.drawString(font, value, x + w - 135, y + 9, TEXT, false);
    }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>();
        String query = search.toLowerCase(Locale.ROOT).trim();
        for (ClientModule module : OurClient.modules().all()) {
            if (categoryFor(module) != category) continue;
            if (!query.isEmpty() && !pretty(module.id()).toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        if (selectedIndex >= result.size()) selectedIndex = Math.max(0, result.size() - 1);
        return result;
    }

    private static int categoryFor(ClientModule module) {
        if (module instanceof CatalogModule catalog) return catalog.category().ordinal();
        return switch (module.id()) {
            case "aim-assist", "trigger-bot", "crystal-macro", "attribute-swap" -> 0;
            case "auto-walk", "auto-jump", "air-jump", "sprint", "freecam", "high-jump", "bunny-hop" -> 1;
            case "tracers", "esp", "xray", "fullbright" -> 2;
            case "auto-schematic-builder", "saved-bases", "scaffold" -> 3;
            case "waypoints", "coordinates", "compass", "radar", "fps-counter", "cps-counter",
                 "keystrokes", "ping-display", "server-info", "potion-effects", "armor-hud",
                 "item-counter", "timer" -> 5;
            default -> 4;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        int margin = 16;
        int gap = 12;
        int sidebar = Math.min(178, Math.max(148, width / 5));
        int contentX = margin + sidebar + gap;
        int contentW = Math.max(220, width - contentX - margin);

        if (settingsId != null) return handleSettingsClick(mx, my);

        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            int y = 78 + i * 34;
            if (inside(mx, my, margin + 8, y, sidebar - 16, 29)) {
                category = i;
                selectedIndex = 0;
                scroll = 0;
                editingSearch = false;
                return true;
            }
        }

        int searchY = margin + 47;
        if (inside(mx, my, contentX + 14, searchY, contentW - 28, 30)) {
            editingSearch = true;
            selectedIndex = 0;
            scroll = 0;
            return true;
        }

        List<ClientModule> modules = modulesForCategory();
        int listTop = searchY + 40;
        int listBottom = height - margin - 14;
        int rowH = 34;
        int visibleRows = Math.max(1, (listBottom - listTop) / rowH);
        int maxScroll = Math.max(0, modules.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < modules.size(); i++) {
            int y = listTop + (i - scroll) * rowH;
            if (!inside(mx, my, contentX + 14, y, contentW - 28, 30)) continue;
            selectedIndex = i;
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && modules.get(i) instanceof ToggleableModule t) {
                toggle(modules.get(i).id(), t);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                settingsId = modules.get(i).id();
                editingSetting = null;
            }
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    private boolean handleSettingsClick(double mx, double my) {
        int w = Math.min(450, Math.max(300, width - 42));
        int h = settingsHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        if (inside(mx, my, x + w - 95, y, 95, 44)) {
            settingsId = null;
            editingSetting = null;
            return true;
        }

        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            editingSetting = null;
            return true;
        }

        int row = y + 54;
        if (module instanceof ToggleableModule t) {
            if (inside(mx, my, x, row, w, 30)) {
                toggle(settingsId, t);
                return true;
            }
            row += 38;
        }

        if (settingsId.equals("aim-assist")) {
            if (inside(mx, my, x, row, w, 30)) {
                editingSetting = "range";
                return true;
            }
            if (inside(mx, my, x, row + 38, w, 30)) {
                editingSetting = "smoothness";
                return true;
            }
        } else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, x, row, w, 30)) {
            editingSetting = "placements";
            return true;
        }
        return true;
    }

    private void toggle(String id, ToggleableModule t) {
        try {
            t.setEnabled(!t.enabled());
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException e) {
            OurClient.LOGGER.error("Failed to toggle '{}'", id, e);
            try { t.setEnabled(false); } catch (RuntimeException ignored) {}
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (settingsId != null) {
            if (editingSetting != null) adjustSetting(vertical > 0 ? 1 : -1);
            return true;
        }

        List<ClientModule> modules = modulesForCategory();
        int listTop = 16 + 47 + 40;
        int listBottom = height - 16 - 14;
        int visibleRows = Math.max(1, (listBottom - listTop) / 34);
        int maxScroll = Math.max(0, modules.size() - visibleRows);
        if (maxScroll > 0) {
            int amount = vertical > 0 ? -1 : 1;
            scroll = Math.max(0, Math.min(maxScroll, scroll + amount));
            return true;
        }
        return true;
    }

    private void adjustSetting(int direction) {
        ClientConfig c = OurClient.config();
        if (c == null || editingSetting == null) return;
        switch (editingSetting) {
            case "range" -> c.aimRange = Math.max(1f, Math.min(64f, c.aimRange + direction));
            case "smoothness" -> c.aimSmoothing = Math.max(.01f, Math.min(1f, c.aimSmoothing + direction * .01f));
            case "placements" -> c.schematicPlacementsPerTick = Math.max(1, Math.min(20, c.schematicPlacementsPerTick + direction));
            default -> { return; }
        }
        OurClient.syncAndSaveConfigFromModules();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingSearch || editingSetting != null) {
                editingSearch = false;
                editingSetting = null;
                return true;
            }
            if (settingsId != null) {
                settingsId = null;
                return true;
            }
            onClose();
            return true;
        }

        if (settingsId != null && editingSetting != null && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
            adjustSetting(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }

        if (editingSearch && key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            selectedIndex = 0;
            scroll = 0;
            return true;
        }

        List<ClientModule> modules = modulesForCategory();
        if (key == GLFW.GLFW_KEY_LEFT) {
            category = (category + CATEGORY_NAMES.length - 1) % CATEGORY_NAMES.length;
            selectedIndex = 0;
            scroll = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            category = (category + 1) % CATEGORY_NAMES.length;
            selectedIndex = 0;
            scroll = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP && !modules.isEmpty()) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            ensureSelectionVisible(modules.size());
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) {
            selectedIndex = Math.min(modules.size() - 1, selectedIndex + 1);
            ensureSelectionVisible(modules.size());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty()) {
            ClientModule m = modules.get(selectedIndex);
            if (m instanceof ToggleableModule t) toggle(m.id(), t);
            return true;
        }
        if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) {
            settingsId = modules.get(selectedIndex).id();
            editingSetting = null;
            return true;
        }
        return super.keyPressed(event);
    }

    private void ensureSelectionVisible(int size) {
        int visibleRows = Math.max(1, (height - 16 - 14 - (16 + 47 + 40)) / 34);
        if (selectedIndex < scroll) scroll = selectedIndex;
        if (selectedIndex >= scroll + visibleRows) scroll = selectedIndex - visibleRows + 1;
        scroll = Math.max(0, Math.min(Math.max(0, size - visibleRows), scroll));
    }

    private void clampScroll() {
        List<ClientModule> modules = modulesForCategory();
        int visibleRows = Math.max(1, (height - 16 - 14 - (16 + 47 + 40)) / 34);
        scroll = Math.max(0, Math.min(Math.max(0, modules.size() - visibleRows), scroll));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editingSearch && !Character.isISOControl(event.codepoint())) {
            search += new String(Character.toChars(event.codepoint()));
            selectedIndex = 0;
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String pretty(String id) {
        String[] words = id.split("[-_]");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) out.append(' ');
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }
}
