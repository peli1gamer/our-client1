package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;

/** Client-only free camera. The real player remains at the original server position. */
public final class FreecamModule implements ToggleableModule {
    private static final double BASE_SPEED = 0.55D;
    private static final double SPRINT_MULTIPLIER = 3.0D;
    private static final double MOUSE_SENSITIVITY = 0.12D;

    private final DoubleBuffer cursorBuffer = BufferUtils.createDoubleBuffer(2);
    private boolean enabled;
    private ItemEntity camera;
    private Entity previousCamera;
    private ClientInput previousInput;
    private LocalPlayer inputOwner;
    private double cursorX;
    private double cursorY;

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
                enabled = false;
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

        freezePlayerInput(client.player);
        if (camera == null) {
            enter(client);
            if (!enabled) return;
        }

        updateLook(client);
        updateMovement(client);
    }

    private void enter(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            enabled = false;
            return;
        }

        previousCamera = client.getCameraEntity();
        freezePlayerInput(player);

        camera = new ItemEntity(client.level, player.getX(), player.getEyeY() - 0.25D, player.getZ(),
                net.minecraft.world.item.ItemStack.EMPTY);
        camera.setYRot(player.getYRot());
        camera.setXRot(player.getXRot());

        client.setCameraEntity(camera);
        recenterCursor(client);
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
        if (inputOwner != null && previousInput != null) {
            previousInput = null;
        }
        previousInput = player.input;
        player.input = new ClientInput();
        inputOwner = player;
    }

    private void restoreInput(Minecraft client) {
        if (inputOwner == null || previousInput == null) return;
        if (client.player == inputOwner) {
            inputOwner.input = previousInput;
        }
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

    private void updateLook(Minecraft client) {
        long window = client.getWindow().handle();
        if (window == 0L || camera == null) return;

        org.lwjgl.glfw.GLFW.glfwGetCursorPos(window, cursorBuffer);
        double dx = cursorBuffer.get(0) - cursorX;
        double dy = cursorBuffer.get(1) - cursorY;
        if (dx != 0.0D || dy != 0.0D) {
            camera.setYRot(camera.getYRot() + (float) (dx * MOUSE_SENSITIVITY));
            camera.setXRot(clampPitch(camera.getXRot() - (float) (dy * MOUSE_SENSITIVITY)));
        }
        recenterCursor(client);
    }

    private void updateMovement(Minecraft client) {
        if (camera == null) return;
        double forward = (client.options.keyUp.isDown() ? 1.0D : 0.0D)
                - (client.options.keyDown.isDown() ? 1.0D : 0.0D);
        double strafe = (client.options.keyRight.isDown() ? 1.0D : 0.0D)
                - (client.options.keyLeft.isDown() ? 1.0D : 0.0D);
        double vertical = (client.options.keyJump.isDown() ? 1.0D : 0.0D)
                - (client.options.keyShift.isDown() ? 1.0D : 0.0D);

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

        Vector3d movement = new Vector3d(
                strafe * cos - forward * sin,
                vertical,
                strafe * sin + forward * cos
        ).mul(speed);

        camera.setPos(camera.getX() + movement.x, camera.getY() + movement.y, camera.getZ() + movement.z);
    }

    private void recenterCursor(Minecraft client) {
        long window = client.getWindow().handle();
        if (window == 0L) return;

        double centerX = client.getWindow().getWidth() / 2.0D;
        double centerY = client.getWindow().getHeight() / 2.0D;
        cursorX = centerX;
        cursorY = centerY;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window, centerX, centerY);
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
