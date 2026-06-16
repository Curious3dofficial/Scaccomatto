import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Intro extends MainMenu {
    private static final int INTRO_FPS = 120;
    private static final long BLACK_HOLD_MS = 1000L;
    private static final long LOGO_ANIMATION_MS = 5000L;
    private static final long POST_LOGO_BLACK_HOLD_MS = 1000L;
    private static final long LINE_EXPAND_MS = 1100L;
    private static final long LINE_HOLD_MS = 600L;
    private static final long LINE_WIPE_MS = 630L;
    private static final long WHITE_HOLD_MS = 500L;
    private static final int MENU_FADE_IN_MS = 1500;
    private static final int LOGO_OPACITY_STEPS = 32;
    private static final long TOTAL_DURATION_MS = BLACK_HOLD_MS
            + LOGO_ANIMATION_MS
            + POST_LOGO_BLACK_HOLD_MS
            + LINE_EXPAND_MS
            + LINE_HOLD_MS
            + LINE_WIPE_MS
            + WHITE_HOLD_MS;
    private static final float LOGO_START_SCALE = 1.008f;
    private static final float LOGO_END_SCALE = 1.035f;

    private final IntroPanel introPanel;
    private boolean finished;
    private Timer menuFadeTimer;

    public Intro() {
        super(false, false);
        setTitle("Scaccomatto");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setBackground(Color.BLACK);

        introPanel = new IntroPanel(loadIntroImage(), this::finishIntro);
        JPanel introContainer = new JPanel(new BorderLayout());
        introContainer.setBackground(Color.BLACK);
        introContainer.add(introPanel, BorderLayout.CENTER);
        setContentPane(introContainer);
        installIntroAltF4Close();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_SPACE) {
                    finishIntro();
                }
            }
        });

        setVisible(true);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void installIntroAltF4Close() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK),
                "closeDuringIntro");
        actionMap.put("closeDuringIntro", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dispose();
                System.exit(0);
            }
        });
    }

    private void finishIntro() {
        if (finished) return;
        finished = true;
        introPanel.stopRendering();
        setTitle("MainWindow");
        showMainApplicationContent();
        revalidate();
        repaint();
        startMainMenuFadeIn();
    }

    private void startMainMenuFadeIn() {
        final float[] overlayAlpha = {1f};
        JComponent fadeOverlay = new JComponent() {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        overlayAlpha[0]));
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        fadeOverlay.setOpaque(false);
        setGlassPane(fadeOverlay);
        fadeOverlay.setVisible(true);

        long startedAtNanos = System.nanoTime();
        menuFadeTimer = AnimationTiming.createUiTimer(event -> {
            float progress = AnimationTiming.progress(startedAtNanos, MENU_FADE_IN_MS);
            float smoothProgress = progress * progress * (3f - 2f * progress);
            overlayAlpha[0] = 1f - smoothProgress;
            fadeOverlay.repaint();
            if (progress >= 1f) {
                ((Timer) event.getSource()).stop();
                menuFadeTimer = null;
                fadeOverlay.setVisible(false);
                mainMenuFocus();
            }
        });
        menuFadeTimer.start();
    }

    private void mainMenuFocus() {
        JComponent content = (JComponent) getContentPane();
        content.requestFocusInWindow();
    }

    private BufferedImage loadIntroImage() {
        try {
            URL imageUrl = getClass().getResource("/assets/screens/alekhine.png");
            if (imageUrl != null) return ImageIO.read(imageUrl);
        } catch (IOException ignored) {
        }

        String[] paths = {
            "src/assets/screens/alekhine.png",
            "Scaccomatto_final/Scaccomatto/src/assets/screens/alekhine.png",
            "assets/screens/alekhine.png"
        };
        for (String path : paths) {
            try {
                File imageFile = new File(path);
                if (imageFile.isFile()) return ImageIO.read(imageFile);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static final class IntroPanel extends Canvas {
        private final BufferedImage image;
        private final Runnable completion;
        private BufferedImage scaledLogo;
        private int scaledForWidth;
        private int scaledForHeight;
        private volatile boolean rendering;
        private volatile boolean completionQueued;
        private Thread renderThread;
        private long animationStartedAtNanos;

        IntroPanel(BufferedImage image, Runnable completion) {
            this.image = image;
            this.completion = completion;
            setBackground(Color.BLACK);
            setIgnoreRepaint(true);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startRendering();
        }

        @Override
        public void removeNotify() {
            stopRendering();
            super.removeNotify();
        }

        private synchronized void startRendering() {
            if (rendering) return;
            rendering = true;
            completionQueued = false;
            animationStartedAtNanos = System.nanoTime();
            renderThread = new Thread(this::renderLoop, "startup-intro-renderer");
            renderThread.setDaemon(true);
            renderThread.start();
        }

        synchronized void stopRendering() {
            rendering = false;
            if (renderThread != null) {
                renderThread.interrupt();
                renderThread = null;
            }
        }

        private void renderLoop() {
            long frameDurationNanos = 1_000_000_000L / INTRO_FPS;
            long nextFrameAt = System.nanoTime();
            while (rendering && isDisplayable()) {
                try {
                    if (getBufferStrategy() == null) {
                        createBufferStrategy(3);
                    }

                    long now = System.nanoTime();
                    long elapsedMs = Math.min(
                            (now - animationStartedAtNanos) / 1_000_000L,
                            TOTAL_DURATION_MS);
                    renderFrame(elapsedMs);
                    if (elapsedMs >= TOTAL_DURATION_MS) {
                        queueCompletion();
                        break;
                    }

                    nextFrameAt += frameDurationNanos;
                    long remainingNanos = nextFrameAt - System.nanoTime();
                    if (remainingNanos > 0L) {
                        long sleepMillis = remainingNanos / 1_000_000L;
                        int sleepNanos = (int) (remainingNanos % 1_000_000L);
                        Thread.sleep(sleepMillis, sleepNanos);
                    } else if (remainingNanos < -frameDurationNanos * 2L) {
                        nextFrameAt = System.nanoTime();
                    }
                } catch (IllegalStateException ignored) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void renderFrame(long elapsedMs) {
            BufferStrategy strategy = getBufferStrategy();
            if (strategy == null) return;
            do {
                do {
                    Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                    try {
                        drawFrame(g, elapsedMs);
                    } finally {
                        g.dispose();
                    }
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
        }

        private void queueCompletion() {
            if (completionQueued) return;
            completionQueued = true;
            SwingUtilities.invokeLater(completion);
        }

        private void drawFrame(Graphics2D g, long elapsedMs) {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            long logoElapsedMs = Math.max(0L, elapsedMs - BLACK_HOLD_MS);
            long revealElapsedMs = logoElapsedMs - LOGO_ANIMATION_MS - POST_LOGO_BLACK_HOLD_MS;
            drawWhiteReveal(g, revealElapsedMs);

            float logoProgress = clamp01(logoElapsedMs / (float) LOGO_ANIMATION_MS);
            float imageAlpha = introImageAlpha(logoProgress);
            if (image != null && imageAlpha > 0.001f) {
                drawLogo(g, logoProgress, imageAlpha);
            }
        }

        private void drawLogo(Graphics2D g, float progress, float imageAlpha) {
            ensureScaledLogos();
            if (scaledLogo == null) return;
            int logoWidth = scaledLogo.getWidth();
            int logoHeight = scaledLogo.getHeight();

            float growth = logoGrowthProgress(progress);
            float logoScale = LOGO_START_SCALE
                    + (LOGO_END_SCALE - LOGO_START_SCALE) * smootherStep(growth);
            float pulse = introPulse(progress);
            if (pulse > 0f) {
                float radius = Math.max(1f, Math.min(getWidth(), getHeight()) * 0.46f);
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        Math.min(1f, pulse * 0.34f)));
                g.setPaint(new RadialGradientPaint(
                        new Point(getWidth() / 2, getHeight() / 2),
                        radius,
                        new float[]{0f, 0.38f, 1f},
                        new Color[]{
                            new Color(255, 255, 255, 210),
                            new Color(184, 211, 255, 80),
                            new Color(0, 0, 0, 0)
                        }));
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            Graphics2D logoGraphics = (Graphics2D) g.create();
            logoGraphics.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    imageAlpha));
            logoGraphics.translate(getWidth() / 2.0, getHeight() / 2.0);
            logoGraphics.scale(logoScale, logoScale);
            logoGraphics.translate(-logoWidth / 2.0, -logoHeight / 2.0);
            logoGraphics.drawImage(scaledLogo, 0, 0, null);
            logoGraphics.dispose();
        }

        private void ensureScaledLogos() {
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            if (image == null || canvasWidth <= 0 || canvasHeight <= 0) return;
            if (scaledLogo != null
                    && scaledForWidth == canvasWidth
                    && scaledForHeight == canvasHeight) {
                return;
            }

            int logoWidth = canvasWidth;
            int logoHeight = Math.max(
                    1,
                    Math.round(logoWidth * image.getHeight() / (float) image.getWidth()));
            if (logoHeight > canvasHeight) {
                logoHeight = canvasHeight;
                logoWidth = Math.max(
                        1,
                        Math.round(logoHeight * image.getWidth() / (float) image.getHeight()));
            }

            scaledLogo = scaleImage(image, logoWidth, logoHeight);
            scaledForWidth = canvasWidth;
            scaledForHeight = canvasHeight;
        }

        private BufferedImage scaleImage(BufferedImage source, int width, int height) {
            if (source == null) return null;
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();
            return scaled;
        }

        private void drawWhiteReveal(Graphics2D g, long revealElapsedMs) {
            int width = getWidth();
            int height = getHeight();
            int lineThickness = 2;
            int lineY = (height - lineThickness) / 2;

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            if (revealElapsedMs < 0L) return;

            if (revealElapsedMs < LINE_EXPAND_MS) {
                float progress = clamp01(revealElapsedMs / (float) LINE_EXPAND_MS);
                int lineWidth = Math.max(2, Math.round(width * easeInOutSine(progress)));
                g.setColor(Color.WHITE);
                g.fillRect(0, lineY, lineWidth, lineThickness);
                return;
            }

            long wipeElapsedMs = revealElapsedMs - LINE_EXPAND_MS;
            if (wipeElapsedMs < LINE_HOLD_MS) {
                g.setColor(Color.WHITE);
                g.fillRect(0, lineY, width, lineThickness);
                return;
            }

            wipeElapsedMs -= LINE_HOLD_MS;
            if (wipeElapsedMs < LINE_WIPE_MS) {
                float progress = clamp01(wipeElapsedMs / (float) LINE_WIPE_MS);
                int wipeHeight = Math.max(
                        lineThickness,
                        Math.round(height * easeInOutCubic(progress)));
                int wipeY = (height - wipeHeight) / 2;
                g.setColor(Color.WHITE);
                g.fillRect(0, wipeY, width, wipeHeight);
                return;
            }

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        }

        private static float logoGrowthProgress(float progress) {
            return progress;
        }

        private static float introImageAlpha(float progress) {
            if (progress < 0.18f) {
                return opacityStep(smootherStep(progress / 0.18f));
            }
            if (progress < 0.76f) return 1f;
            return opacityStep(1f - smootherStep((progress - 0.76f) / 0.24f));
        }

        private static float opacityStep(float alpha) {
            float clamped = clamp01(alpha);
            float keyframePosition = clamped * (LOGO_OPACITY_STEPS - 1);
            int lowerStep = (int) Math.floor(keyframePosition);
            int upperStep = Math.min(LOGO_OPACITY_STEPS - 1, lowerStep + 1);
            float blend = keyframePosition - lowerStep;
            float lowerAlpha = lowerStep / (float) (LOGO_OPACITY_STEPS - 1);
            float upperAlpha = upperStep / (float) (LOGO_OPACITY_STEPS - 1);
            return lowerAlpha + (upperAlpha - lowerAlpha) * smootherStep(blend);
        }

        private static float introPulse(float progress) {
            float first = pulseAt(progress, 0.25f, 0.16f);
            float second = pulseAt(progress, 0.64f, 0.20f) * 0.72f;
            return Math.max(first, second);
        }

        private static float pulseAt(float progress, float center, float width) {
            float distance = Math.abs(progress - center) / Math.max(0.001f, width);
            if (distance >= 1f) return 0f;
            float edge = 1f - distance;
            return edge * edge * (3f - 2f * edge);
        }

        private static float smootherStep(float value) {
            float progress = clamp01(value);
            return progress * progress * progress
                    * (progress * (progress * 6f - 15f) + 10f);
        }

        private static float easeInOutCubic(float value) {
            float progress = clamp01(value);
            return progress < 0.5f
                    ? 4f * progress * progress * progress
                    : 1f - (float) Math.pow(-2f * progress + 2f, 3) / 2f;
        }

        private static float easeInOutSine(float value) {
            float progress = clamp01(value);
            return (float) (-(Math.cos(Math.PI * progress) - 1.0) / 2.0);
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }
}
