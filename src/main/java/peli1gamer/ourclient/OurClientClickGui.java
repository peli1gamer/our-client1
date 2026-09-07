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

/** Responsive ClickGUI backed by the real client modules. */
public final class OurClientClickGui extends Screen {
    private static final int PANEL_WIDTH = 250;
    private static final int TITLE_HEIGHT = 28;
    private static final int ROW_HEIGHT = 25;
    private static final int GAP = 12;
    private static final int BG = 0xC8141420;
    private static final int PANEL = 0xD9141422;
    private static final int ROW = 0x80202030;
    private static final int HOVER = 0xA02B2538;
    private static final int ACCENT = 0xFF9B7BFF;
    private static final int ACCENT_DARK = 0xFF5E3DB8;
    private static final int TEXT = 0xFFE6E1F2;
    private static final int MUTED = 0xFFAAA4B8;

    private String search = "";
    private String selectedModuleId;
    private boolean editingSearch;
    private String editingSetting;
    private double scrollOffset;

    public OurClientClickGui() {
        super(Component.literal("Our Client"));
    }

    @Override
    protected void init() {
        selectedModuleId = null;
        editingSearch = false;
        editingSetting = null;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BG);

        int searchWidth = Math.min(310, Math.max(160, width - 40));
        int searchX = (width - searchWidth) / 2;
        drawPanel(graphics, searchX, 14, searchWidth, 70, "SEARCH");
        String value = search.isEmpty() ? "Search modules..." : search;
        graphics.drawString(font, value + (editingSearch ? "_" : ""), searchX + 14, 48,
                search.isEmpty() ? MUTED : TEXT, false);

        List<Category> categories = categories();
        Layout layout = layout(categories.size());
        scrollOffset = clampScroll(layout, height);

        graphics.enableScissor(0, 94, width, Math.max(95, height - 24));
        for (int i = 0; i < categories.size(); i++) {
            int x = layout.startX + (i % layout.columns) * (layout.panelWidth + GAP);
            int y = (int) (100 + (i / layout.columns) * layout.rowHeight - scrollOffset);
            renderCategory(graphics, categories.get(i), x, y, layout.panelWidth, mouseX, mouseY);
        }
        graphics.disableScissor();

