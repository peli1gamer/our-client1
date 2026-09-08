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

/**
 * Arson's standalone ClickGUI. It borrows the usability patterns we like from modern
 * clients, but deliberately uses its own layout, branding, colors and interactions.
 */
public final class OurClientClickGui extends Screen {
    private static final int ORANGE = 0xFFFF6A00;
    private static final int ORANGE_BRIGHT = 0xFFFF8A24;
    private static final int ORANGE_DARK = 0xFF9E3B00;
    private static final int BG = 0xE807090D;
    private static final int PANEL = 0xF20D1017;
    private static final int CARD = 0xF4161A23;
    private static final int CARD_HOVER = 0xFF1E232D;
    private static final int CARD_SELECTED = 0xFF171C26;
    private static final int BORDER = 0xFF292E39;
    private static final int TEXT = 0xFFF4F1EA;
    private static final int MUTED = 0xFF9297A3;
    private static final int OFF = 0xFF707783;
    private static final int ON = 0xFFFF7620;

    private static final int SHELL_W = 1160;
    private static final int SHELL_H = 690;
    private static final int SIDEBAR_W = 210;
    private static final int HEADER_H = 58;
    private static final int GAP = 12;
    private static final int CARD_H = 64;

    private final List<ClientModuleManager.Category> categories = List.of(ClientModuleManager.Category.values());
    private ClientModuleManager.Category activeCategory = ClientModuleManager.Category.COMBAT;
    private String search = "";
    private boolean editingSearch;
    private String settingsId;
    private int scroll;
    private int selectedIndex;
    private ModuleSettings.Entry selectedSetting;

    public OurClientClickGui() {
        super(Component.literal("Arson Client"));
    }

    @Override
    public void init() {
        super.init();
        if (OurClient.modules().inCategory(activeCategory).isEmpty()) {
            for (ClientModuleManager.Category category : categories) {
                if (!OurClient.modules().inCategory(category).isEmpty()) {
                    activeCategory = category;
                    break;
                }
            }
        }
        clampSelection();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int shellW = Math.min(SHELL_W, Math.max(420, width - 32));
        int shellH = Math.min(SHELL_H, Math.max(300, height - 32));
        int sx = (width - shellW) / 2;
        int sy = (height - shellH) / 2;

        // Subtle orange halo and the actual client shell.
        g.fill(sx - 3, sy - 3, sx + shellW + 3, sy + shellH + 3, 0x552A1204);
        g.fill(sx, sy, sx + shellW, sy + shellH, PANEL);
        g.fill(sx, sy, sx + 3, sy + shellH, ORANGE_DARK);

        drawSidebar(g, sx, sy, shellH, mouseX, mouseY);
        drawHeader(g, sx + SIDEBAR_W, sy, shellW - SIDEBAR_W, mouseX, mouseY);
        drawContent(g, sx + SIDEBAR_W, sy + HEADER_H, shellW - SIDEBAR_W, shellH - HEADER_H, mouseX, mouseY);

        if (settingsId != null) drawSettings(g, sx, sy, shellW, shellH);
    }

