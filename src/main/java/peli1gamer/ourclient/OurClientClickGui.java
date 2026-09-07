package peli1gamer.ourclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Simple keyboard/mouse friendly module browser with category navigation. */
public final class OurClientClickGui extends Screen {
    private static final int ACCENT = 0xFFFF6A00;
    private static final int BG = 0xE8101118;
    private static final int PANEL = 0xE51A1B25;
    private static final int ROW = 0xA0252632;
    private static final int SELECTED = 0xFFB83A00;
    private static final int HOVER = 0xA0443024;
    private static final int TEXT = 0xFFECE8F5;
    private static final int MUTED = 0xFFAAA5B7;

    private final String[] categoryNames = {"COMBAT", "VISUAL", "MOVEMENT", "WORLD", "CLIENT"};
    private int category;
    private int selectedIndex;
    private String search = "";
    private boolean editingSearch;
    private String settingsId;
    private String editingSetting;

    public OurClientClickGui() { super(Component.literal("Arson Client")); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        int left = 18, top = 18, sidebar = Math.min(150, Math.max(120, width / 4));
        g.fill(left, top, left + sidebar, height - 18, PANEL);
        g.fill(left, top, left + 4, height - 18, ACCENT);
        g.drawString(font, "ARSON CLIENT", left + 16, top + 16, TEXT, false);
        g.drawString(font, "Modules", left + 16, top + 32, MUTED, false);
        for (int i = 0; i < categoryNames.length; i++) {
            int y = top + 54 + i * 34;
            boolean active = i == category;
            boolean hover = inside(mouseX, mouseY, left + 8, y, sidebar - 16, 29);
            g.fill(left + 8, y, left + sidebar - 8, y + 29, active ? SELECTED : (hover ? HOVER : ROW));
            g.drawString(font, categoryNames[i], left + 18, y + 9, TEXT, false);
        }
        int contentX = left + sidebar + 18, contentW = width - contentX - 18;
        g.fill(contentX, top, contentX + contentW, height - 18, PANEL);
        g.drawString(font, categoryNames[category], contentX + 18, top + 14, TEXT, false);
        g.drawString(font, "Left/right: category   Up/down: module   Enter: toggle   O: settings", contentX + 18, top + 30, MUTED, false);
        int searchY = top + 48;
        g.fill(contentX + 14, searchY, contentX + contentW - 14, searchY + 30, ROW);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, searchText + (editingSearch ? "_" : ""), contentX + 24, searchY + 10, search.isEmpty() ? MUTED : TEXT, false);
        List<ClientModule> modules = modulesForCategory();
        if (modules.isEmpty()) g.drawString(font, "No matching modules", contentX + 24, searchY + 55, MUTED, false);
        else {
            selectedIndex = Math.max(0, Math.min(selectedIndex, modules.size() - 1));
            for (int i = 0; i < modules.size(); i++) {
                int y = searchY + 42 + i * 34;
                ClientModule module = modules.get(i);
                boolean active = module instanceof ToggleableModule t && t.enabled();
                boolean selected = i == selectedIndex;
                boolean hover = inside(mouseX, mouseY, contentX + 14, y, contentW - 28, 30);
                g.fill(contentX + 14, y, contentX + contentW - 14, y + 30, selected ? (active ? SELECTED : HOVER) : (active ? SELECTED : ROW));
                g.drawString(font, pretty(module.id()), contentX + 24, y + 10, TEXT, false);
                g.drawString(font, active ? "ON" : "OFF", contentX + contentW - 58, y + 10, active ? TEXT : MUTED, false);
                if (selected) g.fill(contentX + 14, y, contentX + 17, y + 30, ACCENT);
            }
        }
        g.drawString(font, "RIGHT SHIFT = GUI   |   ESC = close", contentX + 18, height - 32, MUTED, false);
        if (settingsId != null) renderSettings(g);
    }

    private void renderSettings(GuiGraphics g) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; return; }
        int w = Math.min(430, width - 36), h = settingsHeight();
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xF2181822);
        g.fill(x, y, x + w, y + 3, ACCENT);
        g.drawString(font, pretty(settingsId) + " settings", x + 18, y + 16, TEXT, false);
        g.drawString(font, "ESC closes this panel", x + 18, y + 32, MUTED, false);
        int row = y + 55;
        if (module instanceof ToggleableModule t) { setting(g, x, row, w, "Enabled", t.enabled() ? "ON" : "OFF"); row += 38; }
        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, x, row, w, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange)); row += 38;
            setting(g, x, row, w, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing));
        } else if (c != null && settingsId.equals("auto-schematic-builder")) {
            setting(g, x, row, w, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick));
        } else {
            g.drawString(font, "No adjustable settings yet.", x + 18, row + 8, MUTED, false);
        }
        g.drawString(font, "Click a value, then use wheel or ←/→", x + 18, y + h - 28, MUTED, false);
    }

    private int settingsHeight() {
        if (settingsId != null && settingsId.equals("aim-assist")) return 190;
        if (settingsId != null && settingsId.equals("auto-schematic-builder")) return 155;
        return 130;
    }

    private void setting(GuiGraphics g, int x, int y, int w, String label, String value) {
        g.drawString(font, label, x + 18, y + 9, TEXT, false);
        g.fill(x + w - 145, y, x + w - 18, y + 28, ROW);
        g.drawString(font, value, x + w - 135, y + 9, TEXT, false);
    }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>();
        for (ClientModule module : OurClient.modules().all()) {
            int cat = switch (module.id()) {
                case "aim-assist" -> 0;
                case "tracers", "esp" -> 1;
                case "freecam" -> 2;
                case "auto-schematic-builder", "saved-bases", "scaffold" -> 3;
                default -> 4;
            };
            if (cat == category && (search.isBlank() || pretty(module.id()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) result.add(module);
        }
        return result;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y(); int button = event.button();
        int left = 18, top = 18, sidebar = Math.min(150, Math.max(120, width / 4));
        for (int i = 0; i < categoryNames.length; i++) { int y = top + 54 + i * 34; if (inside(mx, my, left + 8, y, sidebar - 16, 29)) { category = i; selectedIndex = 0; return true; } }
        int contentX = left + sidebar + 18, contentW = width - contentX - 18, searchY = top + 48;
        if (inside(mx, my, contentX + 14, searchY, contentW - 28, 30)) { editingSearch = true; settingsId = null; return true; }
        if (settingsId != null) return handleSettingsClick(mx, my);
        List<ClientModule> modules = modulesForCategory();
        for (int i = 0; i < modules.size(); i++) { int y = searchY + 42 + i * 34; if (inside(mx, my, contentX + 14, y, contentW - 28, 30)) { selectedIndex = i; if (button == GLFW.GLFW_MOUSE_BUTTON_1 && modules.get(i) instanceof ToggleableModule t) toggle(modules.get(i).id(), t); else if (button == GLFW.GLFW_MOUSE_BUTTON_2) settingsId = modules.get(i).id(); return true; } }
        return super.mouseClicked(event, doubled);
    }

    private boolean handleSettingsClick(double mx, double my) {
        int w = Math.min(430, width - 36), h = settingsHeight(), x = (width - w) / 2, y = (height - h) / 2;
        ClientModule module = OurClient.modules().get(settingsId);
        if (inside(mx, my, x + w - 70, y, 70, 42)) { settingsId = null; editingSetting = null; return true; }
        int row = y + 55;
        if (module instanceof ToggleableModule t && inside(mx, my, x, row, w, 30)) { toggle(settingsId, t); return true; }
        row += 38;
        if (settingsId.equals("aim-assist")) {
            if (inside(mx, my, x, row, w, 30)) { editingSetting = "range"; return true; }
            if (inside(mx, my, x, row + 38, w, 30)) { editingSetting = "smoothness"; return true; }
        } else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, x, row, w, 30)) editingSetting = "placements";
        return true;
    }

    private void toggle(String id, ToggleableModule t) { try { t.setEnabled(!t.enabled()); OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException e) { OurClient.LOGGER.error("Failed to toggle '{}'", id, e); try { t.setEnabled(false); } catch (RuntimeException ignored) {} } }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { if (editingSetting != null) { adjustSetting(v > 0 ? 1 : -1); return true; } return super.mouseScrolled(mx, my, h, v); }
    private void adjustSetting(int direction) {
        ClientConfig c = OurClient.config(); if (c == null || editingSetting == null) return;
        switch (editingSetting) {
            case "range" -> c.aimRange = Math.max(1f, Math.min(64f, c.aimRange + direction));
            case "smoothness" -> c.aimSmoothing = Math.max(.01f, Math.min(1f, c.aimSmoothing + direction * .01f));
            case "placements" -> c.schematicPlacementsPerTick = Math.max(1, Math.min(20, c.schematicPlacementsPerTick + direction));
            default -> { return; }
        }
        OurClient.syncAndSaveConfigFromModules();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (editingSearch || editingSetting != null) { editingSearch = false; editingSetting = null; return true; } if (settingsId != null) { settingsId = null; return true; } onClose(); return true; }
        if (settingsId != null && editingSetting != null && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) { adjustSetting(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1); return true; }
        if (editingSearch && key == GLFW.GLFW_KEY_BACKSPACE) { if (!search.isEmpty()) search = search.substring(0, search.length() - 1); return true; }
        List<ClientModule> modules = modulesForCategory();
        if (key == GLFW.GLFW_KEY_LEFT) { category = (category + categoryNames.length - 1) % categoryNames.length; selectedIndex = 0; return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { category = (category + 1) % categoryNames.length; selectedIndex = 0; return true; }
        if (key == GLFW.GLFW_KEY_UP && !modules.isEmpty()) { selectedIndex = (selectedIndex + modules.size() - 1) % modules.size(); return true; }
        if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) { selectedIndex = (selectedIndex + 1) % modules.size(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty()) { ClientModule m = modules.get(selectedIndex); if (m instanceof ToggleableModule t) toggle(m.id(), t); return true; }
        if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) { settingsId = modules.get(selectedIndex).id(); editingSetting = null; return true; }
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event) { if (editingSearch && !Character.isISOControl(event.codepoint())) { search += new String(Character.toChars(event.codepoint())); selectedIndex = 0; return true; } return super.charTyped(event); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static String pretty(String id) { String[] words = id.split("[-_]"); StringBuilder out = new StringBuilder(); for (String word : words) { if (!out.isEmpty()) out.append(' '); if (!word.isEmpty()) out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); } return out.toString(); }
}
