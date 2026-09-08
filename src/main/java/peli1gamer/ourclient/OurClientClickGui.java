package peli1gamer.ourclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/** Native Arson GUI with self-contained input handling. */
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
    private static final int MARGIN = 14;
    private static final int HEADER = 48;
    private static final int CATEGORY_W = 116;
    private static final int CATEGORY_GAP = 4;
    private static final int TAB_H = 25;
    private static final int ROW_H = 27;
    private static final int ROW_GAP = 3;

    private int category;
    private int selectedIndex;
    private double scroll;
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

    private int contentTop() { return MARGIN + HEADER + 8; }
    private int contentBottom() { return height - MARGIN; }
    private int listLeft() { return MARGIN + CATEGORY_W + 8; }
    private int listRight() { return Math.max(listLeft() + 180, width * 2 / 3); }
    private int settingsLeft() { return listRight() + 8; }
    private int viewportTop() { return contentTop() + 32; }
    private int viewportHeight() { return Math.max(1, contentBottom() - 8 - viewportTop()); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        drawHeader(g);
        drawCategories(g, mouseX, mouseY);
        List<ClientModule> modules = modulesForCategory();
        clampScroll(modules.size());
        drawModulePanel(g, mouseX, mouseY, modules);
        drawSettingsPanel(g);
    }

    private void drawHeader(GuiGraphics g) {
        g.fill(MARGIN, MARGIN, width - MARGIN, MARGIN + HEADER, PANEL);
        g.fill(MARGIN, MARGIN, MARGIN + 4, MARGIN + HEADER, ACCENT);
        g.drawString(font, "ARSON CLIENT", MARGIN + 14, MARGIN + 10, TEXT, false);
        g.drawString(font, "Native module control", MARGIN + 14, MARGIN + 27, MUTED, false);
    }

    private void drawCategories(GuiGraphics g, int mouseX, int mouseY) {
        int x = MARGIN;
        int y = contentTop();
        int bottom = contentBottom();
        g.fill(x, y, x + CATEGORY_W, bottom, PANEL);
        g.drawString(font, "CATEGORIES", x + 10, y + 9, MUTED, false);

        int tabY = y + 27;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = i == category;
            boolean hover = inside(mouseX, mouseY, x + 6, tabY, CATEGORY_W - 12, TAB_H);
            g.fill(x + 6, tabY, x + CATEGORY_W - 6, tabY + TAB_H, active ? SELECTED : (hover ? HOVER : ROW));
            if (active) g.fill(x + 6, tabY, x + 9, tabY + TAB_H, ACCENT);
            g.drawString(font, CATEGORIES[i], x + 15, tabY + 8, active ? TEXT : MUTED, false);
            tabY += TAB_H + CATEGORY_GAP;
        }
    }

    private void drawModulePanel(GuiGraphics g, int mouseX, int mouseY, List<ClientModule> modules) {
        int left = listLeft();
        int top = contentTop();
        int right = listRight();
        int bottom = contentBottom();
        g.fill(left, top, right, bottom, PANEL);
        g.drawString(font, CATEGORIES[category], left + 10, top + 9, TEXT, false);
        g.drawString(font, "Scroll • Enter toggle • Right-click settings", left + 10, top + 24, MUTED, false);

        int viewportBottom = bottom - 8;
        int viewport = viewportHeight();
        if (modules.isEmpty()) {
            g.drawString(font, "No modules", left + 10, viewportTop() + 10, MUTED, false);
            return;
        }

        int step = ROW_H + ROW_GAP;
        int first = Math.max(0, (int) Math.floor(scroll / step) - 1);
        int last = Math.min(modules.size() - 1, (int) Math.ceil((scroll + viewport) / step));
        for (int i = first; i <= last; i++) {
            int rowY = viewportTop() + i * step - (int) scroll;
            if (rowY + ROW_H < viewportTop() || rowY > viewportBottom) continue;
            ClientModule module = modules.get(i);
            boolean active = module instanceof ToggleableModule t && t.enabled();
            boolean selected = i == selectedIndex;
            boolean hover = inside(mouseX, mouseY, left + 7, rowY, right - left - 19, ROW_H);
            g.fill(left + 7, rowY, right - 12, rowY + ROW_H, selected ? SELECTED : (hover ? HOVER : ROW));
            if (active) g.fill(left + 7, rowY, left + 10, rowY + ROW_H, ACCENT);
            g.drawString(font, pretty(module.id()), left + 17, rowY + 7, TEXT, false);
            g.drawString(font, active ? "ON" : (module instanceof ToggleableModule ? "OFF" : "INFO"), right - 48, rowY + 7, active ? ACCENT : MUTED, false);
        }

        int max = maxScroll(modules.size());
        if (max > 0) {
            int trackX = right - 7;
            int trackTop = viewportTop();
            int trackBottom = viewportBottom;
            int trackH = Math.max(1, trackBottom - trackTop);
            int thumbH = Math.max(22, (int) ((double) viewport / totalContentHeight(modules.size()) * trackH));
            int thumbY = trackTop + (int) (scroll / max * Math.max(0, trackH - thumbH));
            g.fill(trackX, trackTop, trackX + 3, trackBottom, ROW);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
        }
    }

    private void drawSettingsPanel(GuiGraphics g) {
        int left = settingsLeft();
        int top = contentTop();
        int right = width - MARGIN;
        int bottom = contentBottom();
        g.fill(left, top, right, bottom, PANEL);
        g.fill(left, top, left + 3, bottom, ACCENT);

        if (settingsId == null) {
            g.drawString(font, "Select a module", left + 14, top + 14, TEXT, false);
            g.drawString(font, "Right-click a module", left + 14, top + 35, MUTED, false);
            return;
        }

        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; editingSetting = null; return; }
        g.drawString(font, pretty(settingsId), left + 14, top + 14, TEXT, false);
        g.drawString(font, "ESC closes", right - 62, top + 14, MUTED, false);

        int row = top + 43;
        if (module instanceof ToggleableModule t) {
            setting(g, left, right, row, "Enabled", t.enabled() ? "ON" : "OFF", false);
            row += 36;
        }
        ClientConfig c = OurClient.config();
        if (c != null && settingsId.equals("aim-assist")) {
            setting(g, left, right, row, "Range", String.format(Locale.ROOT, "%.1f", c.aimRange), "range".equals(editingSetting));
            row += 36;
            setting(g, left, right, row, "Smoothness", String.format(Locale.ROOT, "%.2f", c.aimSmoothing), "smoothness".equals(editingSetting));
            g.drawString(font, "Wheel or ←/→", left + 14, row + 38, MUTED, false);
        } else if (c != null && settingsId.equals("auto-schematic-builder")) {
            setting(g, left, right, row, "Placements / tick", Integer.toString(c.schematicPlacementsPerTick), "placements".equals(editingSetting));
            g.drawString(font, "Wheel or ←/→", left + 14, row + 38, MUTED, false);
        } else {
            g.drawString(font, "No adjustable settings", left + 14, row + 8, MUTED, false);
        }
    }

    private void setting(GuiGraphics g, int left, int right, int y, String label, String value, boolean selected) {
        g.drawString(font, label, left + 14, y + 8, TEXT, false);
        int vx = Math.max(left + 95, right - 125);
        g.fill(vx, y, right - 12, y + 26, selected ? HOVER : ROW);
        g.drawString(font, value, vx + 8, y + 8, TEXT, false);
    }

    private List<ClientModule> modulesForCategory() {
        List<ClientModule> result = new ArrayList<>();
        for (ClientModule module : OurClient.modules().all()) {
            if (categoryFor(module) == category) result.add(module);
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
            int catX = MARGIN + 6;
            int catY = contentTop() + 27;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (inside(mx, my, catX, catY, CATEGORY_W - 12, TAB_H)) {
                    category = i;
                    selectedIndex = 0;
                    scroll = 0;
                    settingsId = null;
                    editingSetting = null;
                    return true;
                }
                catY += TAB_H + CATEGORY_GAP;
            }

            if (inside(mx, my, listLeft(), viewportTop(), listRight() - listLeft(), viewportHeight())) {
                List<ClientModule> modules = modulesForCategory();
                int offset = (int) my - viewportTop() + (int) scroll;
                int index = offset / (ROW_H + ROW_GAP);
                int within = offset % (ROW_H + ROW_GAP);
                if (within < ROW_H && index >= 0 && index < modules.size()) {
                    select(index, modules);
                    ClientModule module = modules.get(index);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                        settingsId = module.id();
                        editingSetting = null;
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule t) {
                        toggle(module.id(), t);
                    }
                }
                return true;
            }

            if (settingsId != null) handleSettingsClick(mx, my);
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI mouse handling failed; ignoring click", exception);
            editingSetting = null;
            return true;
        }
    }

    private void handleSettingsClick(double mx, double my) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) { settingsId = null; return; }
        int left = settingsLeft(), right = width - MARGIN, row = contentTop() + 43;
        if (module instanceof ToggleableModule t) {
            if (inside(mx, my, left, row, right - left, 26)) { toggle(settingsId, t); return; }
            row += 36;
        }
        if (settingsId.equals("aim-assist")) {
            if (inside(mx, my, left, row, right - left, 26)) { editingSetting = "range"; return; }
            if (inside(mx, my, left, row + 36, right - left, 26)) { editingSetting = "smoothness"; return; }
        } else if (settingsId.equals("auto-schematic-builder") && inside(mx, my, left, row, right - left, 26)) {
            editingSetting = "placements";
        }
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
            if (inside(mx, my, listLeft(), viewportTop(), listRight() - listLeft(), viewportHeight())) {
                List<ClientModule> modules = modulesForCategory();
                scroll -= Math.signum(vertical) * Math.max(10, Math.abs(vertical) * 24);
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
                if (editingSetting != null) { editingSetting = null; return true; }
                if (settingsId != null) { settingsId = null; return true; }
                onClose();
                return true;
            }
            if (settingsId != null && editingSetting != null && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
                adjustSetting(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
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
            if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty() && modules.get(selectedIndex) instanceof ToggleableModule t) {
                toggle(modules.get(selectedIndex).id(), t);
                return true;
            }
            if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) { settingsId = modules.get(selectedIndex).id(); editingSetting = null; return true; }
            return true;
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("ClickGUI key handling failed; ignoring key", exception);
            return true;
        }
    }

    @Override
    public void onClose() {
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