        if (selectedModuleId != null) renderSettings(graphics);
        String footer = "OUR CLIENT  |  Click toggle  |  ... settings  |  Scroll  |  ESC close";
        graphics.drawString(font, footer, Math.max(8, (width - font.width(footer)) / 2), height - 18, MUTED, false);
    }

    private void renderCategory(GuiGraphics graphics, Category category, int x, int y, int panelWidth, int mouseX, int mouseY) {
        List<ClientModule> modules = filtered(category.modules());
        int panelHeight = TITLE_HEIGHT + Math.max(1, modules.size()) * ROW_HEIGHT + 8;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        graphics.fill(x, y, x + panelWidth, y + 2, ACCENT);
        drawCentered(graphics, category.name(), x, y + 9, panelWidth, TEXT);
        graphics.drawString(font, "-", x + panelWidth - 17, y + 9, ACCENT, false);

        if (modules.isEmpty()) {
            drawCentered(graphics, "No modules", x, y + TITLE_HEIGHT + 7, panelWidth, MUTED);
            return;
        }
        for (int i = 0; i < modules.size(); i++) {
            ClientModule module = modules.get(i);
            int rowY = y + TITLE_HEIGHT + i * ROW_HEIGHT;
            boolean hovered = inside(mouseX, mouseY, x + 4, rowY, panelWidth - 8, ROW_HEIGHT);
            boolean enabled = module instanceof ToggleableModule toggleable && toggleable.enabled();
            int color = enabled ? ACCENT_DARK : (hovered ? HOVER : ROW);
            graphics.fill(x + 4, rowY, x + panelWidth - 4, rowY + ROW_HEIGHT - 2, color);
            if (enabled) graphics.fill(x + 4, rowY, x + 7, rowY + ROW_HEIGHT - 2, ACCENT);
            graphics.drawString(font, pretty(module.id()), x + 13, rowY + 7, TEXT, false);
            graphics.drawString(font, "...", x + panelWidth - 28, rowY + 7, enabled ? TEXT : MUTED, false);
        }
    }

    private void renderSettings(GuiGraphics graphics) {
        ClientModule module = OurClient.modules().get(selectedModuleId);
        if (module == null) {
            selectedModuleId = null;
            editingSetting = null;
            return;
        }
        int panelWidth = Math.min(410, Math.max(180, width - 40));
        int panelHeight = selectedModuleId.equals("aim-assist") ? 210
                : selectedModuleId.equals("auto-schematic-builder") ? 190 : 145;
        int x = (width - panelWidth) / 2;
        int y = Math.max(95, height - panelHeight - 42);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xEA171421);
        graphics.fill(x, y, x + panelWidth, y + 2, ACCENT);
        graphics.drawString(font, pretty(selectedModuleId) + " Settings", x + 18, y + 16, TEXT, false);
        graphics.drawString(font, "X", x + panelWidth - 22, y + 16, ACCENT, false);

        int rowY = y + 48;
        if (module instanceof ToggleableModule toggleable) {
            settingRow(graphics, "Enabled", toggleable.enabled() ? "ON" : "OFF", x, rowY, panelWidth, toggleable.enabled());
            rowY += 38;
        }
        ClientConfig config = OurClient.config();
        if (config != null && selectedModuleId.equals("aim-assist")) {
            settingRow(graphics, "Range", String.format(Locale.ROOT, "%.1f", config.aimRange), x, rowY, panelWidth, "range".equals(editingSetting));
            rowY += 38;
            settingRow(graphics, "Smoothness", String.format(Locale.ROOT, "%.2f", config.aimSmoothing), x, rowY, panelWidth, "smoothness".equals(editingSetting));
            rowY += 38;
            graphics.drawString(font, editingSetting == null ? "Click a value, then use wheel / arrows" : "Wheel / arrows to adjust", x + 18, rowY + 8, MUTED, false);
        } else if (config != null && selectedModuleId.equals("auto-schematic-builder")) {
            settingRow(graphics, "Placements / tick", Integer.toString(config.schematicPlacementsPerTick), x, rowY, panelWidth, "placements".equals(editingSetting));
            graphics.drawString(font, editingSetting == null ? "Click the value, then use wheel / arrows" : "Wheel / arrows to adjust", x + 18, rowY + 50, MUTED, false);
        } else {
            graphics.drawString(font, "More settings will appear as the module grows.", x + 18, rowY + 10, MUTED, false);
        }
    }

    private void settingRow(GuiGraphics graphics, String label, String value, int x, int y, int panelWidth, boolean active) {
        graphics.drawString(font, label, x + 18, y + 9, TEXT, false);
        int valueWidth = Math.min(130, panelWidth / 3);
        int valueX = x + panelWidth - valueWidth - 18;
        graphics.fill(valueX, y, x + panelWidth - 18, y + 28, active ? ACCENT_DARK : ROW);
        drawCentered(graphics, value, valueX, y + 9, panelWidth - (valueX - x) - 18, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int searchWidth = Math.min(310, Math.max(160, width - 40));
        int searchX = (width - searchWidth) / 2;
        if (inside(mouseX, mouseY, searchX, 14, searchWidth, 70)) {
            editingSearch = true;
            editingSetting = null;
            return true;
        }
        if (selectedModuleId != null && handleSettingsClick(mouseX, mouseY)) return true;

        List<Category> categories = categories();
        Layout layout = layout(categories.size());
        for (int i = 0; i < categories.size(); i++) {
            int x = layout.startX + (i % layout.columns) * (layout.panelWidth + GAP);
            int y = (int) (100 + (i / layout.columns) * layout.rowHeight - scrollOffset);
            List<ClientModule> modules = filtered(categories.get(i).modules());
            for (int row = 0; row < modules.size(); row++) {
                int rowY = y + TITLE_HEIGHT + row * ROW_HEIGHT;
                if (!inside(mouseX, mouseY, x + 4, rowY, layout.panelWidth - 8, ROW_HEIGHT)) continue;
                ClientModule module = modules.get(row);
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseX >= x + layout.panelWidth - 44) {
                    selectedModuleId = module.id();
                    editingSearch = false;
                    editingSetting = null;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule toggleable) {
                    toggle(module.id(), toggleable);
                }
                return true;
            }
        }
        editingSearch = false;
        editingSetting = null;
        return super.mouseClicked(event, doubled);
    }

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        int panelWidth = Math.min(410, Math.max(180, width - 40));
        int panelHeight = selectedModuleId.equals("aim-assist") ? 210
                : selectedModuleId.equals("auto-schematic-builder") ? 190 : 145;
        int x = (width - panelWidth) / 2;
        int y = Math.max(95, height - panelHeight - 42);
        if (inside(mouseX, mouseY, x + panelWidth - 45, y, 45, 38)) {
            selectedModuleId = null;
            editingSetting = null;
            return true;
        }
        ClientModule module = OurClient.modules().get(selectedModuleId);
        if (module instanceof ToggleableModule toggleable && inside(mouseX, mouseY, x, y + 42, panelWidth, 34)) {
            toggle(selectedModuleId, toggleable);
            return true;
        }
        if (selectedModuleId.equals("aim-assist")) {
            if (inside(mouseX, mouseY, x, y + 80, panelWidth, 34)) {
                editingSetting = "range";
                return true;
            }
            if (inside(mouseX, mouseY, x, y + 118, panelWidth, 34)) {
                editingSetting = "smoothness";
                return true;
            }
        } else if (selectedModuleId.equals("auto-schematic-builder") && inside(mouseX, mouseY, x, y + 80, panelWidth, 34)) {
            editingSetting = "placements";
            return true;
        }
        return false;
    }

    private void toggle(String id, ToggleableModule toggleable) {
        try {
            toggleable.setEnabled(!toggleable.enabled());
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Failed to toggle '{}' from ClickGUI", id, exception);
            try { toggleable.setEnabled(false); } catch (RuntimeException ignored) { }
            try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException ignored) { }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editingSetting != null && OurClient.config() != null) {
            adjustSetting(verticalAmount > 0 ? 1 : -1);
            return true;
        }
        if (mouseY >= 94) {
            scrollOffset -= verticalAmount * 24.0;
            scrollOffset = clampScroll(layout(categories().size()), height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void adjustSetting(int direction) {
        ClientConfig config = OurClient.config();
        if (config == null || editingSetting == null) return;
        switch (editingSetting) {
            case "range" -> config.aimRange = Math.max(1.0f, Math.min(64.0f, config.aimRange + direction));
            case "smoothness" -> config.aimSmoothing = Math.max(0.01f, Math.min(1.0f, config.aimSmoothing + direction * 0.01f));
            case "placements" -> config.schematicPlacementsPerTick = Math.max(1, Math.min(20, config.schematicPlacementsPerTick + direction));
            default -> { return; }
        }
        OurClient.syncAndSaveConfigFromModules();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editingSearch || editingSetting != null) {
                editingSearch = false;
                editingSetting = null;
                return true;
            }
            onClose();
            return true;
        }
        if (editingSearch && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        if (editingSetting != null && OurClient.config() != null
                && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            adjustSetting(keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            scrollOffset = 0;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editingSearch && !Character.isISOControl(event.codepoint())) {
            search += new String(Character.toChars(event.codepoint()));
            return true;
        }
        return super.charTyped(event);
    }

    private List<ClientModule> filtered(List<ClientModule> modules) {
        if (search.isBlank()) return modules;
        String needle = search.toLowerCase(Locale.ROOT);
        return modules.stream().filter(module -> pretty(module.id()).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    private List<Category> categories() {
        List<ClientModule> combat = new ArrayList<>();
        List<ClientModule> visual = new ArrayList<>();
        List<ClientModule> movement = new ArrayList<>();
        List<ClientModule> world = new ArrayList<>();
        List<ClientModule> client = new ArrayList<>();
        for (ClientModule module : OurClient.modules().all()) {
            switch (module.id()) {
                case "aim-assist" -> combat.add(module);
                case "tracers" -> visual.add(module);
                case "freecam" -> movement.add(module);
                case "auto-schematic-builder", "saved-bases" -> world.add(module);
                default -> client.add(module);
            }
        }
        List<Category> result = new ArrayList<>();
        if (!combat.isEmpty()) result.add(new Category("COMBAT", combat));
        if (!visual.isEmpty()) result.add(new Category("VISUAL", visual));
        if (!movement.isEmpty()) result.add(new Category("MOVEMENT", movement));
        if (!world.isEmpty()) result.add(new Category("WORLD", world));
        if (!client.isEmpty()) result.add(new Category("CLIENT", client));
        return result;
    }

    private Layout layout(int categoryCount) {
        if (categoryCount <= 0) return new Layout(1, 145, GAP, 0);
        int columns = Math.max(1, Math.min(4, (width + GAP) / (PANEL_WIDTH + GAP)));
        columns = Math.min(columns, categoryCount);
        int available = Math.max(145, width - GAP * (columns + 1));
        int panelWidth = Math.min(PANEL_WIDTH, available / columns);
        int rowHeight = 100 + categoryHeight() + GAP;
        int totalWidth = columns * panelWidth + (columns - 1) * GAP;
        int startX = Math.max(GAP, (width - totalWidth) / 2);
        return new Layout(columns, panelWidth, startX, rowHeight);
    }

    private int categoryHeight() {
        int max = 1;
        for (Category category : categories()) max = Math.max(max, filtered(category.modules()).size());
        return TITLE_HEIGHT + max * ROW_HEIGHT + 8;
    }

    private double clampScroll(Layout layout, int screenHeight) {
        int rows = (categories().size() + layout.columns - 1) / layout.columns;
        double contentBottom = 100 + Math.max(0, rows - 1) * layout.rowHeight + categoryHeight();
        double max = Math.max(0, contentBottom - (screenHeight - 24));
        return Math.max(0, Math.min(max, scrollOffset));
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int panelWidth, int panelHeight, String title) {
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        graphics.fill(x, y, x + panelWidth, y + 2, ACCENT);
        drawCentered(graphics, title, x, y + 10, panelWidth, TEXT);
        graphics.drawString(font, "-", x + panelWidth - 18, y + 10, ACCENT, false);
    }

    private void drawCentered(GuiGraphics graphics, String value, int x, int y, int boxWidth, int color) {
        graphics.drawString(font, value, x + (boxWidth - font.width(value)) / 2, y, color, false);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static String pretty(String id) {
        String[] words = id.split("[-_]");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private record Category(String name, List<ClientModule> modules) {}
    private record Layout(int columns, int panelWidth, int startX, int rowHeight) {}
}
