package peli1gamer.ourclient;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Simple native profile manager for the Arson Click GUI. */
public final class ConfigsScreen extends Screen {
    private static final int BG = 0xF20A0A0D, PANEL = 0xF2181820, ROW = 0xFF22222C;
    private static final int HOVER = 0xFF2B2B37, ACCENT = 0xFFFF6A00, TEXT = 0xFFECE8F5, MUTED = 0xFFAAA5B7;
    private final ClientConfigManager configs;
    private String name = "default";
    private String selected;
    private String status = "Type a profile name, then SAVE.";

    public ConfigsScreen() {
        super(Component.literal("Arson Configs"));
        this.configs = new ClientConfigManager(net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath());
        this.selected = "default";
    }

    @Override protected void init() { rebuildButtons(); }

    private void rebuildButtons() {
        clearWidgets();
        int panelW = Math.min(560, width - 32), left = (width - panelW) / 2;
        int top = 28;
        addRenderableWidget(Button.builder(Component.literal("SAVE"), b -> save()).bounds(left + 18, top + 70, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD"), b -> load()).bounds(left + 114, top + 70, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("DELETE"), b -> delete()).bounds(left + 210, top + 70, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> onClose()).bounds(left + panelW - 108, top + 70, 90, 20).build());
        List<String> profiles = configs.list();
        int y = top + 110;
        for (String profile : profiles) {
            if (y > height - 30) break;
            String display = profile.equals(selected) ? "• " + profile : profile;
            addRenderableWidget(Button.builder(Component.literal(display), b -> { selected = profile; name = profile; rebuildButtons(); }).bounds(left + 18, y, panelW - 36, 20).build());
            y += 24;
        }
    }

    private void save() {
        OurClient.syncAndSaveConfigFromModules();
        if (configs.save(name, OurClient.config())) { selected = name.trim().toLowerCase(); status = "Saved " + selected; rebuildButtons(); }
        else status = "Invalid profile name. Use letters, numbers, -, _, or .";
    }

    private void load() {
        ClientConfig loaded = configs.load(selected);
        if (loaded == null) { status = "Profile not found: " + selected; return; }
        OurClient.loadConfig(loaded);
        status = "Loaded " + selected;
    }

    private void delete() {
        if (configs.delete(selected)) { status = "Deleted " + selected; selected = "default"; name = selected; rebuildButtons(); }
        else status = "The default profile cannot be deleted.";
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);
        int panelW = Math.min(560, width - 32), left = (width - panelW) / 2, top = 28;
        g.fill(left, top, left + panelW, height - 20, PANEL);
        g.fill(left, top, left + panelW, top + 3, ACCENT);
        g.drawString(font, "ARSON CONFIGS", left + 18, top + 16, TEXT, false);
        g.drawString(font, "Profile name: " + name + "_", left + 18, top + 42, TEXT, false);
        g.drawString(font, status, left + 18, height - 34, MUTED, false);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (event.codepoint() >= 32 && event.codepoint() <= 126 && name.length() < 32) {
            name += Character.toLowerCase((char) event.codepoint());
            return true;
        }
        return super.charTyped(event);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !name.isEmpty()) { name = name.substring(0, name.length() - 1); return true; }
        if (event.key() == GLFW.GLFW_KEY_ENTER) { save(); return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public void onClose() { minecraft.setScreen(new OurClientClickGui()); }
}
