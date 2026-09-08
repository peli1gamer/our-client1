package peli1gamer.ourclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class OurClientClickGui extends Screen {
    private static final int BG = 0xF20B0B10;
    private static final int PANEL = 0xF2181822;
    private static final int ROW = 0xFF20202C;
    private static final int HOVER = 0xFF2A2A38;
    private static final int SELECTED = 0xFF333344;
    private static final int ACCENT = 0xFFFF6A00;
    private static final int TEXT = 0xFFECE8F5;
    private static final int MUTED = 0xFFAAA5B7;
    private static final String[] CATEGORY_NAMES = {"COMBAT", "MOVEMENT", "RENDER", "WORLD", "PLAYER", "UTILITY"};
    private static final int TOP = 18;
    private static final int BOTTOM = 18;
    private static final int SIDEBAR_MIN = 160;
    private static final int SIDEBAR_MAX = 190;
    private static final int ROW_HEIGHT = 34;
    private static final int ROW_GAP = 4;
    private int category;
    private int selectedIndex;
    private double moduleScroll;
    private String search = "";
    private boolean editingSearch;
    private String settingsId;
    private String editingSetting;

    public OurClientClickGui() { super(Component.literal("Arson Client")); }

    @Override
    protected void init() {
        clearFocus();
        clampScroll();
    }

    private int sidebarWidth() { return Math.min(SIDEBAR_MAX, Math.max(SIDEBAR_MIN, width / 4)); }
    private int contentX() { return 18 + sidebarWidth() + 18; }
    private int contentWidth() { return Math.max(120, width - contentX() - 18); }
    private int searchY() { return TOP + 48; }
    private int listTop() { return searchY() + 42; }
    private int listBottom() { return height - BOTTOM - 12; }
    private int viewportHeight() { return Math.max(1, listBottom() - listTop()); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        int left = 18;
        int sidebar = sidebarWidth();
        int contentX = contentX();
        int contentW = contentWidth();
        g.fill(left, TOP, left + sidebar, height - BOTTOM, PANEL);
        g.fill(left, TOP, left + 4, height - BOTTOM, ACCENT);
        g.drawString(font, "ARSON CLIENT", left + 16, TOP + 16, TEXT, false);
        g.drawString(font, "MODULES", left + 16, TOP + 32, MUTED, false);
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            int y = TOP + 54 + i * 31;
            boolean active = i == category;
            boolean hover = inside(mouseX, mouseY, left + 8, y, sidebar - 16, 27);
            g.fill(left + 8, y, left + sidebar - 8, y + 27, active ? SELECTED : (hover ? HOVER : ROW));
            g.drawString(font, CATEGORY_NAMES[i], left + 18, y + 8, TEXT, false);
        }
        g.fill(contentX, TOP, contentX + contentW, height - BOTTOM, PANEL);
        g.drawString(font, CATEGORY_NAMES[category], contentX + 18, TOP + 14, TEXT, false);
        g.drawString(font, "Mouse wheel: scroll   ↑/↓: select   Enter: toggle   Right-click: settings", contentX + 18, TOP + 30, MUTED, false);
        int searchY = searchY();
        int searchLeft = contentX + 14;
        int searchWidth = contentW - 28;
        boolean searchHover = inside(mouseX, mouseY, searchLeft, searchY, searchWidth, 30);
        g.fill(searchLeft, searchY, searchLeft + searchWidth, searchY + 30, editingSearch || searchHover ? HOVER : ROW);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, searchText + (editingSearch ? "_" : ""), searchLeft + 10, searchY + 10, search.isEmpty() ? MUTED : TEXT, false);
        List<ClientModule> modules = modulesForCategory();
        clampScroll(modules.size());
        renderModuleViewport(g, mouseX, mouseY, modules);
        if (settingsId != null) renderSettings(g);
    }

    private void renderModuleViewport(GuiGraphics g, int mouseX, int mouseY, List<ClientModule> modules) {
        int x = contentX() + 14;
        int w = contentWidth() - 28;
        int top = listTop();
        int bottom = listBottom();
        int viewport = viewportHeight();
        int maxScroll = maxScroll(modules.size());
        g.fill(x, top, x + w, bottom, 0x33101018);
        if (modules.isEmpty()) {
            g.drawString(font, "No matching modules", x + 10, top + 12, MUTED, false);
            return;
        }
        int first = Math.max(0, (int) Math.floor(moduleScroll / (ROW_HEIGHT + ROW_GAP)) - 1);
        int last = Math.min(modules.size() - 1, (int) Math.ceil((moduleScroll + viewport) / (ROW_HEIGHT + ROW_GAP)));
        for (int i = first; i <= last; i++) {
            int y = top + i * (ROW_HEIGHT + ROW_GAP) - (int) moduleScroll;
            if (y + ROW_HEIGHT < top || y > bottom) continue;
            ClientModule module = modules.get(i);
            boolean active = module instanceof ToggleableModule t && t.enabled();
            boolean selected = i == selectedIndex;
            boolean hover = inside(mouseX, mouseY, x, y, w - 8, ROW_HEIGHT);
            g.fill(x, y, x + w - 8, y + ROW_HEIGHT, selected ? SELECTED : (hover ? HOVER : ROW));
            g.fill(x, y, x + 3, y + ROW_HEIGHT, active ? ACCENT : 0x00000000);
            g.drawString(font, pretty(module.id()), x + 12, y + 7, TEXT, false);
            g.drawString(font, active ? "ON" : "OFF", x + w - 48, y + 7, active ? ACCENT : MUTED, false);
        }
        if (maxScroll > 0) {
            int trackX = x + w - 6;
            int trackTop = top;
            int trackBottom = bottom;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(24, (int) ((double) viewport / Math.max(viewport, totalContentHeight(modules.size())) * trackHeight));
            int thumbRange = Math.max(0, trackHeight - thumbHeight);
            int thumbY = trackTop + (int) (moduleScroll / maxScroll * thumbRange);
            g.fill(trackX, trackTop, trackX + 3, trackBottom, ROW);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, ACCENT);
        }
        if (moduleScroll > 0) g.fill(x, top, x + w - 8, top + 3, 0xAA0B0B10);
        if (moduleScroll < maxScroll) g.fill(x, bottom - 3, x + w - 8, bottom, 0xAA0B0B10);
    }

    private int totalContentHeight(int count) {
        if (count <= 0) return 0;
        return count * ROW_HEIGHT + (count - 1) * ROW_GAP;
    }
    private int maxScroll(int count) { return Math.max(0, totalContentHeight(count) - viewportHeight()); }
    private void clampScroll() { clampScroll(modulesForCategory().size()); }
    private void clampScroll(int count) {
        int max = maxScroll(count);
        if (moduleScroll < 0) moduleScroll = 0;
        if (moduleScroll > max) moduleScroll = max;
    }
    private void ensureSelectedVisible(List<ClientModule> modules) {
        if (modules.isEmpty()) { selectedIndex = 0; moduleScroll = 0; return; }
        selectedIndex = Math.max(0, Math.min(selectedIndex, modules.size() - 1));
        int step = ROW_HEIGHT + ROW_GAP;
        int rowTop = selectedIndex * step;
        int rowBottom = rowTop + ROW_HEIGHT;
        int viewTop = (int) moduleScroll;
        int viewBottom = viewTop + viewportHeight();
        if (rowTop < viewTop) moduleScroll = rowTop;
        else if (rowBottom > viewBottom) moduleScroll = rowBottom - viewportHeight();
        clampScroll(modules.size());
    }

    private void renderSettings(GuiGraphics g) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; editingSetting = null; return; }
        int w = Math.min(430, width - 36);
        int h = settingsHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        g.fill(0, 0, width, height, 0x66000000);
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 3, ACCENT);
        g.drawString(font, pretty(settingsId) + " settings", x + 18, y + 16, TEXT, false);
        g.drawString(font, "ESC closes", x + w - 72, y + 16, MUTED, false);
        int row = y + 48;
        if (module instanceof ToggleableModule t) {
            setting(g, x, row, w, "Enabled", t.enabled() ? "ON" : "OFF", editingSetting == null);
            row += 38;
        }
        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, x, row, w, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange), "range".equals(editingSetting));
            row += 38;
            setting(g, x, row, w, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing), "smoothness".equals(editingSetting));
        } else if (c != null && settingsId.equals("auto-schematic-builder")) {
            setting(g, x, row, w, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick), "placements".equals(editingSetting));
        } else {
            g.drawString(font, "No adjustable settings yet.", x + 18, row + 8, MUTED, false);
        }
        g.drawString(font, "Click a value, then use wheel or ←/→", x + 18, y + h - 20, MUTED, false);
    }
    private int settingsHeight() {
        if (settingsId != null && settingsId.equals("aim-assist")) return 190;
        if (settingsId != null && settingsId.equals("auto-schematic-builder")) return 155;
        return 130;
    }
    private void setting(GuiGraphics g, int x, int y, int w, String label, String value, boolean highlighted) {
        g.drawString(font, label, x + 18, y + 9, TEXT, false);
        int vx = x + w - 145;
        g.fill(vx, y, x + w - 18, y + 28, highlighted ? HOVER : ROW);
        g.drawString(font, value, vx + 10, y + 9, TEXT, false);
    }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        for (ClientModule module : OurClient.modules().all()) {
            if (categoryFor(module) != category) continue;
            if (!query.isEmpty() && !pretty(module.id()).toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        if (result.isEmpty()) selectedIndex = 0;
        else selectedIndex = Math.max(0, Math.min(selectedIndex, result.size() - 1));
        return result;
    }
    private static int categoryFor(ClientModule module) {
        if (module instanceof CatalogModule catalog) return catalog.category().ordinal();
        return switch (module.id()) {
            case "aim-assist", "trigger-bot", "crystal-macro", "attribute-swap" -> 0;
            case "auto-walk", "auto-jump", "air-jump", "sprint", "freecam", "high-jump", "bunny-hop" -> 1;
            case "tracers", "esp", "xray", "fullbright" -> 2;
            case "auto-schematic-builder", "saved-bases", "scaffold" -> 3;
            default -> 4;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        try {
            double mx = event.x();
            double my = event.y();
            int button = event.button();
            if (settingsId != null) return handleSettingsClick(mx, my);
            int left = 18;
            int sidebar = sidebarWidth();
            for (int i = 0; i < CATEGORY_NAMES.length; i++) {
                int y = TOP + 54 + i * 31;
                if (inside(mx, my, left + 8, y, sidebar - 16, 27)) {
                    category = i; selectedIndex = 0; moduleScroll = 0; editingSearch = false; return true;
                }
            }
            int sx = contentX() + 14;
            int sw = contentWidth() - 28;
            if (inside(mx, my, sx, searchY(), sw, 30)) { editingSearch = true; editingSetting = null; return true; }
            List<ClientModule> modules = modulesForCategory();
            int listX = contentX() + 14;
            int listW = contentWidth() - 36;
            int top = listTop();
            int bottom = listBottom();
            if (inside(mx, my, listX, top, listW, bottom - top)) {
                int localY = (int) my - top + (int) moduleScroll;
                int step = ROW_HEIGHT + ROW_GAP;
                int index = localY / step;
                int within = localY % step;
                if (within < ROW_HEIGHT && index >= 0 && index < modules.size()) {
                    selectedIndex = index;
                    ensureSelectedVisible(modules);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_1 && modules.get(index) instanceof ToggleableModule t) toggle(modules.get(index).id(), t);
                    else if (button == GLFW.GLFW_MOUSE_BUTTON_2) { settingsId = modules.get(index).id(); editingSetting = null; }
                    return true;
                }
            }
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI mouse handling failed; ignoring click", exception);
            editingSearch = false; editingSetting = null; return true;
        }
    }

    private boolean handleSettingsClick(double mx, double my) {
        int w = Math.min(430, width - 36);
        int h = settingsHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; return true; }
        if (inside(mx, my, x + w - 90, y, 90, 34)) { settingsId = null; editingSetting = null; return true; }
        int row = y + 48;
        if (module instanceof ToggleableModule t && inside(mx, my, x, row, w, 30)) { toggle(settingsId, t); return true; }
        row += 38;
        if (settingsId.equals("aim-assist")) {
            if (inside(mx, my, x, row, w, 30)) { editingSetting = "range"; return true; }
            if (inside(mx, my, x, row + 38, w, 30)) { editingSetting = "smoothness"; return true; }
        } else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, x, row, w, 30)) editingSetting = "placements";
        return true;
    }
    private void toggle(String id, ToggleableModule t) {
        try {
            t.setEnabled(!t.enabled());
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException e) {
            OurClient.LOGGER.error("Failed to toggle '{}'", id, e);
            try { t.setEnabled(false); } catch (RuntimeException ignored) { }
        }
    }
    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        try {
            if (settingsId != null) {
                if (editingSetting != null) adjustSetting(v > 0 ? 1 : -1);
                return true;
            }
            List<ClientModule> modules = modulesForCategory();
            if (inside(mx, my, contentX(), TOP, contentWidth(), height - TOP - BOTTOM)) {
                double amount = Math.max(12.0, Math.abs(v) * 28.0);
                moduleScroll -= Math.signum(v) * amount;
                clampScroll(modules.size());
            }
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI scroll handling failed; ignoring scroll", exception); return true;
        }
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
        try {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (editingSearch || editingSetting != null) { editingSearch = false; editingSetting = null; return true; }
                if (settingsId != null) { settingsId = null; return true; }
                onClose(); return true;
            }
            if (settingsId != null) {
                if (editingSetting != null && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) adjustSetting(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
                return true;
            }
            if (editingSearch) {
                if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                    selectedIndex = 0; moduleScroll = 0;
                } else if (key == GLFW.GLFW_KEY_ENTER) editingSearch = false;
                return true;
            }
            List<ClientModule> modules = modulesForCategory();
            if (key == GLFW.GLFW_KEY_LEFT) { category = (category + CATEGORY_NAMES.length - 1) % CATEGORY_NAMES.length; selectedIndex = 0; moduleScroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_RIGHT) { category = (category + 1) % CATEGORY_NAMES.length; selectedIndex = 0; moduleScroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_UP && !modules.isEmpty()) { selectedIndex = (selectedIndex + modules.size() - 1) % modules.size(); ensureSelectedVisible(modules); return true; }
            if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) { selectedIndex = (selectedIndex + 1) % modules.size(); ensureSelectedVisible(modules); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_UP) { moduleScroll -= viewportHeight() * .8; clampScroll(modules.size()); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_DOWN) { moduleScroll += viewportHeight() * .8; clampScroll(modules.size()); return true; }
            if (key == GLFW.GLFW_KEY_HOME) { moduleScroll = 0; selectedIndex = 0; return true; }
            if (key == GLFW.GLFW_KEY_END && !modules.isEmpty()) { selectedIndex = modules.size() - 1; ensureSelectedVisible(modules); return true; }
            if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty()) { ClientModule m = modules.get(selectedIndex); if (m instanceof ToggleableModule t) toggle(m.id(), t); return true; }
            if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) { settingsId = modules.get(selectedIndex).id(); editingSetting = null; return true; }
            return true;
        } catch (RuntimeException exception) { OurClient.LOGGER.error("ClickGUI key handling failed; ignoring key", exception); return true; }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        try {
            if (editingSearch && !Character.isISOControl(event.codepoint())) {
                String chars = new String(Character.toChars(event.codepoint()));
                if (search.length() < 64) search += chars;
                selectedIndex = 0; moduleScroll = 0; return true;
            }
        } catch (RuntimeException exception) { OurClient.LOGGER.error("ClickGUI text input failed; ignoring character", exception); }
        return true;
    }
    @Override
    public void onClose() { editingSearch = false; editingSetting = null; settingsId = null; super.onClose(); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return w > 0 && h > 0 && mx >= x && mx < x + w && my >= y && my < y + h; }
    private static String pretty(String id) {
        String[] words = id.split("[-_]");
        StringBuilder out = new StringBuilder();
        for (String word : words) { if (!out.isEmpty()) out.append(' '); if (!word.isEmpty()) out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); }
        return out.toString();
    }
}
