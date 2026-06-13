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
    private BotPanel botScreen;
    private OnlinePanel onlineScreen;
    private AnalysisPanel analysisPanel;
    private CreditsPanel creditsScreen;
    private Image backgroundImage;
    private Image appLogoImage;
    private String selectedLocalVariant = "Classic";
    private final java.util.List<JButton> localVariantButtons = new java.util.ArrayList<>();
    private JLabel localVariantStatusLabel;
    private boolean mainMenuDarkMode = false;
    private boolean mainMenuTransitioning = false;
    private boolean mainMenuTransitionToDark = false;
    private boolean mainMenuTransitionFromDark = false;
    private float mainMenuTransitionProgress = 1f;
    private float mainMenuTransitionOverlayAlpha = 1f;
    private Point mainMenuTransitionOrigin = new Point(0, 0);
    private Timer mainMenuTransitionTimer;
    private JComponent mainMenuTransitionOverlay;
    private JLabel mainTitleLabel;
    private JLabel mainCreditsLabel;
    private final java.util.List<MenuItemPanel> mainMenuItems = new java.util.ArrayList<>();
    private ThemeToggleButton mainMenuThemeToggle;
    private final Deque<String> navigationHistory = new ArrayDeque<>();
    private String currentCard = "MENU";
    private JComponent activeScreen;
    private Runnable activeScreenCleanup;
    private ProportionalUiScaler proportionalUiScaler;
    private int fullscreenDesignWidth;
    private int fullscreenDesignHeight;
    private boolean exitDialogOpen = false;
    private JComponent gameLaunchOverlay;
    private Timer gameLaunchOverlayTimer;
    private long gameLaunchOverlayStartedAt;
    private static final int GAME_LAUNCH_OVERLAY_MS = 3000;
    private StartupIntroPane startupIntroPane;
    private Timer startupIntroTimer;
    private static final long STARTUP_INTRO_MS = 5000L;
    private static final long STARTUP_MENU_REVEAL_MS = 1000L;
    
    public MainMenu() {
        setTitle("Scaccomatto");
        Rectangle screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        fullscreenDesignWidth = Math.max(1, screenBounds.width);
        fullscreenDesignHeight = Math.max(1, screenBounds.height);
        double launchScale = 0.78;
        int launchWidth = Math.max(960, (int) Math.round(fullscreenDesignWidth * launchScale));
        int launchHeight = Math.max(540, (int) Math.round(fullscreenDesignHeight * launchScale));
        setSize(
                Math.min(fullscreenDesignWidth, launchWidth),
                Math.min(fullscreenDesignHeight, launchHeight));
        setMinimumSize(new Dimension(
                Math.min(fullscreenDesignWidth, 960),
                Math.min(fullscreenDesignHeight, 540)));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);
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
        
        menuPanel = createMenuPanel();
        localVariantPanel = createLocalVariantPanel();
        
        botScreen = new BotPanel();
        onlineScreen = new OnlinePanel(this);
        analysisPanel = new AnalysisPanel(this);
        creditsScreen = new CreditsPanel(this);
        
        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(localVariantPanel, "LOCAL_VARIANT");
        mainPanel.add(botScreen, "BOT");
        mainPanel.add(onlineScreen, "ONLINE");
        mainPanel.add(analysisPanel, "ANALYSIS");
        mainPanel.add(creditsScreen, "CREDITS");
        
        add(mainPanel);
        
        showCard("MENU", false);
        installGlobalEscapeBackKey();
        installAltF4Hotkey();
        installStartupIntro();
        setVisible(true);
        proportionalUiScaler = new ProportionalUiScaler(
                mainPanel,
                fullscreenDesignWidth,
                fullscreenDesignHeight);
        SwingUtilities.invokeLater(this::startStartupIntro);
    }

    private void installStartupIntro() {
        startupIntroPane = new StartupIntroPane(loadStartupIntroImage());
        startupIntroPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                "skipStartupIntro");
        startupIntroPane.getActionMap().put("skipStartupIntro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skipStartupIntro();
            }
        });
        setGlassPane(startupIntroPane);
        startupIntroPane.setVisible(true);
    }

    private void startStartupIntro() {
        if (startupIntroPane == null) return;
        if (startupIntroTimer != null) startupIntroTimer.stop();
        long startedAt = System.currentTimeMillis();
        startupIntroTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startedAt;
            startupIntroPane.setElapsedMs(elapsed);
            if (elapsed < STARTUP_INTRO_MS + STARTUP_MENU_REVEAL_MS) return;
            ((Timer) e.getSource()).stop();
            startupIntroTimer = null;
            startupIntroPane.setVisible(false);
            mainPanel.requestFocusInWindow();
        });
        startupIntroTimer.setCoalesce(true);
        startupIntroTimer.start();
    }

    private void skipStartupIntro() {
        if (startupIntroPane == null || !startupIntroPane.isVisible()) return;
        if (startupIntroTimer != null) {
            startupIntroTimer.stop();
            startupIntroTimer = null;
        }
        startupIntroPane.setVisible(false);
        mainPanel.requestFocusInWindow();
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
        private long elapsedMs;

        StartupIntroPane(BufferedImage image) {
            this.image = image;
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

            float revealT = clamp01((elapsedMs - STARTUP_INTRO_MS)
                    / (float) STARTUP_MENU_REVEAL_MS);
            float overlayAlpha = 1f - smoothStep(revealT);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            float introT = clamp01(elapsedMs / (float) STARTUP_INTRO_MS);
            float imageAlpha = introImageAlpha(introT) * overlayAlpha;
            if (image != null && imageAlpha > 0.001f) {
                float scale = 1.075f - 0.075f * smoothStep(introT);
                int baseW = getWidth();
                int baseH = Math.max(1, Math.round(baseW * image.getHeight()
                        / (float) image.getWidth()));
                if (baseH > getHeight()) {
                    baseH = getHeight();
                    baseW = Math.max(1, Math.round(baseH * image.getWidth()
                            / (float) image.getHeight()));
                }
                int drawW = Math.max(1, Math.round(baseW * scale));
                int drawH = Math.max(1, Math.round(baseH * scale));
                int x = (getWidth() - drawW) / 2;
                int y = (getHeight() - drawH) / 2;

                float pulse = introPulse(introT);
                if (pulse > 0f) {
                    float radius = Math.max(1f, Math.min(getWidth(), getHeight()) * 0.46f);
                    g.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER,
                            Math.min(1f, pulse * 0.34f * overlayAlpha)));
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

                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, imageAlpha));
                g.drawImage(image, x, y, drawW, drawH, null);
            }
            g.dispose();
        }

        private static float introImageAlpha(float t) {
            if (t < 0.18f) return smoothStep(t / 0.18f);
            if (t < 0.76f) return 1f;
            return 1f - smoothStep((t - 0.76f) / 0.24f);
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
                MainMenu.this.actionPerformed(new ActionEvent(MainMenu.this, ActionEvent.ACTION_PERFORMED, "BACK_TO_MENU"));
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

    public void beginGameLaunchOverlay() {
        if (gameLaunchOverlayTimer != null) {
            gameLaunchOverlayTimer.stop();
            gameLaunchOverlayTimer = null;
        }
        if (gameLaunchOverlay == null) {
            JPanel overlay = new JPanel();
            overlay.setOpaque(true);
            overlay.setBackground(Color.WHITE);
            overlay.setFocusTraversalKeysEnabled(false);
            gameLaunchOverlay = overlay;
        }
        gameLaunchOverlayStartedAt = System.currentTimeMillis();
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

    public void finishGameLaunchOverlay(JComponent gameScreen) {
        if (gameLaunchOverlay == null || !gameLaunchOverlay.isVisible()) return;
        long elapsed = System.currentTimeMillis() - gameLaunchOverlayStartedAt;
        int remaining = (int) Math.max(0, GAME_LAUNCH_OVERLAY_MS - elapsed);
        gameLaunchOverlayTimer = new Timer(remaining, e -> {
            ((Timer) e.getSource()).stop();
            gameLaunchOverlayTimer = null;
            gameLaunchOverlay.setVisible(false);
            if (gameScreen != null) {
                gameScreen.requestFocusInWindow();
            }
        });
        gameLaunchOverlayTimer.setRepeats(false);
        gameLaunchOverlayTimer.start();
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
            URL res = getClass().getResource("/assets/bg.jpg");
            if (res != null) {
                return ImageIO.read(res);
            }
        } catch (IOException e) {
            System.err.println("Error loading background image from resources: " + e.getMessage());
        }

        String[] paths = {
            "src/assets/bg.jpg",
            "Scaccomatto_final/Scaccomatto/src/assets/bg.jpg",
            "assets/bg.jpg",
            "bg.jpg"
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

        System.err.println("Background image not found at /assets/bg.jpg or filesystem fallback paths.");
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
    
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(230, 230, 230));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                if (mainMenuDarkMode) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(7, 10, 16, 148));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            }
        };
        panel.setLayout(new BorderLayout());

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.setOpaque(false);
        leftContainer.setBorder(BorderFactory.createEmptyBorder(16, 44, 14, 0));

        JPanel topBlock = new JPanel();
        topBlock.setOpaque(false);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));

        if (appLogoImage != null) {
            Image scaled = appLogoImage.getScaledInstance(84, 84, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            logoLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            titleRow.add(logoLabel);
            titleRow.add(Box.createHorizontalStrut(16));
        }

        mainTitleLabel = new JLabel("Scaccomatto");
        mainTitleLabel.setFont(new Font("Georgia", Font.BOLD, 74));
        mainTitleLabel.setForeground(new Color(28, 28, 30));
        mainTitleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        titleRow.add(mainTitleLabel);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        topBlock.add(titleRow);
        topBlock.add(Box.createVerticalStrut(16));

        JPanel menuList = new JPanel();
        menuList.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuList.setOpaque(false);
        menuList.setLayout(new BoxLayout(menuList, BoxLayout.Y_AXIS));
        menuList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        MenuItemPanel playFriend = new MenuItemPanel("Play with Friend", "Local 2-player", new PeopleIcon(new Color(76, 175, 228)));
        MenuItemPanel playBot = new MenuItemPanel("Play with Bot", "Choose difficulty", new BotIcon(new Color(88, 97, 110)));
        MenuItemPanel online = new MenuItemPanel("Online", "Matchmaking & rooms", new GlobeIcon(new Color(90, 187, 137)));
        MenuItemPanel analysis = new MenuItemPanel("Analysis", "Review games, best moves", new ChartIcon(new Color(104, 149, 255)));
        MenuItemPanel puzzles = new MenuItemPanel("Puzzles", "Tactics & puzzle rush", new PuzzleIcon(new Color(244, 172, 78)));
        MenuItemPanel lessons = new MenuItemPanel("Lessons", "Guided training paths", new LessonIcon(new Color(153, 120, 235)));

        playFriend.setActionCommand("PLAY_FRIEND");
        playBot.setActionCommand("PLAY_BOT");
        online.setActionCommand("ONLINE");
        analysis.setActionCommand("ANALYSIS");
        puzzles.setActionCommand("PUZZLES");
        lessons.setActionCommand("LESSONS");

        menuList.add(playFriend);
        menuList.add(Box.createVerticalStrut(18));
        menuList.add(playBot);
        menuList.add(Box.createVerticalStrut(18));
        menuList.add(online);
        menuList.add(Box.createVerticalStrut(18));
        menuList.add(analysis);
        menuList.add(Box.createVerticalStrut(18));
        menuList.add(puzzles);
        menuList.add(Box.createVerticalStrut(18));
        menuList.add(lessons);
        mainMenuItems.clear();
        mainMenuItems.add(playFriend);
        mainMenuItems.add(playBot);
        mainMenuItems.add(online);
        mainMenuItems.add(analysis);
        mainMenuItems.add(puzzles);
        mainMenuItems.add(lessons);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        mainCreditsLabel = new JLabel("Credits");
        mainCreditsLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        mainCreditsLabel.setForeground(new Color(88, 93, 104));
        mainCreditsLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainCreditsLabel.addMouseListener(new MenuLabelListener(mainCreditsLabel, "CREDITS"));

        QuitPillButton quitButton = new QuitPillButton();
        quitButton.setActionCommand("QUIT");

        footer.add(mainCreditsLabel, BorderLayout.WEST);
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

        int cardWidth = 228;
        int cardHeight = 470;
        MenuCardPanel menuCard = new MenuCardPanel();
        menuCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuCard.setBorder(BorderFactory.createEmptyBorder(20, 10, 16, 10));
        menuCard.setLayout(new BorderLayout(0, 12));
        menuCard.add(menuScrollPane, BorderLayout.CENTER);
        menuCard.add(footer, BorderLayout.SOUTH);
        menuCard.setPreferredSize(new Dimension(cardWidth, cardHeight));
        menuCard.setMinimumSize(new Dimension(cardWidth, 420));
        menuCard.setMaximumSize(new Dimension(cardWidth, Integer.MAX_VALUE));

        leftContainer.add(topBlock, BorderLayout.NORTH);
        leftContainer.add(menuCard, BorderLayout.CENTER);

        JPanel contentHolder = new JPanel(new BorderLayout());
        contentHolder.setOpaque(false);
        contentHolder.add(leftContainer, BorderLayout.WEST);

        JLayeredPane overlayPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                for (Component c : getComponentsInLayer(JLayeredPane.DEFAULT_LAYER)) {
                    c.setBounds(0, 0, getWidth(), getHeight());
                }
                if (mainMenuThemeToggle != null) {
                    int s = mainMenuThemeToggle.getPreferredSize().width;
                    int margin = 18;
                    mainMenuThemeToggle.setBounds(getWidth() - s - margin, getHeight() - s - margin, s, s);
                }
                if (mainMenuTransitionOverlay != null) {
                    mainMenuTransitionOverlay.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };
        overlayPane.setOpaque(false);
        overlayPane.add(contentHolder, JLayeredPane.DEFAULT_LAYER);
        mainMenuThemeToggle = new ThemeToggleButton();
        overlayPane.add(mainMenuThemeToggle, JLayeredPane.PALETTE_LAYER);
        mainMenuTransitionOverlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                if (!mainMenuTransitioning) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float maxRadius = (float) maxDistanceToCorners(
                    mainMenuTransitionOrigin.x,
                    mainMenuTransitionOrigin.y,
                    getWidth(),
                    getHeight()
                );
                float eased = 1f - (float) Math.pow(1f - mainMenuTransitionProgress, 3);
                float radius = maxRadius * eased;
                int baseAlpha = mainMenuTransitionToDark ? 148 : 225;
                int alpha = Math.max(0, Math.min(255, (int) (baseAlpha * mainMenuTransitionOverlayAlpha)));
                g2.setClip(new java.awt.geom.Ellipse2D.Float(
                    mainMenuTransitionOrigin.x - radius,
                    mainMenuTransitionOrigin.y - radius,
                    radius * 2f,
                    radius * 2f
                ));
                g2.setColor(mainMenuTransitionToDark
                    ? new Color(7, 10, 16, alpha)
                    : new Color(246, 248, 252, alpha)
                );
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainMenuTransitionOverlay.setOpaque(false);
        overlayPane.add(mainMenuTransitionOverlay, JLayeredPane.DRAG_LAYER);

        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleMainMenuDarkMode");
        am.put("toggleMainMenuDarkMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMainMenuDarkMode();
            }
        });
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
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "menuCredits");
        am.put("menuCredits", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "CREDITS"));
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_8, 0), "menuQuit");
        am.put("menuQuit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "QUIT"));
            }
        });

        panel.add(overlayPane, BorderLayout.CENTER);
        applyMainMenuTheme();
        return panel;
    }

    private void applyMainMenuTheme() {
        if (mainTitleLabel != null) {
            mainTitleLabel.setForeground(mainMenuDarkMode ? new Color(236, 241, 248) : new Color(28, 28, 30));
        }
        if (mainCreditsLabel != null) {
            mainCreditsLabel.setForeground(mainMenuDarkMode ? new Color(205, 214, 228) : new Color(88, 93, 104));
        }
        for (MenuItemPanel item : mainMenuItems) {
            item.refreshTheme();
        }
        if (menuPanel != null) menuPanel.repaint();
    }

    private void toggleMainMenuDarkMode() {
        toggleMainMenuDarkModeFrom(mainMenuThemeToggle);
    }

    private void toggleMainMenuDarkModeFrom(Component sourceComp) {
        mainMenuTransitionToDark = !mainMenuDarkMode;
        ThemeToggleButton sourceToggle = (sourceComp instanceof ThemeToggleButton)
            ? (ThemeToggleButton) sourceComp
            : mainMenuThemeToggle;
        if (sourceToggle != null) {
            sourceToggle.animateSpin();
            sourceToggle.repaint();
        }
        if (sourceComp != null && menuPanel != null && sourceComp.isShowing()) {
            Point p = SwingUtilities.convertPoint(
                sourceComp,
                sourceComp.getWidth() / 2,
                sourceComp.getHeight() / 2,
                menuPanel
            );
            mainMenuTransitionOrigin = p;
        } else if (mainMenuThemeToggle != null && menuPanel != null) {
            Point p = SwingUtilities.convertPoint(
                mainMenuThemeToggle,
                mainMenuThemeToggle.getWidth() / 2,
                mainMenuThemeToggle.getHeight() / 2,
                menuPanel
            );
            mainMenuTransitionOrigin = p;
        }
        mainMenuDarkMode = mainMenuTransitionToDark;
        applyMainMenuTheme();
        mainMenuTransitioning = false;
        mainMenuTransitionProgress = 1f;
        mainMenuTransitionOverlayAlpha = 0f;
        if (mainMenuTransitionTimer != null && mainMenuTransitionTimer.isRunning()) {
            mainMenuTransitionTimer.stop();
        }
        if (mainMenuTransitionOverlay != null) mainMenuTransitionOverlay.repaint();
    }

    private void startMainMenuTransition() {
        mainMenuTransitioning = true;
        mainMenuTransitionProgress = 0f;
        mainMenuTransitionOverlayAlpha = 1f;
        long start = System.currentTimeMillis();
        final int revealMs = 580;
        final int fadeMs = 140;
        if (mainMenuTransitionTimer != null && mainMenuTransitionTimer.isRunning()) {
            mainMenuTransitionTimer.stop();
        }
        mainMenuTransitionTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed <= revealMs) {
                mainMenuTransitionProgress = elapsed / (float) revealMs;
                mainMenuTransitionOverlayAlpha = 1f;
            } else if (elapsed <= revealMs + fadeMs) {
                mainMenuTransitionProgress = 1f;
                float fadeT = (elapsed - revealMs) / (float) fadeMs;
                mainMenuTransitionOverlayAlpha = Math.max(0f, 1f - fadeT);
                // Commit real theme once full-screen reveal is achieved.
                if (mainMenuDarkMode != mainMenuTransitionToDark) {
                    mainMenuDarkMode = mainMenuTransitionToDark;
                    applyMainMenuTheme();
                }
            } else {
                mainMenuTransitionProgress = 1f;
                mainMenuTransitionOverlayAlpha = 0f;
                mainMenuDarkMode = mainMenuTransitionToDark;
                applyMainMenuTheme();
                mainMenuTransitioning = false;
                ((Timer) e.getSource()).stop();
            }
            if (mainMenuTransitionOverlay != null) mainMenuTransitionOverlay.repaint();
            if (menuPanel != null) menuPanel.repaint();
        });
        mainMenuTransitionTimer.start();
    }

    private double maxDistanceToCorners(int x, int y, int w, int h) {
        double d1 = Point.distance(x, y, 0, 0);
        double d2 = Point.distance(x, y, w, 0);
        double d3 = Point.distance(x, y, 0, h);
        double d4 = Point.distance(x, y, w, h);
        return Math.max(Math.max(d1, d2), Math.max(d3, d4));
    }

    private JPanel createLocalVariantPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(20, 23, 28),
                    0, getHeight(), new Color(33, 37, 45)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(26, 34, 22, 34));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Chess Variants");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 56));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Pick a mode for your local match");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        subtitle.setForeground(new Color(184, 191, 205));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        panel.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 14, 14));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));

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
        Color normalBg = new Color(53, 61, 76);
        Color hoverBg = new Color(75, 84, 110);
        Color selectedBg = new Color(84, 116, 170);
        Color normalBorder = new Color(86, 98, 122);
        Color hoverBorder = new Color(139, 120, 199);
        Color selectedBorder = new Color(145, 176, 229);

        localVariantButtons.clear();
        for (LocalVariantOption option : options) {
            JButton card = createRoundedFillButton("<html><div style='text-align:left;'><b>" + option.label + "</b><br>" + option.description + "</div></html>", 20);
            card.setHorizontalAlignment(SwingConstants.LEFT);
            card.setHorizontalTextPosition(SwingConstants.RIGHT);
            card.setIconTextGap(16);
            card.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            card.setForeground(Color.WHITE);
            card.setFocusPainted(false);
            card.setBackground(normalBg);
            card.putClientProperty("roundedBorderColor", normalBorder);
            card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.putClientProperty("variant_normal_bg", normalBg);
            card.putClientProperty("variant_hover_bg", hoverBg);
            card.putClientProperty("variant_selected_bg", selectedBg);
            card.putClientProperty("variant_normal_border", normalBorder);
            card.putClientProperty("variant_hover_border", hoverBorder);
            card.putClientProperty("variant_selected_border", selectedBorder);

            Icon icon = loadVariantIcon(option.iconFile);
            if (icon != null) card.setIcon(icon);

            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    boolean chosen = option.value.equals(selectedLocalVariant);
                    if (!chosen) {
                        card.setBackground(hoverBg);
                        card.putClientProperty("roundedBorderColor", hoverBorder);
                        card.repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
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
        JButton back = createRoundedFillButton("Back", 16);
        back.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        back.setFocusPainted(false);
        back.setBackground(new Color(95, 101, 114));
        back.setForeground(Color.WHITE);
        back.putClientProperty("roundedBorderColor", new Color(123, 128, 140));
        back.setBorder(BorderFactory.createEmptyBorder(8, 28, 8, 28));
        back.addActionListener(e -> MainMenu.this.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "BACK_TO_MENU")));

        JButton play = createRoundedFillButton("Play", 16);
        play.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        play.setFocusPainted(false);
        play.setBackground(new Color(112, 169, 80));
        play.setForeground(Color.WHITE);
        play.putClientProperty("roundedBorderColor", new Color(145, 198, 115));
        play.setBorder(BorderFactory.createEmptyBorder(8, 32, 8, 32));
        play.addActionListener(e -> {
            launchGameWithOverlay(
                    () -> new ChessGame(this, 600, 0, selectedLocalVariant));
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
        localVariantStatusLabel.setForeground(Color.WHITE);
        localVariantStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 37 / 2));
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
            Color normalBg = colorProp(b, "variant_normal_bg", new Color(53, 61, 76));
            Color selectedBg = colorProp(b, "variant_selected_bg", new Color(84, 116, 170));
            Color normalBorder = colorProp(b, "variant_normal_border", new Color(86, 98, 122));
            Color selectedBorder = colorProp(b, "variant_selected_border", new Color(145, 176, 229));
            b.setBackground(chosen ? selectedBg : normalBg);
            b.putClientProperty("roundedBorderColor", chosen ? selectedBorder : normalBorder);
            b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
            b.repaint();
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
            int arc = 20;
            int w = getWidth() - 10;
            int h = getHeight() - 10;
            if (mainMenuDarkMode) {
                g2.setColor(new Color(0, 0, 0, 62));
            } else {
                g2.setColor(new Color(0, 0, 0, 20));
            }
            g2.fillRoundRect(4, 6, w, h, arc, arc);
            g2.setColor(mainMenuDarkMode ? new Color(23, 29, 39, 236) : new Color(246, 248, 252, 235));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(mainMenuDarkMode ? new Color(70, 84, 104, 220) : new Color(224, 229, 238, 210));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class MenuItemPanel extends JPanel {
        private final JLabel title;
        private final JLabel subtitle;
        private String actionCommand;
        private boolean hover;

        MenuItemPanel(String titleText, String subtitleText, Icon icon) {
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLabel = new JLabel(icon);
            iconLabel.setPreferredSize(new Dimension(38, 38));

            title = new JLabel(titleText);
            title.setFont(new Font("Segoe UI", Font.BOLD, 21));

            subtitle = new JLabel(subtitleText);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));

            JPanel textWrap = new JPanel();
            textWrap.setOpaque(false);
            textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
            textWrap.add(title);
            textWrap.add(Box.createVerticalStrut(3));
            textWrap.add(subtitle);

            add(iconLabel, BorderLayout.WEST);
            add(textWrap, BorderLayout.CENTER);

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
            if (isHover) {
                setBackground(mainMenuDarkMode ? new Color(52, 62, 78) : new Color(238, 242, 249));
                setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            } else {
                setBackground(new Color(0, 0, 0, 0));
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            }
            repaint();
        }

        void refreshTheme() {
            title.setForeground(mainMenuDarkMode ? new Color(232, 239, 248) : new Color(36, 38, 44));
            subtitle.setForeground(mainMenuDarkMode ? new Color(168, 182, 200) : new Color(120, 126, 136));
            setHover(hover);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int insetX = 0;
                int insetY = 0;
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(insetX + 1, insetY + 2, w, h, 18, 18);
                g2.setColor(getBackground());
                g2.fillRoundRect(insetX, insetY, w, h, 18, 18);
                g2.setColor(mainMenuDarkMode ? new Color(92, 112, 140) : new Color(211, 219, 232));
                g2.drawRoundRect(insetX, insetY, w - 1, h - 1, 18, 18);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private class ThemeToggleButton extends JComponent {
        private static final int SIZE = 42;
        private static final int DURATION_MS = 300;
        private float spinDegrees = 0f;
        private Timer spinTimer;
        private long spinStartMs;
        private float startAngle;
        private float endAngle;

        ThemeToggleButton() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleMainMenuDarkModeFrom(ThemeToggleButton.this);
                }
            });
        }

        void animateSpin() {
            startAngle = spinDegrees;
            endAngle = startAngle + 360f;
            spinStartMs = System.currentTimeMillis();
            if (spinTimer != null && spinTimer.isRunning()) spinTimer.stop();
            spinTimer = new Timer(16, e -> {
                float t = (System.currentTimeMillis() - spinStartMs) / (float) DURATION_MS;
                if (t >= 1f) {
                    spinDegrees = endAngle;
                    spinTimer.stop();
                } else {
                    float eased = 1f - (float) Math.pow(1f - t, 3);
                    spinDegrees = startAngle + (endAngle - startAngle) * eased;
                }
                repaint();
            });
            spinTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            int cx = w / 2;
            int cy = h / 2;
            g2.rotate(Math.toRadians(spinDegrees), cx, cy);
            if (mainMenuDarkMode) {
                g2.setColor(new Color(255, 255, 255));
                g2.drawOval(cx - 5, cy - 5, 10, 10);
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    int x1 = cx + (int) (Math.cos(a) * 8);
                    int y1 = cy + (int) (Math.sin(a) * 8);
                    int x2 = cx + (int) (Math.cos(a) * 10);
                    int y2 = cy + (int) (Math.sin(a) * 10);
                    g2.drawLine(x1, y1, x2, y2);
                }
            } else {
                g2.setColor(new Color(0, 0, 0));
                Shape outer = new Ellipse2D.Float(cx - 7f, cy - 7f, 14f, 14f);
                Shape inner = new Ellipse2D.Float(cx - 3f, cy - 8f, 14f, 14f);
                Area crescent = new Area(outer);
                crescent.subtract(new Area(inner));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(crescent);
            }

            g2.dispose();
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
            setPreferredSize(new Dimension(102, 30));
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
            int arc = 18;
            Color top = hover ? new Color(233, 77, 77) : new Color(206, 66, 66);
            Color bottom = hover ? new Color(172, 41, 41) : new Color(150, 34, 34);
            GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(new Color(255, 255, 255, hover ? 70 : 40));
            g2.fillRoundRect(2, 2, getWidth() - 5, (getHeight() / 2) - 2, arc, arc);
            g2.setColor(new Color(92, 20, 20));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            g2.setColor(new Color(255, 255, 255, 230));
            g2.setFont(new Font("Bahnschrift", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            String text = "Quit";
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
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(20, 23, 28),
                    0, getHeight(), new Color(33, 37, 45)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(141, 184, 93, 36));
                g2.fillOval(getWidth() - 330, -140, 420, 290);
                g2.setColor(new Color(77, 112, 167, 30));
                g2.fillOval(-170, getHeight() - 250, 380, 320);
                g2.dispose();
            }
        };
        mainContainer.setBorder(BorderFactory.createEmptyBorder(26, 34, 22, 34));
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
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JLabel title = new JLabel("Play Against Bot");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 50));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Pick a bot profile and start your match");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitle.setForeground(new Color(184, 191, 205));

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
                g2.setColor(new Color(155, 204, 102));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(176, 220, 124));
                g2.drawRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Bahnschrift", Font.BOLD, 13));
        badge.setForeground(new Color(18, 23, 14));
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        header.add(titleWrap, BorderLayout.WEST);
        header.add(badgeWrap, BorderLayout.EAST);

        JPanel body = new JPanel(new GridLayout(1, 2, 24, 0));
        body.setOpaque(false);

        JPanel leftCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int arc = 22;
                g2.setColor(new Color(34, 38, 45));
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
                g2.setColor(new Color(78, 84, 97));
                g2.drawRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(48, 54, 65));
                g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
                g2.dispose();
            }
        };
        leftCard.setOpaque(false);
        leftCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

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
        botPhotoLabel.setPreferredSize(new Dimension(128, 128));
        botPhotoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        botPhotoLabel.setVerticalAlignment(SwingConstants.CENTER);
        botPhotoLabel.setOpaque(false);
        botPhotoLabel.setBorder(BorderFactory.createLineBorder(new Color(78, 84, 97), 1, true));

        JLabel botNameLabel = new JLabel();
        botNameLabel.setFont(new Font("Bahnschrift", Font.BOLD, 34));
        botNameLabel.setForeground(Color.WHITE);

        JLabel botStatsLabel = new JLabel();
        botStatsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        botStatsLabel.setForeground(new Color(184, 191, 205));

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
        botHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        botHeaderPanel.add(botPhotoLabel, BorderLayout.WEST);
        botHeaderPanel.add(headerTextWrap, BorderLayout.CENTER);
        leftCard.add(botHeaderPanel, BorderLayout.NORTH);

        JTextArea infoText = new JTextArea();
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        infoText.setForeground(new Color(214, 220, 231));
        infoText.setBackground(new Color(34, 38, 45));
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JScrollPane infoScroll = new JScrollPane(infoText);
        infoScroll.setBorder(null);
        infoScroll.getViewport().setBackground(new Color(34, 38, 45));
        infoScroll.setBackground(new Color(34, 38, 45));
        infoScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleThemedScrollPane(infoScroll);
        leftCard.add(infoScroll, BorderLayout.CENTER);

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
        botPanel.setBotSelectionListener(bot -> updateBotInfoPanel(bot, botPhotoLabel, botNameLabel, botStatsLabel, infoText));
        updateBotInfoPanel(botPanel.getSelectedBot(), botPhotoLabel, botNameLabel, botStatsLabel, infoText);

        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backPanel.setOpaque(false);
        backPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JButton backButton = createRoundedFillButton("Back to Menu", 14);
        backButton.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(66, 71, 83));
        backButton.setFocusPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.putClientProperty("roundedBorderColor", new Color(108, 114, 126));
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        backButton.addActionListener(ev -> returnToMenuFromEmbeddedScreen());
        backPanel.add(backButton);

        rightCard.add(botPanel, BorderLayout.CENTER);
        rightCard.add(backPanel, BorderLayout.SOUTH);

        body.add(leftCard);
        body.add(rightCard);

        mainContainer.add(header, BorderLayout.NORTH);
        mainContainer.add(body, BorderLayout.CENTER);

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

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(comboWrap);
        body.add(Box.createVerticalStrut(16));
        body.add(summary);

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
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
                showCard("LOCAL_VARIANT", true);
                break;
                
            case "PLAY_BOT":
                showBotSelectionScreen();
                break;
                
            case "ONLINE":
                showCard("ONLINE", true);
                break;
                
            case "ANALYSIS":
                showCard("ANALYSIS", true);
                break;

            case "PUZZLES":
                new puzzles(this);
                break;

            case "LESSONS":
                showUnderDevelopmentDialog("Lessons");
                break;
                
            case "START_ANALYSIS":
                new AnalysisGame(this);
                break;
                
            case "CREDITS":
                showCard("CREDITS", true);
                break;
                
            case "QUIT":
                showExitConfirmationDialog();
                break;
                
            case "BACK_TO_MENU":
                goBackOneScreen();
                break;
        }
    }
    
    // Public method to show the main menu (called from other panels like Credits)
    public void showMenu() {
        showCard("MENU", false);
    }

    public boolean isMainMenuDarkMode() {
        return mainMenuDarkMode;
    }

    public void setMainMenuDarkModeEnabled(boolean darkMode) {
        if (mainMenuDarkMode == darkMode) return;
        toggleMainMenuDarkMode();
    }

    public void toggleThemeFromComponent(Component sourceComp) {
        toggleMainMenuDarkModeFrom(sourceComp);
    }

    public JComponent createSharedThemeToggleControl() {
        ThemeToggleButton toggle = new ThemeToggleButton();
        return toggle;
    }

    private void updateBotInfoPanel(ChessBot bot, JLabel photoLabel, JLabel nameLabel, JLabel statsLabel, JTextArea storyText) {
        if (bot == null) return;
        nameLabel.setText(bot.getName());
        statsLabel.setText(getBotStatsLine(bot));
        storyText.setText(getBotStory(bot));
        ImageIcon icon = loadBotPortrait(bot, 120, 120);
        photoLabel.setIcon(icon);
        photoLabel.setText(icon == null ? "No photo" : "");
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
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                Object borderColor = getClientProperty("roundedBorderColor");
                g2.setColor(borderColor instanceof Color ? (Color) borderColor : new Color(110, 120, 138));
                g2.drawRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        return button;
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
        return new VariantFramedIcon(img, 68);
    }

    private void showExitConfirmationDialog() {
        dispose();
        System.exit(0);
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
