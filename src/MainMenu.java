import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.util.ArrayDeque;
import java.util.Deque;

public class MainMenu extends JFrame implements ActionListener {
    private static class LocalVariantOption {
        final String label;
        final String description;
        final String value;
        final String iconFile;
        final boolean enabled;

        LocalVariantOption(String label, String description, String value, String iconFile, boolean enabled) {
            this.label = label;
            this.description = description;
            this.value = value;
            this.iconFile = iconFile;
            this.enabled = enabled;
        }
    }
    
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel menuPanel;
    private JPanel localVariantPanel;
    private OnlinePanel onlineScreen;
    private AnalysisPanel analysisPanel;
    private Image backgroundImage;
    private java.util.Timer mainMenuAtmosphereTimer;
    private long mainMenuAtmosphereStartNanos;
    private final Object mainMenuAtmosphereFrameLock = new Object();
    private BufferedImage mainMenuAtmosphereFrontFrame;
    private BufferedImage mainMenuAtmosphereBackFrame;
    private volatile int mainMenuAtmosphereWidth;
    private volatile int mainMenuAtmosphereHeight;
    private Image appLogoImage;
    private String selectedLocalVariant = "Classic";
    private final java.util.List<JButton> localVariantButtons = new java.util.ArrayList<>();
    private JLabel localVariantStatusLabel;
    private JLabel mainTitleLabel;
    private final Deque<String> navigationHistory = new ArrayDeque<>();
    private String currentCard = "MENU";
    private JComponent activeScreen;
    private Runnable activeScreenCleanup;
    private ProportionalUiScaler proportionalUiScaler;
    private int screenDesignWidth;
    private int screenDesignHeight;
    private boolean exitDialogOpen = false;
    private JComponent gameLaunchOverlay;
    private Timer gameLaunchOverlayTimer;
    private long gameLaunchOverlayStartedAtNanos;
    private static final int GAME_LAUNCH_OVERLAY_MS = 3000;
    private static final int GAME_LAUNCH_CURTAIN_MS = 850;
    private Timer variantLaunchCinematicTimer;
    private static final int SPELL_LAUNCH_CHARGE_MS = 2000;
    private static final int SPELL_LAUNCH_CIRCLE_MS = 1000;
    private Timer variantCloseTimer;
    private static final int VARIANT_CLOSE_MS = 240;
    private AccountModalPane accountOverlay;
    private Timer accountOverlayTimer;
    private static final int ACCOUNT_OVERLAY_ANIMATION_MS = 300;
    private StartupIntroPane startupIntroPane;
    private Timer startupIntroTimer;
    private KeyEventDispatcher startupIntroKeyDispatcher;
    private volatile boolean startupIntroRunning;
    private static final long STARTUP_BLACK_HOLD_MS = 1000L;
    private static final long STARTUP_INTRO_MS = 5000L;
    private static final long STARTUP_POST_INTRO_BLACK_HOLD_MS = 1000L;
    private static final long STARTUP_LINE_EXPAND_MS = 1100L;
    private static final long STARTUP_LINE_HOLD_MS = 600L;
    private static final long STARTUP_LINE_WIPE_MS = 630L;
    private static final long STARTUP_WHITE_HOLD_MS = 500L;
    private static final long STARTUP_MENU_FADE_MS = 1600L;
    private static final long STARTUP_MENU_DRIFT_MS = 1000L;
    private static final long STARTUP_FRAME_NANOS = 1_000_000_000L / 60L;
    private static final float STARTUP_LOGO_START_SCALE = 1.008f;
    private static final float STARTUP_LOGO_SCALE = 1.035f;
    private ApplicationTopBar applicationTopBar;
    private AWTEventListener applicationTopBarMouseTracker;
    
    public MainMenu() {
        this(true, true);
    }

    public MainMenu(boolean playStartupIntro) {
        this(playStartupIntro, true);
    }

