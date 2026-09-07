package peli1gamer.ourclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Vector3d;

/** Client-only free camera. The real player remains at the original server position. */
public final class FreecamModule implements ToggleableModule {
    private static final double BASE_SPEED = 0.55D;
    private static final double SPRINT_MULTIPLIER = 3.0D;
    private static final double MOUSE_SENSITIVITY = 0.12D;

    private boolean enabled;
    private ItemEntity camera;
    private double cursorX;
    private double cursorY;

    @Override
    public String id() {
        return "freecam";
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;

        Minecraft client = Minecraft.getInstance();
        if (enabled) {
            enter(client);
        } else {
            exit(client);
        }
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
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

        camera = new ItemEntity(client.level, player.getX(), player.getEyeY() - 0.25D, player.getZ(), net.minecraft.world.item.ItemStack.EMPTY);
        camera.setYRot(player.getYRot());
        camera.setXRot(player.getXRot());

        client.setCameraEntity(camera);
        recenterCursor(client);
    }

    private void exit(Minecraft client) {
        if (client.player != null) {
            client.setCameraEntity(client.player);
        }
        camera = null;
    }

    private void updateLook(Minecraft client) {
        long window = client.getWindow().getWindow();
        double[] x = new double[1];
        double[] y = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(window, x, y);

        double dx = x[0] - cursorX;
        double dy = y[0] - cursorY;
        if (dx != 0.0D || dy != 0.0D) {
            camera.setYRot(camera.getYRot() + (float) (dx * MOUSE_SENSITIVITY));
            camera.setXRot(clampPitch(camera.getXRot() + (float) (dy * MOUSE_SENSITIVITY)));
        }
        recenterCursor(client);
    }

    private void updateMovement(Minecraft client) {
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
        long window = client.getWindow().getWindow();
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
