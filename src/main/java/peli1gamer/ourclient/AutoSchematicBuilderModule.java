package peli1gamer.ourclient;

import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-side schematic placer. Schematics live in config/our-client1/schematics/*.json.
 * One placement is attempted per tick to avoid freezing the client on very large builds.
 */
public final class AutoSchematicBuilderModule implements ToggleableModule {
    private static final int MAX_PLACEMENT_RANGE = 6;
    private static final int MAX_ATTEMPTS_PER_TICK = 1;

    private boolean enabled;
    private Schematic schematic;
    private BlockPos origin;
    private int cursor;
    private boolean loaded;
    private String requestedFile = "build.json";

    @Override public String id() { return "auto-schematic-builder"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) resetProgress();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) return;
        if (!loaded) loadSchematic(client);
        if (schematic == null) return;

        if (origin == null) {
            origin = client.player.blockPosition();
            cursor = 0;
            sortBlocks();
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic loaded: " + schematic.name() + " (" + schematic.blocks().size() + " blocks)"), true);
        }

        int attempts = 0;
        while (cursor < schematic.blocks().size() && attempts < MAX_ATTEMPTS_PER_TICK) {
            Schematic.BlockEntry entry = schematic.blocks().get(cursor);
            BlockPos target = origin.offset(entry.x(), entry.y(), entry.z());
            BlockState wanted = entry.state();
            BlockState current = client.level.getBlockState(target);

            if (current.is(wanted.getBlock())) {
                cursor++;
                continue;
            }

            if (place(client, target, wanted)) cursor++;
            attempts++;
        }

        if (cursor >= schematic.blocks().size()) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Schematic complete: " + schematic.name()), true);
            enabled = false;
        }
    }

    private void loadSchematic(Minecraft client) {
        loaded = true;
        Path directory = client.gameDirectory.toPath().resolve("config/our-client1/schematics");
        Path file = directory.resolve(requestedFile);
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                Files.writeString(file, "{\n  \"name\": \"example\",\n  \"blocks\": []\n}\n");
                return;
            }
            schematic = Schematic.parse(Files.readString(file), requestedFile);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic error: " + exception.getMessage()), true);
        }
    }

    private boolean place(Minecraft client, BlockPos target, BlockState wanted) {
        if (client.player.distanceToSqr(Vec3.atCenterOf(target)) > MAX_PLACEMENT_RANGE * MAX_PLACEMENT_RANGE) return false;

        Direction[] directions = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction face : directions) {
            BlockPos support = target.relative(face.getOpposite());
            BlockState supportState = client.level.getBlockState(support);
            if (!supportState.isSolidRender()) continue;

            int slot = findBlockSlot(wanted.getBlock());
            if (slot < 0) return false;
            client.player.getInventory().setSelectedSlot(slot);

            Vec3 hit = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.49,
                    face.getStepY() * 0.49,
                    face.getStepZ() * 0.49);
            BlockHitResult result = new BlockHitResult(hit, face, support, false);
            InteractionResult action = client.gameMode.interactBlock(client.player, InteractionHand.MAIN_HAND, result);
            if (action.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private int findBlockSlot(Block block) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) return slot;
        }
        return -1;
    }

    private void sortBlocks() {
        if (schematic == null) return;
        List<Schematic.BlockEntry> sorted = new ArrayList<>(schematic.blocks());
        sorted.sort(Comparator.comparingInt(Schematic.BlockEntry::y)
                .thenComparingInt(Schematic.BlockEntry::x)
                .thenComparingInt(Schematic.BlockEntry::z));
        schematic = new Schematic(schematic.name(), List.copyOf(sorted));
    }

    private void resetProgress() {
        origin = null;
        cursor = 0;
        loaded = false;
        schematic = null;
    }
}
