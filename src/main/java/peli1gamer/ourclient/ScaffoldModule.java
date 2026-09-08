package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Places blocks beneath/just ahead of the player using blocks already in the hotbar. */
public final class ScaffoldModule implements ToggleableModule {
    private static final double RANGE = 5.0D;
    private boolean enabled;
    private int previousSlot = -1;
    private int activePlacementSlot = -1;
    private LocalPlayer activePlayer;

    @Override public String id() { return "scaffold"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (enabled) {
            activePlayer = client.player;
            if (client.player != null) {
                previousSlot = client.player.getInventory().getSelectedSlot();
                activePlacementSlot = -1;
            }
        } else {
            restoreSelectedSlot(client);
            activePlayer = null;
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled) return;
        if (client.player == null || client.level == null || client.gameMode == null || client.screen != null) {
            restoreSelectedSlot(client);
            return;
        }
        if (!client.player.isAlive()) {
            restoreSelectedSlot(client);
            return;
        }

        if (activePlayer != client.player) {
            // Never restore the old player's hotbar state into a new player.
            previousSlot = -1;
            activePlacementSlot = -1;
            activePlayer = client.player;
            previousSlot = client.player.getInventory().getSelectedSlot();
        }

        LocalPlayer player = client.player;
        BlockPos target = player.blockPosition().below();
        if (!client.level.getBlockState(target).canBeReplaced()) return;

        if (tryPlace(client, target)) return;
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos support = target.relative(direction);
            if (client.level.getBlockState(support).isSolidRender()
                    && tryPlaceAgainst(client, target, support, direction.getOpposite())) return;
        }
    }

    private boolean tryPlace(Minecraft client, BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos support = target.relative(direction);
            if (!client.level.getBlockState(support).isSolidRender()) continue;
            if (tryPlaceAgainst(client, target, support, direction.getOpposite())) return true;
        }
        return false;
    }

    private boolean tryPlaceAgainst(Minecraft client, BlockPos target, BlockPos support, Direction face) {
        int slot = findBlockSlot(client.player);
        if (slot < 0) return false;
        int oldSlot = client.player.getInventory().getSelectedSlot();
        boolean changedSlot = oldSlot != slot;
        if (changedSlot) {
            // This is a short-lived interaction. Do not retain ownership after the
            // slot has been restored, otherwise disabling Scaffold later could undo
            // a manual selection made after this placement.
            if (activePlacementSlot < 0) previousSlot = oldSlot;
            activePlacementSlot = slot;
            client.player.getInventory().setSelectedSlot(slot);
        }
        try {
            Vec3 eye = client.player.getEyePosition();
            Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
            if (eye.distanceToSqr(hit) > RANGE * RANGE) return false;
            InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit, face, support, false));
            if (result.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
            return false;
        } finally {
            if (changedSlot) {
                client.player.getInventory().setSelectedSlot(oldSlot);
                // Ownership ends with the synchronous interaction. Keeping these
                // fields populated would make a later manual slot selection look
                // like a slot still owned by Scaffold.
                activePlacementSlot = -1;
                previousSlot = -1;
            }
        }
    }

    private int findBlockSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem item) {
                Block block = item.getBlock();
                if (!block.defaultBlockState().isAir() && !block.defaultBlockState().canBeReplaced()) return slot;
            }
        }
        return -1;
    }

    private void restoreSelectedSlot(Minecraft client) {
        if (client.player != null && activePlayer == client.player && previousSlot >= 0 && previousSlot < 9) {
            int selected = client.player.getInventory().getSelectedSlot();
            if (activePlacementSlot < 0 || selected == activePlacementSlot) {
                client.player.getInventory().setSelectedSlot(previousSlot);
            }
        }
        previousSlot = -1;
        activePlacementSlot = -1;
    }
}
