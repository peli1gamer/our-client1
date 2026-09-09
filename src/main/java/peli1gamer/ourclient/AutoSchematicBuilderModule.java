package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private static final int MAX_WORK_ITEMS_SCANNED_PER_TICK = 4096;
    private static final int NO_PROGRESS_NOTICE_TICKS = 40;
    private static final int RETRY_DELAY_TICKS = 2;
    private static final int LOAD_RETRY_DELAY_TICKS = 40;
    private static final long MAX_SCHEMATIC_FILE_BYTES = 8L * 1024L * 1024L;

    private boolean enabled;
    private Schematic schematic;
    private BlockPos origin;
    private int cursor;
    private BitSet completed;
    private boolean loaded;
    private int noProgressTicks;
    private int retryCooldown;
    private int loadRetryCooldown;
    private int previousSelectedSlot = -1;
    private int activePlacementSlot = -1;
    private ClientLevel activeLevel;
    private LocalPlayer activePlayer;
    private String requestedFile = "build.json";

    @Override public String id() { return "auto-schematic-builder"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            Minecraft client = Minecraft.getInstance();
            previousSelectedSlot = client.player == null ? -1 : client.player.getInventory().getSelectedSlot();
            activePlacementSlot = -1;
            activeLevel = client.level;
            activePlayer = client.player;
            loadRetryCooldown = 0;
        } else {
            restoreSelectedSlot();
            resetProgress();
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.gameMode == null
                || client.screen != null) return;
        if (!client.player.isAlive()) {
            restoreSelectedSlot();
            return;
        }

        if (activePlayer != null && activePlayer != client.player) {
            restoreSelectedSlot();
            resetProgress();
            activePlayer = client.player;
            activeLevel = client.level;
            previousSelectedSlot = client.player.getInventory().getSelectedSlot();
            activePlacementSlot = -1;
            return;
        }

        if (activeLevel != client.level) {
            restoreSelectedSlot();
            resetProgress();
            activeLevel = client.level;
            activePlayer = client.player;
            previousSelectedSlot = client.player.getInventory().getSelectedSlot();
            activePlacementSlot = -1;
        }

        if (!loaded) {
            if (loadRetryCooldown > 0) {
                loadRetryCooldown--;
                return;
            }
            loadSchematic(client);
            if (!loaded) {
                loadRetryCooldown = LOAD_RETRY_DELAY_TICKS;
                return;
            }
        }
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

            if (current.equals(wanted)) {
                completed.set(index);
                cursor = (index + 1) % schematic.blocks().size();
                madeProgress = true;
                continue;
            }

            if (retryCooldown > 0) break;
            if (place(client, target, wanted)) {
                attempts++;
                if (client.level.getBlockState(target).equals(wanted)) {
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
            ClientModuleManager manager = OurClient.modules();
            if (!manager.setEnabled(id(), false)) setEnabled(false);
            try {
                OurClient.syncAndSaveConfigFromModules();
            } catch (RuntimeException exception) {
                OurClient.LOGGER.error("Could not persist schematic builder completion state", exception);
            }
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
        int limit = Math.min(size, MAX_WORK_ITEMS_SCANNED_PER_TICK);
        for (int offset = 0; offset < limit; offset++) {
            int index = (cursor + offset) % size;
            if (completed.get(index)) continue;
            Schematic.BlockEntry entry = schematic.blocks().get(index);
            BlockPos target = origin.offset(entry.x(), entry.y(), entry.z());
            if (!client.level.isInWorldBounds(target)) return index;
            if (client.level.getBlockState(target).equals(entry.state()) || hasReachableSupport(client, target)) return index;
        }
        cursor = (cursor + limit) % size;
        return -1;
    }

    private boolean hasReachableSupport(Minecraft client, BlockPos target) {
        double maxRangeSquared = MAX_PLACEMENT_RANGE * MAX_PLACEMENT_RANGE;
        Vec3 eye = client.player.getEyePosition();
        for (Direction supportDirection : Direction.values()) {
            BlockPos support = target.relative(supportDirection);
            if (!client.level.isInWorldBounds(support)) continue;
            if (!hasUsableSupport(client, support)) continue;
            Direction face = supportDirection.getOpposite();
            Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
            if (eye.distanceToSqr(hit) <= maxRangeSquared) return true;
        }
        return false;
    }

    private boolean hasUsableSupport(Minecraft client, BlockPos support) {
        if (!client.level.isInWorldBounds(support)) return false;
        return !client.level.getBlockState(support).getCollisionShape(client.level, support).isEmpty();
    }

    private void loadSchematic(Minecraft client) {
        Path directory = client.gameDirectory.toPath().resolve("config/our-client1/schematics");
        Path file = directory.resolve(requestedFile).normalize();
        if (!file.getParent().equals(directory) || Files.isSymbolicLink(file)) {
            schematic = null;
            loaded = false;
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Invalid schematic path."), true);
            return;
        }
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                Files.writeString(file, "{\n  \"name\": \"example\",\n  \"blocks\": []\n}\n", StandardOpenOption.CREATE_NEW);
                schematic = null;
                loaded = false;
                client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Created empty schematic at config/our-client1/schematics/" + requestedFile + ". Add blocks before enabling the builder."), true);
                return;
            }
            long size = Files.size(file);
            if (size > MAX_SCHEMATIC_FILE_BYTES) throw new IllegalArgumentException("Schematic file is too large (max 8 MiB)");
            String json = Files.readString(file);
            Schematic parsed = Schematic.parse(json, requestedFile);
            if (parsed.blocks().isEmpty()) {
                schematic = null;
                loaded = false;
                client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Schematic is empty. Add at least one block before enabling the builder."), true);
                return;
            }
            schematic = parsed;
            loaded = true;
        } catch (IOException | RuntimeException exception) {
            schematic = null;
            loaded = false;
            String message = exception.getMessage();
            if (message == null || message.isBlank()) message = "Invalid schematic format";
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Schematic error: " + message), true);
        }
    }

    private void abortBuild(Minecraft client, String message) {
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
        ClientModuleManager manager = OurClient.modules();
        if (!manager.setEnabled(id(), false)) setEnabled(false);
        try {
            OurClient.syncAndSaveConfigFromModules();
        } catch (RuntimeException exception) {
            OurClient.LOGGER.error("Could not persist schematic builder abort state", exception);
        }
    }

    private boolean place(Minecraft client, BlockPos target, BlockState wanted) {
        if (!client.level.isInWorldBounds(target)) return false;
        int slot = findBlockSlot(client.player, wanted.getBlock());
        if (slot < 0) return false;
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (activePlacementSlot < 0) previousSelectedSlot = currentSlot;
        else if (currentSlot != activePlacementSlot) previousSelectedSlot = currentSlot;
        if (currentSlot != slot) client.player.getInventory().setSelectedSlot(slot);
        activePlacementSlot = slot;

        try {
            for (Direction direction : Direction.values()) {
                BlockPos support = target.relative(direction);
                if (!client.level.isInWorldBounds(support)) continue;
                if (!hasUsableSupport(client, support)) continue;
                Direction face = direction.getOpposite();
                Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
                if (client.player.getEyePosition().distanceToSqr(hit) > MAX_PLACEMENT_RANGE * MAX_PLACEMENT_RANGE) continue;
                BlockHitResult result = new BlockHitResult(hit, face, support, false);
                InteractionResult interaction = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, result);
                if (interaction.consumesAction()) {
                    client.player.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
            }
            return false;
        } finally {
            restoreSelectedSlot();
        }
    }

    private int findBlockSlot(LocalPlayer player, Block wanted) {
        int hotbarSize = Math.min(9, player.getInventory().getContainerSize());
        for (int slot = 0; slot < hotbarSize; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == wanted) return slot;
        }
        return -1;
    }

    private void restoreSelectedSlot() {
        if (activePlayer == null || previousSelectedSlot < 0 || activePlacementSlot < 0) return;
        if (activePlayer.getInventory().getSelectedSlot() == activePlacementSlot) {
            activePlayer.getInventory().setSelectedSlot(previousSelectedSlot);
        }
        previousSelectedSlot = -1;
        activePlacementSlot = -1;
    }

    private void resetProgress() {
        schematic = null;
        origin = null;
        cursor = 0;
        completed = null;
        loaded = false;
        noProgressTicks = 0;
        retryCooldown = 0;
        loadRetryCooldown = 0;
        previousSelectedSlot = -1;
        activePlacementSlot = -1;
        activeLevel = null;
        activePlayer = null;
    }

    private void sortBlocks() {
        if (schematic == null) return;
        List<Schematic.BlockEntry> sorted = new ArrayList<>(schematic.blocks());
        sorted.sort(Comparator.comparingInt(Schematic.BlockEntry::y)
                .thenComparingInt(Schematic.BlockEntry::z)
                .thenComparingInt(Schematic.BlockEntry::x));
        schematic = new Schematic(schematic.name(), List.copyOf(sorted));
    }

    public void setRequestedFile(String requestedFile) {
        if (requestedFile == null || requestedFile.isBlank()) return;
        this.requestedFile = requestedFile;
        if (enabled) {
            restoreSelectedSlot();
            resetProgress();
        }
    }
}