    private void drawSidebar(GuiGraphics g, int sx, int sy, int h, int mouseX, int mouseY) {
        g.fill(sx, sy, sx + SIDEBAR_W, sy + h, 0xF90B0E14);
        g.fill(sx + SIDEBAR_W - 1, sy, sx + SIDEBAR_W, sy + h, BORDER);

        drawFlameMark(g, sx + 20, sy + 22);
        g.drawString(font, "ARSON", sx + 57, sy + 23, TEXT, false);
        g.drawString(font, "CLIENT", sx + 57, sy + 37, ORANGE_BRIGHT, false);
        g.drawString(font, "standalone build", sx + 57, sy + 49, MUTED, false);
        g.fill(sx + 16, sy + 67, sx + SIDEBAR_W - 16, sy + 68, BORDER);

        g.drawString(font, "MODULES", sx + 18, sy + 85, MUTED, false);
        int rowY = sy + 101;
        java.util.Map<ClientModuleManager.Category, Integer> counts = OurClient.modules().counts();
        for (ClientModuleManager.Category category : categories) {
            boolean active = category == activeCategory;
            boolean hover = inside(mouseX, mouseY, sx + 10, rowY, SIDEBAR_W - 20, 34);
            int fill = active ? 0xFF2A180E : hover ? CARD_HOVER : 0x00111111;
            g.fill(sx + 10, rowY, sx + SIDEBAR_W - 10, rowY + 34, fill);
            if (active) g.fill(sx + 10, rowY, sx + 13, rowY + 34, ORANGE);
            drawCategoryGlyph(g, category, sx + 27, rowY + 17, active ? ORANGE : MUTED);
            g.drawString(font, category.title(), sx + 43, rowY + 11, active ? TEXT : MUTED, false);
            String count = Integer.toString(counts.getOrDefault(category, 0));
            g.drawString(font, count, sx + SIDEBAR_W - 22 - font.width(count), rowY + 11, MUTED, false);
            rowY += 38;
        }

        int bottom = sy + h - 68;
        g.fill(sx + 16, bottom, sx + SIDEBAR_W - 16, bottom + 1, BORDER);
        g.drawString(font, "GENERAL", sx + 18, bottom + 14, MUTED, false);
        g.drawString(font, "Settings", sx + 38, bottom + 30, TEXT, false);
        g.drawString(font, "Profiles", sx + 38, bottom + 46, TEXT, false);
        g.drawString(font, "Arson Client", sx + 18, sy + h - 16, ORANGE_BRIGHT, false);
        g.drawString(font, "Fire theme", sx + SIDEBAR_W - 78, sy + h - 16, MUTED, false);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + HEADER_H, 0xF80F131B);
        g.fill(x + 14, y + HEADER_H - 2, x + w - 14, y + HEADER_H, ORANGE);

        g.drawString(font, "Hello, Arson player", x + 18, y + 17, TEXT, false);
        g.drawString(font, activeCategory.title() + " modules", x + 18, y + 34, MUTED, false);

