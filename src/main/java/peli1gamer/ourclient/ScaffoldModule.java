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

    @Override public String id() { return "scaffold"; }
    @Override public String description() { return "Places an available building block under or beside the player."; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings();
        settings.number("range", "Placement range", () -> String.format(java.util.Locale.ROOT, "%.1f", RANGE), () -> {}, () -> {});
        return settings;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (enabled) {
            if (client.player != null) previousSlot = client.player.getInventory().getSelectedSlot();
        } else {
            restoreSlot(client.player);
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.gameMode == null
                || client.screen != null || !client.player.isAlive()) return;
        LocalPlayer player = client.player;
        BlockPos target = player.blockPosition().below();
        if (!client.level.getBlockState(target).canBeReplaced()) return;

        if (tryPlace(client, target)) return;
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos support = target.relative(direction);
            if (client.level.getBlockState(support).isSolidRender() && tryPlaceAgainst(client, target, support, direction.getOpposite())) return;
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
        Vec3 eye = client.player.getEyePosition();
        Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
        if (eye.distanceToSqr(hit) > RANGE * RANGE) return false;

        client.player.getInventory().setSelectedSlot(slot);
        try {
            InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit, face, support, false));
            if (result.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
            return false;
        } finally {
            client.player.getInventory().setSelectedSlot(oldSlot);
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

    private void restoreSlot(LocalPlayer player) {
        if (player != null && previousSlot >= 0 && previousSlot < 9) player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }
}
