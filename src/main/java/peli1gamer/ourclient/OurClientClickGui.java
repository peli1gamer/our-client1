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

/** Stable, self-contained Arson Click GUI. */
public final class OurClientClickGui extends Screen {
    private static final int BG = 0xF20A0A0D, PANEL = 0xF2181820, PANEL_2 = 0xF21D1D27;
    private static final int ROW = 0xFF22222C, HOVER = 0xFF2B2B37, SELECTED = 0xFF343440;
    private static final int ACCENT = 0xFFFF6A00, TEXT = 0xFFECE8F5, MUTED = 0xFFAAA5B7;
    private static final int OFF = 0xFF666272, PLANNED = 0xFFFFB15A;
    private static final String[] CATEGORY_NAMES = { "COMBAT", "MOVEMENT", "RENDER", "WORLD", "PLAYER", "UTILITY" };

    private int category, selectedIndex, scroll;
    private String search = "";
    private boolean editingSearch;
    private String settingsId, editingSetting;

    public OurClientClickGui() { super(Component.literal("Arson Client")); }
    @Override protected void init() { clearFocus(); clampScroll(); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        int margin = 16, gap = 12, sidebar = Math.min(178, Math.max(148, width / 5));
        int contentX = margin + sidebar + gap, contentW = Math.max(220, width - contentX - margin), panelBottom = height - margin;
        drawSidebar(g, margin, sidebar, panelBottom, mouseX, mouseY);
        drawModulePanel(g, contentX, margin, contentW, panelBottom, mouseX, mouseY);
        if (settingsId != null) drawSettings(g);
    }