    protected MainMenu(boolean playStartupIntro, boolean showImmediately) {
        setTitle("Scaccomatto");
        Rectangle screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        screenDesignWidth = Math.max(1, screenBounds.width);
        screenDesignHeight = Math.max(1, screenBounds.height);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showExitConfirmationDialog();
            }
        });
        
        backgroundImage = loadBackgroundImage();
        appLogoImage = loadAppLogoImage();
        if (appLogoImage != null) {
            Image windowIcon = appLogoImage.getScaledInstance(256, 256, Image.SCALE_SMOOTH);
            setIconImage(windowIcon);
        }
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(5, 9, 11));
        
        menuPanel = MainWindow.createContentPanel(command ->
                actionPerformed(new ActionEvent(
                        MainMenu.this,
                        ActionEvent.ACTION_PERFORMED,
                        command)));
        
        mainPanel.add(menuPanel, "MENU");
        
        add(mainPanel);
        installApplicationTopBar();
        
        showCard("MENU", false);
        installGlobalEscapeBackKey();
        installAccountHotkey();
        installAltF4Hotkey();
        if (playStartupIntro) {
            installStartupIntro();
        }
        if (showImmediately) {
            setVisible(true);
        }
        proportionalUiScaler = new ProportionalUiScaler(
                mainPanel,
                screenDesignWidth,
                screenDesignHeight);
        if (playStartupIntro) {
            SwingUtilities.invokeLater(this::startStartupIntro);
        }
    }

    private void installApplicationTopBar() {
        JLayeredPane layeredPane = getLayeredPane();
        applicationTopBar = new ApplicationTopBar();
        layeredPane.add(
                applicationTopBar,
                Integer.valueOf(JLayeredPane.POPUP_LAYER.intValue() + 20));

        ComponentAdapter boundsUpdater = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                applicationTopBar.updateBounds();
            }

            @Override
            public void componentShown(ComponentEvent event) {
                applicationTopBar.updateBounds();
            }
        };
        layeredPane.addComponentListener(boundsUpdater);

        applicationTopBarMouseTracker = event -> {
            if (!(event instanceof MouseEvent) || applicationTopBar == null || !isShowing()) return;
            MouseEvent mouseEvent = (MouseEvent) event;
            if (!(mouseEvent.getSource() instanceof Component)) return;

            Component source = (Component) mouseEvent.getSource();
            if (SwingUtilities.getWindowAncestor(source) != MainMenu.this) return;

            Point pointer;
            try {
                pointer = SwingUtilities.convertPoint(source, mouseEvent.getPoint(), layeredPane);
            } catch (IllegalArgumentException ignored) {
                return;
            }
            applicationTopBar.trackPointer(pointer);
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
                applicationTopBarMouseTracker,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        SwingUtilities.invokeLater(applicationTopBar::updateBounds);
    }

    private final class ApplicationTopBar extends JPanel {
        private static final int BAR_HEIGHT = 42;
        private static final int REVEAL_ZONE_HEIGHT = 5;
        private static final int RETRACT_DISTANCE = 14;
        private static final int SLIDE_DURATION_MS = 115;
        private static final int HIDE_DELAY_MS = 260;

        private final Timer slideTimer;
        private final Timer hideTimer;
        private final java.util.List<JButton> windowControls = new java.util.ArrayList<>();
        private float revealProgress;
        private float slideStartProgress;
        private float slideTargetProgress;
        private long slideStartedAtNanos;

        ApplicationTopBar() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 1, 8));

            JLabel title = new JLabel("SCACCOMATTO");
            title.setForeground(new Color(226, 183, 93));
            title.setFont(UiFonts.title(16f).deriveFont(Font.BOLD));
            add(title, BorderLayout.WEST);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 5));
            controls.setOpaque(false);
            controls.add(createWindowControl("\u2014", false, () -> setState(Frame.ICONIFIED)));
            controls.add(createWindowControl("\u00d7", true, MainMenu.this::showExitConfirmationDialog));
            add(controls, BorderLayout.EAST);

            slideTimer = AnimationTiming.createUiTimer(event -> updateSlide());
            hideTimer = new Timer(HIDE_DELAY_MS, event -> animateTo(0f));
            hideTimer.setRepeats(false);
        }

        private JButton createWindowControl(String text, boolean closeButton, Runnable action) {
            JButton button = new JButton(text);
            button.setPreferredSize(new Dimension(38, 30));
            button.setFocusable(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setBackground(new Color(0, 0, 0, 0));
            button.setForeground(closeButton ? new Color(239, 91, 91) : new Color(215, 205, 181));
            button.setFont(new Font("SansSerif", Font.PLAIN, closeButton ? 24 : 18));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.putClientProperty("close_control", closeButton);
            windowControls.add(button);
            button.addActionListener(event -> action.run());
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    clearControlHoverStates(button);
                    button.setOpaque(true);
                    button.setBackground(closeButton
                            ? new Color(151, 43, 43)
                            : new Color(77, 69, 51));
                    button.setForeground(Color.WHITE);
                    button.repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    resetControlHover(button);
                }
            });
            return button;
        }

        private void resetControlHover(JButton button) {
            boolean closeButton = Boolean.TRUE.equals(
                    button.getClientProperty("close_control"));
            button.setOpaque(false);
            button.setBackground(new Color(0, 0, 0, 0));
            button.setForeground(closeButton
                    ? new Color(239, 91, 91)
                    : new Color(215, 205, 181));
            button.repaint();
        }

        private void clearControlHoverStates(JButton exception) {
            for (JButton control : windowControls) {
                if (control != exception) resetControlHover(control);
            }
        }

        void trackPointer(Point pointer) {
            Component hoveredComponent = SwingUtilities.getDeepestComponentAt(
                    getLayeredPane(),
                    pointer.x,
                    pointer.y);
            for (JButton control : windowControls) {
                if (hoveredComponent == null
                        || (hoveredComponent != control
                        && !SwingUtilities.isDescendingFrom(hoveredComponent, control))) {
                    resetControlHover(control);
                }
            }

            int y = pointer.y;
            if (y <= REVEAL_ZONE_HEIGHT) {
                hideTimer.stop();
                animateTo(1f);
                return;
            }
            if (y <= BAR_HEIGHT + RETRACT_DISTANCE) {
                hideTimer.stop();
            } else if (revealProgress > 0f && !hideTimer.isRunning()) {
                hideTimer.restart();
            }
        }

        void updateBounds() {
            JLayeredPane layeredPane = getLayeredPane();
            if (layeredPane == null) return;
            int y = Math.round(-BAR_HEIGHT * (1f - revealProgress));
            setBounds(0, y, layeredPane.getWidth(), BAR_HEIGHT);
            layeredPane.setLayer(this, JLayeredPane.POPUP_LAYER.intValue() + 20);
            layeredPane.moveToFront(this);
        }

        private void animateTo(float target) {
            if (target <= 0f) clearControlHoverStates(null);
            if (Math.abs(target - slideTargetProgress) < 0.001f
                    && (slideTimer.isRunning() || Math.abs(target - revealProgress) < 0.001f)) {
                return;
            }
            slideStartProgress = revealProgress;
            slideTargetProgress = target;
            slideStartedAtNanos = System.nanoTime();
            if (!slideTimer.isRunning()) slideTimer.start();
        }

        private void updateSlide() {
            float progress = AnimationTiming.progress(slideStartedAtNanos, SLIDE_DURATION_MS);
            float eased = 1f - (float) Math.pow(1f - progress, 3);
            revealProgress = slideStartProgress
                    + (slideTargetProgress - slideStartProgress) * eased;
            updateBounds();
            repaint();
            if (progress >= 1f) {
                revealProgress = slideTargetProgress;
                slideTimer.stop();
                updateBounds();
            }
        }

        void stopAnimations() {
            hideTimer.stop();
            slideTimer.stop();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(7, 12, 15, 246));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(218, 165, 66, 150));
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    protected void showMainApplicationContent() {
        setContentPane(mainPanel);
        showCard("MENU", false);
        mainPanel.revalidate();
        mainPanel.repaint();
        mainPanel.requestFocusInWindow();
    }

    private void installStartupIntro() {
        startupIntroPane = new StartupIntroPane(loadStartupIntroImage(), mainPanel);
        startupIntroKeyDispatcher = e -> {
            if ((e.getKeyCode() == KeyEvent.VK_SPACE
                    || e.getKeyCode() == KeyEvent.VK_ENTER
                    || e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    && startupIntroPane != null
                    && startupIntroPane.isVisible()) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    skipStartupIntro();
                }
                return true;
            }
            return false;
        };
        startupIntroPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                skipStartupIntro();
                event.consume();
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(startupIntroKeyDispatcher);
        setGlassPane(startupIntroPane);
        startupIntroRunning = true;
        startupIntroPane.setVisible(true);
    }

    private void startStartupIntro() {
        if (startupIntroPane == null) return;
        if (startupIntroTimer != null) startupIntroTimer.stop();
        long startedAt = System.nanoTime();
        long[] nextFrameAt = { startedAt };
        startupIntroTimer = new Timer(16, e -> {
            long elapsed = (long) AnimationTiming.elapsedMillis(startedAt);
            startupIntroPane.setElapsedMs(elapsed);
            if (elapsed >= STARTUP_BLACK_HOLD_MS + STARTUP_INTRO_MS
                    + STARTUP_POST_INTRO_BLACK_HOLD_MS
                    + STARTUP_LINE_EXPAND_MS
                    + STARTUP_LINE_HOLD_MS
                    + STARTUP_LINE_WIPE_MS
                    + STARTUP_WHITE_HOLD_MS
                    + STARTUP_MENU_FADE_MS) {
                ((Timer) e.getSource()).stop();
                startupIntroTimer = null;
                startupIntroRunning = false;
                startupIntroPane.setVisible(false);
                removeStartupIntroKeyDispatcher();
                mainPanel.requestFocusInWindow();
                return;
            }

            nextFrameAt[0] += STARTUP_FRAME_NANOS;
            long now = System.nanoTime();
            if (nextFrameAt[0] <= now) {
                nextFrameAt[0] = now + STARTUP_FRAME_NANOS;
            }
            long remainingNanos = nextFrameAt[0] - now;
            int nextDelayMs = Math.max(
                    1,
                    (int) Math.ceil(remainingNanos / 1_000_000.0));
            ((Timer) e.getSource()).setDelay(nextDelayMs);
        });
        startupIntroTimer.setCoalesce(true);
        startupIntroTimer.setInitialDelay(0);
        startupIntroTimer.start();
    }

    private void skipStartupIntro() {
        if (startupIntroPane == null || !startupIntroPane.isVisible()) return;
        if (startupIntroTimer != null) {
            startupIntroTimer.stop();
            startupIntroTimer = null;
        }
        startupIntroRunning = false;
        startupIntroPane.setVisible(false);
        removeStartupIntroKeyDispatcher();
        mainPanel.requestFocusInWindow();
    }

    private void removeStartupIntroKeyDispatcher() {
        if (startupIntroKeyDispatcher == null) return;
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(startupIntroKeyDispatcher);
        startupIntroKeyDispatcher = null;
    }

    private BufferedImage loadStartupIntroImage() {
        try {
            URL url = getClass().getResource("/assets/screens/alekhine.png");
            if (url != null) return ImageIO.read(url);
        } catch (IOException ignored) {
        }
        String[] paths = {
            "Scaccomatto_final/Scaccomatto/src/assets/screens/alekhine.png",
            "src/assets/screens/alekhine.png",
            "assets/screens/alekhine.png"
        };
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.isFile()) return ImageIO.read(file);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static class StartupIntroPane extends JComponent {
        private final BufferedImage image;
        private final BufferedImage redGlitchImage;
        private final BufferedImage cyanGlitchImage;
        private final JComponent menuSource;
        private BufferedImage menuSnapshot;
        private int snapshotWidth;
        private int snapshotHeight;
        private long elapsedMs;

        StartupIntroPane(BufferedImage image, JComponent menuSource) {
            this.image = image;
            this.redGlitchImage = createGlitchTint(image, true);
            this.cyanGlitchImage = createGlitchTint(image, false);
            this.menuSource = menuSource;
            setOpaque(false);
            setFocusable(true);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK
                    | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.KEY_EVENT_MASK);
        }

        void setElapsedMs(long elapsedMs) {
            this.elapsedMs = Math.max(0L, elapsedMs);
            repaint();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            e.consume();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            long introElapsedMs = Math.max(0L, elapsedMs - STARTUP_BLACK_HOLD_MS);
            long transitionElapsedMs = introElapsedMs - STARTUP_INTRO_MS
                    - STARTUP_POST_INTRO_BLACK_HOLD_MS;
            drawLineWipeTransition(g, transitionElapsedMs);

            float introT = clamp01(introElapsedMs / (float) STARTUP_INTRO_MS);
            float imageAlpha = introImageAlpha(introT);
            if (image != null && imageAlpha > 0.001f) {
                int baseW = getWidth();
                int baseH = Math.max(1, Math.round(baseW * image.getHeight()
                        / (float) image.getWidth()));
                if (baseH > getHeight()) {
                    baseH = getHeight();
                    baseW = Math.max(1, Math.round(baseH * image.getWidth()
                            / (float) image.getHeight()));
                }
                float growthT = logoGrowthProgress(introT);
                float logoScale = STARTUP_LOGO_START_SCALE
                        + (STARTUP_LOGO_SCALE - STARTUP_LOGO_START_SCALE)
                        * smootherStep(growthT);

                float pulse = introPulse(introT);
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
                        AlphaComposite.SRC_OVER, imageAlpha));
                logoGraphics.translate(getWidth() / 2.0, getHeight() / 2.0);
                logoGraphics.scale(logoScale, logoScale);
                logoGraphics.translate(-baseW / 2.0, -baseH / 2.0);
                logoGraphics.drawImage(
                        image,
                        0,
                        0,
                        baseW,
                        baseH,
                        null);
                logoGraphics.dispose();

                drawLogoGlitch(
                        g,
                        image,
                        baseW,
                        baseH,
                        logoScale,
                        imageAlpha,
                        introT);
            }

            g.dispose();
        }

        private static float logoGrowthProgress(float introT) {
            float glitchStart = 0.53f;
            float glitchEnd = 0.59f;
            if (introT < glitchStart) return introT;
            if (introT < glitchEnd) return glitchStart;
            return glitchStart
                    + (introT - glitchEnd)
                    * (1f - glitchStart)
                    / (1f - glitchEnd);
        }

        private void drawLogoGlitch(
                Graphics2D g,
                BufferedImage logo,
                int baseW,
                int baseH,
                float logoScale,
                float imageAlpha,
                float introT) {
            float glitchDistance = Math.abs(introT - 0.56f);
            float glitchStrength = 1f - clamp01(glitchDistance / 0.03f);
            if (glitchStrength <= 0f) return;

            Graphics2D glitchGraphics = (Graphics2D) g.create();
            glitchGraphics.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    Math.min(1f, imageAlpha * glitchStrength * 0.9f)));
            glitchGraphics.translate(getWidth() / 2.0, getHeight() / 2.0);
            glitchGraphics.scale(logoScale, logoScale);
            glitchGraphics.translate(-baseW / 2.0, -baseH / 2.0);

            int sliceCount = 9;
            for (int i = 0; i < sliceCount; i++) {
                int y1 = i * baseH / sliceCount;
                int y2 = (i + 1) * baseH / sliceCount;
                int direction = (i % 2 == 0) ? 1 : -1;
                int offset = Math.round(direction * (5f + (i % 3) * 3f) * glitchStrength);
                int colorOffset = Math.round((9f + (i % 3) * 4f) * glitchStrength);
                int sourceY1 = i * logo.getHeight() / sliceCount;
                int sourceY2 = (i + 1) * logo.getHeight() / sliceCount;

                if (redGlitchImage != null) {
                    glitchGraphics.drawImage(
                            redGlitchImage,
                            offset + colorOffset,
                            y1,
                            baseW + offset + colorOffset,
                            y2,
                            0,
                            sourceY1,
                            logo.getWidth(),
                            sourceY2,
                            null);
                }
                if (cyanGlitchImage != null) {
                    glitchGraphics.drawImage(
                            cyanGlitchImage,
                            offset - colorOffset,
                            y1,
                            baseW + offset - colorOffset,
                            y2,
                            0,
                            sourceY1,
                            logo.getWidth(),
                            sourceY2,
                            null);
                }
                glitchGraphics.drawImage(
                        logo,
                        offset,
                        y1,
                        baseW + offset,
                        y2,
                        0,
                        sourceY1,
                        logo.getWidth(),
                        sourceY2,
                        null);
            }
            glitchGraphics.dispose();
        }

        private static BufferedImage createGlitchTint(BufferedImage source, boolean red) {
            if (source == null) return null;
            BufferedImage tinted = new BufferedImage(
                    source.getWidth(),
                    source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    int sourceRed = (argb >>> 16) & 0xff;
                    int sourceGreen = (argb >>> 8) & 0xff;
                    int sourceBlue = argb & 0xff;
                    int luminance = Math.min(255, Math.round(
                            (sourceRed * 0.3f + sourceGreen * 0.59f + sourceBlue * 0.11f)
                            * 1.45f));
                    int color = red
                            ? (luminance << 16)
                            : (luminance << 8) | luminance;
                    tinted.setRGB(x, y, (alpha << 24) | color);
                }
            }
            return tinted;
        }

        private void drawLineWipeTransition(Graphics2D g, long transitionElapsedMs) {
            int width = getWidth();
            int height = getHeight();
            int lineThickness = 2;
            int lineY = (height - lineThickness) / 2;

            if (transitionElapsedMs < 0L) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                return;
            }

            if (transitionElapsedMs < STARTUP_LINE_EXPAND_MS) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                float t = clamp01(transitionElapsedMs / (float) STARTUP_LINE_EXPAND_MS);
                float expansion = easeInOutSine(t);
                int lineWidth = Math.max(2, Math.round(width * expansion));
                g.setColor(Color.WHITE);
                g.fillRect(0, lineY, lineWidth, lineThickness);
                return;
            }

            long wipeElapsedMs = transitionElapsedMs - STARTUP_LINE_EXPAND_MS;
            if (wipeElapsedMs < STARTUP_LINE_HOLD_MS) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                g.setColor(Color.WHITE);
                g.fillRect(0, lineY, width, lineThickness);
                return;
            }

            wipeElapsedMs -= STARTUP_LINE_HOLD_MS;
            if (wipeElapsedMs < STARTUP_LINE_WIPE_MS) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, width, height);
                float t = clamp01(wipeElapsedMs / (float) STARTUP_LINE_WIPE_MS);
                float wipe = easeInOutCubic(t);
                int wipeHeight = Math.max(lineThickness, Math.round(height * wipe));
                int wipeY = (height - wipeHeight) / 2;
                g.setColor(Color.WHITE);
                g.fillRect(0, wipeY, width, wipeHeight);
                return;
            }

            long fadeElapsedMs = wipeElapsedMs - STARTUP_LINE_WIPE_MS;
            if (fadeElapsedMs < STARTUP_WHITE_HOLD_MS) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
                return;
            }

            fadeElapsedMs -= STARTUP_WHITE_HOLD_MS;
            float fadeT = clamp01(fadeElapsedMs / (float) STARTUP_MENU_FADE_MS);
            float driftT = clamp01(fadeElapsedMs / (float) STARTUP_MENU_DRIFT_MS);
            float settleT = smootherStep(driftT);
            BufferedImage snapshot = getMenuSnapshot();
            if (snapshot != null) {
                int driftY = Math.round(-18f * (1f - settleT));
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(snapshot, 0, driftY, width, height, null);
            }
            float whiteAlpha = 1f - smootherStep(fadeT);
            if (whiteAlpha > 0f) {
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, whiteAlpha));
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
        }

        private BufferedImage getMenuSnapshot() {
            int width = getWidth();
            int height = getHeight();
            if (menuSource == null || width <= 0 || height <= 0) return null;
            if (menuSnapshot != null
                    && snapshotWidth == width
                    && snapshotHeight == height) {
                return menuSnapshot;
            }

            menuSnapshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D snapshotGraphics = menuSnapshot.createGraphics();
            snapshotGraphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            menuSource.printAll(snapshotGraphics);
            snapshotGraphics.dispose();
            snapshotWidth = width;
            snapshotHeight = height;
            return menuSnapshot;
        }

        private static float introImageAlpha(float t) {
            if (t < 0.18f) return smootherStep(t / 0.18f);
            if (t < 0.76f) return 1f;
            return 1f - smootherStep((t - 0.76f) / 0.24f);
        }

        private static float introPulse(float t) {
            float first = pulseAt(t, 0.25f, 0.16f);
            float second = pulseAt(t, 0.64f, 0.20f) * 0.72f;
            return Math.max(first, second);
        }

        private static float pulseAt(float t, float center, float width) {
            float distance = Math.abs(t - center) / Math.max(0.001f, width);
            if (distance >= 1f) return 0f;
            float edge = 1f - distance;
            return edge * edge * (3f - 2f * edge);
        }

        private static float smoothStep(float t) {
            t = clamp01(t);
            return t * t * (3f - 2f * t);
        }

        private static float smootherStep(float t) {
            t = clamp01(t);
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }

        private static float easeInOutCubic(float t) {
            t = clamp01(t);
            return t < 0.5f
                    ? 4f * t * t * t
                    : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }

        private static float easeInOutSine(float t) {
            t = clamp01(t);
            return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
        }

        private static float easeOutCubic(float t) {
            t = clamp01(t);
            float inverse = 1f - t;
            return 1f - inverse * inverse * inverse;
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }

    private void installAltF4Hotkey() {
        JRootPane root = getRootPane();
        if (root == null) return;
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK), "altF4Exit");
        am.put("altF4Exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExitConfirmationDialog();
            }
        });
    }

    private void installGlobalEscapeBackKey() {
        JRootPane root = getRootPane();
        if (root == null) return;
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "globalBack");
        am.put("globalBack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (accountOverlay != null && accountOverlay.isVisible()) {
                    closeAccountOverlay();
                    return;
                }
                MainMenu.this.actionPerformed(new ActionEvent(MainMenu.this, ActionEvent.ACTION_PERFORMED, "BACK_TO_MENU"));
            }
        });
    }

    private void installAccountHotkey() {
        JRootPane root = getRootPane();
        if (root == null) return;
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "openAccount");
        actionMap.put("openAccount", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!"MENU".equals(currentCard)
                        || startupIntroRunning
                        || exitDialogOpen
                        || (accountOverlay != null && accountOverlay.isVisible())) {
                    return;
                }
                showAccountOverlay();
            }
        });
    }

    private void showCard(String cardName, boolean pushCurrent) {
        if (cardName == null || cardName.equals(currentCard)) return;
        if (pushCurrent && currentCard != null) {
            navigationHistory.push(currentCard);
        }
        cardLayout.show(mainPanel, cardName);
        currentCard = cardName;
    }

    private void ensureLocalVariantPanel() {
        if (localVariantPanel != null) return;
        localVariantPanel = createLocalVariantPanel();
        mainPanel.add(localVariantPanel, "LOCAL_VARIANT");
    }

    private void ensureOnlineScreen() {
        if (onlineScreen != null) return;
        onlineScreen = new OnlinePanel(this);
        mainPanel.add(onlineScreen, "ONLINE");
    }

    private void ensureAnalysisPanel() {
        if (analysisPanel != null) return;
        analysisPanel = new AnalysisPanel(this);
        mainPanel.add(analysisPanel, "ANALYSIS");
    }

    private void goBackOneScreen() {
        if ("ACTIVE_SCREEN".equals(currentCard)) {
            clearEmbeddedScreen();
        }
        if (!navigationHistory.isEmpty()) {
            String previous = navigationHistory.pop();
            cardLayout.show(mainPanel, previous);
            currentCard = previous;
            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }

    public void showEmbeddedScreen(JComponent screen, Runnable cleanup) {
        if (screen == null) return;
        boolean replacingEmbeddedScreen = "ACTIVE_SCREEN".equals(currentCard);
        String previousCard = currentCard;
        clearEmbeddedScreen();
        activeScreen = screen;
        activeScreenCleanup = cleanup;
        mainPanel.add(screen, "ACTIVE_SCREEN");
        if (proportionalUiScaler != null) {
            proportionalUiScaler.registerTree(screen);
        }
        if (!replacingEmbeddedScreen && previousCard != null) {
            navigationHistory.push(previousCard);
        }
        cardLayout.show(mainPanel, "ACTIVE_SCREEN");
        currentCard = "ACTIVE_SCREEN";
        mainPanel.revalidate();
        mainPanel.repaint();
        screen.requestFocusInWindow();
    }

    private void showAccountOverlay() {
        if (accountOverlay != null && accountOverlay.isVisible()) return;
        if (accountOverlayTimer != null) {
            accountOverlayTimer.stop();
            accountOverlayTimer = null;
        }

        accountOverlay = new AccountModalPane(
                new AccountPanel(this),
                backgroundImage);
        accountOverlay.setAnimationProgress(0f);
        setGlassPane(accountOverlay);
        accountOverlay.setBounds(
                0,
                0,
                getRootPane().getWidth(),
                getRootPane().getHeight());
        accountOverlay.prepareBackground();
        accountOverlay.doLayout();
        accountOverlay.validate();

        accountOverlay.setVisible(true);
        getRootPane().revalidate();
        getRootPane().repaint();
        accountOverlay.paintImmediately(accountOverlay.getBounds());
        // Hide the heavyweight menu canvas only after the overlay has pixels ready.
        menuPanel.setVisible(false);
        accountOverlay.repaint();
        accountOverlay.requestFocusInWindow();

        long startedAt = System.nanoTime();
        accountOverlayTimer = AnimationTiming.createUiTimer(event -> {
            float progress = AnimationTiming.progress(
                    startedAt,
                    ACCOUNT_OVERLAY_ANIMATION_MS);
            accountOverlay.setAnimationProgress(progress);
            if (progress < 1f) return;
            ((Timer) event.getSource()).stop();
            accountOverlayTimer = null;
            accountOverlay.setAnimationProgress(1f);
        });
        accountOverlayTimer.start();
    }

    public void closeAccountOverlay() {
        if (accountOverlay == null || !accountOverlay.isVisible()) return;
        if (accountOverlayTimer != null) {
            accountOverlayTimer.stop();
            accountOverlayTimer = null;
        }

        AccountModalPane closingOverlay = accountOverlay;
        float closingStartProgress = closingOverlay.getAnimationProgress();
        closingOverlay.setClosing(true);
        long startedAt = System.nanoTime();
        accountOverlayTimer = AnimationTiming.createUiTimer(event -> {
            float progress = AnimationTiming.progress(
                    startedAt,
                    Math.max(
                            1,
                            Math.round(
                                    ACCOUNT_OVERLAY_ANIMATION_MS
                                            * closingStartProgress)));
            closingOverlay.setAnimationProgress(
                    closingStartProgress * (1f - progress));
            if (progress < 1f) return;
            ((Timer) event.getSource()).stop();
            accountOverlayTimer = null;
            closingOverlay.setVisible(false);
            if (accountOverlay == closingOverlay) accountOverlay = null;
            menuPanel.setVisible(true);
            mainPanel.revalidate();
            mainPanel.repaint();
            mainPanel.requestFocusInWindow();
        });
        accountOverlayTimer.start();
    }

    private static final class AccountModalPane extends JComponent {
        private final AccountPanel content;
        private final Image backgroundImage;
        private BufferedImage cachedBackground;
        private int cachedBackgroundWidth = -1;
        private int cachedBackgroundHeight = -1;
        private float animationProgress;
        private boolean closing;

        AccountModalPane(AccountPanel content, Image backgroundImage) {
            this.content = content;
            this.backgroundImage = backgroundImage;
            setLayout(null);
            setOpaque(false);
            setFocusable(true);
            add(content);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    event.consume();
                }
            });
        }

        void setClosing(boolean closing) {
            this.closing = closing;
        }

        float getAnimationProgress() {
            return animationProgress;
        }

        void prepareBackground() {
            getCachedBackground();
        }

        void setAnimationProgress(float progress) {
            animationProgress = Math.max(0f, Math.min(1f, progress));
            repaint();
        }

        @Override
        public void doLayout() {
            Dimension preferred = content.getPreferredSize();
            int maxWidth = Math.max(1, getWidth() - 100);
            int maxHeight = Math.max(1, getHeight() - 90);
            float fit = Math.min(
                    1f,
                    Math.min(
                            maxWidth / (float) preferred.width,
                            maxHeight / (float) preferred.height));
            int width = Math.max(1, Math.round(preferred.width * fit));
            int height = Math.max(1, Math.round(preferred.height * fit));
            int centeredY = (getHeight() - height) / 2;
            int restingY = Math.min(
                    Math.max(18, getHeight() - height - 18),
                    centeredY + 20);
            content.setBounds(
                    (getWidth() - width) / 2,
                    restingY,
                    width,
                    height);
            content.doLayout();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            BufferedImage background = getCachedBackground();
            if (background != null) {
                g2.drawImage(background, 0, 0, null);
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            float eased = smootherStep(animationProgress);
            int shadeAlpha = Math.round(104f + 48f * eased);
            g2.setColor(new Color(0, 3, 5, shadeAlpha));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // A cool translucent wash separates the static scene from the UI.
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(197, 221, 226, 26),
                    0, getHeight(), new Color(5, 13, 17, 76)));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.dispose();
        }

        private BufferedImage getCachedBackground() {
            int width = getWidth();
            int height = getHeight();
            if (backgroundImage == null || width <= 0 || height <= 0) return null;
            if (cachedBackground != null
                    && cachedBackgroundWidth == width
                    && cachedBackgroundHeight == height) {
                return cachedBackground;
            }

            int imageWidth = backgroundImage.getWidth(this);
            int imageHeight = backgroundImage.getHeight(this);
            if (imageWidth <= 0 || imageHeight <= 0) return null;

            int blurWidth = Math.max(1, width / 2);
            int blurHeight = Math.max(1, height / 2);
            BufferedImage softened = new BufferedImage(
                    blurWidth,
                    blurHeight,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D softenGraphics = softened.createGraphics();
            softenGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math.max(
                    blurWidth / (double) imageWidth,
                    blurHeight / (double) imageHeight);
            int drawWidth = Math.max(1, (int) Math.ceil(imageWidth * scale));
            int drawHeight = Math.max(1, (int) Math.ceil(imageHeight * scale));
            softenGraphics.drawImage(
                    backgroundImage,
                    (blurWidth - drawWidth) / 2,
                    (blurHeight - drawHeight) / 2,
                    drawWidth,
                    drawHeight,
                    this);
            softenGraphics.dispose();

            BufferedImage scaled = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D cacheGraphics = scaled.createGraphics();
            cacheGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            cacheGraphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            cacheGraphics.drawImage(
                    softened,
                    0,
                    0,
                    width,
                    height,
                    this);
            cacheGraphics.dispose();

            cachedBackground = scaled;
            cachedBackgroundWidth = width;
            cachedBackgroundHeight = height;
            return cachedBackground;
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            float eased = smootherStep(animationProgress);
            if (eased <= 0.001f) return;
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(eased));
            g2.translate(0, Math.round(18f * (1f - eased)));
            super.paintChildren(g2);
            g2.dispose();
        }

        private float easeOutBack(float value) {
            float t = Math.max(0f, Math.min(1f, value));
            float c1 = closing ? 0f : 1.15f;
            float c3 = c1 + 1f;
            return 1f + c3 * (float) Math.pow(t - 1f, 3)
                    + c1 * (float) Math.pow(t - 1f, 2);
        }

        private static float smootherStep(float value) {
            float t = Math.max(0f, Math.min(1f, value));
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }
    }

    public void beginGameLaunchOverlay() {
        if (gameLaunchOverlayTimer != null) {
            gameLaunchOverlayTimer.stop();
            gameLaunchOverlayTimer = null;
        }
        if (gameLaunchOverlay == null) {
            gameLaunchOverlay = new GameLaunchCurtainPane();
        }
        ((GameLaunchCurtainPane) gameLaunchOverlay).setOpenProgress(0f);
        gameLaunchOverlayStartedAtNanos = System.nanoTime();
        setGlassPane(gameLaunchOverlay);
        gameLaunchOverlay.setVisible(true);
        gameLaunchOverlay.revalidate();
        gameLaunchOverlay.repaint();
        gameLaunchOverlay.paintImmediately(gameLaunchOverlay.getBounds());
    }

    public void launchGameWithOverlay(Runnable launchAction) {
        if (launchAction == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launchGameWithOverlay(launchAction));
            return;
        }
        beginGameLaunchOverlay();
        // Let Swing paint the white frame before game construction occupies the EDT.
        SwingUtilities.invokeLater(launchAction);
    }

    private void launchVariantWithCinematic(String variant, Runnable launchAction) {
        if (variant == null || launchAction == null || variantLaunchCinematicTimer != null) return;

        JButton selectedCard = null;
        for (JButton card : localVariantButtons) {
            if (variant.equals(card.getClientProperty("variant_value"))) {
                selectedCard = card;
                break;
            }
        }
        if (selectedCard == null || !selectedCard.isShowing()
                || mainPanel.getWidth() <= 0 || mainPanel.getHeight() <= 0) {
            launchGameWithOverlay(launchAction);
            return;
        }

        BufferedImage snapshot = new BufferedImage(
                mainPanel.getWidth(),
                mainPanel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D snapshotGraphics = snapshot.createGraphics();
        mainPanel.printAll(snapshotGraphics);
        snapshotGraphics.dispose();

        Rectangle cardBounds = SwingUtilities.convertRectangle(
                selectedCard.getParent(),
                selectedCard.getBounds(),
                mainPanel);
        VariantLaunchCinematicPane cinematic = new VariantLaunchCinematicPane(
                snapshot, cardBounds);
        setGlassPane(cinematic);
        cinematic.setVisible(true);
        cinematic.requestFocusInWindow();

        long startedAt = System.nanoTime();
        variantLaunchCinematicTimer = AnimationTiming.createUiTimer(e -> {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
            cinematic.setElapsedMs(elapsedMs);
            if (elapsedMs < SPELL_LAUNCH_CHARGE_MS + SPELL_LAUNCH_CIRCLE_MS) {
                return;
            }

            ((Timer) e.getSource()).stop();
            variantLaunchCinematicTimer = null;
            cinematic.setVisible(false);
            launchGameWithOverlay(launchAction);
        });
        variantLaunchCinematicTimer.setCoalesce(true);
        variantLaunchCinematicTimer.setInitialDelay(0);
        variantLaunchCinematicTimer.start();
    }

    private void closeVariantScreenToMenu() {
        if (variantCloseTimer != null) return;
        if (!"LOCAL_VARIANT".equals(currentCard)
                || mainPanel.getWidth() <= 0
                || mainPanel.getHeight() <= 0) {
            goBackOneScreen();
            return;
        }

        BufferedImage snapshot = new BufferedImage(
                mainPanel.getWidth(),
                mainPanel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D snapshotGraphics = snapshot.createGraphics();
        mainPanel.printAll(snapshotGraphics);
        snapshotGraphics.dispose();

        VariantClosePane closePane = new VariantClosePane(snapshot);
        setGlassPane(closePane);
        closePane.setVisible(true);
        closePane.requestFocusInWindow();

        goBackOneScreen();

        long startedAt = System.nanoTime();
        variantCloseTimer = AnimationTiming.createUiTimer(e -> {
            float progress = (System.nanoTime() - startedAt)
                    / (VARIANT_CLOSE_MS * 1_000_000f);
            closePane.setProgress(progress);
            if (progress < 1f) return;

            ((Timer) e.getSource()).stop();
            variantCloseTimer = null;
            closePane.setVisible(false);
            mainPanel.requestFocusInWindow();
        });
        variantCloseTimer.setCoalesce(true);
        variantCloseTimer.setInitialDelay(0);
        variantCloseTimer.start();
    }

    public void finishGameLaunchOverlay(JComponent gameScreen) {
        if (gameLaunchOverlay == null || !gameLaunchOverlay.isVisible()) return;
        long elapsed = (long) AnimationTiming.elapsedMillis(gameLaunchOverlayStartedAtNanos);
        int remaining = (int) Math.max(0, GAME_LAUNCH_OVERLAY_MS - elapsed);
        GameLaunchCurtainPane curtains = (GameLaunchCurtainPane) gameLaunchOverlay;
        long curtainStartedAt = System.nanoTime() + remaining * 1_000_000L;
        gameLaunchOverlayTimer = AnimationTiming.createUiTimer(e -> {
            float progress = (System.nanoTime() - curtainStartedAt)
                    / (GAME_LAUNCH_CURTAIN_MS * 1_000_000f);
            if (progress < 0f) return;
            if (progress < 1f) {
                curtains.setOpenProgress(progress);
                return;
            }

            curtains.setOpenProgress(1f);
            ((Timer) e.getSource()).stop();
            gameLaunchOverlayTimer = null;
            curtains.setVisible(false);
            if (gameScreen != null) gameScreen.requestFocusInWindow();
        });
        gameLaunchOverlayTimer.setInitialDelay(remaining);
        gameLaunchOverlayTimer.setCoalesce(true);
        gameLaunchOverlayTimer.start();
    }

    private static class GameLaunchCurtainPane extends JComponent {
        private float openProgress;

        GameLaunchCurtainPane() {
            setOpaque(false);
            setFocusable(true);
            setFocusTraversalKeysEnabled(false);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK
                    | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.KEY_EVENT_MASK);
        }

        void setOpenProgress(float openProgress) {
            this.openProgress = Math.max(0f, Math.min(1f, openProgress));
            repaint();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            e.consume();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float eased = easeInOutCubic(openProgress);
            int width = getWidth();
            int height = getHeight();
            int leftWidth = (width + 1) / 2;
            int rightWidth = width - leftWidth;
            int travel = Math.round(leftWidth * eased);

            g.setColor(new Color(5, 9, 11));
            g.fillRect(-travel, 0, leftWidth, height);
            g.fillRect(leftWidth + travel, 0, rightWidth, height);

            g.setColor(new Color(214, 162, 61, 42));
            g.fillRect(-travel, 0, leftWidth, Math.max(1, height));
            g.fillRect(leftWidth + travel, 0, rightWidth, Math.max(1, height));

            if (openProgress > 0f && openProgress < 1f) {
                int shadowWidth = Math.max(8, Math.round(28f * (1f - openProgress)));
                int shadowAlpha = Math.round(80f * (1f - openProgress));
                GradientPaint leftShadow = new GradientPaint(
                        leftWidth - travel - shadowWidth,
                        0,
                        new Color(0, 0, 0, 0),
                        leftWidth - travel,
                        0,
                        new Color(0, 0, 0, shadowAlpha));
                g.setPaint(leftShadow);
                g.fillRect(
                        leftWidth - travel - shadowWidth,
                        0,
                        shadowWidth,
                        height);

                GradientPaint rightShadow = new GradientPaint(
                        leftWidth + travel,
                        0,
                        new Color(0, 0, 0, shadowAlpha),
                        leftWidth + travel + shadowWidth,
                        0,
                        new Color(0, 0, 0, 0));
                g.setPaint(rightShadow);
                g.fillRect(leftWidth + travel, 0, shadowWidth, height);
            }
            g.dispose();
        }

        private static float easeInOutCubic(float t) {
            t = Math.max(0f, Math.min(1f, t));
            return t < 0.5f
                    ? 4f * t * t * t
                    : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }
    }

    private static class VariantLaunchCinematicPane extends JComponent {
        private final BufferedImage selectorSnapshot;
        private final Rectangle cardBounds;
        private long elapsedMs;

        VariantLaunchCinematicPane(BufferedImage selectorSnapshot, Rectangle cardBounds) {
            this.selectorSnapshot = selectorSnapshot;
            this.cardBounds = new Rectangle(cardBounds);
            setOpaque(true);
            setBackground(Color.BLACK);
            setFocusable(true);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK
                    | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.KEY_EVENT_MASK);
        }

        void setElapsedMs(long elapsedMs) {
            this.elapsedMs = Math.max(0L, elapsedMs);
            repaint();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            e.consume();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float chargeT = clamp01(elapsedMs / (float) SPELL_LAUNCH_CHARGE_MS);
            float circleT = clamp01(
                    (elapsedMs - SPELL_LAUNCH_CHARGE_MS)
                            / (float) SPELL_LAUNCH_CIRCLE_MS);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.drawImage(
                    selectorSnapshot,
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    null);

            float totalTransitionT = clamp01(
                    elapsedMs / (float) (SPELL_LAUNCH_CHARGE_MS + SPELL_LAUNCH_CIRCLE_MS));
            float dimProgress = totalTransitionT * totalTransitionT * totalTransitionT;
            float dimAlpha = 0.72f * dimProgress;
            g.setColor(new Color(0, 0, 0, Math.min(210, Math.round(dimAlpha * 255f))));
            g.fillRect(0, 0, getWidth(), getHeight());

            float scaleX = getWidth() / (float) Math.max(1, selectorSnapshot.getWidth());
            float scaleY = getHeight() / (float) Math.max(1, selectorSnapshot.getHeight());
            Rectangle drawCard = new Rectangle(
                    Math.round(cardBounds.x * scaleX),
                    Math.round(cardBounds.y * scaleY),
                    Math.max(1, Math.round(cardBounds.width * scaleX)),
                    Math.max(1, Math.round(cardBounds.height * scaleY)));
            float centerX = drawCard.x + drawCard.width / 2f;
            float centerY = drawCard.y + drawCard.height / 2f;

            float tileBrightness = smootherStep(chargeT);
            g.setColor(new Color(
                    255,
                    255,
                    255,
                    Math.round(255f * tileBrightness)));
            g.fillRoundRect(
                    drawCard.x,
                    drawCard.y,
                    drawCard.width,
                    drawCard.height,
                    20,
                    20);

            if (circleT > 0f) {
                float maxRadius = (float) Math.max(
                        Point.distance(centerX, centerY, 0, 0),
                        Math.max(
                                Point.distance(centerX, centerY, getWidth(), 0),
                                Math.max(
                                        Point.distance(centerX, centerY, 0, getHeight()),
                                        Point.distance(centerX, centerY, getWidth(), getHeight()))));
                float radius = maxRadius * easeInOutCubic(circleT);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Float(
                        centerX - radius,
                        centerY - radius,
                        radius * 2f,
                        radius * 2f));
            }
            g.dispose();
        }

        private static float smootherStep(float t) {
            t = clamp01(t);
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }

        private static float easeInOutCubic(float t) {
            t = clamp01(t);
            return t < 0.5f
                    ? 4f * t * t * t
                    : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }

    private static class VariantClosePane extends JComponent {
        private final BufferedImage snapshot;
        private float progress;

        VariantClosePane(BufferedImage snapshot) {
            this.snapshot = snapshot;
            setOpaque(false);
            setFocusable(true);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK
                    | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.KEY_EVENT_MASK);
        }

        void setProgress(float progress) {
            this.progress = clamp01(progress);
            repaint();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            e.consume();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            e.consume();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float eased = smootherStep(progress);
            float fadeT = smootherStep(clamp01((progress - 0.18f) / 0.82f));
            float scaleX = 1f - 0.025f * eased;
            float scaleY = 1f - 0.045f * eased;
            float alpha = 1f - fadeT;

            int drawWidth = Math.max(1, Math.round(getWidth() * scaleX));
            int drawHeight = Math.max(2, Math.round(getHeight() * scaleY));
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2
                    + Math.round(6f * eased);
            int arc = Math.max(6, Math.round(12f * eased));

            if (alpha > 0.001f) {
                int shadowAlpha = Math.round(42f * alpha * (1f - progress));
                g.setColor(new Color(0, 0, 0, shadowAlpha));
                g.fillRoundRect(
                        drawX - 4,
                        drawY + 3,
                        drawWidth + 8,
                        drawHeight + 6,
                        arc + 6,
                        arc + 6);

                Shape oldClip = g.getClip();
                g.clip(new RoundRectangle2D.Float(
                        drawX,
                        drawY,
                        drawWidth,
                        drawHeight,
                        arc,
                        arc));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.drawImage(
                        snapshot,
                        drawX,
                        drawY,
                        drawWidth,
                        drawHeight,
                        null);
                g.setColor(new Color(0, 0, 0, Math.round(36f * eased * alpha)));
                g.fillRect(drawX, drawY, drawWidth, drawHeight);
                g.setClip(oldClip);

                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setStroke(new BasicStroke(1f));
                g.setColor(new Color(214, 151, 58, Math.round(90f * alpha)));
                g.drawRoundRect(
                        drawX,
                        drawY,
                        Math.max(0, drawWidth - 1),
                        Math.max(1, drawHeight - 1),
                        arc,
                        arc);
            }
            g.dispose();
        }

        private static float smootherStep(float value) {
            float t = clamp01(value);
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }

    public void returnToMenuFromEmbeddedScreen() {
        clearEmbeddedScreen();
        navigationHistory.clear();
        cardLayout.show(mainPanel, "MENU");
        currentCard = "MENU";
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void clearEmbeddedScreen() {
        Runnable cleanup = activeScreenCleanup;
        activeScreenCleanup = null;
        if (cleanup != null) cleanup.run();
        if (activeScreen != null) {
            mainPanel.remove(activeScreen);
            activeScreen = null;
        }
    }

    private Image loadBackgroundImage() {
        try {
            URL res = getClass().getResource("/assets/bg2.png");
            if (res != null) {
                return ImageIO.read(res);
            }
        } catch (IOException e) {
            System.err.println("Error loading background image from resources: " + e.getMessage());
        }

        String[] paths = {
            "src/assets/bg2.png",
            "Scaccomatto_final/Scaccomatto/src/assets/bg2.png",
            "assets/bg2.png",
            "bg2.png"
        };

        for (String p : paths) {
            File f = new File(p);
            if (!f.exists()) continue;
            try {
                return ImageIO.read(f);
            } catch (IOException e) {
                System.err.println("Error loading background image from " + p + ": " + e.getMessage());
            }
        }

        System.err.println("Background image not found at /assets/bg2.png or filesystem fallback paths.");
        return null;
    }

    private Image loadAppLogoImage() {
        String[] candidates = {
            "/assets/icon.png",
            "/assets/logo.png",
            "/assets/pieces/wk.png"
        };
        for (String path : candidates) {
            try {
                URL res = getClass().getResource(path);
                if (res != null) {
                    Image img = ImageIO.read(res);
                    if (img != null) return img;
                }
            } catch (IOException ignored) {
            }
        }

        String[] fs = {
            "Scaccomatto_final/Scaccomatto/src/assets/pieces/wk.png",
            "src/assets/pieces/wk.png",
            "assets/pieces/wk.png",
            "Scaccomatto_final/Scaccomatto/src/assets/logo.png",
            "src/assets/logo.png",
            "assets/logo.png"
        };
        for (String path : fs) {
            try {
                File f = new File(path);
                if (!f.exists()) continue;
                Image img = ImageIO.read(f);
                if (img != null) return img;
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private void drawMainMenuAtmosphere(Graphics2D graphics, int width, int height) {
        if (width <= 0 || height <= 0) return;
        double seconds = (System.nanoTime() - mainMenuAtmosphereStartNanos) / 1_000_000_000.0;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawFogBank(g, width, height, seconds, 0.08, 0.29, 1.04, 0.44, 92, 0.00, 1.00);
        drawFogBank(g, width, height, seconds, 0.38, 0.46, 0.92, 0.38, 78, 1.75, 1.30);
        drawFogBank(g, width, height, seconds, 0.76, 0.26, 0.82, 0.34, 68, 3.40, 1.65);
        drawFogBank(g, width, height, seconds, 0.59, 0.72, 1.00, 0.40, 74, 5.10, 1.15);
        drawFogBank(g, width, height, seconds, 0.90, 0.56, 0.70, 0.32, 60, 7.20, 1.85);
        drawFogBank(g, width, height, seconds, 0.30, 0.82, 0.86, 0.30, 54, 8.60, 2.05);

        for (int i = 0; i < 66; i++) {
            double seed = i * 1.61803398875;
            double cycle = (seconds * (0.026 + (i % 5) * 0.006) + seed * 0.17) % 1.0;
            double baseX = ((i * 73) % 997) / 997.0;
            double sway = Math.sin(seconds * (0.42 + (i % 4) * 0.09) + seed) * 0.035;
            int x = (int) Math.round((baseX + sway) * width);
            int y = (int) Math.round((1.08 - cycle * 1.18) * height);
            int size = 2 + i % 4;
            int alpha = 34 + (int) Math.round(
                    48 * (0.5 + 0.5 * Math.sin(seconds * 0.7 + seed)));
            g.setColor(new Color(1, 3, 4, Math.max(18, Math.min(82, alpha))));
            g.fillOval(x, y, size, size);
        }

        for (int i = 0; i < 24; i++) {
            double seed = i * 2.3999632297;
            double baseX = 0.08 + ((i * 41) % 83) / 100.0;
            double baseY = 0.16 + ((i * 29) % 67) / 100.0;
            double xWave = Math.sin(seconds * (0.34 + (i % 5) * 0.055) + seed);
            double yWave = Math.cos(seconds * (0.27 + (i % 4) * 0.048) + seed * 1.31);
            double curl = Math.sin(seconds * 0.16 + seed * 0.63);
            int x = (int) Math.round((baseX + xWave * 0.035 + curl * 0.018) * width);
            int y = (int) Math.round((baseY + yWave * 0.045 + xWave * 0.012) * height);
            float pulse = (float) (0.5 + 0.5
                    * Math.sin(seconds * (1.7 + (i % 6) * 0.19) + seed));
            int coreSize = 2 + i % 3;
            int glowRadius = 8 + i % 7;
            int glowAlpha = Math.round(28 + 72 * pulse);
            int coreAlpha = Math.round(115 + 140 * pulse);
            drawFirefly(g, x, y, glowRadius, coreSize, glowAlpha, coreAlpha);
        }

        for (int i = 0; i < 18; i++) {
            double seed = i * 1.41421356237;
            double travel = (seconds * (0.045 + (i % 4) * 0.009) + seed * 0.23) % 1.0;
            int x = (int) Math.round((((i * 61) % 101) / 101.0
                    + Math.sin(seconds * 0.46 + seed) * 0.025) * width);
            int y = (int) Math.round((1.06 - travel * 1.12) * height);
            int alpha = 65 + (i % 5) * 18;
            int size = 1 + i % 3;
            g.setColor(new Color(224, 126 + (i % 3) * 18, 42, alpha));
            g.fillOval(x, y, size, size + 1);
        }
        g.dispose();
    }

    private void drawFirefly(
            Graphics2D g,
            int x,
            int y,
            int glowRadius,
            int coreSize,
            int glowAlpha,
            int coreAlpha) {
        Graphics2D glow = (Graphics2D) g.create();
        glow.setPaint(new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(x, y),
                Math.max(1f, glowRadius),
                new float[]{0f, 0.28f, 1f},
                new Color[]{
                    new Color(255, 230, 116, Math.min(150, glowAlpha)),
                    new Color(245, 170, 58, Math.max(5, glowAlpha / 3)),
                    new Color(219, 112, 20, 0)
                }));
        glow.fillOval(
                x - glowRadius,
                y - glowRadius,
                glowRadius * 2,
                glowRadius * 2);
        glow.setColor(new Color(255, 241, 159, Math.min(255, coreAlpha)));
        glow.fillOval(x - coreSize / 2, y - coreSize / 2, coreSize, coreSize);
        glow.dispose();
    }

    private void drawFogBank(
            Graphics2D g,
            int width,
            int height,
            double seconds,
            double baseX,
            double baseY,
            double widthRatio,
            double heightRatio,
            int maxAlpha,
            double phase,
            double speed) {
        double driftX = Math.sin(seconds * 0.075 * speed + phase) * width * 0.14;
        double driftY = Math.cos(seconds * 0.052 * speed + phase * 0.7) * height * 0.055;
        int fogWidth = Math.max(1, (int) Math.round(width * widthRatio));
        int fogHeight = Math.max(1, (int) Math.round(height * heightRatio));
        float centerX = (float) (width * baseX + driftX);
        float centerY = (float) (height * baseY + driftY);
        float radius = Math.max(fogWidth, fogHeight) * 0.52f;

        Graphics2D fog = (Graphics2D) g.create();
        fog.scale(1.0, fogHeight / (double) fogWidth);
        float scaledCenterY = (float) (centerY * fogWidth / (double) fogHeight);
        fog.setPaint(new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(centerX, scaledCenterY),
                Math.max(1f, radius),
                new float[]{0f, 0.42f, 1f},
                new Color[]{
                    new Color(44, 49, 50, maxAlpha),
                    new Color(25, 30, 31, Math.max(1, Math.round(maxAlpha * 0.62f))),
                    new Color(3, 6, 8, 0)
                }));
        int drawSize = Math.round(radius * 2f);
        fog.fillOval(
                Math.round(centerX - radius),
                Math.round(scaledCenterY - radius),
                drawSize,
                drawSize);
        fog.dispose();
    }

    private void renderMainMenuAtmosphereFrame(int width, int height) {
        if (width <= 0 || height <= 0) return;

        BufferedImage frame;
        synchronized (mainMenuAtmosphereFrameLock) {
            if (mainMenuAtmosphereBackFrame == null) {
                mainMenuAtmosphereBackFrame = new BufferedImage(
                        width, height, BufferedImage.TYPE_INT_ARGB);
            } else if (mainMenuAtmosphereBackFrame.getWidth() != width
                    || mainMenuAtmosphereBackFrame.getHeight() != height) {
                mainMenuAtmosphereFrontFrame = null;
                mainMenuAtmosphereBackFrame = new BufferedImage(
                        width, height, BufferedImage.TYPE_INT_ARGB);
            }
            frame = mainMenuAtmosphereBackFrame;
        }

        Graphics2D g = frame.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(6, 10, 12));
        g.fillRect(0, 0, width, height);
        if (backgroundImage != null) {
            int imageWidth = backgroundImage.getWidth(null);
            int imageHeight = backgroundImage.getHeight(null);
            double scale = Math.max(
                    width / (double) Math.max(1, imageWidth),
                    height / (double) Math.max(1, imageHeight));
            int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
            int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
            int drawX = (width - drawWidth) / 2;
            int drawY = (height - drawHeight) / 2;
            g.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, null);
        }
        g.setComposite(AlphaComposite.SrcOver);
        drawMainMenuAtmosphere(g, width, height);
        g.setPaint(new GradientPaint(
                0, 0, new Color(2, 7, 10, 80),
                width * 0.55f, 0, new Color(2, 7, 10, 0)));
        g.fillRect(0, 0, width, height);
        g.dispose();

        synchronized (mainMenuAtmosphereFrameLock) {
            if (mainMenuAtmosphereWidth != width || mainMenuAtmosphereHeight != height) return;
            BufferedImage previousFront = mainMenuAtmosphereFrontFrame;
            mainMenuAtmosphereFrontFrame = frame;
            mainMenuAtmosphereBackFrame = previousFront;
        }
    }
    
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = getWidth();
                int height = getHeight();
                mainMenuAtmosphereWidth = width;
                mainMenuAtmosphereHeight = height;

                boolean frameDrawn = false;
                synchronized (mainMenuAtmosphereFrameLock) {
                    if (mainMenuAtmosphereFrontFrame != null
                            && mainMenuAtmosphereFrontFrame.getWidth() == width
                            && mainMenuAtmosphereFrontFrame.getHeight() == height) {
                        g.drawImage(mainMenuAtmosphereFrontFrame, 0, 0, this);
                        frameDrawn = true;
                    }
                }
                if (!frameDrawn) {
                    g.setColor(new Color(6, 10, 12));
                    g.fillRect(0, 0, width, height);
                    if (backgroundImage != null) {
                        int imageWidth = backgroundImage.getWidth(this);
                        int imageHeight = backgroundImage.getHeight(this);
                        double scale = Math.max(
                                width / (double) Math.max(1, imageWidth),
                                height / (double) Math.max(1, imageHeight));
                        int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
                        int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
                        int drawX = (width - drawWidth) / 2;
                        int drawY = (height - drawHeight) / 2;
                        g.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, this);
                    }
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(
                            0, 0, new Color(2, 7, 10, 80),
                            width * 0.55f, 0, new Color(2, 7, 10, 0)));
                    g2.fillRect(0, 0, width, height);
                    g2.dispose();
                }
            }
        };
        panel.setLayout(new BorderLayout());
        mainMenuAtmosphereStartNanos = System.nanoTime();
        if (mainMenuAtmosphereTimer != null) mainMenuAtmosphereTimer.cancel();
        mainMenuAtmosphereTimer = new java.util.Timer("main-menu-atmosphere", true);
        mainMenuAtmosphereTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                int width = mainMenuAtmosphereWidth;
                int height = mainMenuAtmosphereHeight;
                renderMainMenuAtmosphereFrame(width, height);
                if (!startupIntroRunning) panel.repaint();
            }
        }, 0L, AnimationTiming.FRAME_DELAY_MS);

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.setOpaque(false);
        leftContainer.setBorder(BorderFactory.createEmptyBorder(28, 54, 34, 0));

        JPanel topBlock = new JPanel();
        topBlock.setOpaque(false);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));

        if (appLogoImage != null) {
            Image scaled = appLogoImage.getScaledInstance(72, 72, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            logoLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            titleRow.add(logoLabel);
            titleRow.add(Box.createHorizontalStrut(14));
        }

        mainTitleLabel = new JLabel("SCACCOMATTO");
        mainTitleLabel.setFont(new Font("Georgia", Font.PLAIN, 55));
        mainTitleLabel.setForeground(new Color(226, 181, 105));
        mainTitleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        titleRow.add(mainTitleLabel);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        topBlock.add(titleRow);
        JLabel tagline = new JLabel("S T R A T E G Y  -  T A C T I C S  -  V I C T O R Y");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(new Color(177, 142, 91));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 88, 0, 0));
        topBlock.add(tagline);
        topBlock.add(Box.createVerticalStrut(24));

        JPanel menuList = new JPanel();
        menuList.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuList.setOpaque(false);
        menuList.setLayout(new BoxLayout(menuList, BoxLayout.Y_AXIS));
        menuList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        Color menuGold = new Color(224, 176, 91);
        MenuItemPanel playFriend = new MenuItemPanel("Play with Friend", "Local 2-player", new PeopleIcon(menuGold));
        MenuItemPanel playBot = new MenuItemPanel("Play with Bot", "Choose difficulty", new BotIcon(menuGold));
        MenuItemPanel online = new MenuItemPanel("Online", "Matchmaking & rooms", new GlobeIcon(menuGold));
        MenuItemPanel analysis = new MenuItemPanel("Analysis", "Review games, best moves", new ChartIcon(menuGold));
        MenuItemPanel puzzles = new MenuItemPanel("Puzzles", "Tactics & puzzle rush", new PuzzleIcon(menuGold));
        MenuItemPanel lessons = new MenuItemPanel("Lessons", "Guided training paths", new LessonIcon(menuGold));

        playFriend.setActionCommand("PLAY_FRIEND");
        playBot.setActionCommand("PLAY_BOT");
        online.setActionCommand("ONLINE");
        analysis.setActionCommand("ANALYSIS");
        puzzles.setActionCommand("PUZZLES");
        lessons.setActionCommand("LESSONS");

        menuList.add(playFriend);
        menuList.add(playBot);
        menuList.add(online);
        menuList.add(analysis);
        menuList.add(puzzles);
        menuList.add(lessons);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(16, 12, 0, 12));

        QuitPillButton quitButton = new QuitPillButton();
        quitButton.setActionCommand("QUIT");

        footer.add(quitButton, BorderLayout.EAST);

        JScrollPane menuScrollPane = new JScrollPane(
            menuList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        menuScrollPane.setBorder(BorderFactory.createEmptyBorder());
        menuScrollPane.setOpaque(false);
        menuScrollPane.getViewport().setOpaque(false);
        menuScrollPane.setWheelScrollingEnabled(true);
        menuScrollPane.getVerticalScrollBar().setUnitIncrement(30);
        menuScrollPane.getVerticalScrollBar().setBlockIncrement(110);
        menuScrollPane.addMouseWheelListener(e -> {
            JScrollBar bar = menuScrollPane.getVerticalScrollBar();
            int delta = e.getUnitsToScroll() * bar.getUnitIncrement();
            bar.setValue(bar.getValue() + delta);
            e.consume();
        });
        menuList.addMouseWheelListener(e -> {
            JScrollBar bar = menuScrollPane.getVerticalScrollBar();
            int delta = e.getUnitsToScroll() * bar.getUnitIncrement();
            bar.setValue(bar.getValue() + delta);
            e.consume();
        });

        // Keep panel resizing uniform so it never becomes stretched in one axis.
        final double cardScale = 0.875;
        int cardWidth = (int) Math.round(640 * cardScale);
        int cardHeight = (int) Math.round(680 * cardScale);
        MenuCardPanel menuCard = new MenuCardPanel();
        menuCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuCard.setBorder(BorderFactory.createEmptyBorder(24, 34, 24, 34));
        menuCard.setLayout(new BorderLayout(0, 8));
        menuCard.add(menuScrollPane, BorderLayout.CENTER);
        menuCard.add(footer, BorderLayout.SOUTH);
        menuCard.setPreferredSize(new Dimension(cardWidth, cardHeight));
        menuCard.setMinimumSize(new Dimension(cardWidth, cardHeight));
        menuCard.setMaximumSize(new Dimension(cardWidth, cardHeight));

        JPanel cardHolder = new JPanel(new GridBagLayout());
        cardHolder.setOpaque(false);
        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridx = 0;
        cardConstraints.gridy = 0;
        cardConstraints.weightx = 1.0;
        cardConstraints.weighty = 1.0;
        cardConstraints.anchor = GridBagConstraints.NORTHWEST;
        cardHolder.add(menuCard, cardConstraints);

        leftContainer.add(topBlock, BorderLayout.NORTH);
        leftContainer.add(cardHolder, BorderLayout.CENTER);

        JPanel contentHolder = new JPanel(new BorderLayout());
        contentHolder.setOpaque(false);
        contentHolder.add(leftContainer, BorderLayout.WEST);

        panel.add(contentHolder, BorderLayout.CENTER);

        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "menuPlayFriend");
        am.put("menuPlayFriend", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "PLAY_FRIEND"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "menuPlayBot");
        am.put("menuPlayBot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "PLAY_BOT"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "menuOnline");
        am.put("menuOnline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "ONLINE"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0), "menuAnalysis");
        am.put("menuAnalysis", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "ANALYSIS"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0), "menuPuzzles");
        am.put("menuPuzzles", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "PUZZLES"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0), "menuLessons");
        am.put("menuLessons", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "LESSONS"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "menuQuit");
        am.put("menuQuit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "QUIT"));
            }
        });

        panel.add(contentHolder, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLocalVariantPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (backgroundImage != null) {
                    int imageWidth = backgroundImage.getWidth(this);
                    int imageHeight = backgroundImage.getHeight(this);
                    double scale = Math.max(
                            getWidth() / (double) Math.max(1, imageWidth),
                            getHeight() / (double) Math.max(1, imageHeight));
                    int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
                    int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
                    g2.drawImage(
                            backgroundImage,
                            (getWidth() - drawWidth) / 2,
                            (getHeight() - drawHeight) / 2,
                            drawWidth,
                            drawHeight,
                            this);
                }
                g2.setColor(new Color(1, 6, 9, 184));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(0, 4, 7, 35),
                        0, getHeight(), new Color(0, 4, 7, 115)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(26, 48, 24, 48));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JPanel brandRow = new JPanel();
        brandRow.setOpaque(false);
        brandRow.setLayout(new BoxLayout(brandRow, BoxLayout.X_AXIS));
        if (appLogoImage != null) {
            JLabel logo = new JLabel(new ImageIcon(
                    appLogoImage.getScaledInstance(68, 68, Image.SCALE_SMOOTH)));
            logo.setAlignmentY(Component.CENTER_ALIGNMENT);
            brandRow.add(logo);
            brandRow.add(Box.createHorizontalStrut(18));
        }

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("SCACCOMATTO");
        brand.setFont(UiFonts.title(50f));
        brand.setForeground(new Color(231, 184, 100));
        JLabel motto = new JLabel("—  S T R A T E G Y .  T A C T I C S .  V I C T O R Y .  —");
        motto.setFont(UiFonts.subtext(13f));
        motto.setForeground(new Color(181, 145, 91));
        brandText.add(brand);
        brandText.add(Box.createVerticalStrut(3));
        brandText.add(motto);
        brandRow.add(brandText);
        brandRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(brandRow);
        heading.add(Box.createVerticalStrut(28));

        JLabel title = new JLabel("Chess Variants");
        title.setFont(UiFonts.title(35f));
        title.setForeground(new Color(244, 238, 224));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Pick a mode for your local match...");
        subtitle.setFont(UiFonts.subtext(18f));
        subtitle.setForeground(new Color(190, 154, 105));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 18, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        LocalVariantOption[] options = {
            new LocalVariantOption("Classic", "Standard chess rules", "Classic", "classic.png", true),
            new LocalVariantOption("Chess960", "Randomized back rank setup!", "Chess960", "960.png", true),
            new LocalVariantOption("King of the Hill", "Reach the center with your king", "King of the Hill", "kinghill.png", true),
            new LocalVariantOption("Three-check", "Win by giving three checks", "Three-check", "3checks.png", true),
            new LocalVariantOption("Atomic", "Captures explode nearby pieces!", "Atomic", "atomic.png", true),
            new LocalVariantOption("Spell Chess", "Cast spells and see the magic!", "Spell Chess", "spellchess.png", true),
            new LocalVariantOption("Fog of War", "Limited vision outside piece range", "Fog of War", "fogofwar.png", true),
            new LocalVariantOption("Duck Chess", "Normal mode card enabled", "Duck Chess", "duck.png", true)
        };
        Color normalBg = new Color(9, 14, 16, 205);
        Color hoverBg = new Color(24, 24, 20, 224);
        Color selectedBg = new Color(52, 42, 27, 224);
        Color normalBorder = new Color(112, 81, 43, 185);
        Color hoverBorder = new Color(177, 127, 54, 225);
        Color selectedBorder = new Color(247, 190, 74);

        localVariantButtons.clear();
        for (LocalVariantOption option : options) {
            JButton card = createRoundedFillButton("", 30);
            card.putClientProperty("variant_card_title", option.label);
            card.putClientProperty("variant_card_subtitle", option.description);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.setHorizontalTextPosition(SwingConstants.RIGHT);
            card.setIconTextGap(22);
            card.setFont(UiFonts.title(20f));
            card.setForeground(new Color(242, 234, 219));
            card.setFocusPainted(false);
            card.setBackground(normalBg);
            card.putClientProperty("roundedBorderColor", normalBorder);
            card.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setPreferredSize(new Dimension(620, 108));
            card.putClientProperty("variant_normal_bg", normalBg);
            card.putClientProperty("variant_hover_bg", hoverBg);
            card.putClientProperty("variant_selected_bg", selectedBg);
            card.putClientProperty("variant_normal_border", normalBorder);
            card.putClientProperty("variant_hover_border", hoverBorder);
            card.putClientProperty("variant_selected_border", selectedBorder);
            card.putClientProperty("variant_hovered", Boolean.FALSE);

            Icon icon = loadVariantIcon(option.iconFile, 78);
            if (icon != null) card.setIcon(icon);

            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.putClientProperty("variant_hovered", Boolean.TRUE);
                    boolean chosen = option.value.equals(selectedLocalVariant);
                    if (!chosen) {
                        animateVariantCardColors(card, hoverBg, hoverBorder);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.putClientProperty("variant_hovered", Boolean.FALSE);
                    refreshLocalVariantSelection();
                }
            });

            card.addActionListener(e -> {
                selectedLocalVariant = option.value;
                refreshLocalVariantSelection();
            });
            card.putClientProperty("variant_value", option.value);
            localVariantButtons.add(card);
            grid.add(card);
        }

        refreshLocalVariantSelection();
        panel.add(grid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton back = createRoundedFillButton("Back", 10);
        back.setFont(UiFonts.title(19f));
        back.setFocusPainted(false);
        back.setBackground(new Color(8, 13, 15, 225));
        back.setForeground(new Color(239, 225, 199));
        back.putClientProperty("roundedBorderColor", new Color(151, 105, 46));
        back.setBorder(BorderFactory.createEmptyBorder(10, 34, 10, 34));
        back.addActionListener(e -> closeVariantScreenToMenu());

        JButton play = createRoundedFillButton("Play", 10);
        play.setFont(UiFonts.title(19f));
        play.setFocusPainted(false);
        play.setBackground(new Color(151, 102, 37));
        play.setForeground(new Color(255, 247, 225));
        play.putClientProperty("roundedBorderColor", new Color(232, 180, 88));
        play.setBorder(BorderFactory.createEmptyBorder(10, 38, 10, 38));
        play.addActionListener(e -> {
            String variant = selectedLocalVariant;
            Runnable launchAction = () -> new ChessGame(this, 600, 0, variant);
            launchVariantWithCinematic(variant, launchAction);
        });

        // Local variant keybinds:
        // 1 2
        // 3 4
        // 5 6
        // 7 8
        // Enter = Play, Esc = Back
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();
        String[] variantOrder = new String[] {
            "Classic",
            "Chess960",
            "King of the Hill",
            "Three-check",
            "Atomic",
            "Spell Chess",
            "Fog of War",
            "Duck Chess"
        };
        int[] keyCodes = new int[] {
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
            KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8
        };
        for (int i = 0; i < variantOrder.length; i++) {
            final String variantValue = variantOrder[i];
            String actionKey = "selectLocalVariant" + (i + 1);
            im.put(KeyStroke.getKeyStroke(keyCodes[i], 0), actionKey);
            am.put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedLocalVariant = variantValue;
                    refreshLocalVariantSelection();
                }
            });
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "playLocalVariant");
        am.put("playLocalVariant", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                play.doClick();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "backFromLocalVariant");
        am.put("backFromLocalVariant", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                back.doClick();
            }
        });

        localVariantStatusLabel = new JLabel();
        localVariantStatusLabel.setForeground(new Color(211, 171, 105));
        localVariantStatusLabel.setFont(UiFonts.subtext(18f));
        localVariantStatusLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(localVariantStatusLabel, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);

        actions.add(back);
        actions.add(play);
        panel.add(footer, BorderLayout.SOUTH);
        refreshLocalVariantSelection();
        return panel;
    }

    private void refreshLocalVariantSelection() {
        for (JButton b : localVariantButtons) {
            Object v = b.getClientProperty("variant_value");
            boolean chosen = (v instanceof String) && ((String) v).equals(selectedLocalVariant);
            Color normalBg = colorProp(b, "variant_normal_bg", new Color(9, 14, 16, 205));
            Color selectedBg = colorProp(b, "variant_selected_bg", new Color(52, 42, 27, 224));
            Color normalBorder = colorProp(b, "variant_normal_border", new Color(112, 81, 43, 185));
            Color hoverBg = colorProp(b, "variant_hover_bg", new Color(24, 24, 20, 224));
            Color hoverBorder = colorProp(b, "variant_hover_border", new Color(177, 127, 54, 225));
            Color selectedBorder = colorProp(b, "variant_selected_border", new Color(247, 190, 74));
            boolean hovered = Boolean.TRUE.equals(b.getClientProperty("variant_hovered"));
            Color targetBg = chosen ? selectedBg : hovered ? hoverBg : normalBg;
            Color targetBorder = chosen ? selectedBorder : hovered ? hoverBorder : normalBorder;
            b.putClientProperty("variant_selected_glow", chosen);
            animateVariantCardColors(b, targetBg, targetBorder);
            b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        }

        if (localVariantStatusLabel != null) {
            if ("King of the Hill".equals(selectedLocalVariant)) {
                localVariantStatusLabel.setText("King of the Hill is coming soon. Stay Tuned!");
            } else {
                localVariantStatusLabel.setText("Selected variant: " + selectedLocalVariant);
            }
        }
    }

    private Color colorProp(JComponent c, String key, Color fallback) {
        Object value = c.getClientProperty(key);
        return value instanceof Color ? (Color) value : fallback;
    }

    private void animateVariantCardColors(JButton card, Color targetBg, Color targetBorder) {
        Timer runningTimer = (Timer) card.getClientProperty("variant_color_timer");
        if (runningTimer != null) runningTimer.stop();

        Color startBg = card.getBackground();
        Color startBorder = colorProp(card, "roundedBorderColor", targetBorder);
        if (startBg.equals(targetBg) && startBorder.equals(targetBorder)) {
            card.repaint();
            return;
        }

        long startedAt = System.nanoTime();
        int durationMs = 220;
        Timer timer = AnimationTiming.createUiTimer(null);
        timer.addActionListener(e -> {
            float t = AnimationTiming.progress(startedAt, durationMs);
            float eased = t * t * (3f - 2f * t);
            card.setBackground(interpolateColor(startBg, targetBg, eased));
            card.putClientProperty(
                    "roundedBorderColor",
                    interpolateColor(startBorder, targetBorder, eased));
            card.repaint();
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                card.putClientProperty("variant_color_timer", null);
            }
        });
        timer.setCoalesce(true);
        card.putClientProperty("variant_color_timer", timer);
        timer.start();
    }

    private static Color interpolateColor(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }
    
    private abstract class MenuIcon implements Icon {
        protected final int size;
        protected final Color primary;

        protected MenuIcon(int size, Color primary) {
            this.size = size;
            this.primary = primary;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private class PeopleIcon extends MenuIcon {
        PeopleIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillOval(x + 3, y + 5, 12, 12);
            g2.setColor(primary.brighter());
            g2.fillOval(x + 16, y + 3, 12, 12);
            g2.setColor(primary);
            g2.fillRoundRect(x + 2, y + 17, 14, 10, 6, 6);
            g2.setColor(primary.brighter());
            g2.fillRoundRect(x + 14, y + 17, 16, 10, 6, 6);
            g2.dispose();
        }
    }

    private class BotIcon extends MenuIcon {
        BotIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillRoundRect(x + 4, y + 8, 24, 16, 8, 8);
            g2.setColor(new Color(245, 245, 245));
            g2.fillOval(x + 10, y + 13, 4, 4);
            g2.fillOval(x + 18, y + 13, 4, 4);
            g2.setColor(primary.darker());
            g2.fillRoundRect(x + 14, y + 4, 4, 6, 3, 3);
            g2.dispose();
        }
    }

    private class GlobeIcon extends MenuIcon {
        GlobeIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillOval(x + 4, y + 4, 24, 24);
            g2.setColor(new Color(234, 252, 242));
            g2.drawOval(x + 8, y + 8, 16, 16);
            g2.drawLine(x + 16, y + 6, x + 16, y + 26);
            g2.drawLine(x + 6, y + 16, x + 26, y + 16);
            g2.dispose();
        }
    }

    private class ChartIcon extends MenuIcon {
        ChartIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillRoundRect(x + 4, y + 5, 24, 22, 6, 6);
            g2.setColor(new Color(235, 240, 255));
            g2.drawLine(x + 8, y + 22, x + 12, y + 16);
            g2.drawLine(x + 12, y + 16, x + 18, y + 20);
            g2.drawLine(x + 18, y + 20, x + 24, y + 10);
            g2.fillOval(x + 7, y + 21, 3, 3);
            g2.fillOval(x + 11, y + 15, 3, 3);
            g2.fillOval(x + 17, y + 19, 3, 3);
            g2.fillOval(x + 23, y + 9, 3, 3);
            g2.dispose();
        }
    }

    private class PuzzleIcon extends MenuIcon {
        PuzzleIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            g2.fillRoundRect(x + 5, y + 5, 22, 22, 8, 8);
            g2.setColor(new Color(255, 248, 233));
            g2.fillOval(x + 13, y + 8, 6, 6);
            g2.fillRect(x + 15, y + 13, 2, 5);
            g2.fillRoundRect(x + 11, y + 17, 10, 6, 4, 4);
            g2.dispose();
        }
    }

    private class LessonIcon extends MenuIcon {
        LessonIcon(Color primary) {
            super(32, primary);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(primary);
            Polygon cap = new Polygon(
                new int[] {x + 16, x + 28, x + 16, x + 4},
                new int[] {y + 5, y + 11, y + 17, y + 11},
                4
            );
            g2.fillPolygon(cap);
            g2.fillRoundRect(x + 11, y + 15, 10, 10, 3, 3);
            g2.setColor(primary.darker());
            g2.drawLine(x + 24, y + 12, x + 24, y + 22);
            g2.fillOval(x + 22, y + 21, 4, 4);
            g2.dispose();
        }
    }

    private class MenuCardPanel extends JPanel {
        MenuCardPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        }

        void addMenuItem(MenuItemPanel panel) {
            add(panel);
            add(Box.createVerticalStrut(12));
        }

        void addFooter(JComponent footer) {
            add(Box.createVerticalStrut(8));
            add(footer);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 18;
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(5, 7, w - 4, h - 4, arc, arc);
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(13, 19, 21, 235),
                    0, h, new Color(3, 10, 13, 242)));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(182, 137, 67, 210));
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class MenuItemPanel extends JPanel {
        private final JLabel title;
        private final JLabel subtitle;
        private final JLabel chevron;
        private String actionCommand;
        private boolean hover;
        private float hoverProgress;
        private Timer hoverTimer;

        MenuItemPanel(String titleText, String subtitleText, Icon icon) {
            setOpaque(false);
            setLayout(new BorderLayout(18, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            setPreferredSize(new Dimension(500, 82));
            setMinimumSize(new Dimension(0, 82));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel iconWrap = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(112, 96, 72, 135));
                    g2.drawLine(getWidth() - 1, 15, getWidth() - 1, getHeight() - 15);
                    g2.dispose();
                }
            };
            iconWrap.setOpaque(false);
            iconWrap.setPreferredSize(new Dimension(72, 70));
            JLabel iconLabel = new JLabel(icon);
            iconWrap.add(iconLabel);

            title = new JLabel(titleText);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));

            subtitle = new JLabel(subtitleText);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));

            JPanel textWrap = new JPanel();
            textWrap.setOpaque(false);
            textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
            textWrap.add(title);
            textWrap.add(Box.createVerticalStrut(3));
            textWrap.add(subtitle);

            chevron = new JLabel(">");
            chevron.setFont(new Font("Georgia", Font.PLAIN, 29));
            chevron.setHorizontalAlignment(SwingConstants.CENTER);
            chevron.setPreferredSize(new Dimension(30, 40));

            add(iconWrap, BorderLayout.WEST);
            add(textWrap, BorderLayout.CENTER);
            add(chevron, BorderLayout.EAST);

            addMouseListener(new MenuItemListener(this));
            refreshTheme();
        }

        void setActionCommand(String actionCommand) {
            this.actionCommand = actionCommand;
        }

        String getActionCommand() {
            return actionCommand;
        }

        void setHover(boolean isHover) {
            hover = isHover;
            if (hoverTimer != null) hoverTimer.stop();
            float start = hoverProgress;
            float end = isHover ? 1f : 0f;
            long startedAt = System.nanoTime();
            hoverTimer = AnimationTiming.createUiTimer(e -> {
                float t = AnimationTiming.progress(startedAt, 180f);
                float eased = t * t * (3f - 2f * t);
                hoverProgress = start + (end - start) * eased;
                repaint();
                if (t >= 1f) ((Timer) e.getSource()).stop();
            });
            hoverTimer.setCoalesce(true);
            hoverTimer.start();
        }

        void refreshTheme() {
            title.setForeground(new Color(240, 235, 225));
            subtitle.setForeground(new Color(174, 156, 126));
            chevron.setForeground(new Color(190, 151, 87));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hoverProgress > 0.001f) {
                int alpha = Math.round(hoverProgress * 145f);
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(83, 63, 35, alpha),
                        getWidth(), 0, new Color(26, 31, 31, Math.round(alpha * 0.35f))));
                g2.fillRoundRect(0, 2, getWidth() - 1, getHeight() - 4, 13, 13);
                g2.setColor(new Color(229, 177, 85, Math.round(255f * hoverProgress)));
                g2.fillRoundRect(0, 6, 5, getHeight() - 12, 5, 5);
            }
            g2.setColor(new Color(111, 101, 84, 80));
            g2.drawLine(26, getHeight() - 1, getWidth() - 8, getHeight() - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class MenuItemListener extends MouseAdapter {
        private final MenuItemPanel item;

        MenuItemListener(MenuItemPanel item) {
            this.item = item;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            item.setHover(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            item.setHover(false);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (item.getActionCommand() == null) return;
            ActionEvent ae = new ActionEvent(item, ActionEvent.ACTION_PERFORMED, item.getActionCommand());
            MainMenu.this.actionPerformed(ae);
        }
    }

    private class QuitPillButton extends JPanel {
        private String actionCommand;
        private boolean hover;

        QuitPillButton() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(108, 38));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (actionCommand == null) return;
                    ActionEvent ae = new ActionEvent(QuitPillButton.this, ActionEvent.ACTION_PERFORMED, actionCommand);
                    MainMenu.this.actionPerformed(ae);
                }
            });
        }

        void setActionCommand(String actionCommand) {
            this.actionCommand = actionCommand;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(new Color(110, 24, 22, 110));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.setColor(hover ? new Color(255, 92, 77) : new Color(225, 64, 55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            FontMetrics fm = g2.getFontMetrics();
            String text = "Exit  >";
            int textX = (getWidth() - fm.stringWidth(text)) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }
    
    // Inner class for handling menu label hover and click
    private class MenuLabelListener extends MouseAdapter {
        private JLabel label;
        private String actionCommand;
        private Font originalFont;
        
        public MenuLabelListener(JLabel label, String actionCommand) {
            this.label = label;
            this.actionCommand = actionCommand;
            this.originalFont = label.getFont();
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            // Make text bold on hover
            label.setFont(new Font(originalFont.getName(), Font.BOLD, originalFont.getSize()));
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            // Return to original font
            label.setFont(originalFont);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            // Trigger the action
            ActionEvent ae = new ActionEvent(label, ActionEvent.ACTION_PERFORMED, actionCommand);
            MainMenu.this.actionPerformed(ae);
        }
    }
    
        private void showBotSelectionScreen() {
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (backgroundImage != null) {
                    int imageWidth = backgroundImage.getWidth(this);
                    int imageHeight = backgroundImage.getHeight(this);
                    double scale = Math.max(
                            getWidth() / (double) Math.max(1, imageWidth),
                            getHeight() / (double) Math.max(1, imageHeight));
                    int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
                    int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
                    g2.drawImage(
                            backgroundImage,
                            (getWidth() - drawWidth) / 2,
                            (getHeight() - drawHeight) / 2,
                            drawWidth,
                            drawHeight,
                            this);
                }
                g2.setColor(new Color(1, 6, 9, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(0, 5, 8, 45),
                        0, getHeight(), new Color(0, 4, 6, 120)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainContainer.setBorder(BorderFactory.createEmptyBorder(24, 42, 20, 42));
        InputMap bim = mainContainer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap bam = mainContainer.getActionMap();
        bim.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeBotSelection");
        bam.put("closeBotSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.returnToMenuFromEmbeddedScreen();
            }
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Play Against Bot");
        title.setFont(UiFonts.title(50f));
        title.setForeground(new Color(242, 232, 210));

        JLabel subtitle = new JLabel("Pick a bot profile and start your match");
        subtitle.setFont(UiFonts.subtext(20f));
        subtitle.setForeground(new Color(190, 151, 93));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(title);
        titleWrap.add(Box.createVerticalStrut(4));
        titleWrap.add(subtitle);

        JLabel badge = new JLabel("BOT ARENA", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int arc = 16;
                g2.setColor(new Color(8, 13, 15, 225));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(151, 105, 46));
                g2.drawRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(UiFonts.title(16f));
        badge.setForeground(new Color(221, 177, 103));
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        header.add(titleWrap, BorderLayout.WEST);
        header.add(badgeWrap, BorderLayout.EAST);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setPreferredSize(new Dimension(1480, 820));

        JPanel leftCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int arc = 22;
                g2.setColor(new Color(5, 10, 12, 48));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int arc = 22;
                g2.setColor(new Color(133, 92, 42));
                g2.drawRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(213, 157, 70, 70));
                g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
                g2.dispose();
            }
        };
        leftCard.setOpaque(false);
        leftCard.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));

        JLabel botPhotoLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int arc = 18;
                g2.setColor(new Color(27, 31, 37));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        botPhotoLabel.setPreferredSize(new Dimension(210, 285));
        botPhotoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        botPhotoLabel.setVerticalAlignment(SwingConstants.CENTER);
        botPhotoLabel.setOpaque(false);
        botPhotoLabel.setBorder(BorderFactory.createLineBorder(new Color(188, 133, 57), 2, true));

        JTextArea botNameLabel = new JTextArea(2, 1);
        botNameLabel.setFont(UiFonts.title(38f));
        botNameLabel.setForeground(new Color(244, 238, 225));
        botNameLabel.setOpaque(false);
        botNameLabel.setEditable(false);
        botNameLabel.setFocusable(false);
        botNameLabel.setLineWrap(true);
        botNameLabel.setWrapStyleWord(true);
        botNameLabel.setBorder(null);
        botNameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JTextArea botStatsLabel = new JTextArea(2, 1);
        botStatsLabel.setFont(UiFonts.subtext(20f));
        botStatsLabel.setForeground(new Color(203, 158, 91));
        botStatsLabel.setOpaque(false);
        botStatsLabel.setEditable(false);
        botStatsLabel.setFocusable(false);
        botStatsLabel.setLineWrap(true);
        botStatsLabel.setWrapStyleWord(true);
        botStatsLabel.setBorder(null);
        botStatsLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel headerTextWrap = new JPanel();
        headerTextWrap.setOpaque(false);
        headerTextWrap.setLayout(new BoxLayout(headerTextWrap, BoxLayout.Y_AXIS));
        headerTextWrap.add(Box.createVerticalStrut(8));
        headerTextWrap.add(botNameLabel);
        headerTextWrap.add(Box.createVerticalStrut(6));
        headerTextWrap.add(botStatsLabel);
        headerTextWrap.add(Box.createVerticalGlue());

        JPanel botHeaderPanel = new JPanel(new BorderLayout(14, 0));
        botHeaderPanel.setOpaque(false);
        botHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
        botHeaderPanel.add(botPhotoLabel, BorderLayout.WEST);
        botHeaderPanel.add(headerTextWrap, BorderLayout.CENTER);
        leftCard.add(botHeaderPanel, BorderLayout.NORTH);

        JTextArea infoText = new JTextArea();
        infoText.setFont(UiFonts.subtext(21f));
        infoText.setForeground(new Color(225, 215, 195));
        infoText.setBackground(new Color(5, 10, 12));
        infoText.setOpaque(false);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JScrollPane infoScroll = new JScrollPane(infoText);
        infoScroll.setBorder(null);
        infoScroll.setOpaque(false);
        infoScroll.getViewport().setOpaque(false);
        infoScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleThemedScrollPane(infoScroll);
        leftCard.add(infoScroll, BorderLayout.CENTER);

        JLabel playstyleValue = createBotProfileValueLabel();
        JLabel strengthValue = createBotProfileValueLabel();
        JLabel weaknessValue = createBotProfileValueLabel();
        JPanel profileStrip = new JPanel(new GridLayout(1, 3, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(7, 12, 14, 62));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(new Color(107, 79, 43, 170));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileStrip.setOpaque(false);
        profileStrip.setPreferredSize(new Dimension(500, 104));
        profileStrip.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        profileStrip.add(createBotProfileStat("PLAYSTYLE", playstyleValue));
        profileStrip.add(createBotProfileStat("STRENGTH", strengthValue));
        profileStrip.add(createBotProfileStat("WEAKNESS", weaknessValue));
        leftCard.add(profileStrip, BorderLayout.SOUTH);

        JPanel rightCard = new JPanel(new BorderLayout());
        rightCard.setOpaque(false);

        BotPanel botPanel = new BotPanel();
        botPanel.setStartGameListener(e -> {
            ChessBot selectedBot = botPanel.getSelectedBot();
            launchGameWithOverlay(
                    () -> new ChessGame(this, 600, 0, selectedBot));
        });
        botPanel.setSpectateListener(e -> {
            ChessBot selectedBot = botPanel.getSelectedBot();
            showSpectateSelectionScreen(selectedBot);
        });
        java.util.function.Consumer<ChessBot> refreshBotProfile = bot -> {
            updateBotInfoPanel(bot, botPhotoLabel, botNameLabel, botStatsLabel, infoText);
            playstyleValue.setText(getBotPlaystyle(bot));
            strengthValue.setText(getBotStrength(bot));
            weaknessValue.setText(getBotWeakness(bot));
        };
        botPanel.setBotSelectionListener(refreshBotProfile::accept);
        refreshBotProfile.accept(botPanel.getSelectedBot());

        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        backPanel.setOpaque(false);
        backPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JButton backButton = createRoundedFillButton("Back to Menu", 14);
        backButton.setFont(UiFonts.title(20f));
        backButton.setForeground(new Color(239, 225, 199));
        backButton.setBackground(new Color(8, 13, 15, 150));
        backButton.setFocusPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.putClientProperty("roundedBorderColor", new Color(151, 105, 46));
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        backButton.addActionListener(ev -> returnToMenuFromEmbeddedScreen());
        backPanel.add(backButton);

        rightCard.add(botPanel, BorderLayout.CENTER);

        GridBagConstraints bodyConstraints = new GridBagConstraints();
        bodyConstraints.gridy = 0;
        bodyConstraints.fill = GridBagConstraints.BOTH;
        bodyConstraints.weighty = 1.0;
        bodyConstraints.gridx = 0;
        bodyConstraints.weightx = 0.42;
        bodyConstraints.insets = new Insets(0, 0, 0, 11);
        body.add(leftCard, bodyConstraints);
        bodyConstraints.gridx = 1;
        bodyConstraints.weightx = 0.58;
        bodyConstraints.insets = new Insets(0, 11, 0, 0);
        body.add(rightCard, bodyConstraints);

        JPanel stage = new JPanel(new BorderLayout());
        stage.setOpaque(false);
        stage.add(body, BorderLayout.CENTER);
        stage.add(backPanel, BorderLayout.SOUTH);

        JPanel stageHolder = new JPanel(new GridBagLayout());
        stageHolder.setOpaque(false);
        GridBagConstraints stageConstraints = new GridBagConstraints();
        stageConstraints.gridx = 0;
        stageConstraints.gridy = 0;
        stageConstraints.weightx = 1.0;
        stageConstraints.weighty = 1.0;
        stageConstraints.fill = GridBagConstraints.BOTH;
        stageConstraints.anchor = GridBagConstraints.NORTH;
        stageConstraints.insets = new Insets(0, 0, 0, 0);
        stageHolder.add(stage, stageConstraints);

        mainContainer.add(header, BorderLayout.NORTH);
        mainContainer.add(stageHolder, BorderLayout.CENTER);

        showEmbeddedScreen(mainContainer, null);
    }

    private void showSpectateSelectionScreen(ChessBot preferredBot) {
        JPanel root = new JPanel(new BorderLayout(0, 22)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(18, 22, 33),
                    0, getHeight(), new Color(28, 34, 44)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(88, 134, 220, 40));
                g2.fillOval(-140, -160, 420, 420);
                g2.setColor(new Color(146, 198, 102, 34));
                g2.fillOval(getWidth() - 280, getHeight() - 320, 420, 420);
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(22, 28, 20, 28));

        JLabel title = new JLabel("Spectate Bot Battle", SwingConstants.CENTER);
        title.setFont(new Font("Bahnschrift", Font.BOLD, 56));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Choose White and Black bots", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        subtitle.setForeground(new Color(190, 202, 223));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitle);

        ChessBot[] bots = ChessBot.getAllBots();
        JComboBox<ChessBot> whiteBotBox = new JComboBox<>(bots);
        JComboBox<ChessBot> blackBotBox = new JComboBox<>(bots);
        if (preferredBot != null) {
            whiteBotBox.setSelectedItem(preferredBot);
            blackBotBox.setSelectedItem(preferredBot);
        }

        JPanel whitePanel = createSpectateBotSelectorCard("White Bot", whiteBotBox, true);
        JPanel blackPanel = createSpectateBotSelectorCard("Black Bot", blackBotBox, false);

        JPanel vsWrap = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = Math.min(getWidth(), getHeight()) - 20;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                g2.setPaint(new GradientPaint(x, y, new Color(66, 98, 156), x, y + s, new Color(45, 68, 112)));
                g2.fillOval(x, y, s, s);
                g2.setColor(new Color(154, 186, 241));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x, y, s, s);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        vsWrap.setOpaque(false);
        JLabel vsLabel = new JLabel("VS", SwingConstants.CENTER);
        vsLabel.setFont(new Font("Bahnschrift", Font.BOLD, 42));
        vsLabel.setForeground(new Color(244, 248, 255));
        vsWrap.add(vsLabel);
        vsWrap.setPreferredSize(new Dimension(180, 180));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 14);
        center.add(whitePanel, c);
        c.gridx = 1;
        c.weightx = 0.28;
        c.insets = new Insets(0, 0, 0, 0);
        center.add(vsWrap, c);
        c.gridx = 2;
        c.weightx = 1.0;
        c.insets = new Insets(0, 14, 0, 0);
        center.add(blackPanel, c);

        JButton startBtn = createRoundedFillButton("Start Spectating", 18);
        startBtn.setPreferredSize(new Dimension(310, 58));
        startBtn.setFont(new Font("Bahnschrift", Font.BOLD, 25));
        startBtn.setForeground(Color.WHITE);
        startBtn.setBackground(new Color(64, 118, 201));
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.putClientProperty("roundedBorderColor", new Color(128, 176, 244));
        startBtn.addActionListener(ev -> {
            ChessBot whiteBot = (ChessBot) whiteBotBox.getSelectedItem();
            ChessBot blackBot = (ChessBot) blackBotBox.getSelectedItem();
            launchGameWithOverlay(
                    () -> new ChessGame(this, 600, 0, whiteBot, blackBot, true));
        });

        JButton backBtn = createRoundedFillButton("Back", 15);
        backBtn.setFont(new Font("Bahnschrift", Font.BOLD, 17));
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(73, 79, 92));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.putClientProperty("roundedBorderColor", new Color(124, 132, 148));
        backBtn.addActionListener(ev -> showBotSelectionScreen());

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JPanel startWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        startWrap.setOpaque(false);
        startWrap.add(startBtn);
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backWrap.setOpaque(false);
        backWrap.add(backBtn);
        actions.add(backWrap, BorderLayout.WEST);
        actions.add(startWrap, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "backToBotSelection");
        am.put("backToBotSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showBotSelectionScreen();
            }
        });
        showEmbeddedScreen(root, null);
    }

    private JPanel createSpectateBotSelectorCard(String heading, JComboBox<ChessBot> comboBox, boolean whiteSide) {
        Color cardTop = whiteSide ? new Color(40, 48, 63) : new Color(41, 45, 60);
        Color cardBottom = whiteSide ? new Color(28, 33, 45) : new Color(27, 31, 43);
        Color border = whiteSide ? new Color(134, 171, 230) : new Color(165, 177, 200);
        Color accent = whiteSide ? new Color(123, 169, 241) : new Color(169, 182, 208);

        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                g2.setPaint(new GradientPaint(0, 0, cardTop, 0, h, cardBottom));
                g2.fillRoundRect(0, 0, w, h, 26, 26);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w, h, 22, 22);
                g2.setColor(new Color(255, 255, 255, 24));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        card.setPreferredSize(new Dimension(410, 420));

        JLabel head = new JLabel(heading);
        head.setFont(new Font("Bahnschrift", Font.BOLD, 44));
        head.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Select bot profile");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        sub.setForeground(new Color(198, 208, 226));

        styleSpectateCombo(comboBox);

        JLabel portrait = new JLabel();
        portrait.setPreferredSize(new Dimension(104, 104));
        portrait.setHorizontalAlignment(SwingConstants.CENTER);
        portrait.setVerticalAlignment(SwingConstants.CENTER);
        portrait.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(109, 124, 149), 1, true),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        JLabel botName = new JLabel();
        botName.setFont(new Font("Bahnschrift", Font.BOLD, 29));
        botName.setForeground(Color.WHITE);

        JLabel stars = new JLabel();
        stars.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 26));
        stars.setForeground(new Color(255, 219, 66));

        JLabel statsLine = new JLabel();
        statsLine.setFont(new Font("Segoe UI", Font.PLAIN, 19));
        statsLine.setForeground(new Color(213, 220, 234));

        JLabel detailsLine = new JLabel();
        detailsLine.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        detailsLine.setForeground(new Color(192, 202, 220));

        JLabel desc = new JLabel();
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        desc.setForeground(new Color(189, 198, 214));

        java.util.function.Consumer<ChessBot> refreshInfo = bot -> {
            if (bot == null) {
                portrait.setIcon(null);
                portrait.setText("");
                botName.setText("");
                stars.setText("");
                statsLine.setText("");
                detailsLine.setText("");
                desc.setText("");
                return;
            }
            ImageIcon icon = loadBotPortrait(bot, 100, 100);
            portrait.setIcon(icon);
            portrait.setText(icon == null ? "?" : "");
            botName.setText(bot.getName());
            stars.setText(bot.getDifficultyStars());
            statsLine.setText("ELO " + bot.getElo());
            detailsLine.setText("Depth " + bot.getDepth() + "   •   Skill " + bot.getSkillLevel());
            desc.setText(bot.getDescription());
        };
        refreshInfo.accept((ChessBot) comboBox.getSelectedItem());
        comboBox.addActionListener(e -> refreshInfo.accept((ChessBot) comboBox.getSelectedItem()));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(head);
        titleWrap.add(Box.createVerticalStrut(2));
        titleWrap.add(sub);

        JPanel comboWrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                g2.setColor(new Color(45, 55, 72));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(110, 130, 165));
                g2.drawRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(255, 255, 255, 24));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        comboWrap.setOpaque(false);
        comboWrap.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        comboWrap.add(comboBox, BorderLayout.CENTER);
        comboWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel summary = new JPanel(new BorderLayout(12, 0));
        summary.setOpaque(false);
        JPanel portraitWrap = new JPanel(new BorderLayout());
        portraitWrap.setOpaque(false);
        portraitWrap.add(portrait, BorderLayout.NORTH);
        summary.add(portraitWrap, BorderLayout.EAST);

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        textWrap.add(botName);
        textWrap.add(Box.createVerticalStrut(2));
        textWrap.add(stars);
        textWrap.add(Box.createVerticalStrut(4));
        textWrap.add(statsLine);
        textWrap.add(Box.createVerticalStrut(2));
        textWrap.add(detailsLine);
        textWrap.add(Box.createVerticalStrut(4));
        textWrap.add(desc);
        summary.add(textWrap, BorderLayout.CENTER);
        // --- Bot strength area (revamp: add slider showing approximate strength) ---
        JLabel strengthHeading = new JLabel("BOT STRENGTH");
        strengthHeading.setFont(new Font("Bahnschrift", Font.BOLD, 13));
        strengthHeading.setForeground(new Color(185, 143, 80));

        int minElo = 200;
        int maxElo = 3000;
        int initElo = 800;
        try {
            ChessBot sample = (ChessBot) comboBox.getSelectedItem();
            if (sample != null) initElo = sample.getElo();
        } catch (Exception ex) {
            // ignore and use defaults
        }

        JSlider strengthSlider = new JSlider(JSlider.HORIZONTAL, minElo, maxElo, initElo);
        strengthSlider.setOpaque(false);
        strengthSlider.setMajorTickSpacing(600);
        strengthSlider.setPaintTicks(true);
        strengthSlider.setPaintLabels(false);
        strengthSlider.setPreferredSize(new Dimension(340, 28));
        strengthSlider.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));

        JPanel strengthWrap = new JPanel(new BorderLayout());
        strengthWrap.setOpaque(false);
        JPanel strengthLabelRow = new JPanel(new BorderLayout());
        strengthLabelRow.setOpaque(false);
        JLabel minLabel = new JLabel(String.valueOf(minElo));
        JLabel midLabel = new JLabel(String.valueOf(initElo));
        JLabel maxLabel = new JLabel(String.valueOf(maxElo));
        minLabel.setForeground(new Color(200, 190, 160));
        midLabel.setForeground(new Color(200, 190, 160));
        maxLabel.setForeground(new Color(200, 190, 160));
        minLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        midLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        maxLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        strengthLabelRow.add(minLabel, BorderLayout.WEST);
        strengthLabelRow.add(midLabel, BorderLayout.CENTER);
        strengthLabelRow.add(maxLabel, BorderLayout.EAST);
        strengthWrap.add(strengthHeading, BorderLayout.NORTH);
        strengthWrap.add(strengthSlider, BorderLayout.CENTER);
        strengthWrap.add(strengthLabelRow, BorderLayout.SOUTH);

        // Listen for combo changes and update slider
        comboBox.addActionListener(e -> {
            ChessBot b = (ChessBot) comboBox.getSelectedItem();
            if (b != null) {
                strengthSlider.setValue(b.getElo());
                midLabel.setText(String.valueOf(b.getElo()));
            }
        });

        // append strength area to the body below the summary
        JPanel bodyWithStrength = new JPanel();
        bodyWithStrength.setOpaque(false);
        bodyWithStrength.setLayout(new BoxLayout(bodyWithStrength, BoxLayout.Y_AXIS));
        bodyWithStrength.add(comboWrap);
        bodyWithStrength.add(Box.createVerticalStrut(16));
        bodyWithStrength.add(summary);
        bodyWithStrength.add(Box.createVerticalStrut(14));
        bodyWithStrength.add(strengthWrap);

        JPanel sideBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sideBadge.setOpaque(false);
        JLabel badge = new JLabel(whiteSide ? "WHITE SIDE" : "BLACK SIDE");
        badge.setFont(new Font("Bahnschrift", Font.BOLD, 13));
        badge.setForeground(new Color(24, 31, 45));
        badge.setOpaque(true);
        badge.setBackground(accent);
        badge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        sideBadge.add(badge);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleWrap, BorderLayout.CENTER);
        top.add(sideBadge, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(bodyWithStrength, BorderLayout.CENTER);
        return card;
    }

    private void styleSpectateCombo(JComboBox<ChessBot> comboBox) {
        comboBox.setFont(new Font("Segoe UI Semibold", Font.BOLD, 21));
        comboBox.setBackground(new Color(45, 55, 72));
        comboBox.setForeground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        comboBox.setOpaque(false);
        comboBox.setPreferredSize(new Dimension(240, 46));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        comboBox.setMaximumRowCount(8);
        comboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("\u25BE");
                b.setFont(new Font("Segoe UI", Font.BOLD, 14));
                b.setForeground(new Color(220, 229, 243));
                b.setBackground(new Color(62, 76, 100));
                b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                b.setFocusable(false);
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // keep wrapper background as the only background layer
            }
        });
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Segoe UI Semibold", Font.BOLD, 21));
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (value instanceof ChessBot) {
                    ChessBot b = (ChessBot) value;
                    label.setText(b.getName() + "  (ELO " + b.getElo() + ")");
                }
                if (isSelected) {
                    label.setBackground(new Color(87, 117, 172));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(44, 49, 62));
                    label.setForeground(new Color(232, 238, 248));
                }
                return label;
            }
        });
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        switch (command) {
            case "BOT_6":
            case "BOT_10":
            case "BOT_14":
            case "BOT_18":
                 showBotSelectionScreen();
                break;

            case "PLAY_FRIEND":
                ensureLocalVariantPanel();
                showCard("LOCAL_VARIANT", true);
                break;
                
            case "PLAY_BOT":
                showBotSelectionScreen();
                break;
                
            case "ONLINE":
                ensureOnlineScreen();
                showCard("ONLINE", true);
                break;
                
            case "ANALYSIS":
                ensureAnalysisPanel();
                showCard("ANALYSIS", true);
                break;

            case "PUZZLES":
                new puzzles(this);
                break;

            case "LESSONS":
                showUnderDevelopmentDialog("Lessons");
                break;

            case "ACCOUNT":
                showAccountOverlay();
                break;
                
            case "START_ANALYSIS":
                new AnalysisGame(this);
                break;
                
            case "QUIT":
                showExitConfirmationDialog();
                break;
                
            case "BACK_TO_MENU":
                goBackOneScreen();
                break;
        }
    }
    
    // Public method used by embedded panels to return to the main menu.
    public void showMenu() {
        showCard("MENU", false);
    }

    public void openSection(String command) {
        actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command));
    }

    private void updateBotInfoPanel(
            ChessBot bot,
            JLabel photoLabel,
            JTextArea nameLabel,
            JTextArea statsLabel,
            JTextArea storyText) {
        if (bot == null) return;
        nameLabel.setText(bot.getName());
        statsLabel.setText(getBotStatsLine(bot));
        storyText.setText(getBotStory(bot));
        ImageIcon icon = loadBotPortrait(bot, 210, 265);
        photoLabel.setIcon(icon);
        photoLabel.setText(icon == null ? "No photo" : "");
    }

    private JLabel createBotProfileValueLabel() {
        JLabel label = new JLabel();
        label.setFont(UiFonts.subtext(16f));
        label.setForeground(new Color(232, 211, 175));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JPanel createBotProfileStat(String heading, JLabel valueLabel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(
                0, 0, 0, 1, new Color(111, 80, 42, 90)));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(UiFonts.title(13f));
        headingLabel.setForeground(new Color(185, 143, 80));
        headingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(headingLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(valueLabel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private String getBotPlaystyle(ChessBot bot) {
        if (bot == ChessBot.BOBBY_BEGINNER) return "Adventurous";
        if (bot == ChessBot.CASUAL_CARL) return "Opportunistic";
        if (bot == ChessBot.TACTICAL_TOM) return "Aggressive";
        if (bot == ChessBot.STRATEGIC_SARAH) return "Positional";
        return "Relentless";
    }

    private String getBotStrength(ChessBot bot) {
        if (bot == ChessBot.BOBBY_BEGINNER) return "Surprise Moves";
        if (bot == ChessBot.CASUAL_CARL) return "Tactical Shots";
        if (bot == ChessBot.TACTICAL_TOM) return "Combinations";
        if (bot == ChessBot.STRATEGIC_SARAH) return "Long Plans";
        return "Everything";
    }

    private String getBotWeakness(ChessBot bot) {
        if (bot == ChessBot.BOBBY_BEGINNER) return "Loose Pieces";
        if (bot == ChessBot.CASUAL_CARL) return "Defense";
        if (bot == ChessBot.TACTICAL_TOM) return "Patience";
        if (bot == ChessBot.STRATEGIC_SARAH) return "Time";
        return "Mercy";
    }

    private String getBotStatsLine(ChessBot bot) {
        if (bot == ChessBot.MAGNUS_AI) {
            return "Skill ??? - Depth 20 - ~6700 ELO";
        }
        return "Skill " + bot.getSkillLevel() + " - Depth " + bot.getDepth() + " - ~" + bot.getElo() + " ELO";
    }

    private String getBotStory(ChessBot bot) {
        if (bot == null) return "";

        if (bot == ChessBot.BOBBY_BEGINNER) {
            return "Bobby got into chess because it looked cool in movies and his friend said \"bro, it's easy.\" " +
                "He plays on vibes, loves moving knights because they \"jump,\" and celebrates every capture like it's a highlight reel. " +
                "He's basically learning in real time - and somehow having the most fun out of everyone.";
        }
        if (bot == ChessBot.CASUAL_CARL) {
            return "Carl's a busy guy: college, errands, life... chess is his \"chill break\" game. " +
                "He knows the rules, knows a couple openings, and plays like someone who's watched enough YouTube to be dangerous but not enough to be consistent. " +
                "If you hang a piece, Carl will take it - then immediately forget to defend his queen.";
        }
        if (bot == ChessBot.TACTICAL_TOM) {
            return "Tom treats chess like a puzzle hunt. He's the type who pauses mid-conversation because he just saw a fork in his head. " +
                "He loves sharp lines, traps, and \"gotcha\" moves, and he absolutely lives for that one moment where the combo works and you can feel the check coming. " +
                "Against Tom, one lazy move becomes a full-on crime scene.";
        }
        if (bot == ChessBot.STRATEGIC_SARAH) {
            return "Sarah's into chess the way some people are into planning and perfection - calm, patient, and slightly terrifying. " +
                "She doesn't rush; she builds positions like she's arranging a room: everything has a purpose, nothing is wasted. " +
                "She studies endgames for fun (yes, really), and she wins by slowly turning \"this seems equal\" into \"why do I have no moves?\"";
        }
        return "Engine strength: 2400.\n" +
            "Emotional damage strength: 2800.\n\n" +
            "Knows theory 17 moves deep but will still roast your move in 0.3 seconds.\n" +
            "Plays solid, calculates fast, and somehow sighs digitally when you hang a piece.\n\n" +
            "Built from pure tactics, sarcasm, and opening prep.\n" +
            "Blunders? Not in this household.\n\n" +
            "Grandmother Levy - here to teach, punish, and say \"chat, what was that?\"";
    }

    private void showUnderDevelopmentDialog(String featureName) {
        JOptionPane.showMessageDialog(
            this,
            featureName + " is under development.",
            "Coming Soon",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private ImageIcon loadBotPortrait(ChessBot bot, int width, int height) {
        String base = getBotImageBaseName(bot);
        String[] candidates = {
            "/assets/avatars/" + base + ".png",
            "/assets/avatars/" + base + ".jpg",
            "/assets/avatars/" + base + ".jpeg"
        };

        for (String candidate : candidates) {
            try {
                URL url = getClass().getResource(candidate);
                if (url != null) {
                    Image img = ImageIO.read(url);
                    if (img != null) {
                        return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        String[] fs = {
            "Scaccomatto_final/Scaccomatto/src/assets/avatars/" + base + ".png",
            "Scaccomatto_final/Scaccomatto/src/assets/avatars/" + base + ".jpg",
            "Scaccomatto_final/Scaccomatto/src/assets/avatars/" + base + ".jpeg"
        };
        for (String path : fs) {
            try {
                File f = new File(path);
                if (!f.exists()) continue;
                Image img = ImageIO.read(f);
                if (img != null) {
                    return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }

    private String getBotImageBaseName(ChessBot bot) {
        if (bot == ChessBot.BOBBY_BEGINNER) return "bobby";
        if (bot == ChessBot.CASUAL_CARL) return "Carl";
        if (bot == ChessBot.TACTICAL_TOM) return "Tom";
        if (bot == ChessBot.STRATEGIC_SARAH) return "Sarah";
        return "levy";
    }

    private String showLocalVariantPicker() {
        LocalVariantOption[] options = {
            new LocalVariantOption("Classic", "Standard chess rules", "Classic", "classic.png", true),
            new LocalVariantOption("Chess960", "Randomized back rank setup", "Chess960", "960.png", true),
            new LocalVariantOption("King of the Hill", "Reach the center with your king", "King of the Hill", "kinghill.png", true),
            new LocalVariantOption("Three-check", "Win by giving three checks", "Three-check", "3checks.png", true),
            new LocalVariantOption("Atomic", "Captures explode nearby pieces", "Atomic", "atomic.png", true),
            new LocalVariantOption("Spell Chess", "Cast spells and outplay", "Spell Chess", "spellchess.png", true),
            new LocalVariantOption("Fog of War", "Limited vision outside piece range", "Fog of War", "fogofwar.png", true),
            new LocalVariantOption("Duck Chess (Coming Soon)", "8th variant coming soon", "Duck Chess", "duck.png", false)
        };

        final String[] selected = { "Classic" };
        final boolean[] accepted = { false };

        JDialog dialog = new JDialog(this, "Local Variant", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(920, 680);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(new Color(24, 29, 36));
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 86, 106), 1, true),
            BorderFactory.createEmptyBorder(18, 20, 16, 20)
        ));

        JLabel title = new JLabel("Chess Variants");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Bahnschrift", Font.BOLD, 54));

        JLabel subtitle = new JLabel("Choose a mode for your local match");
        subtitle.setForeground(new Color(191, 201, 220));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        JPanel grid = new JPanel(new GridLayout(4, 2, 12, 12));
        grid.setOpaque(false);

        java.util.List<JButton> variantButtons = new java.util.ArrayList<>();
        Color normalBg = new Color(53, 61, 76);
        Color hoverBg = new Color(75, 84, 110);
        Color selectedBg = new Color(84, 116, 170);
        Color normalBorder = new Color(86, 98, 122);
        Color hoverBorder = new Color(139, 120, 199);
        Color selectedBorder = new Color(145, 176, 229);

        for (LocalVariantOption option : options) {
            JButton card = createRoundedFillButton("<html><div style='text-align:left;'><b>"
                    + option.label + "</b><br>" + option.description + "</div></html>", 20);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.setHorizontalTextPosition(SwingConstants.RIGHT);
            card.setIconTextGap(16);
            card.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            card.setForeground(Color.WHITE);
            card.setFocusPainted(false);
            card.setCursor(option.enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            card.setBackground(option.enabled ? normalBg : new Color(64, 68, 76));
            card.putClientProperty("roundedBorderColor", option.enabled ? normalBorder : new Color(92, 92, 92));
            card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
            card.setEnabled(option.enabled);
            card.setPreferredSize(new Dimension(400, 106));
            card.putClientProperty("variant_normal_bg", normalBg);
            card.putClientProperty("variant_hover_bg", hoverBg);
            card.putClientProperty("variant_selected_bg", selectedBg);
            card.putClientProperty("variant_normal_border", normalBorder);
            card.putClientProperty("variant_hover_border", hoverBorder);
            card.putClientProperty("variant_selected_border", selectedBorder);

            Icon icon = loadVariantIcon(option.iconFile);
            if (icon != null) card.setIcon(icon);

            if (option.enabled) {
                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        boolean chosen = option.value.equals(selected[0]);
                        if (!chosen) {
                            card.setBackground(hoverBg);
                            card.putClientProperty("roundedBorderColor", hoverBorder);
                            card.repaint();
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        boolean chosen = option.value.equals(selected[0]);
                        card.setBackground(chosen ? selectedBg : normalBg);
                        card.putClientProperty("roundedBorderColor", chosen ? selectedBorder : normalBorder);
                        card.repaint();
                    }
                });

                card.addActionListener(e -> {
                    selected[0] = option.value;
                    for (JButton b : variantButtons) {
                        boolean chosen = b == card;
                        b.setBackground(chosen ? selectedBg : normalBg);
                        b.putClientProperty("roundedBorderColor", chosen ? selectedBorder : normalBorder);
                        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
                        b.repaint();
                    }
                });
                variantButtons.add(card);
            }

            grid.add(card);
        }

        if (!variantButtons.isEmpty()) {
            JButton first = variantButtons.get(0);
            first.setBackground(selectedBg);
            first.putClientProperty("roundedBorderColor", selectedBorder);
            first.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
            first.repaint();
        }

        JButton cancel = createRoundedFillButton("Cancel", 16);
        cancel.setFont(new Font("Bahnschrift", Font.BOLD, 24));
        cancel.setFocusPainted(false);
        cancel.setBackground(new Color(95, 101, 114));
        cancel.setForeground(Color.WHITE);
        cancel.putClientProperty("roundedBorderColor", new Color(123, 128, 140));
        cancel.setBorder(BorderFactory.createEmptyBorder(8, 28, 8, 28));
        cancel.addActionListener(e -> dialog.dispose());

        JButton play = createRoundedFillButton("Play", 16);
        play.setFont(new Font("Bahnschrift", Font.BOLD, 24));
        play.setFocusPainted(false);
        play.setBackground(new Color(112, 169, 80));
        play.setForeground(Color.WHITE);
        play.putClientProperty("roundedBorderColor", new Color(145, 198, 115));
        play.setBorder(BorderFactory.createEmptyBorder(8, 36, 8, 36));
        play.addActionListener(e -> {
            accepted[0] = true;
            dialog.dispose();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(play);

        root.add(header, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);

        return accepted[0] ? selected[0] : null;
    }

    private JButton createRoundedFillButton(String text, int arc) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                boolean selectedGlow = Boolean.TRUE.equals(
                        getClientProperty("variant_selected_glow"));
                Object variantTitle = getClientProperty("variant_card_title");
                boolean transparentVariantTile = variantTitle instanceof String;
                Color background = getBackground();
                boolean goldFill = background.getRed() > 90
                        && background.getGreen() > 55
                        && background.getRed() > background.getBlue() * 1.5;
                if (!transparentVariantTile || selectedGlow) {
                    if (goldFill) {
                        g2.setPaint(new GradientPaint(
                                0, 0, scaleColor(background, 1.22f),
                                0, h, scaleColor(background, 0.72f)));
                    } else {
                        g2.setColor(background);
                    }
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                    if (selectedGlow && !transparentVariantTile) {
                        g2.setColor(new Color(255, 202, 101, 30));
                        g2.fillRoundRect(2, 2, Math.max(0, w - 4), Math.max(0, h - 4), arc, arc);
                    }
                }
                Object borderColor = getClientProperty("roundedBorderColor");
                g2.setColor(borderColor instanceof Color ? (Color) borderColor : new Color(110, 120, 138));
                g2.setStroke(new BasicStroke(selectedGlow ? 1.8f : 1f));
                g2.drawRoundRect(0, 0, w, h, arc, arc);

                if (variantTitle instanceof String) {
                    Icon icon = getIcon();
                    int contentX = 20;
                    if (icon != null) {
                        int iconY = (getHeight() - icon.getIconHeight()) / 2;
                        icon.paintIcon(this, g2, contentX, iconY);
                        contentX += icon.getIconWidth() + 22;
                    }

                    g2.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(UiFonts.title(21f));
                    g2.setColor(new Color(242, 234, 219));
                    FontMetrics titleMetrics = g2.getFontMetrics();
                    int titleY = getHeight() / 2 - 3;
                    g2.drawString((String) variantTitle, contentX, titleY);

                    Object variantSubtitle = getClientProperty("variant_card_subtitle");
                    if (variantSubtitle instanceof String) {
                        g2.setFont(UiFonts.subtext(14f));
                        g2.setColor(new Color(190, 164, 126));
                        g2.drawString(
                                (String) variantSubtitle,
                                contentX,
                                titleY + titleMetrics.getDescent() + 18);
                    }
                    g2.dispose();
                    return;
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        return button;
    }

    private static Color scaleColor(Color color, float factor) {
        return new Color(
                Math.max(0, Math.min(255, Math.round(color.getRed() * factor))),
                Math.max(0, Math.min(255, Math.round(color.getGreen() * factor))),
                Math.max(0, Math.min(255, Math.round(color.getBlue() * factor))),
                color.getAlpha());
    }

    private void styleThemedScrollPane(JScrollPane scrollPane) {
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(14, 0));
        vBar.setUnitIncrement(20);
        vBar.setOpaque(false);
        vBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(106, 136, 192);
                trackColor = new Color(38, 44, 55);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createArrowButton("▲");
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createArrowButton("▼");
            }

            private JButton createArrowButton(String glyph) {
                JButton b = new JButton(glyph);
                b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 9));
                b.setForeground(new Color(210, 220, 236));
                b.setBackground(new Color(56, 63, 78));
                b.setBorder(BorderFactory.createLineBorder(new Color(90, 102, 124), 1, true));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setPreferredSize(new Dimension(14, 14));
                return b;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(38, 44, 55));
                g2.fillRoundRect(trackBounds.x + 2, trackBounds.y, trackBounds.width - 4, trackBounds.height, 10, 10);
                g2.setColor(new Color(70, 80, 98));
                g2.drawRoundRect(trackBounds.x + 2, trackBounds.y, trackBounds.width - 5, trackBounds.height - 1, 10, 10);
                g2.dispose();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = thumbBounds.x + 2;
                int y = thumbBounds.y + 1;
                int w = thumbBounds.width - 4;
                int h = thumbBounds.height - 2;
                GradientPaint gp = new GradientPaint(
                    x, y, new Color(134, 164, 218),
                    x, y + h, new Color(82, 115, 173)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setColor(new Color(176, 201, 238));
                g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
                g2.dispose();
            }
        });
    }

    private Icon loadVariantIcon(String file) {
        return loadVariantIcon(file, 68);
    }

    private Icon loadVariantIcon(String file, int size) {
        if (file == null || file.trim().isEmpty()) return null;
        String[] fs = {
            "Scaccomatto_final/Scaccomatto/src/assets/multiplayer/" + file,
            "src/assets/multiplayer/" + file,
            "assets/multiplayer/" + file
        };
        BufferedImage img = null;
        try {
            URL res = getClass().getResource("/assets/multiplayer/" + file);
            if (res != null) {
                img = ImageIO.read(res);
            }
        } catch (IOException ignored) {
        }
        if (img == null) {
            for (String p : fs) {
                try {
                    File f = new File(p);
                    if (!f.exists()) continue;
                    img = ImageIO.read(f);
                    if (img != null) break;
                } catch (IOException ignored) {
                }
            }
        }
        if (img == null) return null;
        return new VariantFramedIcon(img, size);
    }

    private void showExitConfirmationDialog() {
        dispose();
        System.exit(0);
    }

    @Override
    public void dispose() {
        if (accountOverlayTimer != null) {
            accountOverlayTimer.stop();
            accountOverlayTimer = null;
        }
        if (accountOverlay != null) {
            accountOverlay.setVisible(false);
            accountOverlay = null;
        }
        if (applicationTopBarMouseTracker != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(applicationTopBarMouseTracker);
            applicationTopBarMouseTracker = null;
        }
        if (applicationTopBar != null) {
            applicationTopBar.stopAnimations();
            applicationTopBar = null;
        }
        if (variantLaunchCinematicTimer != null) {
            variantLaunchCinematicTimer.stop();
            variantLaunchCinematicTimer = null;
        }
        if (variantCloseTimer != null) {
            variantCloseTimer.stop();
            variantCloseTimer = null;
        }
        if (mainMenuAtmosphereTimer != null) {
            mainMenuAtmosphereTimer.cancel();
            mainMenuAtmosphereTimer = null;
        }
        synchronized (mainMenuAtmosphereFrameLock) {
            mainMenuAtmosphereFrontFrame = null;
            mainMenuAtmosphereBackFrame = null;
        }
        super.dispose();
    }

    private BufferedImage loadExitDialogImage(String resourcePath, String filePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) return ImageIO.read(url);
        } catch (IOException ignored) {
        }
        try {
            File f = new File(filePath);
            if (f.exists()) return ImageIO.read(f);
        } catch (IOException ignored) {
        }
        return null;
    }

    private void applyRoundedDialogShape(JDialog dialog, int arc) {
        try {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc));
        } catch (UnsupportedOperationException ignored) {
            // Keep painted rounded card when shaped windows are unsupported.
        }
    }

    private static class VariantFramedIcon implements Icon {
        private final Image image;
        private final int imageSize;
        private final int size;

        VariantFramedIcon(BufferedImage img, int size) {
            this.size = size;
            this.imageSize = size - 14;
            this.image = img.getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH);
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 18;
            int w = size;
            int h = size;

            GradientPaint gp = new GradientPaint(
                x, y, new Color(140, 108, 198),
                x, y + h, new Color(90, 72, 140)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, w, h, arc, arc);

            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillRoundRect(x + 2, y + 2, w - 4, (h / 2) - 1, arc, arc);

            g2.setColor(new Color(176, 138, 228, 120));
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

            int ix = x + (w - imageSize) / 2;
            int iy = y + (h - imageSize) / 2;
            Shape oldClip = g2.getClip();
            int innerArc = 10;
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(ix, iy, imageSize, imageSize, innerArc, innerArc));
            g2.drawImage(image, ix, iy, null);
            g2.setClip(oldClip);
            g2.dispose();
        }
        }
    
}
