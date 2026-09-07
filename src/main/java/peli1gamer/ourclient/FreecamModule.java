package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Client-only free camera. The real player remains at the original server position. */
public final class FreecamModule implements ToggleableModule {
    private static final double BASE_SPEED = 0.55D;
    private static final double SPRINT_MULTIPLIER = 3.0D;

    private boolean enabled;
    private ItemEntity camera;
    private Entity previousCamera;
    private ClientInput previousInput;
    private LocalPlayer inputOwner;
    private float lockedPlayerYaw;
    private float lockedPlayerPitch;

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
            } catch (RuntimeException exception) {
                this.enabled = false;
                restoreInput(client);
                restoreCamera(client);
                camera = null;
                previousCamera = null;
                OurClient.LOGGER.error("Could not enable freecam; state was rolled back", exception);
                syncDisabledConfig();
            }
        } else {
            exit(client);
        }
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

        // Let Minecraft's normal mouse handling calculate the exact same rotation
        // deltas it would use in ordinary gameplay, then transfer those deltas to
        // the free camera and put the real player back at its original rotation.
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
        updateMovement(client);
        syncCameraTransform();
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
        syncCameraTransform();
        client.setCameraEntity(camera);

        // Use Minecraft's normal cursor grabbing rather than manually moving the
        // GLFW cursor every tick. This removes the jitter/over-rotation caused by
        // the old recentering implementation.
        client.mouseHandler.grabMouse();
    }

    private void exit(Minecraft client) {
        restoreInput(client);
        restoreCamera(client);
        camera = null;
        previousCamera = null;
    }

    private void cleanupAfterWorldLoss(Minecraft client) {
        restoreInput(client);
        restoreCamera(client);
        enabled = false;
        camera = null;
        previousCamera = null;
        previousInput = null;
        inputOwner = null;
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

    private void updateMovement(Minecraft client) {
        if (camera == null) return;
        double forward = (client.options.keyUp.isDown() ? 1.0D : 0.0D) - (client.options.keyDown.isDown() ? 1.0D : 0.0D);
        double strafe = (client.options.keyRight.isDown() ? 1.0D : 0.0D) - (client.options.keyLeft.isDown() ? 1.0D : 0.0D);
        double vertical = (client.options.keyJump.isDown() ? 1.0D : 0.0D) - (client.options.keyShift.isDown() ? 1.0D : 0.0D);
        if (forward == 0.0D && strafe == 0.0D && vertical == 0.0D) return;

        double length = Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0D) {
            forward /= length;
            strafe /= length;
        }

        double speed = BASE_SPEED * (client.options.keySprint.isDown() ? SPRINT_MULTIPLIER : 1.0D);
        double yaw = Math.toRadians(camera.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double mx = strafe * cos - forward * sin;
        double mz = strafe * sin + forward * cos;
        camera.setPos(camera.getX() + mx * speed, camera.getY() + vertical * speed, camera.getZ() + mz * speed);
    }

    private void syncCameraTransform() {
        if (camera == null) return;
        camera.xo = camera.getX();
        camera.yo = camera.getY();
        camera.zo = camera.getZ();
        camera.xRotO = camera.getXRot();
        camera.yRotO = camera.getYRot();
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
