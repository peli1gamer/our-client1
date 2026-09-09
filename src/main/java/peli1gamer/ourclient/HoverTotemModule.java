package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import peli1gamer.ourclient.mixin.AbstractContainerScreenAccessor;

/**
 * Moves a hovered Totem of Undying into the offhand and optionally a preferred hotbar slot.
 * Uses a tiny native accessor mixin because the hovered slot is not publicly exposed in 1.21.11.
 */
public final class HoverTotemModule implements ToggleableModule {
    private static final int OFFHAND_BUTTON = 40;
    private static final int PLAYER_INVENTORY_LAST_SLOT = 35;
    private static final int CLICK_COOLDOWN_TICKS = 3;

    private boolean enabled;
    private boolean hotbarTotem = true;
    private int hotbarSlot = 1;
    private boolean autoSwitchToTotem;
    private boolean autoInvOpen;

    private boolean shouldOpenInventory;
    private boolean autoOpenedInventory;
    private int clickCooldown;

    @Override public String id() { return "hover-totem"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        resetState();
    }

    public boolean hotbarTotem() { return hotbarTotem; }
    public void setHotbarTotem(boolean value) { hotbarTotem = value; }
    public int hotbarSlot() { return hotbarSlot; }
    public void setHotbarSlot(int value) { hotbarSlot = clampSlot(value); }
    public boolean autoSwitchToTotem() { return autoSwitchToTotem; }
    public void setAutoSwitchToTotem(boolean value) { autoSwitchToTotem = value; }
    public boolean autoInvOpen() { return autoInvOpen; }
    public void setAutoInvOpen(boolean value) { autoInvOpen = value; }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.gameMode == null || !client.player.isAlive()) return;
        if (clickCooldown > 0) clickCooldown--;

        if (autoInvOpen && client.screen == null
                && !client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                && hasTotemInInventory(client)) {
            shouldOpenInventory = true;
        }

        if (shouldOpenInventory && client.screen == null) {
            if (hasTotemInInventory(client)) {
                client.setScreen(new InventoryScreen(client.player));
                shouldOpenInventory = false;
                autoOpenedInventory = true;
            } else {
                shouldOpenInventory = false;
            }
            return;
        }

        if (!(client.screen instanceof InventoryScreen inventoryScreen)) {
            if (autoOpenedInventory && client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                autoOpenedInventory = false;
            }
            return;
        }

        if (autoInvOpen && autoOpenedInventory && client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            autoOpenedInventory = false;
            client.player.closeContainer();
            return;
        }

        Slot focusedSlot = getHoveredSlot(inventoryScreen);
        if (focusedSlot == null || focusedSlot.getContainerSlot() > PLAYER_INVENTORY_LAST_SLOT) return;
        if (!focusedSlot.getItem().is(Items.TOTEM_OF_UNDYING)) return;

        if (autoSwitchToTotem) {
            client.player.getInventory().setSelectedSlot(hotbarSlot - 1);
        }

        if (clickCooldown > 0) return;

        int slotIndex = focusedSlot.getContainerSlot();
        int syncId = inventoryScreen.getMenu().containerId;

        if (!client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            swap(client, syncId, slotIndex, OFFHAND_BUTTON);
            return;
        }

        if (hotbarTotem) {
            int hotbarIndex = hotbarSlot - 1;
            if (!client.player.getInventory().getItem(hotbarIndex).is(Items.TOTEM_OF_UNDYING)
                    && hotbarIndex != slotIndex) {
                swap(client, syncId, slotIndex, hotbarIndex);
            }
        }
    }

    private void swap(Minecraft client, int syncId, int slotIndex, int button) {
        client.gameMode.handleInventoryMouseClick(syncId, slotIndex, button, ClickType.SWAP, client.player);
        clickCooldown = CLICK_COOLDOWN_TICKS;
    }

    private static Slot getHoveredSlot(InventoryScreen screen) {
        return ((AbstractContainerScreenAccessor) screen).arson$getHoveredSlot();
    }

    private static boolean hasTotemInInventory(Minecraft client) {
        for (int i = 0; i < 36; i++) {
            if (client.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private void resetState() {
        shouldOpenInventory = false;
        autoOpenedInventory = false;
        clickCooldown = 0;
    }

    private static int clampSlot(int value) {
        return Math.max(1, Math.min(9, value));
    }
}