    private void drawSidebar(GuiGraphics g, int left, int sidebar, int bottom, int mouseX, int mouseY) {
        g.fill(left, 16, left + sidebar, bottom, PANEL); g.fill(left, 16, left + 4, bottom, ACCENT);
        g.drawString(font, "ARSON", left + 18, 28, TEXT, false); g.drawString(font, "CLIENT", left + 18, 42, ACCENT, false);
        g.drawString(font, "MODULES", left + 18, 61, MUTED, false);
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            int y = 78 + i * 34; boolean active = i == category, hover = inside(mouseX, mouseY, left + 8, y, sidebar - 16, 29);
            g.fill(left + 8, y, left + sidebar - 8, y + 29, active ? SELECTED : (hover ? HOVER : ROW));
            if (active) g.fill(left + 8, y, left + 11, y + 29, ACCENT);
            g.drawString(font, CATEGORY_NAMES[i], left + 20, y + 10, active ? TEXT : MUTED, false);
        }
        int footerY = bottom - 48;
        g.drawString(font, "↑ ↓  select", left + 18, footerY, MUTED, false);
        g.drawString(font, "ENTER  toggle   O  settings", left + 18, footerY + 14, MUTED, false);
        g.drawString(font, "ESC  close", left + 18, footerY + 28, MUTED, false);
    }

    private void drawModulePanel(GuiGraphics g, int x, int top, int w, int bottom, int mouseX, int mouseY) {
        g.fill(x, top, x + w, bottom, PANEL); g.drawString(font, CATEGORY_NAMES[category], x + 18, top + 14, TEXT, false);
        List<ClientModule> modules = modulesForCategory(); int enabled = 0;
        for (ClientModule m : modules) if (m instanceof ToggleableModule t && !(m instanceof CatalogModule) && t.enabled()) enabled++;
        g.drawString(font, enabled + " enabled  •  " + modules.size() + " modules", x + 18, top + 30, MUTED, false);
        int searchY = top + 47, searchH = 30;
        g.fill(x + 14, searchY, x + w - 14, searchY + searchH, PANEL_2);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, searchText + (editingSearch ? "_" : ""), x + 24, searchY + 10, search.isEmpty() ? MUTED : TEXT, false);
        int listTop = searchY + 40, listBottom = bottom - 14, rowH = 34;
        int visibleRows = Math.max(1, (listBottom - listTop) / rowH), maxScroll = Math.max(0, modules.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (modules.isEmpty()) { g.drawString(font, "No matching modules", x + 24, listTop + 16, MUTED, false); return; }
        for (int i = 0; i < modules.size(); i++) {
            int y = listTop + (i - scroll) * rowH; if (y + 30 < listTop || y >= listBottom) continue;
            ClientModule module = modules.get(i); boolean planned = module instanceof CatalogModule;
            boolean active = module instanceof ToggleableModule t && !planned && t.enabled(); boolean selected = i == selectedIndex;
            boolean hover = inside(mouseX, mouseY, x + 14, y, w - 28, 30);
            g.fill(x + 14, y, x + w - 14, y + 30, selected ? SELECTED : (hover ? HOVER : ROW));
            if (active) g.fill(x + 14, y, x + 17, y + 30, ACCENT);
            g.drawString(font, pretty(module.id()), x + 26, y + 9, TEXT, false);
            g.drawString(font, planned ? "PLANNED" : (active ? "ON" : "OFF"), x + w - (planned ? 76 : 54), y + 9, planned ? PLANNED : (active ? ACCENT : OFF), false);
        }
        if (maxScroll > 0) {
            int trackX = x + w - 8, trackTop = listTop, trackBottom = listBottom, trackH = Math.max(10, trackBottom - trackTop);
            int thumbH = Math.max(18, trackH * visibleRows / modules.size()), thumbTravel = Math.max(0, trackH - thumbH);
            int thumbY = trackTop + thumbTravel * scroll / maxScroll;
            g.fill(trackX, trackTop, trackX + 3, trackBottom, ROW); g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
        }
    }

    private void drawSettings(GuiGraphics g) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; editingSetting = null; return; }
        int w = Math.min(450, Math.max(300, width - 42)), h = settingsHeight(), x = (width - w) / 2, y = (height - h) / 2;
        g.fill(0, 0, width, height, 0x66000000); g.fill(x, y, x + w, y + h, PANEL); g.fill(x, y, x + w, y + 3, ACCENT);
        g.drawString(font, pretty(settingsId), x + 18, y + 16, TEXT, false);
        int row = y + 54;
        if (module instanceof ToggleableModule t) { setting(g, x, row, w, "Enabled", t.enabled() ? "ON" : "OFF"); row += 38; }
        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, x, row, w, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange)); row += 38;
            setting(g, x, row, w, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing));
        } else if (c != null && settingsId.equals("auto-schematic-builder")) setting(g, x, row, w, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick));
        g.drawString(font, "Wheel / ← → changes selected setting", x + 18, y + h - 42, MUTED, false);
        g.drawString(font, editingSetting != null ? "Editing: " + editingSetting + "  (wheel / ← →)" : "Right-click a module to open settings", x + 18, y + h - 28, editingSetting != null ? ACCENT : MUTED, false);
        g.drawString(font, "CLOSE", x + w - 75, y + 17, TEXT, false);
    }

    private int settingsHeight() { if (settingsId == null) return 120; if (settingsId.equals("aim-assist")) return 220; if (settingsId.equals("auto-schematic-builder")) return 180; return 130; }
    private void setting(GuiGraphics g, int x, int y, int w, String label, String value) { g.drawString(font, label, x + 18, y + 9, TEXT, false); g.fill(x + w - 145, y, x + w - 18, y + 28, ROW); g.drawString(font, value, x + w - 135, y + 9, TEXT, false); }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>(); String query = search.toLowerCase(Locale.ROOT).trim();
        for (ClientModule module : OurClient.modules().all()) {
            if (categoryFor(module) != category) continue;
            if (!query.isEmpty() && !pretty(module.id()).toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        if (selectedIndex >= result.size()) selectedIndex = Math.max(0, result.size() - 1); return result;
    }

    private static int categoryFor(ClientModule module) {
        if (module instanceof CatalogModule catalog) return catalog.category().ordinal();
        return switch (module.id()) {
            case "aim-assist", "trigger-bot", "crystal-macro", "anchor-macro", "attribute-swap", "auto-weapon" -> 0;
            case "auto-walk", "auto-jump", "air-jump", "sprint", "auto-sprint", "freecam", "high-jump", "bunny-hop", "fast-climb", "anti-void", "no-fall", "elytra-fly" -> 1;
            case "tracers", "esp", "xray", "fullbright", "block-esp", "player-esp", "mob-esp", "item-esp", "storage-esp", "void-esp", "trail",
                 "glazed-players", "glazed-mobs", "glazed-items", "villager-esp", "pillager-esp", "wandering-esp", "amethyst-esp", "beehive-esp", "deepslate-esp", "dripstone-esp", "kelp-esp", "vine-esp", "light-esp" -> 2;
            case "auto-schematic-builder", "saved-bases", "scaffold" -> 3;
            case "waypoints", "coordinates", "compass", "radar", "fps-counter", "cps-counter", "keystrokes", "ping-display", "server-info", "potion-effects", "armor-hud", "item-counter", "timer" -> 5;
            default -> 4;
        };
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y(); int button = event.button(), margin = 16, gap = 12;
        int sidebar = Math.min(178, Math.max(148, width / 5)), contentX = margin + sidebar + gap, contentW = Math.max(220, width - contentX - margin);
        if (settingsId != null) return handleSettingsClick(mx, my);
        for (int i = 0; i < CATEGORY_NAMES.length; i++) { int y = 78 + i * 34; if (inside(mx, my, margin + 8, y, sidebar - 16, 29)) { category = i; selectedIndex = 0; scroll = 0; editingSearch = false; return true; } }
        int searchY = margin + 47;
        if (inside(mx, my, contentX + 14, searchY, contentW - 28, 30)) { editingSearch = true; selectedIndex = 0; scroll = 0; return true; }
        List<ClientModule> modules = modulesForCategory(); int listTop = searchY + 40, listBottom = height - margin - 14, rowH = 34;
        int visibleRows = Math.max(1, (listBottom - listTop) / rowH); int maxScroll = Math.max(0, modules.size() - visibleRows); scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < modules.size(); i++) { int y = listTop + (i - scroll) * rowH; if (y + 30 < listTop || y >= listBottom || !inside(mx, my, contentX + 14, y, contentW - 28, 30)) continue;
            selectedIndex = i; ClientModule module = modules.get(i); if (module instanceof CatalogModule) return true;
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule) toggle(module.id()); else if (button == GLFW.GLFW_MOUSE_BUTTON_2) { settingsId = module.id(); editingSetting = null; }
            return true; }
        return true;
    }

    private boolean handleSettingsClick(double mx, double my) {
        int w = Math.min(450, Math.max(300, width - 42)), h = settingsHeight(), x = (width - w) / 2, y = (height - h) / 2;
        if (inside(mx, my, x + w - 95, y, 95, 44)) { settingsId = null; editingSetting = null; return true; }
        ClientModule module = OurClient.modules().get(settingsId); if (module == null || module instanceof CatalogModule) return true;
        int row = y + 54; if (module instanceof ToggleableModule) { if (inside(mx, my, x, row, w, 30)) { toggle(settingsId); return true; } row += 38; }
        if (settingsId.equals("aim-assist")) { if (inside(mx, my, x, row, w, 30)) { editingSetting = "range"; return true; } if (inside(mx, my, x, row + 38, w, 30)) { editingSetting = "smoothness"; return true; } }
        else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, x, row, w, 30)) editingSetting = "placements";
        return true;
    }

    private void toggle(String id) { if (!OurClient.modules().toggle(id)) OurClient.LOGGER.warn("ClickGUI could not toggle module '{}'", id); else try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException e) { OurClient.LOGGER.error("Could not persist ClickGUI toggle for '{}'", id, e); } }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (vertical == 0.0D) return true;
        if (settingsId != null) { int w = Math.min(450, Math.max(300, width - 42)), h = settingsHeight(), x = (width - w) / 2, y = (height - h) / 2; if (editingSetting != null && inside(mx, my, x, y, w, h)) adjustSetting(vertical > 0 ? 1 : -1); return true; }
        int margin = 16, gap = 12, sidebar = Math.min(178, Math.max(148, width / 5)), contentX = margin + sidebar + gap, contentW = Math.max(220, width - contentX - margin), listTop = margin + 47 + 40, listBottom = height - margin - 14;
        if (!inside(mx, my, contentX, listTop, contentW, Math.max(1, listBottom - listTop))) return true;
        List<ClientModule> modules = modulesForCategory(); int visibleRows = Math.max(1, (listBottom - listTop) / 34), maxScroll = Math.max(0, modules.size() - visibleRows);
        scroll = Math.max(0, Math.min(maxScroll, scroll + (vertical > 0 ? -1 : 1))); return true;
    }

    private void adjustSetting(int direction) { ClientConfig c = OurClient.config(); if (c == null || editingSetting == null) return; switch (editingSetting) {
        case "range" -> c.aimRange = Math.max(1f, Math.min(32f, c.aimRange + direction));
        case "smoothness" -> c.aimSmoothing = Math.max(0.01f, Math.min(1f, c.aimSmoothing + direction * 0.05f));
        case "placements" -> c.schematicPlacementsPerTick = Math.max(1, Math.min(64, c.schematicPlacementsPerTick + direction));
        default -> { return; }
    } try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException e) { OurClient.LOGGER.error("Could not persist ClickGUI setting change", e); } }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key(); if (key == GLFW.GLFW_KEY_ESCAPE) { if (settingsId != null) { settingsId = null; editingSetting = null; } else if (editingSearch) { editingSearch = false; } else onClose(); return true; }
        if (editingSearch) { if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); selectedIndex = 0; scroll = 0; return true; } if (key == GLFW.GLFW_KEY_ENTER) { editingSearch = false; return true; } }
        List<ClientModule> modules = modulesForCategory();
        if (key == GLFW.GLFW_KEY_UP) { selectedIndex = Math.max(0, selectedIndex - 1); ensureSelectionVisible(modules.size()); return true; }
        if (key == GLFW.GLFW_KEY_DOWN) { selectedIndex = Math.min(Math.max(0, modules.size() - 1), selectedIndex + 1); ensureSelectionVisible(modules.size()); return true; }
        if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty()) { ClientModule m = modules.get(selectedIndex); if (m instanceof ToggleableModule) toggle(m.id()); return true; }
        if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) { ClientModule m = modules.get(selectedIndex); if (!(m instanceof CatalogModule)) { settingsId = m.id(); editingSetting = null; } return true; }
        return super.keyPressed(event);
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (editingSearch) { char c = event.codepoint() <= Character.MAX_VALUE ? (char) event.codepoint() : 0; if (c >= 32 && c != 127) { search += c; selectedIndex = 0; scroll = 0; return true; } }
        return super.charTyped(event);
    }

    private void ensureSelectionVisible(int size) { int listTop = 16 + 47 + 40, listBottom = height - 16 - 14, visible = Math.max(1, (listBottom - listTop) / 34); if (selectedIndex < scroll) scroll = selectedIndex; else if (selectedIndex >= scroll + visible) scroll = selectedIndex - visible + 1; scroll = Math.max(0, Math.min(Math.max(0, size - visible), scroll)); }
    private void clampScroll() { List<ClientModule> modules = modulesForCategory(); int listTop = 16 + 47 + 40, listBottom = height - 16 - 14, visible = Math.max(1, (listBottom - listTop) / 34); scroll = Math.max(0, Math.min(Math.max(0, modules.size() - visible), scroll)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static String pretty(String id) { String[] parts = id.split("-"); StringBuilder b = new StringBuilder(); for (String p : parts) { if (p.isEmpty()) continue; if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)); } return b.toString(); }
}
