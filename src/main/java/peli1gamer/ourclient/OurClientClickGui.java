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

/**
 * Native Arson GUI. The screen owns no Minecraft widget children: all hit testing,
 * selection and scrolling are handled locally so vanilla focus/navigation cannot
 * receive a partially handled click event.
 */
public final class OurClientClickGui extends Screen {
    private static final int BG = 0xF2090910;
    private static final int PANEL = 0xF2161720;
    private static final int ROW = 0xFF20212B;
    private static final int HOVER = 0xFF2B2D39;
    private static final int SELECTED = 0xFF353746;
    private static final int ACCENT = 0xFFFF6A00;
    private static final int TEXT = 0xFFECE8F5;
    private static final int MUTED = 0xFFAAA5B7;
    private static final String[] CATEGORIES = {"COMBAT", "MOVEMENT", "RENDER", "WORLD", "PLAYER", "UTILITY"};

    private static final int MARGIN = 16;
    private static final int HEADER = 62;
    private static final int TAB_H = 28;
    private static final int ROW_H = 38;
    private static final int ROW_GAP = 5;

    private int category;
    private int selectedIndex;
    private double scroll;
    private String search = "";
    private boolean searchEditing;
    private String settingsId;
    private String editingSetting;

    public OurClientClickGui() {
        super(Component.literal("Arson Client"));
    }

    @Override
    protected void init() {
        clearFocus();
        clampScroll();
    }

