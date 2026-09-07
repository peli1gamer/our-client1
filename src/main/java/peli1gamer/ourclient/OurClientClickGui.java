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

/** Screenshot-inspired floating ClickGUI backed by the real client modules. */
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
    private boolean editingNumber;

    public OurClientClickGui() { super(Component.literal("Our Client")); }

    @Override
    protected void init() {
        selectedModuleId = null;
        editingSearch = false;
        editingNumber = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BG);
        int searchWidth = Math.min(310, Math.max(160, width - 40));
        int searchX = (width - searchWidth) / 2;
        drawPanel(graphics, searchX, 14, searchWidth, 70, "SEARCH");
        String placeholder = search.isEmpty() ? "Search modules..." : search;
        graphics.drawString(font, placeholder + (editingSearch ? "_" : ""), searchX + 14, 48,
                search.isEmpty() ? MUTED : TEXT, false);

        List<Category> categories = categories();
        int columns = Math.max(1, Math.min(5, categories.size()));
        int available = width - GAP * (columns + 1);
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(145, available / columns));
        int totalWidth = columns * panelWidth + (columns - 1) * GAP;
        int startX = Math.max(GAP, (width - totalWidth) / 2);

        for (int i = 0; i < categories.size(); i++) {
            int x = startX + (i % columns) * (panelWidth + GAP);
            int y = 100 + (i / columns) * 260;
            renderCategory(graphics, categories.get(i), x, y, panelWidth, mouseX, mouseY);
        }

        if (selectedModuleId != null) renderSettings(graphics);
        String footer = "OUR CLIENT  |  Click to toggle  |  ... settings  |  ESC close";
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
        if (module == null) { selectedModuleId = null; return; }
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
            settingRow(graphics, "Range", String.format(Locale.ROOT, "%.1f", config.aimRange), x, rowY, panelWidth, false);
            rowY += 38;
            settingRow(graphics, "Smoothness", String.format(Locale.ROOT, "%.2f", config.aimSmoothing), x, rowY, panelWidth, false);
            rowY += 38;
            graphics.drawString(font, editingNumber ? "Wheel / arrows to adjust" : "Range 1-64 | Smoothness 0.01-1.00", x + 18, rowY + 8, MUTED, false);
        } else if (config != null && selectedModuleId.equals("auto-schematic-builder")) {
            settingRow(graphics, "Placements / tick", Integer.toString(config.schematicPlacementsPerTick), x, rowY, panelWidth, false);
            graphics.drawString(font, "Wheel / arrows to adjust", x + 18, rowY + 50, MUTED, false);
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
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        int searchWidth = Math.min(310, Math.max(160, width - 40));
        int searchX = (width - searchWidth) / 2;
        if (inside(mouseX, mouseY, searchX, 14, searchWidth, 70)) {
            editingSearch = true;
            editingNumber = false;
            return true;
        }
        if (selectedModuleId != null && handleSettingsClick(mouseX, mouseY)) return true;

        List<Category> categories = categories();
        int columns = Math.max(1, Math.min(5, categories.size()));
        int available = width - GAP * (columns + 1);
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(145, available / columns));
        int totalWidth = columns * panelWidth + (columns - 1) * GAP;
        int startX = Math.max(GAP, (width - totalWidth) / 2);
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            int x = startX + (i % columns) * (panelWidth + GAP);
            int y = 100 + (i / columns) * 260;
            List<ClientModule> modules = filtered(category.modules());
            for (int row = 0; row < modules.size(); row++) {
                int rowY = y + TITLE_HEIGHT + row * ROW_HEIGHT;
                if (!inside(mouseX, mouseY, x + 4, rowY, panelWidth - 8, ROW_HEIGHT)) continue;
                ClientModule module = modules.get(row);
                if (mouseX >= x + panelWidth - 44) {
                    selectedModuleId = module.id();
                    editingSearch = false;
                } else if (module instanceof ToggleableModule toggleable && button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    toggle(module.id(), toggleable);
                }
                return true;
            }
        }
        editingSearch = false;
        return super.mouseClicked(event, doubled);
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

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        int panelWidth = Math.min(410, Math.max(180, width - 40));
        int panelHeight = selectedModuleId.equals("aim-assist") ? 210
                : selectedModuleId.equals("auto-schematic-builder") ? 190 : 145;
        int x = (width - panelWidth) / 2;
        int y = Math.max(95, height - panelHeight - 42);
        if (inside(mouseX, mouseY, x + panelWidth - 45, y, 45, 38)) {
            selectedModuleId = null;
            editingNumber = false;
            return true;
        }
        ClientModule module = OurClient.modules().get(selectedModuleId);
        if (module instanceof ToggleableModule toggleable && inside(mouseX, mouseY, x, y + 42, panelWidth, 34)) {
            toggle(selectedModuleId, toggleable);
            return true;
        }
        if ((selectedModuleId.equals("aim-assist") && inside(mouseX, mouseY, x, y + 80, panelWidth, 80))
                || (selectedModuleId.equals("auto-schematic-builder") && inside(mouseX, mouseY, x, y + 80, panelWidth, 38))) {
            editingNumber = true;
            editingSearch = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editingNumber && OurClient.config() != null && selectedModuleId != null) {
            ClientConfig config = OurClient.config();
            if (selectedModuleId.equals("aim-assist")) config.aimRange += (float) verticalAmount;
            else if (selectedModuleId.equals("auto-schematic-builder")) config.schematicPlacementsPerTick += verticalAmount > 0 ? 1 : -1;
            OurClient.syncAndSaveConfigFromModules();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (editingSearch && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        if (editingNumber && OurClient.config() != null && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            int direction = keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1;
            if (selectedModuleId.equals("aim-assist")) OurClient.config().aimRange += direction;
            else if (selectedModuleId.equals("auto-schematic-builder")) OurClient.config().schematicPlacementsPerTick += direction;
            OurClient.syncAndSaveConfigFromModules();
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
        List<ClientModule> combat = new ArrayList<>(), visual = new ArrayList<>(), movement = new ArrayList<>(), world = new ArrayList<>(), client = new ArrayList<>();
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
}
