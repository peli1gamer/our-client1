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
 * Arson Client's rebuilt module browser.
 *
 * The screen intentionally owns no module-specific registration logic: all module discovery comes
 * from ClientModuleManager, which makes adding a new module a single registry operation.
 */
public final class OurClientClickGui extends Screen {
    private static final int ACCENT = 0xFFFF6A00;
    private static final int ACCENT_DARK = 0xFF9C3C00;
    private static final int BACKGROUND = 0xF30B0D12;
    private static final int SURFACE = 0xF5161820;
    private static final int SURFACE_2 = 0xFF1C1F28;
    private static final int SURFACE_3 = 0xFF232731;
    private static final int TEXT = 0xFFF4F1EA;
    private static final int MUTED = 0xFFA7A9B2;
    private static final int ON = 0xFFFF7A1A;
    private static final int OFF = 0xFF747984;

    private static final int MARGIN = 14;
    private static final int HEADER = 52;
    private static final int SIDEBAR = 170;
    private static final int GAP = 10;
    private static final int CARD_HEIGHT = 62;

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
        g.fill(0, 0, width, height, BACKGROUND);
        drawHeader(g);
        drawSidebar(g, mouseX, mouseY);
        drawContent(g, mouseX, mouseY);
        if (settingsId != null) drawSettings(g, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics g) {
        g.fill(0, 0, width, HEADER, SURFACE);
        g.fill(0, HEADER - 2, width, HEADER, ACCENT);
        g.drawString(font, "ARSON", MARGIN, 13, ACCENT, false);
        g.drawString(font, "CLIENT", MARGIN + font.width("ARSON") + 7, 13, TEXT, false);
        g.drawString(font, "Modules", MARGIN, 31, MUTED, false);
        g.drawString(font, "Right Shift", width - 120, 14, TEXT, false);
        g.drawString(font, "close", width - 50, 14, MUTED, false);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int x = MARGIN;
        int y = HEADER + MARGIN;
        int w = SIDEBAR;
        int h = height - HEADER - MARGIN * 2;
        g.fill(x, y, x + w, y + h, SURFACE);

        g.drawString(font, "CATEGORIES", x + 12, y + 12, MUTED, false);
        int rowY = y + 34;
        MapCounts counts = new MapCounts();
        for (ClientModuleManager.Category category : categories) {
            boolean active = category == activeCategory;
            boolean hover = inside(mouseX, mouseY, x + 8, rowY, w - 16, 32);
            int fill = active ? ACCENT_DARK : hover ? SURFACE_3 : SURFACE_2;
            g.fill(x + 8, rowY, x + w - 8, rowY + 32, fill);
            if (active) g.fill(x + 8, rowY, x + 11, rowY + 32, ACCENT);
            g.drawString(font, category.title(), x + 18, rowY + 10, TEXT, false);
            int count = counts.value(category);
            String countText = Integer.toString(count);
            g.drawString(font, countText, x + w - 26 - font.width(countText), rowY + 10, MUTED, false);
            rowY += 36;
        }

        int footerY = y + h - 58;
        g.drawString(font, "LEFT / RIGHT", x + 12, footerY, MUTED, false);
        g.drawString(font, "category", x + 12, footerY + 14, TEXT, false);
        g.drawString(font, "UP / DOWN", x + 90, footerY, MUTED, false);
        g.drawString(font, "module", x + 90, footerY + 14, TEXT, false);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        int x = MARGIN + SIDEBAR + GAP;
        int y = HEADER + MARGIN;
        int w = width - x - MARGIN;
        int h = height - HEADER - MARGIN * 2;
        g.fill(x, y, x + w, y + h, SURFACE);

        String title = activeCategory.title();
        g.drawString(font, title, x + 16, y + 14, TEXT, false);
        List<ClientModule> all = filteredModules();
        int activeCount = 0;
        for (ClientModule module : all) if (module instanceof ToggleableModule t && t.enabled()) activeCount++;
        String status = activeCount + " active / " + all.size() + " shown";
        g.drawString(font, status, x + 16, y + 31, MUTED, false);

        int searchX = x + w - 220;
        int searchY = y + 11;
        g.fill(searchX, searchY, x + w - 14, searchY + 28, SURFACE_2);
        String searchText = search.isBlank() ? "Search..." : search;
        g.drawString(font, searchText + (editingSearch ? "_" : ""), searchX + 10, searchY + 9,
                search.isBlank() ? MUTED : TEXT, false);

        int listTop = y + 54;
        int listBottom = y + h - 10;
        if (all.isEmpty()) {
            g.drawCenteredString(font, "No modules match your search.", x + w / 2, listTop + 40, MUTED);
            return;
        }

        int columns = w >= 720 ? 2 : 1;
        int cardW = (w - 16 - (columns - 1) * GAP) / columns;
        int rows = (all.size() + columns - 1) / columns;
        int contentHeight = rows * (CARD_HEIGHT + GAP) - GAP;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (listBottom - listTop))));

        g.enableScissor(x + 1, listTop, x + w - 1, listBottom);
        for (int i = 0; i < all.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cardX = x + 8 + col * (cardW + GAP);
            int cardY = listTop + row * (CARD_HEIGHT + GAP) - scroll;
            drawModuleCard(g, all.get(i), i, cardX, cardY, cardW, mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void drawModuleCard(GuiGraphics g, ClientModule module, int index, int x, int y, int w, int mouseX, int mouseY) {
        boolean active = module instanceof ToggleableModule t && t.enabled();
        boolean selected = index == selectedIndex;
        boolean hover = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
        int fill = selected ? SURFACE_3 : hover ? 0xFF20232C : SURFACE_2;
        g.fill(x, y, x + w, y + CARD_HEIGHT, fill);
        g.fill(x, y, x + 4, y + CARD_HEIGHT, active ? ON : OFF);

        g.drawString(font, pretty(module.id()), x + 14, y + 11, TEXT, false);
        String description = descriptionLine(module.id(), w - 130);
        g.drawString(font, description, x + 14, y + 29, MUTED, false);

        int pillW = 54;
        g.fill(x + w - pillW - 12, y + 11, x + w - 12, y + 34, active ? ACCENT_DARK : SURFACE_3);
        g.drawCenteredString(font, active ? "ON" : "OFF", x + w - 12 - pillW / 2, y + 17, active ? TEXT : MUTED);
        g.drawString(font, "L toggle", x + w - 78, y + 45, MUTED, false);
        g.drawString(font, "R settings", x + w - 76, y + 53, MUTED, false);
    }

    private void drawSettings(GuiGraphics g, int mouseX, int mouseY) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            return;
        }

        int w = Math.min(500, width - 28);
        int h = Math.min(height - 28, 360);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        g.fill(0, 0, width, height, 0x66000000);
        g.fill(x, y, x + w, y + h, SURFACE);
        g.fill(x, y, x + w, y + 4, ACCENT);
        g.drawString(font, pretty(module.id()), x + 16, y + 16, TEXT, false);
        g.drawString(font, "Right click closes this panel", x + w - 178, y + 16, MUTED, false);
        drawWrapped(g, OurClient.modules().descriptionOf(module.id()), x + 16, y + 35, w - 32, MUTED);

        int rowY = y + 65;
        if (module instanceof ToggleableModule toggleable) {
            drawSettingRow(g, "Enabled", toggleable.enabled() ? "ON" : "OFF", x + 14, rowY, w - 28, true);
            rowY += 36;
        }

        List<ModuleSettings.Entry> entries = module.settings().entries().stream().filter(ModuleSettings.Entry::visible).toList();
        if (entries.isEmpty()) {
            g.drawString(font, "This module has no extra settings yet.", x + 16, rowY + 10, MUTED, false);
        } else {
            for (ModuleSettings.Entry entry : entries) {
                drawSettingRow(g, entry.label(), entry.value().get(), x + 14, rowY, w - 28, entry == selectedSetting);
                rowY += 34;
                if (rowY > y + h - 38) break;
            }
        }
        g.drawString(font, "LEFT/RIGHT or mouse wheel changes the selected setting", x + 16, y + h - 19, MUTED, false);
    }

    private void drawSettingRow(GuiGraphics g, String label, String value, int x, int y, int w, boolean selected) {
        g.fill(x, y, x + w, y + 28, selected ? SURFACE_3 : SURFACE_2);
        g.drawString(font, label, x + 10, y + 9, TEXT, false);
        g.drawRightString(font, value, x + w - 10, y + 9, selected ? ACCENT : MUTED);
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
        String[] words = text.split(" ");
        String line = "";
        int lineY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(font, line, x, lineY, color, false);
                line = word;
                lineY += 11;
            } else line = candidate;
        }
        if (!line.isEmpty()) g.drawString(font, line, x, lineY, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (settingsId != null) {
            return settingsMouseClicked(mx, my, button);
        }

        int sidebarX = MARGIN;
        int sidebarY = HEADER + MARGIN + 34;
        for (ClientModuleManager.Category category : categories) {
            if (inside(mx, my, sidebarX + 8, sidebarY, SIDEBAR - 16, 32)) {
                activeCategory = category;
                selectedIndex = 0;
                scroll = 0;
                return true;
            }
            sidebarY += 36;
        }

        int contentX = MARGIN + SIDEBAR + GAP;
        int contentW = width - contentX - MARGIN;
        if (inside(mx, my, contentX + contentW - 220, HEADER + MARGIN + 11, 206, 28)) {
            editingSearch = true;
            return true;
        }

        List<ClientModule> modules = filteredModules();
        int columns = contentW >= 720 ? 2 : 1;
        int cardW = (contentW - 16 - (columns - 1) * GAP) / columns;
        int listTop = HEADER + MARGIN + 54;
        for (int i = 0; i < modules.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cardX = contentX + 8 + col * (cardW + GAP);
            int cardY = listTop + row * (CARD_HEIGHT + GAP) - scroll;
            if (!inside(mx, my, cardX, cardY, cardW, CARD_HEIGHT)) continue;
            selectedIndex = i;
            ClientModule module = modules.get(i);
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && module instanceof ToggleableModule toggleable) toggle(module.id(), toggleable);
            else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                settingsId = module.id();
                selectedSetting = null;
            }
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    private boolean settingsMouseClicked(double mx, double my, int button) {
        ClientModule module = OurClient.modules().get(settingsId);
        if (module == null) {
            settingsId = null;
            return true;
        }
        int w = Math.min(500, width - 28);
        int h = Math.min(height - 28, 360);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        if (!inside(mx, my, x, y, w, h)) {
            settingsId = null;
            selectedSetting = null;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_1) return true;

        int rowY = y + 65;
        if (module instanceof ToggleableModule toggleable) {
            if (inside(mx, my, x + 14, rowY, w - 28, 28)) {
                toggle(module.id(), toggleable);
                return true;
            }
            rowY += 36;
        }

        for (ModuleSettings.Entry entry : module.settings().entries()) {
            if (!entry.visible()) continue;
            if (inside(mx, my, x + 14, rowY, w - 28, 28)) {
                selectedSetting = entry;
                if (button == GLFW.GLFW_MOUSE_BUTTON_1) adjust(entry, 1);
                return true;
            }
            rowY += 34;
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
        if (direction > 0) entry.increment().run();
        else entry.decrement().run();
        try { OurClient.syncAndSaveConfigFromModules(); } catch (RuntimeException exception) {
            OurClient.LOGGER.warn("Could not save module setting change", exception);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (settingsId != null && selectedSetting != null) {
            adjust(selectedSetting, vertical > 0 ? 1 : -1);
            return true;
        }
        scroll -= (int) Math.signum(vertical) * 42;
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
            ensureSelectionVisible(modules.size());
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN && !modules.isEmpty()) {
            selectedIndex = Math.floorMod(selectedIndex + 1, modules.size());
            ensureSelectionVisible(modules.size());
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

    private void ensureSelectionVisible(int size) {
        int columns = (width - (MARGIN + SIDEBAR + GAP) - MARGIN) >= 720 ? 2 : 1;
        int row = selectedIndex / columns;
        int target = row * (CARD_HEIGHT + GAP);
        int viewport = height - (HEADER + MARGIN + 54) - MARGIN - 10;
        if (target < scroll) scroll = target;
        else if (target + CARD_HEIGHT > scroll + viewport) scroll = target + CARD_HEIGHT - viewport;
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

    private static final class MapCounts {
        private final java.util.Map<ClientModuleManager.Category, Integer> counts = OurClient.modules().counts();
        int value(ClientModuleManager.Category category) { return counts.getOrDefault(category, 0); }
    }
}