        int searchW = Math.min(280, Math.max(140, w / 3));
        int searchX = x + w - searchW - 18;
        int searchY = y + 14;
        boolean hover = inside(mouseX, mouseY, searchX, searchY, searchW, 28);
        g.fill(searchX, searchY, searchX + searchW, searchY + 28, hover || editingSearch ? CARD_HOVER : CARD);
        g.fill(searchX, searchY, searchX + searchW, searchY + 1, editingSearch ? ORANGE : BORDER);
        g.drawString(font, "Search", searchX + 11, searchY + 9, search.isBlank() ? MUTED : TEXT, false);
        if (!search.isBlank()) g.drawString(font, search, searchX + 53, searchY + 9, TEXT, false);
        if (editingSearch) g.drawString(font, "_", searchX + 53 + font.width(search), searchY + 9, ORANGE_BRIGHT, false);
        g.drawString(font, "RShift", searchX + searchW - 43, searchY + 9, MUTED, false);
    }

    private void drawContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xF70A0D13);
        List<ClientModule> modules = filteredModules();
        int activeCount = 0;
        for (ClientModule module : modules) if (module instanceof ToggleableModule t && t.enabled()) activeCount++;

        g.drawString(font, activeCategory.title(), x + 18, y + 17, TEXT, false);
        g.drawString(font, activeCount + " enabled  •  " + modules.size() + " modules", x + 18, y + 35, MUTED, false);

        int listTop = y + 53;
        int listBottom = y + h - 12;
        if (modules.isEmpty()) {
            g.drawCenteredString(font, "No modules match your search.", x + w / 2, listTop + 45, MUTED);
            return;
        }

        int columns = w >= 650 ? 2 : 1;
        int cardW = Math.max(120, (w - 36 - (columns - 1) * GAP) / columns);
        int rows = (modules.size() + columns - 1) / columns;
        int contentHeight = Math.max(0, rows * (CARD_H + GAP) - GAP);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (listBottom - listTop))));

        g.enableScissor(x + 1, listTop, x + w - 1, listBottom);
        for (int i = 0; i < modules.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cardX = x + 14 + col * (cardW + GAP);
            int cardY = listTop + row * (CARD_H + GAP) - scroll;
            drawModuleCard(g, modules.get(i), i, cardX, cardY, cardW, mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void drawModuleCard(GuiGraphics g, ClientModule module, int index, int x, int y, int w, int mouseX, int mouseY) {
        boolean active = module instanceof ToggleableModule t && t.enabled();
        boolean selected = index == selectedIndex;
        boolean hover = inside(mouseX, mouseY, x, y, w, CARD_H);

        int fill = selected ? CARD_SELECTED : hover ? CARD_HOVER : CARD;
        g.fill(x, y, x + w, y + CARD_H, fill);
        g.fill(x, y, x + w, y + 1, selected ? ORANGE : BORDER);
        g.fill(x, y + 1, x + 3, y + CARD_H - 1, active ? ON : OFF);

        g.drawString(font, pretty(module.id()), x + 14, y + 11, TEXT, false);
        String description = descriptionLine(module.id(), Math.max(80, w - 105));
        g.drawString(font, description, x + 14, y + 29, MUTED, false);
        g.drawString(font, "Keybind: none", x + 14, y + 47, MUTED, false);

        int toggleX = x + w - 57;
        int toggleY = y + 12;
        g.fill(toggleX, toggleY, toggleX + 43, toggleY + 20, active ? ORANGE_DARK : 0xFF3A3F4A);
        g.fill(toggleX + (active ? 25 : 3), toggleY + 3, toggleX + (active ? 40 : 18), toggleY + 18, active ? ORANGE_BRIGHT : 0xFF949AA6);
    }

    private void drawSettings(GuiGraphics g, int sx, int sy, int shellW, int shellH) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            selectedSetting = null;
            return;
        }

        int drawerW = Math.min(470, shellW - 26);
        int x = sx + shellW - drawerW - 13;
        int y = sy + 13;
        int h = shellH - 26;

        g.fill(sx, sy, sx + shellW, sy + shellH, 0x66000000);
        g.fill(x - 3, y - 3, x + drawerW + 3, y + h + 3, 0x442A1204);
        g.fill(x, y, x + drawerW, y + h, 0xFF10141C);
        g.fill(x, y, x + drawerW, y + 4, ORANGE);
        g.fill(x, y + 4, x + 1, y + h, ORANGE_DARK);

        g.drawString(font, pretty(module.id()), x + 18, y + 18, TEXT, false);
        g.drawString(font, "Module settings", x + 18, y + 35, ORANGE_BRIGHT, false);
        g.drawString(font, "ESC closes  •  LEFT/RIGHT adjusts", x + drawerW - 178, y + 19, MUTED, false);

        drawWrapped(g, OurClient.modules().descriptionOf(module.id()), x + 18, y + 54, drawerW - 36, MUTED);

        int rowY = y + 84;
        if (module instanceof ToggleableModule toggleable) {
            drawSettingRow(g, "Enabled", toggleable.enabled() ? "ON" : "OFF", x + 16, rowY, drawerW - 32, false);
            rowY += 38;
        }

        List<ModuleSettings.Entry> entries = module.settings().entries().stream().filter(ModuleSettings.Entry::visible).toList();
        if (entries.isEmpty()) {
            g.drawString(font, "This module has no extra settings yet.", x + 18, rowY + 12, MUTED, false);
        } else {
            for (ModuleSettings.Entry entry : entries) {
                if (rowY > y + h - 55) break;
                drawSettingRow(g, entry.label(), entry.value().get(), x + 16, rowY, drawerW - 32, entry == selectedSetting);
                rowY += 36;
            }
        }

        g.drawString(font, "Wheel / LEFT / RIGHT", x + 18, y + h - 30, MUTED, false);
        g.drawString(font, "change selected value", x + 18, y + h - 17, ORANGE_BRIGHT, false);
    }

    private void drawSettingRow(GuiGraphics g, String label, String value, int x, int y, int w, boolean selected) {
        g.fill(x, y, x + w, y + 30, selected ? 0xFF242A34 : CARD);
        g.fill(x, y, x + 2, y + 30, selected ? ORANGE : BORDER);
        g.drawString(font, label, x + 12, y + 10, TEXT, false);
        int valueX = x + w - 12 - font.width(value);
        g.drawString(font, value, valueX, y + 10, selected ? ORANGE_BRIGHT : MUTED, false);
    }

    private void drawFlameMark(GuiGraphics g, int x, int y) {
        // Small vector flame, keeping the logo native to the GUI and resource-pack independent.
        g.fill(x + 8, y, x + 14, y + 7, ORANGE);
        g.fill(x + 5, y + 6, x + 17, y + 17, ORANGE);
        g.fill(x + 3, y + 13, x + 19, y + 22, ORANGE_DARK);
        g.fill(x + 7, y + 10, x + 15, y + 21, ORANGE_BRIGHT);
        g.fill(x + 9, y + 14, x + 14, y + 21, 0xFFFFB04A);
    }

    private void drawCategoryGlyph(GuiGraphics g, ClientModuleManager.Category category, int cx, int cy, int color) {
        String glyph = switch (category) {
            case COMBAT -> "C";
            case MOVEMENT -> "M";
            case PLAYER -> "P";
            case RENDER -> "V";
            case WORLD -> "W";
            case MISC -> "*";
        };
        g.drawCenteredString(font, glyph, cx, cy - 4, color);
    }

    private List<ClientModule> filteredModules() {
        String filter = search.trim().toLowerCase(Locale.ROOT);
        List<ClientModule> result = new ArrayList<>();
        for (ClientModule module : OurClient.modules().inCategory(activeCategory)) {
            if (filter.isEmpty()
                    || module.id().toLowerCase(Locale.ROOT).contains(filter)
                    || pretty(module.id()).toLowerCase(Locale.ROOT).contains(filter)
                    || OurClient.modules().descriptionOf(module.id()).toLowerCase(Locale.ROOT).contains(filter)) {
                result.add(module);
            }
        }
        return result;
    }

    private String descriptionLine(String id, int availableWidth) {
        String description = OurClient.modules().descriptionOf(id);
        if (font.width(description) <= availableWidth) return description;
        String result = description;
        while (result.length() > 3 && font.width(result + "…") > availableWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        String line = "";
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(font, line, x, lineY, color, false);
                line = word;
                lineY += 11;
            } else {
                line = candidate;
            }
        }
        if (!line.isEmpty()) g.drawString(font, line, x, lineY, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        int shellW = Math.min(SHELL_W, Math.max(420, width - 32));
        int shellH = Math.min(SHELL_H, Math.max(300, height - 32));
        int sx = (width - shellW) / 2;
        int sy = (height - shellH) / 2;

        if (settingsId != null) return settingsMouseClicked(mx, my, button, sx, sy, shellW, shellH);

        int rowY = sy + 101;
        for (ClientModuleManager.Category category : categories) {
            if (inside(mx, my, sx + 10, rowY, SIDEBAR_W - 20, 34)) {
                activeCategory = category;
                selectedIndex = 0;
                scroll = 0;
                return true;
            }
            rowY += 38;
        }

        int contentX = sx + SIDEBAR_W;
        int contentW = shellW - SIDEBAR_W;
        int searchW = Math.min(280, Math.max(140, contentW / 3));
        int searchX = contentX + contentW - searchW - 18;
        if (inside(mx, my, searchX, sy + 14, searchW, 28)) {
            editingSearch = true;
            return true;
        }

        List<ClientModule> modules = filteredModules();
        int columns = contentW >= 650 ? 2 : 1;
        int cardW = Math.max(120, (contentW - 36 - (columns - 1) * GAP) / columns);
        int listTop = sy + HEADER_H + 53;
        for (int i = 0; i < modules.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cardX = contentX + 14 + col * (cardW + GAP);
            int cardY = listTop + row * (CARD_H + GAP) - scroll;
            if (!inside(mx, my, cardX, cardY, cardW, CARD_H)) continue;
            selectedIndex = i;
            ClientModule module = modules.get(i);
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule toggleable) {
                toggle(module.id(), toggleable);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                settingsId = module.id();
                selectedSetting = null;
            }
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    private boolean settingsMouseClicked(double mx, double my, int button, int sx, int sy, int shellW, int shellH) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            selectedSetting = null;
            return true;
        }

        int drawerW = Math.min(470, shellW - 26);
        int x = sx + shellW - drawerW - 13;
        int y = sy + 13;
        int h = shellH - 26;
        if (!inside(mx, my, x, y, drawerW, h)) {
            settingsId = null;
            selectedSetting = null;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_1) return true;

        int rowY = y + 84;
        if (module instanceof ToggleableModule toggleable) {
            if (inside(mx, my, x + 16, rowY, drawerW - 32, 30)) {
                toggle(module.id(), toggleable);
                return true;
            }
            rowY += 38;
        }
        for (ModuleSettings.Entry entry : module.settings().entries()) {
            if (!entry.visible()) continue;
            if (inside(mx, my, x + 16, rowY, drawerW - 32, 30)) {
                selectedSetting = entry;
                adjust(entry, 1);
                return true;
            }
            rowY += 36;
        }
        return true;
    }

    private void toggle(String id, ToggleableModule toggleable) {
        try {
            toggleable.setEnabled(!toggleable.enabled());
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Failed to toggle module '{}' from ClickGUI", id, exception);
            try { toggleable.setEnabled(false); } catch (RuntimeException ignored) { }
            try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException ignored) { }
        }
    }

    private void adjust(ModuleSettings.Entry entry, int direction) {
        try {
            if (direction > 0) entry.increment().run();
            else entry.decrement().run();
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.warn("Could not change module setting", exception);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (settingsId != null && selectedSetting != null) {
            adjust(selectedSetting, vertical > 0 ? 1 : -1);
            return true;
        }
        scroll -= (int) Math.signum(vertical) * 48;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingSearch) {
                editingSearch = false;
                return true;
            }
            if (settingsId != null) {
                settingsId = null;
                selectedSetting = null;
                return true;
            }
            onClose();
            return true;
        }

        if (settingsId != null && selectedSetting != null) {
            if (key == GLFW.GLFW_KEY_LEFT) { adjust(selectedSetting, -1); return true; }
            if (key == GLFW.GLFW_KEY_RIGHT) { adjust(selectedSetting, 1); return true; }
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                selectAdjacentSetting(key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
                return true;
            }
        }

        if (editingSearch) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                selectedIndex = 0;
                scroll = 0;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                editingSearch = false;
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int delta = key == GLFW.GLFW_KEY_RIGHT ? 1 : -1;
            int index = categories.indexOf(activeCategory);
            for (int i = 0; i < categories.size(); i++) {
                index = Math.floorMod(index + delta, categories.size());
                if (!OurClient.modules().inCategory(categories.get(index)).isEmpty()) {
                    activeCategory = categories.get(index);
                    selectedIndex = 0;
                    scroll = 0;
                    break;
                }
            }
            return true;
        }

        List<ClientModule> modules = filteredModules();
        if (key == GLFW.GLFW_KEY_UP && !modules.isEmpty()) {
            selectedIndex = Math.floorMod(selectedIndex - 1, modules.size());
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) {
            selectedIndex = Math.floorMod(selectedIndex + 1, modules.size());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && !modules.isEmpty()) {
            ClientModule module = modules.get(Math.min(selectedIndex, modules.size() - 1));
            if (module instanceof ToggleableModule toggleable) toggle(module.id(), toggleable);
            return true;
        }
        if (key == GLFW.GLFW_KEY_O && !modules.isEmpty()) {
            ClientModule module = modules.get(Math.min(selectedIndex, modules.size() - 1));
            settingsId = module.id();
            selectedSetting = null;
            return true;
        }
        return super.keyPressed(event);
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

    private void selectAdjacentSetting(int delta) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) return;
        List<ModuleSettings.Entry> entries = module.settings().entries().stream().filter(ModuleSettings.Entry::visible).toList();
        if (entries.isEmpty()) return;
        if (selectedSetting == null) {
            selectedSetting = entries.get(0);
            return;
        }
        int index = entries.indexOf(selectedSetting);
        selectedSetting = entries.get(Math.floorMod(index + delta, entries.size()));
    }

    private void clampSelection() {
        int size = filteredModules().size();
        selectedIndex = size == 0 ? 0 : Math.min(selectedIndex, size - 1);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
