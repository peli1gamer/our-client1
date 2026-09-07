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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/** Client-side JSON schematic placer. */
public final class AutoSchematicBuilderModule implements ToggleableModule {
    private static final double MAX_PLACEMENT_RANGE = 6.0D;
    private static final int MAX_ATTEMPTS_PER_TICK = 20;
    private static final int NO_PROGRESS_NOTICE_TICKS = 40;
    private static final int RETRY_DELAY_TICKS = 2;
    private static final long MAX_SCHEMATIC_FILE_BYTES = 8L * 1024L * 1024L;

    private boolean enabled;
    private Schematic schematic;
    private BlockPos origin;
    private int cursor;
    private BitSet completed;
    private boolean loaded;
    private int noProgressTicks;
    private int retryCooldown;
    private int previousSelectedSlot = -1;
    private String requestedFile = "build.json";

    @Override public String id() { return "auto-schematic-builder"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) previousSelectedSlot = client.player.getInventory().getSelectedSlot();
        } else {
            restoreSelectedSlot();
            resetProgress();
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) return;
        if (!client.player.isAlive()) return;
        if (previousSelectedSlot < 0) previousSelectedSlot = client.player.getInventory().getSelectedSlot();
        if (!loaded) loadSchematic(client);
        if (schematic == null) return;

        if (origin == null) {
            origin = client.player.blockPosition();
            cursor = 0;
            completed = new BitSet(schematic.blocks().size());
            sortBlocks();
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic loaded: " + schematic.name() + " (" + schematic.blocks().size() + " blocks)"), true);
        }

        if (completed == null || completed.length() > schematic.blocks().size()) {
            completed = new BitSet(schematic.blocks().size());
        }
        if (retryCooldown > 0) retryCooldown--;

        ClientConfig config = OurClient.config();
        int attemptsThisTick = config == null ? 1 : Math.min(MAX_ATTEMPTS_PER_TICK,
                Math.max(1, config.schematicPlacementsPerTick));
        int attempts = 0;
        boolean madeProgress = false;

        while (completed.cardinality() < schematic.blocks().size() && attempts < attemptsThisTick) {
            int index = findNextWorkItem(client);
            if (index < 0) break;
            cursor = index;

            Schematic.BlockEntry entry = schematic.blocks().get(index);
            BlockPos target = origin.offset(entry.x(), entry.y(), entry.z());
            if (!client.level.isInWorldBounds(target)) {
                abortBuild(client, "Schematic target is outside the current world bounds.");
                return;
            }
            BlockState wanted = entry.state();
            BlockState current = client.level.getBlockState(target);

            if (current.is(wanted.getBlock())) {
                completed.set(index);
                cursor = (index + 1) % schematic.blocks().size();
                madeProgress = true;
                continue;
            }

            if (retryCooldown > 0) break;
            if (place(client, target, wanted)) {
                attempts++;
                if (client.level.getBlockState(target).is(wanted.getBlock())) {
                    completed.set(index);
                    cursor = (index + 1) % schematic.blocks().size();
                    madeProgress = true;
                    continue;
                }
                retryCooldown = RETRY_DELAY_TICKS;
                break;
            }
            attempts++;
        }

        if (completed.cardinality() >= schematic.blocks().size()) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic complete: " + schematic.name()), true);
            setEnabled(false);
            OurClient.syncAndSaveConfigFromModules();
            return;
        }

        if (madeProgress) {
            noProgressTicks = 0;
        } else if (++noProgressTicks >= NO_PROGRESS_NOTICE_TICKS) {
            noProgressTicks = 0;
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic paused: move closer or obtain the required block."), true);
        }
    }

    private int findNextWorkItem(Minecraft client) {
        if (schematic == null || origin == null || completed == null || schematic.blocks().isEmpty()) return -1;
        int size = schematic.blocks().size();
        for (int offset = 0; offset < size; offset++) {
            int index = (cursor + offset) % size;
            if (completed.get(index)) continue;
            Schematic.BlockEntry entry = schematic.blocks().get(index);
            BlockPos target = origin.offset(entry.x(), entry.y(), entry.z());
            if (!client.level.isInWorldBounds(target)) return index;
            if (client.level.getBlockState(target).is(entry.state().getBlock()) || hasReachableSupport(client, target)) {
                return index;
            }
        }
        return -1;
    }

    private boolean hasReachableSupport(Minecraft client, BlockPos target) {
        double maxRangeSquared = MAX_PLACEMENT_RANGE * MAX_PLACEMENT_RANGE;
        Vec3 eye = client.player.getEyePosition();
        for (Direction supportDirection : Direction.values()) {
            BlockPos support = target.relative(supportDirection);
            if (!client.level.isInWorldBounds(support)) continue;
            if (!client.level.getBlockState(support).isSolidRender()) continue;
            Direction face = supportDirection.getOpposite();
            Vec3 hit = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.49,
                    face.getStepY() * 0.49,
                    face.getStepZ() * 0.49);
            if (eye.distanceToSqr(hit) <= maxRangeSquared) return true;
        }
        return false;
    }

    private void loadSchematic(Minecraft client) {
        loaded = true;
        Path directory = client.gameDirectory.toPath().resolve("config/our-client1/schematics");
        Path file = directory.resolve(requestedFile).normalize();
        if (!file.getParent().equals(directory) || Files.isSymbolicLink(file)) {
            schematic = null;
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Invalid schematic path."), true);
            return;
        }
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                Files.writeString(file, "{\n  \"name\": \"example\",\n  \"blocks\": []\n}\n",
                        StandardOpenOption.CREATE_NEW);
            }
            long size = Files.size(file);
            if (size > MAX_SCHEMATIC_FILE_BYTES) {
                throw new IllegalArgumentException("Schematic file is too large (max 8 MiB)");
            }
            schematic = Schematic.parse(Files.readString(file), requestedFile);
        } catch (IOException | RuntimeException exception) {
            schematic = null;
            String message = exception.getMessage();
            if (message == null || message.isBlank()) message = "Invalid schematic format";
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Schematic error: " + message), true);
        }
    }

    private void abortBuild(Minecraft client, String message) {
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
        setEnabled(false);
        ClientConfig config = OurClient.config();
        if (config != null) {
            config.schematicBuilder = false;
            config.save(OurClient.configPath(client));
        }
    }

    private boolean place(Minecraft client, BlockPos target, BlockState wanted) {
        if (!client.level.isInWorldBounds(target)) return false;
        int slot = findBlockSlot(client.player, wanted.getBlock());
        if (slot < 0) return false;
        client.player.getInventory().setSelectedSlot(slot);

        double maxRangeSquared = MAX_PLACEMENT_RANGE * MAX_PLACEMENT_RANGE;
        Vec3 eye = client.player.getEyePosition();
        for (Direction supportDirection : Direction.values()) {
            BlockPos support = target.relative(supportDirection);
            if (!client.level.isInWorldBounds(support)) continue;
            if (!client.level.getBlockState(support).isSolidRender()) continue;
            Direction face = supportDirection.getOpposite();
            Vec3 hit = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.49,
                    face.getStepY() * 0.49,
                    face.getStepZ() * 0.49);
            if (eye.distanceToSqr(hit) > maxRangeSquared) continue;
            BlockHitResult result = new BlockHitResult(hit, face, support, false);
            InteractionResult action = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, result);
            if (action.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private int findBlockSlot(LocalPlayer player, Block block) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) return slot;
        }
        return -1;
    }

    private void restoreSelectedSlot() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && previousSelectedSlot >= 0 && previousSelectedSlot < 9) {
            client.player.getInventory().setSelectedSlot(previousSelectedSlot);
        }
        previousSelectedSlot = -1;
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
        completed = null;
        loaded = false;
        schematic = null;
        noProgressTicks = 0;
        retryCooldown = 0;
    }
}
