package peli1gamer.ourclient;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Client-only free camera. The real player remains at the original server position. */
public final class FreecamModule implements ToggleableModule {
    private static final double BASE_SPEED = 0.55D;
    private static final double SPRINT_MULTIPLIER = 3.0D;
    private static final double MAX_FRAME_SECONDS = 0.1D;
    private static boolean renderHookInstalled;
    private static FreecamModule activeInstance;

    private boolean enabled;
    private ItemEntity camera;
    private Entity previousCamera;
    private ClientInput previousInput;
    private LocalPlayer inputOwner;
    private float lockedPlayerYaw;
    private float lockedPlayerPitch;
    private long lastRenderNanos;

    public FreecamModule() { installRenderHook(); }

    @Override public String id() { return "freecam"; }
    @Override public boolean enabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        Minecraft client = Minecraft.getInstance();
        if (enabled) {
            try {
                enter(client);
                if (this.enabled) {
                    activeInstance = this;
                    lastRenderNanos = System.nanoTime();
                }
            } catch (RuntimeException exception) {
                this.enabled = false;
                restoreInput(client);
                restoreCamera(client);
                camera = null;
                previousCamera = null;
                if (activeInstance == this) activeInstance = null;
                OurClient.LOGGER.error("Could not enable freecam; state was rolled back", exception);
                syncDisabledConfig();
            }
        } else {
            exit(client);
        }
    }

    private static void installRenderHook() {
        if (renderHookInstalled) return;
        renderHookInstalled = true;
        WorldRenderEvents.START_MAIN.register(context -> {
            FreecamModule instance = activeInstance;
            if (instance == null || !instance.enabled) return;
            try {
                long now = System.nanoTime();
                double deltaSeconds = (now - instance.lastRenderNanos) / 1_000_000_000.0D;
                instance.lastRenderNanos = now;
                if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0D) deltaSeconds = 0.0D;
                deltaSeconds = Math.min(deltaSeconds, MAX_FRAME_SECONDS);
                instance.updateMovement(Minecraft.getInstance(), deltaSeconds);
            } catch (RuntimeException exception) {
                instance.enabled = false;
                if (activeInstance == instance) activeInstance = null;
                instance.restoreInput(Minecraft.getInstance());
                instance.restoreCamera(Minecraft.getInstance());
                instance.camera = null;
                instance.previousCamera = null;
                OurClient.LOGGER.error("Disabling freecam after a render-frame movement failure", exception);
                instance.syncDisabledConfig();
            }
        });
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled && OurClient.config() != null && OurClient.config().freecam
                && client.player != null && client.level != null && client.screen == null) {
            setEnabled(true);
        }
        if (!enabled) return;
        if (client.player == null || client.level == null) {
            cleanupAfterWorldLoss(client);
            return;
        }
        if (client.screen != null) return;

        if (camera == null) {
            enter(client);
            if (!enabled) return;
        }

        LocalPlayer player = client.player;
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();
        float yawDelta = wrapDegrees(playerYaw - lockedPlayerYaw);
        float pitchDelta = playerPitch - lockedPlayerPitch;

        if (yawDelta != 0.0F || pitchDelta != 0.0F) {
            camera.setYRot(camera.getYRot() + yawDelta);
            camera.setXRot(clampPitch(camera.getXRot() + pitchDelta));
            player.setYRot(lockedPlayerYaw);
            player.setXRot(lockedPlayerPitch);
        }

        freezePlayerInput(player);
        // Movement is handled once per rendered frame using a wall-clock delta.
        // The clamp prevents a long pause/window switch from causing a huge jump.
    }

    private void enter(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            enabled = false;
            return;
        }

        previousCamera = client.getCameraEntity();
        lockedPlayerYaw = player.getYRot();
        lockedPlayerPitch = player.getXRot();
        freezePlayerInput(player);

        camera = new ItemEntity(client.level, player.getX(), player.getEyeY() - 0.25D, player.getZ(),
                net.minecraft.world.item.ItemStack.EMPTY);
        camera.setNoGravity(true);
        camera.setInvulnerable(true);
        camera.setInvisible(true);
        camera.setYRot(lockedPlayerYaw);
        camera.setXRot(lockedPlayerPitch);
        client.setCameraEntity(camera);
        client.mouseHandler.grabMouse();
    }

    private void exit(Minecraft client) {
        if (activeInstance == this) activeInstance = null;
        restoreInput(client);
        restoreCamera(client);
        camera = null;
        previousCamera = null;
        lastRenderNanos = 0L;
    }

    private void cleanupAfterWorldLoss(Minecraft client) {
        if (activeInstance == this) activeInstance = null;
        restoreInput(client);
        restoreCamera(client);
        enabled = false;
        camera = null;
        previousCamera = null;
        previousInput = null;
        inputOwner = null;
        lastRenderNanos = 0L;
        syncDisabledConfig();
    }

    private void syncDisabledConfig() {
        ClientConfig config = OurClient.config();
        if (config != null) {
            config.freecam = false;
            try {
                config.save(OurClient.configPath(Minecraft.getInstance()));
            } catch (RuntimeException exception) {
                OurClient.LOGGER.warn("Could not persist freecam shutdown state", exception);
            }
        }
    }

    private void freezePlayerInput(LocalPlayer player) {
        if (inputOwner == player) return;
        previousInput = player.input;
        player.input = new ClientInput();
        inputOwner = player;
    }

    private void restoreInput(Minecraft client) {
        if (inputOwner == null || previousInput == null) return;
        if (client.player == inputOwner) inputOwner.input = previousInput;
        previousInput = null;
        inputOwner = null;
    }

    private void restoreCamera(Minecraft client) {
        Entity restore = previousCamera;
        if (restore != null && restore != camera && restore.isAlive() && restore.level() == client.level) {
            client.setCameraEntity(restore);
        } else if (client.player != null) {
            client.setCameraEntity(client.player);
        }
    }

    private void updateMovement(Minecraft client, double deltaSeconds) {
        if (camera == null || client.player == null || client.level == null || client.screen != null) return;
        double forward = (client.options.keyUp.isDown() ? 1.0D : 0.0D) - (client.options.keyDown.isDown() ? 1.0D : 0.0D);
        double strafe = (client.options.keyRight.isDown() ? 1.0D : 0.0D) - (client.options.keyLeft.isDown() ? 1.0D : 0.0D);
        double vertical = (client.options.keyJump.isDown() ? 1.0D : 0.0D) - (client.options.keyShift.isDown() ? 1.0D : 0.0D);
        if (forward == 0.0D && strafe == 0.0D && vertical == 0.0D) return;
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0D) return;

        double length = Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0D) {
            forward /= length;
            strafe /= length;
        }

        // BASE_SPEED is the old blocks-per-tick value. Convert it to blocks/sec
        // so the same speed is maintained regardless of render FPS.
        double speed = BASE_SPEED * (client.options.keySprint.isDown() ? SPRINT_MULTIPLIER : 1.0D);
        double distance = speed * deltaSeconds * 20.0D;
        double yaw = Math.toRadians(camera.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double mx = strafe * cos - forward * sin;
        double mz = -strafe * sin + forward * cos;
        camera.setPos(camera.getX() + mx * distance, camera.getY() + vertical * distance, camera.getZ() + mz * distance);
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
