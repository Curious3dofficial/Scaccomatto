import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;

public final class FullscreenToggle {
    private static final int ANIMATION_MS = 10;
    private static final int FRAME_MS = 8;
    private static final String BOUNDS_KEY = "fullscreen.bounds";
    private static final String STATE_KEY = "fullscreen.extendedState";
    private static final String TIMER_KEY = "fullscreen.animationTimer";
    private static boolean installed = false;

    private FullscreenToggle() {
    }

    public static void installGlobalF11Shortcut() {
        if (installed) return;
        installed = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_F11) {
                return false;
            }
            JFrame frame = findActiveFrame();
            if (frame != null) {
                toggle(frame);
                return true;
            }
            return false;
        });
    }

    public static void toggle(JFrame frame) {
        if (frame == null) return;
        SwingUtilities.invokeLater(() -> toggleNow(frame));
    }

    public static void enter(JFrame frame) {
        if (frame == null) return;
        SwingUtilities.invokeLater(() -> {
            GraphicsDevice device = frame.getGraphicsConfiguration() != null
                ? frame.getGraphicsConfiguration().getDevice()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (device.getFullScreenWindow() != frame) {
                enterFullscreen(frame, device);
            }
        });
    }

    private static void toggleNow(JFrame frame) {
        GraphicsDevice device = frame.getGraphicsConfiguration() != null
            ? frame.getGraphicsConfiguration().getDevice()
            : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Window fullscreenWindow = device.getFullScreenWindow();
        if (fullscreenWindow == frame) {
            exitFullscreen(frame, device);
        } else {
            enterFullscreen(frame, device);
        }
    }

    private static void enterFullscreen(JFrame frame, GraphicsDevice device) {
        frame.getRootPane().putClientProperty(BOUNDS_KEY, frame.getBounds());
        frame.getRootPane().putClientProperty(STATE_KEY, frame.getExtendedState());

        frame.setExtendedState(JFrame.NORMAL);
        Rectangle target = getScreenBounds(frame);
        animateBounds(frame, frame.getBounds(), target, () -> {
            device.setFullScreenWindow(frame);
            frame.validate();
        });
    }

    private static void exitFullscreen(JFrame frame, GraphicsDevice device) {
        device.setFullScreenWindow(null);

        Object oldBounds = frame.getRootPane().getClientProperty(BOUNDS_KEY);
        Object oldState = frame.getRootPane().getClientProperty(STATE_KEY);
        Rectangle target = oldBounds instanceof Rectangle ? (Rectangle) oldBounds : null;
        if (target == null) {
            target = frame.getBounds();
        }

        frame.setExtendedState(JFrame.NORMAL);
        Rectangle start = getScreenBounds(frame);
        frame.setBounds(start);
        final Rectangle restoreBounds = target;
        animateBounds(frame, start, restoreBounds, () -> {
            if (oldState instanceof Integer) {
                frame.setExtendedState((Integer) oldState);
            }
            frame.validate();
        });
    }

    private static Rectangle getScreenBounds(JFrame frame) {
        GraphicsConfiguration config = frame.getGraphicsConfiguration();
        if (config != null) {
            return config.getBounds();
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    private static void animateBounds(JFrame frame, Rectangle from, Rectangle to, Runnable done) {
        Object existing = frame.getRootPane().getClientProperty(TIMER_KEY);
        if (existing instanceof Timer) {
            ((Timer) existing).stop();
        }

        long startMs = System.currentTimeMillis();
        Timer timer = new Timer(FRAME_MS, null);
        timer.addActionListener(e -> {
            float linear = (System.currentTimeMillis() - startMs) / (float) ANIMATION_MS;
            float t = Math.max(0f, Math.min(1f, linear));
            float eased = smootherStep(t);
            frame.setBounds(interpolate(from, to, eased));
            frame.revalidate();
            frame.repaint();
            if (t >= 1f) {
                timer.stop();
                frame.getRootPane().putClientProperty(TIMER_KEY, null);
                frame.setBounds(to);
                if (done != null) done.run();
            }
        });
        frame.getRootPane().putClientProperty(TIMER_KEY, timer);
        timer.start();
    }

    private static Rectangle interpolate(Rectangle from, Rectangle to, float t) {
        int x = Math.round(from.x + (to.x - from.x) * t);
        int y = Math.round(from.y + (to.y - from.y) * t);
        int w = Math.round(from.width + (to.width - from.width) * t);
        int h = Math.round(from.height + (to.height - from.height) * t);
        return new Rectangle(x, y, Math.max(1, w), Math.max(1, h));
    }

    private static float smootherStep(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static JFrame findActiveFrame() {
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        while (active != null) {
            if (active instanceof JFrame) {
                return (JFrame) active;
            }
            active = active.getOwner();
        }
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame && window.isActive()) {
                return (JFrame) window;
            }
        }
        return null;
    }
}