    private int tabLeft() { return MARGIN; }
    private int tabWidth() { return Math.max(70, (width - MARGIN * 2 - 5 * 6) / 6); }
    private int contentTop() { return MARGIN + HEADER + TAB_H + 10; }
    private int contentBottom() { return height - MARGIN; }
    private int contentHeight() { return Math.max(1, contentBottom() - contentTop()); }
    private int listLeft() { return MARGIN; }
    private int listRight() { return Math.max(listLeft() + 160, width * 2 / 3 - 8); }
    private int listWidth() { return listRight() - listLeft(); }
    private int settingsLeft() { return listRight() + 10; }
    private int settingsWidth() { return Math.max(160, width - settingsLeft() - MARGIN); }
    private int searchY() { return MARGIN + 30; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        drawHeader(g, mouseX, mouseY);
        drawTabs(g, mouseX, mouseY);

        List<ClientModule> modules = modulesForCategory();
        clampScroll(modules.size());
        drawModulePanel(g, mouseX, mouseY, modules);
        drawSettingsPanel(g, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(MARGIN, MARGIN, width - MARGIN, MARGIN + HEADER, PANEL);
        g.fill(MARGIN, MARGIN, MARGIN + 5, MARGIN + HEADER, ACCENT);
        g.drawString(font, "ARSON CLIENT", MARGIN + 16, MARGIN + 10, TEXT, false);
        g.drawString(font, "Stable native module control", MARGIN + 16, MARGIN + 27, MUTED, false);

        int sx = width - MARGIN - 270;
        int sw = 250;
        boolean hover = inside(mouseX, mouseY, sx, searchY(), sw, 28);
        g.fill(sx, searchY(), sx + sw, searchY() + 28, searchEditing || hover ? HOVER : ROW);
        String text = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, text + (searchEditing ? "_" : ""), sx + 9, searchY() + 9, search.isEmpty() ? MUTED : TEXT, false);
    }

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
        int w = tabWidth();
        for (int i = 0; i < CATEGORIES.length; i++) {
            int x = tabLeft() + i * (w + 5);
            int y = MARGIN + HEADER;
            boolean active = i == category;
            boolean hover = inside(mouseX, mouseY, x, y, w, TAB_H);
            g.fill(x, y, x + w, y + TAB_H, active ? SELECTED : (hover ? HOVER : PANEL));
            if (active) g.fill(x, y, x + w, y + 3, ACCENT);
            g.drawString(font, CATEGORIES[i], x + 9, y + 9, active ? TEXT : MUTED, false);
        }
    }

    private void drawModulePanel(GuiGraphics g, int mouseX, int mouseY, List<ClientModule> modules) {
        int left = listLeft();
        int top = contentTop();
        int right = listRight();
        int bottom = contentBottom();
        g.fill(left, top, right, bottom, PANEL);
        g.drawString(font, CATEGORIES[category] + " MODULES", left + 12, top + 10, TEXT, false);
        g.drawString(font, "Wheel to scroll  •  Enter to toggle  •  Right-click for settings", left + 12, top + 27, MUTED, false);

        int viewportTop = top + 42;
        int viewportBottom = bottom - 8;
        int viewport = Math.max(1, viewportBottom - viewportTop);
        if (modules.isEmpty()) {
            g.drawString(font, "No matching modules", left + 12, viewportTop + 12, MUTED, false);
            return;
        }

        int step = ROW_H + ROW_GAP;
        int first = Math.max(0, (int) Math.floor(scroll / step) - 1);
        int last = Math.min(modules.size() - 1, (int) Math.ceil((scroll + viewport) / step));
        for (int i = first; i <= last; i++) {
            int y = viewportTop + i * step - (int) scroll;
            if (y + ROW_H < viewportTop || y > viewportBottom) continue;
            ClientModule module = modules.get(i);
            boolean selected = i == selectedIndex;
            boolean hover = inside(mouseX, mouseY, left + 8, y, right - left - 22, ROW_H);
            boolean active = module instanceof ToggleableModule t && t.enabled();
            g.fill(left + 8, y, right - 14, y + ROW_H, selected ? SELECTED : (hover ? HOVER : ROW));
            g.fill(left + 8, y, left + 11, y + ROW_H, active ? ACCENT : 0x00000000);
            g.drawString(font, pretty(module.id()), left + 19, y + 8, TEXT, false);
            g.drawString(font, active ? "ON" : (module instanceof ToggleableModule ? "OFF" : "INFO"), right - 54, y + 8, active ? ACCENT : MUTED, false);
        }

        int max = maxScroll(modules.size());
        if (max > 0) {
            int trackX = right - 8;
            int trackTop = viewportTop;
            int trackBottom = viewportBottom;
            int trackH = Math.max(1, trackBottom - trackTop);
            int thumbH = Math.max(24, (int) ((double) viewport / totalContentHeight(modules.size()) * trackH));
            int thumbY = trackTop + (int) (scroll / max * Math.max(0, trackH - thumbH));
            g.fill(trackX, trackTop, trackX + 3, trackBottom, ROW);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
        }
    }

    private void drawSettingsPanel(GuiGraphics g, int mouseX, int mouseY) {
        int left = settingsLeft();
        int top = contentTop();
        int right = width - MARGIN;
        int bottom = contentBottom();
        g.fill(left, top, right, bottom, PANEL);
        g.fill(left, top, left + 3, bottom, ACCENT);

        if (settingsId == null) {
            g.drawString(font, "MODULE SETTINGS", left + 14, top + 14, TEXT, false);
            g.drawString(font, "Select a module and right-click it", left + 14, top + 36, MUTED, false);
            g.drawString(font, "or press O to open its settings.", left + 14, top + 50, MUTED, false);
            g.drawString(font, "All input stays inside Arson's GUI.", left + 14, top + 82, MUTED, false);
            return;
        }

        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; editingSetting = null; return; }
        g.drawString(font, pretty(settingsId), left + 14, top + 14, TEXT, false);
        g.drawString(font, "SETTINGS", left + 14, top + 31, MUTED, false);
        g.drawString(font, "ESC closes settings", right - 105, top + 14, MUTED, false);

        int row = top + 56;
        if (module instanceof ToggleableModule t) {
            setting(g, left, right, row, "Enabled", t.enabled() ? "ON" : "OFF", false);
            row += 42;
        }
        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, left, right, row, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange), "range".equals(editingSetting));
            row += 42;
            setting(g, left, right, row, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing), "smoothness".equals(editingSetting));
            row += 42;
            g.drawString(font, "Click a value, then use wheel or ←/→", left + 14, row + 4, MUTED, false);
        } else if (c != null && settingsId.equals("auto-schematic-builder")) {
            setting(g, left, right, row, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick), "placements".equals(editingSetting));
            g.drawString(font, "Click the value, then use wheel or ←/→", left + 14, row + 46, MUTED, false);
        } else {
            g.drawString(font, "No adjustable settings wired yet.", left + 14, row + 8, MUTED, false);
        }
    }

    private void setting(GuiGraphics g, int left, int right, int y, String label, String value, boolean selected) {
        g.drawString(font, label, left + 14, y + 9, TEXT, false);
        int vx = Math.max(left + 100, right - 145);
        g.fill(vx, y, right - 14, y + 30, selected ? HOVER : ROW);
        g.drawString(font, value, vx + 9, y + 10, TEXT, false);
    }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        for (ClientModule module : OurClient.modules().all()) {
            if (categoryFor(module) != category) continue;
            if (!query.isEmpty() && !pretty(module.id()).toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        selectedIndex = result.isEmpty() ? 0 : Math.min(selectedIndex, result.size() - 1);
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

    private int viewportTop() { return contentTop() + 42; }
    private int viewportHeight() { return Math.max(1, contentBottom() - 8 - viewportTop()); }
    private int totalContentHeight(int count) { return count <= 0 ? 0 : count * ROW_H + (count - 1) * ROW_GAP; }
    private int maxScroll(int count) { return Math.max(0, totalContentHeight(count) - viewportHeight()); }
    private void clampScroll() { clampScroll(modulesForCategory().size()); }
    private void clampScroll(int count) { scroll = Math.max(0, Math.min(scroll, maxScroll(count))); }

    private void select(int index, List<ClientModule> modules) {
        if (modules.isEmpty()) { selectedIndex = 0; return; }
        selectedIndex = Math.max(0, Math.min(index, modules.size() - 1));
        int step = ROW_H + ROW_GAP;
        int rowTop = selectedIndex * step;
        int rowBottom = rowTop + ROW_H;
        if (rowTop < scroll) scroll = rowTop;
        else if (rowBottom > scroll + viewportHeight()) scroll = rowBottom - viewportHeight();
        clampScroll(modules.size());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        try {
            double mx = event.x(), my = event.y();
            int button = event.button();
            int tabW = tabWidth();
            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = tabLeft() + i * (tabW + 5);
                if (inside(mx, my, x, MARGIN + HEADER, tabW, TAB_H)) {
                    category = i; selectedIndex = 0; scroll = 0; settingsId = null; editingSetting = null; searchEditing = false; return true;
                }
            }
            int sx = width - MARGIN - 270;
            if (inside(mx, my, sx, searchY(), 250, 28)) { searchEditing = true; return true; }
            if (inside(mx, my, listLeft(), viewportTop(), listWidth(), viewportHeight())) {
                List<ClientModule> modules = modulesForCategory();
                int index = ((int) my - viewportTop() + (int) scroll) / (ROW_H + ROW_GAP);
                int within = ((int) my - viewportTop() + (int) scroll) % (ROW_H + ROW_GAP);
                if (within < ROW_H && index >= 0 && index < modules.size()) {
                    select(index, modules);
                    ClientModule module = modules.get(index);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_2) { settingsId = module.id(); editingSetting = null; }
                    else if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule t) toggle(module.id(), t);
                }
                return true;
            }
            if (settingsId != null) handleSettingsClick(mx, my);
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI mouse handling failed; ignoring click", exception);
            searchEditing = false;
            editingSetting = null;
            return true;
        }
    }

    private void handleSettingsClick(double mx, double my) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; return; }
        int left = settingsLeft(), right = width - MARGIN, row = contentTop() + 56;
        if (module instanceof ToggleableModule t) {
            if (inside(mx, my, left, row, right - left, 30)) { toggle(settingsId, t); return; }
            row += 42;
        }
        if (settingsId.equals("aim-assist")) {
            if (inside(mx, my, left, row, right - left, 30)) { editingSetting = "range"; return; }
            if (inside(mx, my, left, row + 42, right - left, 30)) { editingSetting = "smoothness"; return; }
        } else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, left, row, right - left, 30)) editingSetting = "placements";
    }

    private void toggle(String id, ToggleableModule module) {
        try {
            module.setEnabled(!module.enabled());
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Failed to toggle '{}'", id, exception);
            try { module.setEnabled(false); } catch (RuntimeException ignored) { }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        try {
            if (settingsId != null && editingSetting != null) {
                adjustSetting(vertical > 0 ? 1 : -1);
                return true;
            }
            if (inside(mx, my, listLeft(), viewportTop(), listWidth(), viewportHeight())) {
                List<ClientModule> modules = modulesForCategory();
                scroll -= Math.signum(vertical) * Math.max(14, Math.abs(vertical) * 32);
                clampScroll(modules.size());
            }
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI scroll handling failed; ignoring scroll", exception);
            return true;
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
                if (searchEditing || editingSetting != null) { searchEditing = false; editingSetting = null; return true; }
                if (settingsId != null) { settingsId = null; return true; }
                onClose();
                return true;
            }
            if (settingsId != null && editingSetting != null && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
                adjustSetting(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1); return true;
            }
            if (searchEditing) {
                if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!search.isEmpty()) search = search.substring(0, search.length() - 1); selectedIndex = 0; scroll = 0; }
                else if (key == GLFW.GLFW_KEY_ENTER) searchEditing = false;
                return true;
            }
            List<ClientModule> modules = modulesForCategory();
            if (key == GLFW.GLFW_KEY_LEFT) { category = (category + CATEGORIES.length - 1) % CATEGORIES.length; selectedIndex = 0; scroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_RIGHT) { category = (category + 1) % CATEGORIES.length; selectedIndex = 0; scroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_UP && !modules.isEmpty()) { select(selectedIndex - 1, modules); return true; }
            if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) { select(selectedIndex + 1, modules); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_UP) { scroll -= viewportHeight() * .8; clampScroll(modules.size()); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_DOWN) { scroll += viewportHeight() * .8; clampScroll(modules.size()); return true; }
            if (key == GLFW.GLFW_KEY_HOME) { selectedIndex = 0; scroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_END && !modules.isEmpty()) { select(modules.size() - 1, modules); return true; }
            if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty() && modules.get(selectedIndex) instanceof ToggleableModule t) { toggle(modules.get(selectedIndex).id(), t); return true; }
            if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) { settingsId = modules.get(selectedIndex).id(); editingSetting = null; return true; }
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI key handling failed; ignoring key", exception);
            return true;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        try {
            if (searchEditing && !Character.isISOControl(event.codepoint())) {
                String chars = new String(Character.toChars(event.codepoint()));
                if (search.length() < 64) search += chars;
                selectedIndex = 0; scroll = 0;
                return true;
            }
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI text input failed; ignoring character", exception);
        }
        return true;
    }

    @Override
    public void onClose() {
        searchEditing = false;
        editingSetting = null;
        settingsId = null;
        super.onClose();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return w > 0 && h > 0 && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String pretty(String id) {
        String[] words = id.split("[-_]");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) out.append(' ');
            if (!word.isEmpty()) out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
