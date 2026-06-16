import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.HierarchyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ChessGame extends JFrame {
        private MainMenu appHost;
        private JComponent embeddedHotkeyTarget;

        private Frame dialogOwner() {
            return appHost != null ? appHost : this;
        }
        private static final int BASE_GAME_WIDTH = 1500;
        private static final int BASE_GAME_HEIGHT = 800;
        private static final int BASE_LEFT_WIDTH = 664;
        private static final int BASE_CENTER_WIDTH = 440;
        private static final int BASE_RIGHT_WIDTH = 420;
        private static final int MIN_LEFT_WIDTH = 420;
        private static final int MIN_CENTER_WIDTH = 340;
        private static final int MIN_RIGHT_WIDTH = 340;
        private static final int SPLIT_DIVIDER_WIDTH = 8;
        private static final int BASE_LAYOUT_WIDTH = BASE_LEFT_WIDTH + BASE_CENTER_WIDTH + BASE_RIGHT_WIDTH;
        private static final int BASE_PLAYER_BAR_HEIGHT = 72;
        private static final int BOARD_SIDE_COMPARTMENT_WIDTH = 38;
        private static final int TIME_CONTROL_WIDTH = 509;
        private static final int TIME_SELECT_HEIGHT = 92;
        private static final int TIME_DETAILS_HEIGHT = 420;
        private static final int TIME_START_HEIGHT = 100;
        private static final int MIN_TIME_CONTROL_WIDTH = 250;
        private static final int COLLAPSED_TIME_CONTROL_HEIGHT =
            TIME_SELECT_HEIGHT + 14 + TIME_START_HEIGHT + 12;
        private static final int TIME_PRESET_WIDTH = 156;
        private static final int TIME_PRESET_HEIGHT = 79;
        private static final int TIME_DETAILS_ANIMATION_MS = 150;
        
        private JLabel topClockLabel;
        private JLabel bottomClockLabel;
        private JLabel topPlayerLabel;
        private JLabel bottomPlayerLabel;
        private CapturedMaterialStrip topCapturedStrip;
        private CapturedMaterialStrip bottomCapturedStrip;
        private JPanel topPanel;
        private JPanel bottomPanel;
        private JPanel leftGamePanel;
        private JPanel boardStackPanel;
        private JPanel leftContentPanel;
        private JPanel evalBarColumnPanel;
        private JPanel evalBarLanePanel;
    private JPanel centerColumnPanel;
    private JSplitPane centerRightSplitPane;
    private JSplitPane leftMainSplitPane;
    private final Map<Component, ResponsiveControlMetrics> responsiveControlMetrics = new IdentityHashMap<>();
    private final Map<Component, Font> leftPanelBaseFonts = new IdentityHashMap<>();
    private boolean applyingResponsiveControlScale = false;
        private Component centerSpellFiller;
        private Board board;
        private SettingsPanel settingsPanel;
        private MoveHistoryPanel moveHistoryPanel;
        private NetworkManager networkManager;
        private boolean onlineMode = false;
        private boolean localIsWhite = true;
        private StockfishEngine stockfishEngine;
        private int botDepth = 12;
        private ChessBot selectedBot = null;
        private ChessBot spectateWhiteBot = null;
        private ChessBot spectateBlackBot = null;
        private int botSkillLevel = 20;
        private StringBuilder uciMoves = new StringBuilder();
        private ChessBot opponentBot;
        private boolean whiteAtBottom = true; // perspective
        private boolean youAreWhite = true;   // player color
        private boolean isBotGame = false;
        private boolean spectateMode = false;
        private boolean playerIsWhite = true;
        private volatile boolean botMoveInProgress = false;
        private String gameVariant = "Classic";
        private String lastOpeningTitle = "";
        private boolean openingNamesEnabled = true;
        
        // NEW: Opening detection
        private OpeningDetector openingDetector;
        private OpeningNamePanel openingNamePanel;
        
        // Timer settings (in seconds)
        private int whiteTimeRemaining;
        private int blackTimeRemaining;
        private int incrementSeconds; // Time added per move
        private Timer timer;
        private boolean whitesTurn = true;
        private boolean timerStarted = false;
        private volatile boolean gameOver = false;
        private volatile boolean ratingResultRecorded = false;
        private final AccountApiClient accountApi = new AccountApiClient();
        private boolean applyingRemoteNetworkEvent = false;


    // In-game time control UI (replaces the separate time selection window)
    private JButton timeSelectBtn;
    private JButton startGameBtn;
    private JButton resetBoardBtn;
    private JButton exitToMenuBtn;
    private boolean gameStarted = false;
    private boolean exitDialogOpen = false;
    private boolean timeDetailsExpanded = false;
    private JPanel timeControlPanel;
    private JPanel timeDetailsPanel;
    private JPanel timeDetailsHostPanel;
    private final List<JButton> timePresetButtons = new ArrayList<>();
    private Timer timeDetailsAnimTimer;
    private TimeUiTheme timeUiTheme = TimeUiTheme.forName("Blue");
    private int timeDetailsCurrentHeight = 0;
    private int timeDetailsExpandedHeight = 350;
    private JPanel whiteSpellPanel;
    private JPanel blackSpellPanel;
    private JLabel whiteSpellElixirLabel;
    private JLabel blackSpellElixirLabel;
    private ElixirBar whiteElixirBar;
    private ElixirBar blackElixirBar;
    private final List<String> whiteActiveSpells = new ArrayList<>();
    private final List<String> blackActiveSpells = new ArrayList<>();
    private final List<SpellCardButton> whiteCardButtons = new ArrayList<>();
    private final List<SpellCardButton> blackCardButtons = new ArrayList<>();
    private SpellCardButton whiteNextCardPreview;
    private SpellCardButton blackNextCardPreview;
    private String whiteNextSpellId;
    private String blackNextSpellId;
    private int whiteSelectedCardIndex = -1;
    private int blackSelectedCardIndex = -1;
    private int whitePendingTargetCardIndex = -1;
    private int blackPendingTargetCardIndex = -1;
    private String whitePendingTargetSpellId = null;
    private String blackPendingTargetSpellId = null;
    private final List<String> allSpellIds = new ArrayList<>();
    private final Map<String, Spell> spellById = new HashMap<>();
    private final Map<String, BufferedImage> spellArtCache = new HashMap<>();
    private final Map<String, BufferedImage> spellArtGrayCache = new HashMap<>();
    private Timer spellAnimTimer;
    private float spellAnimPhase = 0f;
    private static final int PLAYABLE_SPELL_SLOTS = 3;
    private static final int TOTAL_SPELL_SLOTS = 4;
    private static final int NEXT_SLOT_INDEX = TOTAL_SPELL_SLOTS - 1;
    private static final int SPELL_CARD_W = 88;
    private static final int SPELL_CARD_H = 132;
    private static final int SPELL_CARD_GAP = 6;
    private static final int SPELL_CARDS_START_X = 5;
    private static final int SPELL_CARD_Y = 2;
    private static final int SPELL_NEXT_W = 72;
    private static final int SPELL_NEXT_H = 108;
    private static final int SPELL_PANEL_W = 500;
    private static final int SPELL_PANEL_H = 250;
    private static final int SPELL_CARDS_W = 474;
    private static final int SPELL_CARDS_H = 146;
    private static final int SPELL_BAR_W = 460;
    private static final int SPELL_BAR_H = 56;
    private boolean spellCardTransitionAnimating = false;
    private boolean spellResolutionLocked = false;
    private boolean spellResolutionWaitsForFireballTimer = false;
    private int spellResolutionHoldMs = SPELL_RESOLUTION_HOLD_MS;
    private boolean resumeClockAfterSpellResolution = false;
    private Timer spellResolutionReleaseTimer;
    private JComponent spellTravelOverlay;
    private Timer spellTravelTimer;
    private Timer clockOverlayFadeTimer;
    private float clockPauseOverlayAlpha = 0f;
    private static final int SPELL_CARD_TRANSITION_MS = 220;
    private static final int SPELL_RESOLUTION_HOLD_MS = 2550;
    private static final int CLOCK_OVERLAY_FADE_MS = 220;
    private static final int SPELL_TRAVEL_IN_MS = 750;
    private static final int SPELL_TRAVEL_HOLD_MS = 1000;
    private static final int FREEZE_TRAVEL_HOLD_MS = 2400;
    private static final int ENDERMAN_TRAVEL_HOLD_MS = 1000;
    private static final int ENDERMAN_ABSORB_MS = 850;
    private static final int SPELL_TRAVEL_OUT_MS = 800;
    private static final int FOG_CHARGE_MS = 2000;
    private static final int FOG_GROWTH_HOLD_BEFORE_RIPPLE_MS = 200;
    private static final int FOG_RIPPLE_HOLD_MS = FOG_CHARGE_MS;
    private static final int FOG_CARD_SHRINK_MS = 320;
    private static final int SPELL_TRAVEL_TOTAL_MS =
            SPELL_TRAVEL_IN_MS + SPELL_TRAVEL_HOLD_MS + SPELL_TRAVEL_OUT_MS;
    private static final int SPELL_TRAVEL_FRAME_MS = AnimationTiming.FRAME_DELAY_MS;
    private static final int FIREBALL_ANIMATION_LOCK_MS = 6000 + SPELL_TRAVEL_TOTAL_MS;

    private static class TimePreset {
        final String display;
        final int initialSeconds;
        final int incrementSeconds;
        TimePreset(String display, int initialSeconds, int incrementSeconds) {
            this.display = display;
            this.initialSeconds = initialSeconds;
            this.incrementSeconds = incrementSeconds;
        }
    }

    private static class ResponsiveControlMetrics {
        final Font font;
        final Dimension preferredSize;
        final Dimension minimumSize;
        final Dimension maximumSize;

        ResponsiveControlMetrics(Component component) {
            font = component.getFont();
            preferredSize = copyDimension(component.getPreferredSize());
            minimumSize = copyDimension(component.getMinimumSize());
            maximumSize = copyDimension(component.getMaximumSize());
        }

        private static Dimension copyDimension(Dimension source) {
            return source == null ? null : new Dimension(source);
        }
    }

    private static class TimeUiTheme {
        final Color accent;
        final Color startTop;
        final Color startBottom;
        final Color startHoverTop;
        final Color startHoverBottom;
        final Color border;
        final Color borderDark;
        final Color textShadow;

        TimeUiTheme(Color accent) {
            this.accent = accent;
            this.startTop = mix(accent, Color.WHITE, 0.12f);
            this.startBottom = mix(accent, Color.BLACK, 0.22f);
            this.startHoverTop = mix(accent, Color.WHITE, 0.23f);
            this.startHoverBottom = mix(accent, Color.BLACK, 0.12f);
            this.border = mix(accent, Color.WHITE, 0.46f);
            this.borderDark = mix(accent, Color.BLACK, 0.42f);
            this.textShadow = mix(accent, Color.BLACK, 0.62f);
        }

        static TimeUiTheme forName(String name) {
            if ("Green".equals(name)) return new TimeUiTheme(new Color(139, 206, 96));
            if ("Gray".equals(name)) return new TimeUiTheme(new Color(154, 166, 174));
            if ("Purple".equals(name)) return new TimeUiTheme(new Color(146, 104, 210));
            if ("Red".equals(name)) return new TimeUiTheme(new Color(214, 88, 88));
            if ("Orange".equals(name)) return new TimeUiTheme(new Color(214, 135, 61));
            if ("Yellow".equals(name)) return new TimeUiTheme(new Color(235, 181, 28));
            if ("Pink".equals(name)) return new TimeUiTheme(new Color(220, 105, 165));
            return new TimeUiTheme(new Color(98, 165, 225));
        }

        private static Color mix(Color a, Color b, float amountOfB) {
            float t = Math.max(0f, Math.min(1f, amountOfB));
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }
    }

    private JLabel getWhiteClockLabel() {
    return whiteAtBottom ? bottomClockLabel : topClockLabel;}

    private JLabel getBlackClockLabel() {
    return whiteAtBottom ? topClockLabel : bottomClockLabel;}

    private TimePreset selectedPreset = new TimePreset("10 min (Rapid)", 600, 0);

        private void endGameAndReturnToMenu(String message) {
        stopTimer();
        recordTerminalResult("Game Over", message);

        if (networkManager != null) {
            networkManager.close();
        }

        JOptionPane.showMessageDialog(
            this,
            message,
            "Game Over",
            JOptionPane.INFORMATION_MESSAGE
        );

        returnToApplicationMenu();
    }

    private void handleTimeout(boolean whiteFlagged) {
        boolean opponentIsWhite = !whiteFlagged;
        boolean opponentCanMate = board != null && board.hasSufficientMaterialToMate(opponentIsWhite);
        if (opponentCanMate) {
            endGameAndReturnToMenu(whiteFlagged ? "Time's up! Black wins!" : "Time's up! White wins!");
        } else {
            endGameAndReturnToMenu("Draw by timeout vs insufficient material.");
        }
    }

    private void showGamePopup(String title, String message) {
        JDialog dialog = new JDialog(dialogOwner(), title, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(new Color(48, 46, 43));
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(95, 95, 95), 1),
            BorderFactory.createEmptyBorder(18, 20, 16, 20)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel msgLabel = new JLabel("<html><div style='text-align:center; width:320px;'>" + message + "</div></html>", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        msgLabel.setForeground(new Color(225, 225, 225));

        JButton okBtn = new JButton("OK");
        okBtn.setFont(new Font("Arial", Font.BOLD, 15));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(new Color(118, 150, 86));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okBtn.setBorder(BorderFactory.createLineBorder(new Color(166, 193, 123), 2, true));
        okBtn.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(msgLabel, BorderLayout.CENTER);
        content.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        JRootPane dialogRoot = dialog.getRootPane();
        if (dialogRoot != null) {
            dialogRoot.setDefaultButton(okBtn);
            Action closeAction = new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    okBtn.doClick();
                }
            };
            InputMap rootWinMap = dialogRoot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap rootActionMap = dialogRoot.getActionMap();
            rootWinMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closePopup");
            rootActionMap.put("closePopup", closeAction);

            // Redundant binding on dialog content to avoid focus-path edge cases.
            InputMap contentFocusMap = content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap contentActionMap = content.getActionMap();
            contentFocusMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closePopup");
            contentActionMap.put("closePopup", closeAction);
        }
        dialog.pack();
        applyRoundedDialogShape(dialog, 22);
        dialog.setLocationRelativeTo(this);
        okBtn.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private boolean isThreeCheckPopup(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase();
        return "three-check".equals(normalized) || "three check".equals(normalized);
    }

    private boolean isKingCapturedPopup(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase();
        return "king captured".equals(normalized) || "king is captured".equals(normalized);
    }

    private boolean isKingOfTheHillPopup(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase();
        return "king of the hill".equals(normalized);
    }

    private String extractWinner(String message) {
        if (message == null) return "";
        String normalized = message.trim().toLowerCase();
        if (normalized.startsWith("white")) return "White";
        if (normalized.startsWith("black")) return "Black";
        return "";
    }

    private String normalizeThreeCheckMessage(String message) {
        String winner = extractWinner(message);
        if (!winner.isEmpty()) {
            return winner + " wins by giving 3 checks.";
        }
        return message;
    }

    private String normalizeKingHillMessage(String message) {
        String winner = extractWinner(message);
        if (!winner.isEmpty()) {
            return winner + " has won the race!";
        }
        return message;
    }

    private void showResultStyledPopup(String badgeText, String popupTitle, String message) {
        JDialog dialog = new JDialog(dialogOwner(), popupTitle, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(28, 32, 40),
                    0, getHeight(), new Color(18, 22, 29)
                ));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(120, 145, 92, 130));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel badge = new JLabel(badgeText, SwingConstants.CENTER);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(new Color(178, 190, 203));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(popupTitle, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 40));
        titleLabel.setForeground(new Color(244, 248, 255));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        top.add(badge);
        top.add(Box.createVerticalStrut(6));
        top.add(titleLabel);

        JLabel msgLabel = new JLabel(
            "<html><div style='text-align:center; width:460px; line-height:1.35;'>" + message + "</div></html>",
            SwingConstants.CENTER
        );
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 23));
        msgLabel.setForeground(new Color(220, 228, 237));

        JButton okBtn = new JButton("Continue");
        okBtn.setPreferredSize(new Dimension(150, 44));
        okBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(new Color(118, 150, 86));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(174, 205, 138), 1),
            BorderFactory.createEmptyBorder(7, 24, 7, 24)
        ));
        okBtn.addActionListener(e -> dialog.dispose());
        okBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                okBtn.setBackground(new Color(133, 167, 98));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                okBtn.setBackground(new Color(118, 150, 86));
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);

        content.add(top, BorderLayout.NORTH);
        content.add(msgLabel, BorderLayout.CENTER);
        content.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        applyRoundedDialogShape(dialog, 24);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showThreeCheckPopup(String message) {
        showResultStyledPopup("VARIANT RESULT", "Three-check", normalizeThreeCheckMessage(message));
    }

    private void showKingOfTheHillPopup(String message) {
        showResultStyledPopup("VARIANT RESULT", "King of the Hill", normalizeKingHillMessage(message));
    }

    private void showKingCapturedPopup(String message) {
        showResultStyledPopup("GAME RESULT", "King Captured", message);
    }

    public void showInGameInfoPopup(String title, String message) {
        if (isTerminalResultTitle(title)) {
            setGameOverState();
            recordTerminalResult(title, message);
            sendGameResultIfNeeded(title, message);
        }
        if (isThreeCheckPopup(title)) {
            showThreeCheckPopup(message);
        } else if (isKingOfTheHillPopup(title)) {
            showKingOfTheHillPopup(message);
        } else if (isKingCapturedPopup(title)) {
            showKingCapturedPopup(message);
        } else {
            showGamePopup(title, message);
        }
        if (isKingCapturedPopup(title) && onlineMode) {
            exitToMenu();
        }
    }

    private void sendGameResultIfNeeded(String title, String message) {
        if (!onlineMode || networkManager == null || applyingRemoteNetworkEvent) return;
        networkManager.sendGameResult(title, message);
    }

    private boolean isTerminalResultTitle(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase();
        return "draw".equals(normalized)
            || "king captured".equals(normalized)
            || "king is captured".equals(normalized)
            || "king of the hill".equals(normalized)
            || "three-check".equals(normalized)
            || "three check".equals(normalized)
            || "resign".equals(normalized)
            || "checkmate".equals(normalized)
            || "game over".equals(normalized);
    }

    public void recordBoardWin(boolean whiteWins) {
        recordWinner(whiteWins);
        sendGameResultIfNeeded(
                "Checkmate",
                whiteWins ? "White wins!" : "Black wins!");
    }

    private void recordTerminalResult(String title, String message) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase();
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase();
        if ("draw".equals(normalizedTitle) || normalizedMessage.contains("draw")) {
            recordRatingResult("draw");
            return;
        }
        String winner = extractWinner(message);
        if ("White".equals(winner)) {
            recordWinner(true);
        } else if ("Black".equals(winner)) {
            recordWinner(false);
        }
    }

    private void recordWinner(boolean whiteWins) {
        boolean localPlayerWhite = onlineMode ? localIsWhite : playerIsWhite;
        recordRatingResult(whiteWins == localPlayerWhite ? "win" : "loss");
    }

    private void recordRatingResult(String result) {
        if (ratingResultRecorded || result == null || result.isBlank()) return;
        if (spectateMode || (!isBotGame && !onlineMode)) return;
        AccountApiClient.Session session = AccountSession.get();
        if (session == null) return;
        ratingResultRecorded = true;

        SwingWorker<AccountApiClient.RatingUpdate, Void> worker = new SwingWorker<>() {
            @Override
            protected AccountApiClient.RatingUpdate doInBackground() throws Exception {
                return accountApi.recordGameResult(session.token(), result);
            }

            @Override
            protected void done() {
                try {
                    AccountApiClient.RatingUpdate update = get();
                    AccountSession.updateProfile(update.profile());
                    System.out.println("ELO updated: "
                            + update.previousRating()
                            + " -> "
                            + update.newRating()
                            + " ("
                            + signedDelta(update.ratingDelta())
                            + "), "
                            + update.newRank());
                } catch (Exception exception) {
                    ratingResultRecorded = false;
                    System.err.println("Could not update ELO: " + exception.getMessage());
                }
            }
        };
        worker.execute();
    }

    private static String signedDelta(int delta) {
        return delta > 0 ? "+" + delta : String.valueOf(delta);
    }

    private boolean showGameConfirm(String title, String message, String yesText, String noText) {
        final boolean[] accepted = {false};

        JDialog dialog = new JDialog(dialogOwner(), title, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(38, 42, 49), 0, getHeight(), new Color(25, 28, 34)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(112, 127, 150, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 28, 20, 28));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 46));
        titleLabel.setForeground(new Color(245, 248, 252));

        JLabel msgLabel = new JLabel("<html><div style='text-align:center; width:520px; line-height:1.35;'>" + message + "</div></html>", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        msgLabel.setForeground(new Color(220, 228, 237));

        JButton yesBtn = new JButton(yesText);
        yesBtn.setPreferredSize(new Dimension(132, 44));
        yesBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        yesBtn.setForeground(Color.WHITE);
        yesBtn.setBackground(new Color(118, 150, 86));
        yesBtn.setFocusPainted(false);
        yesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        yesBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(174, 205, 138), 1),
            BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        yesBtn.addActionListener(e -> {
            accepted[0] = true;
            dialog.dispose();
        });

        JButton noBtn = new JButton(noText);
        noBtn.setPreferredSize(new Dimension(132, 44));
        noBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        noBtn.setForeground(Color.WHITE);
        noBtn.setBackground(new Color(158, 74, 74));
        noBtn.setFocusPainted(false);
        noBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        noBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(208, 128, 128), 1),
            BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        noBtn.addActionListener(e -> dialog.dispose());

        yesBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                yesBtn.setBackground(new Color(133, 167, 98));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                yesBtn.setBackground(new Color(118, 150, 86));
            }
        });
        noBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                noBtn.setBackground(new Color(176, 88, 88));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                noBtn.setBackground(new Color(158, 74, 74));
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(yesBtn);
        btnPanel.add(noBtn);

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(msgLabel, BorderLayout.CENTER);
        content.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        applyRoundedDialogShape(dialog, 22);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return accepted[0];
    }

    private void applyRoundedDialogShape(JDialog dialog, int arc) {
        try {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc));
        } catch (UnsupportedOperationException ignored) {
            // Fall back to painted rounded panel on platforms without shaped-window support.
        }
    }

    private JPanel createPlayerCard(JLabel nameLabel, CapturedMaterialStrip capturedStrip) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);

        row.add(new PlayerAvatarCard());

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.add(nameLabel);
        textStack.add(capturedStrip);
        row.add(textStack);
        return row;
    }

    private void styleClockLabel(JLabel label) {
        if (label == null) return;
        label.setOpaque(false);
        Dimension clockSize = new Dimension(156, 62);
        label.setMinimumSize(clockSize);
        label.setPreferredSize(clockSize);
        label.setMaximumSize(clockSize);
        label.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
    }

    private JPanel createClockHost(JLabel clockLabel) {
        JPanel host = new JPanel(new GridBagLayout());
        host.setOpaque(false);
        host.add(clockLabel);
        return host;
    }
        public ChessGame(int initialTime, int increment, ChessBot bot) {
            this(initialTime, increment, bot, false);
        }

        public ChessGame(MainMenu host, int initialTime, int increment, ChessBot bot) {
            this(host, initialTime, increment, bot, bot, false);
        }

        public ChessGame(
                MainMenu host,
                int initialTime,
                int increment,
                ChessBot whiteBot,
                ChessBot blackBot,
                boolean spectate) {
            this.appHost = host;
            initializeBotGame(initialTime, increment, whiteBot, blackBot, spectate);
        }

        public ChessGame(int initialTime, int increment, ChessBot bot, boolean spectate) {
            this(initialTime, increment, bot, bot, spectate);
        }

        public ChessGame(int initialTime, int increment, ChessBot whiteBot, ChessBot blackBot, boolean spectate) {
            initializeBotGame(initialTime, increment, whiteBot, blackBot, spectate);
        }

        private void initializeBotGame(
                int initialTime,
                int increment,
                ChessBot whiteBot,
                ChessBot blackBot,
                boolean spectate) {
            this.whiteTimeRemaining = initialTime;
            this.blackTimeRemaining = initialTime;
            this.incrementSeconds = increment;

            this.isBotGame = true;
            this.spectateMode = spectate;
            this.playerIsWhite = true;
            this.spectateWhiteBot = (whiteBot == null) ? ChessBot.CASUAL_CARL : whiteBot;
            this.spectateBlackBot = (blackBot == null) ? this.spectateWhiteBot : blackBot;
            this.selectedBot = this.spectateWhiteBot;

            if (this.selectedBot != null) {
                this.botDepth = this.selectedBot.getDepth();
                this.botSkillLevel = this.selectedBot.getSkillLevel();
            }

            uciMoves.setLength(0);

            try {
                stockfishEngine = new StockfishEngine();
                stockfishEngine.setSkillLevel(botSkillLevel);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialogOwner(),
                    "Failed to start Stockfish\n" + e.getMessage(),
                    "Engine Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }

            initializeGame();
        }

        // Constructor with time control parameters
        public ChessGame(int initialTime, int increment) {
            this(initialTime, increment, "Classic");
        }

        public ChessGame(int initialTime, int increment, String variant) {
            this(null, initialTime, increment, variant);
        }

        public ChessGame(MainMenu host, int initialTime, int increment, String variant) {
            this.appHost = host;
            this.whiteTimeRemaining = initialTime;
            this.blackTimeRemaining = initialTime;
            this.incrementSeconds = increment;
            this.gameVariant = (variant == null || variant.trim().isEmpty()) ? "Classic" : variant;
            
            initializeGame();
        }

        // Online constructor
        public ChessGame(int initialTime, int increment, NetworkManager net, boolean localIsWhite) {
            this(initialTime, increment, net, localIsWhite, "Classic");
        }

        public ChessGame(int initialTime, int increment, NetworkManager net, boolean localIsWhite, String variant) {
            this(null, initialTime, increment, net, localIsWhite, variant);
        }

        public ChessGame(
                MainMenu host,
                int initialTime,
                int increment,
                NetworkManager net,
                boolean localIsWhite,
                String variant) {
            this.appHost = host;
            this.whiteTimeRemaining = initialTime;
            this.blackTimeRemaining = initialTime;
            this.incrementSeconds = increment;
            this.networkManager = net;
            this.onlineMode = (net != null);
            this.localIsWhite = localIsWhite;
            this.gameVariant = (variant == null || variant.trim().isEmpty()) ? "Classic" : variant;

            if (this.networkManager != null) {
                    this.networkManager.setListener(new NetworkManager.Listener() {
                    @Override public void onConnected(boolean amWhite) {}
                    @Override public void onMoveReceived(int fr, int fc, int tr, int tc, String promo) {
                        SwingUtilities.invokeLater(() -> onRemoteMove(fr, fc, tr, tc, promo));
                    }
                    @Override public void onSpellCastReceived(String spellId, boolean casterWhite, SpellTarget target) {
                        SwingUtilities.invokeLater(() -> onRemoteSpellCast(spellId, casterWhite, target));
                    }
                    @Override public void onSpellPhaseReceived(String phaseId, boolean casterWhite, int row, int col) {
                        SwingUtilities.invokeLater(() -> onRemoteSpellPhase(phaseId, casterWhite, row, col));
                    }
                    @Override public void onGameResultReceived(String title, String message) {
                        SwingUtilities.invokeLater(() -> onRemoteGameResult(title, message));
                    }
                    @Override public void onError(String msg) {
                        SwingUtilities.invokeLater(() -> handleNetworkDisconnect(msg));
                    }
                    @Override public void onDrawOffered() { SwingUtilities.invokeLater(() -> onRemoteOfferDraw()); }
                    @Override public void onDrawAccepted() { SwingUtilities.invokeLater(() -> onRemoteDrawAccepted()); }
                    @Override public void onDrawDeclined() { SwingUtilities.invokeLater(() -> onRemoteDrawDeclined()); }
                    @Override public void onResign() { SwingUtilities.invokeLater(() -> onRemoteResign()); }
                });
            }

            initializeGame();
            // Update board with online flags
            if (board != null) {
                board.setOnlineMode(onlineMode, localIsWhite);
                board.setVariantMode(resolveVariantMode());
                board.setFogOfWarEnabled("Fog of War".equalsIgnoreCase(gameVariant));
                // If the local player is black, flip the board so their pieces are at bottom
                if (!localIsWhite) {
                    board.flipBoard();
                }
            }
            // Update player labels to indicate local/remote
            if (localIsWhite) {
                bottomPlayerLabel.setText("White");
                topPlayerLabel.setText("Black");
            } else {
                // Board has been flipped for local black; bottom shows the local player
                bottomPlayerLabel.setText("Black");
                topPlayerLabel.setText("White");
            }
        }
        
        // Bot constructor
        

        
        // Default constructor (for backward compatibility)
        public ChessGame() {
            this(600, 0); // Default: 10 minutes, no increment
        }

        private VariantMode resolveVariantMode() {
            if ("Spell Chess".equalsIgnoreCase(gameVariant)) return VariantMode.SPELL_CHESS;
            if ("Atomic".equalsIgnoreCase(gameVariant) || "Atomic Chess".equalsIgnoreCase(gameVariant)) return VariantMode.ATOMIC;
            if ("Three-check".equalsIgnoreCase(gameVariant) || "Three check".equalsIgnoreCase(gameVariant)) return VariantMode.THREE_CHECK;
            if ("King of the Hill".equalsIgnoreCase(gameVariant) || "King of the hill".equalsIgnoreCase(gameVariant)) return VariantMode.KING_OF_THE_HILL;
            if ("Duck Chess".equalsIgnoreCase(gameVariant) || "Duck".equalsIgnoreCase(gameVariant)) return VariantMode.DUCK_CHESS;
            return VariantMode.NORMAL;
        }
        
        private void initializeGame() {
            setTitle("Java Chess - Game");
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setResizable(true);
            
            // FIXED: Stop timer when window closes to prevent ghost dialogs
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    showExitConfirmationDialog();
                }
            });
            installAltF4Hotkey();

            // Initialize opening detector
            openingDetector = new OpeningDetector();

            // Main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.setBackground(new Color(64, 64, 64));
            mainPanel.setPreferredSize(new Dimension(BASE_LAYOUT_WIDTH, BASE_GAME_HEIGHT));
            
            // LEFT SIDE - Game area
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BorderLayout());
            leftPanel.setPreferredSize(new Dimension(BASE_LEFT_WIDTH, BASE_GAME_HEIGHT));
            leftPanel.setBackground(Color.BLACK);
            this.leftGamePanel = leftPanel;
            
            // Top panel for player info and clock (Player 2 - Black)
            topPanel = new PlayerInfoBarPanel();
            topPanel.setPreferredSize(new Dimension(BASE_LEFT_WIDTH, BASE_PLAYER_BAR_HEIGHT));
            topPanel.setLayout(new BorderLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 10));

            topPlayerLabel = new JLabel("Black");
            topPlayerLabel.setFont(new Font("Arial", Font.BOLD, 22));
            topPlayerLabel.setForeground(new Color(236, 236, 236));
            topPlayerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            topPlayerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            topCapturedStrip = new CapturedMaterialStrip();
            topCapturedStrip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            topCapturedStrip.setAlignmentX(Component.LEFT_ALIGNMENT);

            topClockLabel = new RoundedClockLabel(formatTime(blackTimeRemaining));
            topClockLabel.setFont(new Font("Consolas", Font.BOLD, 38));
            topClockLabel.setForeground(new Color(250, 252, 255));
            topClockLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
            topClockLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            styleClockLabel(topClockLabel);

            JPanel topLeftInfo = createPlayerCard(topPlayerLabel, topCapturedStrip);

            topPanel.add(topLeftInfo, BorderLayout.WEST);
            topPanel.add(createClockHost(topClockLabel), BorderLayout.EAST);
            
            // Board panel
            board = new Board(this);
            board.setBotMode(isBotGame, playerIsWhite);
            board.setVariantMode(resolveVariantMode());
            board.setFogOfWarEnabled("Fog of War".equalsIgnoreCase(gameVariant));

            // In local games, we wait for the user to choose a time control and press Start Game
            // In bot games, the board is immediately playable
            if (!onlineMode && !isBotGame) {
                board.setInputEnabled(false);
            } else {
                gameStarted = true;
                board.setInputEnabled(!spectateMode);
                if (shouldShowMoveHistory()) {
                    showMoveHistoryPanel();
                }
            }

            // Bottom panel for player info and clock (Player 1 - White)
            bottomPanel = new PlayerInfoBarPanel();
            bottomPanel.setPreferredSize(new Dimension(BASE_LEFT_WIDTH, BASE_PLAYER_BAR_HEIGHT));
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 10));

            bottomPlayerLabel = new JLabel("White");
            bottomPlayerLabel.setFont(new Font("Arial", Font.BOLD, 22));
            bottomPlayerLabel.setForeground(new Color(236, 236, 236));
            bottomPlayerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            bottomPlayerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bottomCapturedStrip = new CapturedMaterialStrip();
            bottomCapturedStrip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            bottomCapturedStrip.setAlignmentX(Component.LEFT_ALIGNMENT);

            bottomClockLabel = new RoundedClockLabel(formatTime(whiteTimeRemaining));
            bottomClockLabel.setFont(new Font("Consolas", Font.BOLD, 38));
            bottomClockLabel.setForeground(new Color(250, 252, 255));
            bottomClockLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
            bottomClockLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            styleClockLabel(bottomClockLabel);

            JPanel bottomLeftInfo = createPlayerCard(bottomPlayerLabel, bottomCapturedStrip);

            bottomPanel.add(bottomLeftInfo, BorderLayout.WEST);
            bottomPanel.add(createClockHost(bottomClockLabel), BorderLayout.EAST);
            
            // Keep the game stack compact. BorderLayout.CENTER stretches on tall
            // Linux/HiDPI windows, which leaves a large empty board area.
            boardStackPanel = new JPanel();
            boardStackPanel.setOpaque(false);
            boardStackPanel.setLayout(new BoxLayout(boardStackPanel, BoxLayout.Y_AXIS));
            topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            board.setAlignmentX(Component.LEFT_ALIGNMENT);
            bottomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            boardStackPanel.add(topPanel);
            boardStackPanel.add(board);
            boardStackPanel.add(bottomPanel);

            evalBarLanePanel = new JPanel();
            evalBarLanePanel.setBackground(new Color(20, 20, 20));

            evalBarColumnPanel = new JPanel();
            evalBarColumnPanel.setBackground(Color.BLACK);
            evalBarColumnPanel.setLayout(new BorderLayout());
            evalBarColumnPanel.add(evalBarLanePanel, BorderLayout.CENTER);

            leftContentPanel = new JPanel();
            leftContentPanel.setOpaque(false);
            leftContentPanel.setLayout(new BoxLayout(leftContentPanel, BoxLayout.X_AXIS));
            leftContentPanel.add(boardStackPanel);
            leftContentPanel.add(evalBarColumnPanel);

            BufferedImage doodleUnderlay = loadDoodleUnderlay();
            JPanel boardStackHost = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (doodleUnderlay == null) return;

                    int tileW = doodleUnderlay.getWidth();
                    int tileH = doodleUnderlay.getHeight();
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    for (int y = 0; y < getHeight(); y += tileH) {
                        for (int x = 0; x < getWidth(); x += tileW) {
                            g2.drawImage(doodleUnderlay, x, y, null);
                        }
                    }
                    g2.dispose();
                }
            };
            boardStackHost.setBackground(Color.BLACK);
            GridBagConstraints boardStackGbc = new GridBagConstraints();
            boardStackGbc.gridx = 0;
            boardStackGbc.gridy = 0;
            boardStackGbc.anchor = GridBagConstraints.WEST;
            boardStackGbc.weightx = 1.0;
            boardStackGbc.weighty = 1.0;
            boardStackHost.add(leftContentPanel, boardStackGbc);
            leftPanel.add(boardStackHost, BorderLayout.CENTER);
            
            // CENTER - Time control dropdown (Chess.com style) + Start button
    JPanel centerPanel = new JPanel();

    centerPanel.setBackground(new Color(58, 58, 58));
    centerPanel.setPreferredSize(new Dimension(BASE_CENTER_WIDTH, BASE_GAME_HEIGHT));
            centerPanel.setLayout(new GridBagLayout());
            centerPanel.setBorder(BorderFactory.createEmptyBorder(18, 13, 18, 13));
    this.centerColumnPanel = centerPanel;

    GridBagConstraints centerGbc = new GridBagConstraints();
    centerGbc.gridx = 0;
    centerGbc.anchor = GridBagConstraints.NORTH;
    centerGbc.insets = new Insets(20, 20, 10, 20); // Top margin increased for positioning at top

	    timeSelectBtn = createTimeSelectButton();
	    timeDetailsPanel = createTimeDetailsPanel();
	    timeDetailsExpandedHeight = timeDetailsPanel.getPreferredSize().height;
	    timeDetailsCurrentHeight = timeDetailsExpanded ? timeDetailsExpandedHeight : 0;
	    timeDetailsHostPanel = new JPanel(new BorderLayout());
	    timeDetailsHostPanel.setOpaque(false);
	    timeDetailsHostPanel.add(timeDetailsPanel, BorderLayout.NORTH);
	    timeDetailsHostPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    timeDetailsHostPanel.setPreferredSize(new Dimension(TIME_CONTROL_WIDTH, timeDetailsCurrentHeight));
	    timeDetailsHostPanel.setMinimumSize(new Dimension(0, 0));
	    timeDetailsHostPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeDetailsExpandedHeight));
	    startGameBtn = createStartGameButton();
	    timeControlPanel = new JPanel();
	    timeControlPanel.setOpaque(false);
	    timeControlPanel.setLayout(new GridBagLayout());
	    timeControlPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
	    timeControlPanel.setMinimumSize(new Dimension(
	        MIN_TIME_CONTROL_WIDTH,
	        COLLAPSED_TIME_CONTROL_HEIGHT
	    ));
	    timeControlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

	    GridBagConstraints timeGbc = new GridBagConstraints();
	    timeGbc.gridx = 0;
	    timeGbc.weightx = 1.0;
	    timeGbc.fill = GridBagConstraints.HORIZONTAL;
	    timeGbc.anchor = GridBagConstraints.NORTH;
	    timeGbc.gridy = 0;
	    timeControlPanel.add(timeSelectBtn, timeGbc);
	    timeGbc.gridy = 1;
	    timeControlPanel.add(timeDetailsHostPanel, timeGbc);
	    timeGbc.gridy = 2;
	    timeGbc.insets = new Insets(14, 0, 0, 0);
	    timeControlPanel.add(startGameBtn, timeGbc);

    // For online games and bot games, we don't want local time-selection UI
	    if (onlineMode || isBotGame) {
	        timeSelectBtn.setEnabled(false);
	        if (timeDetailsPanel != null) timeDetailsPanel.setEnabled(false);
	        startGameBtn.setEnabled(false);
	        if (isBotGame) {
	            timeSelectBtn.setVisible(false);
	            if (timeDetailsHostPanel != null) timeDetailsHostPanel.setVisible(false);
	            startGameBtn.setVisible(false);
	        }
	    }

    centerGbc.gridy = 0;
    centerGbc.weightx = 1.0;
    centerGbc.fill = GridBagConstraints.HORIZONTAL;
    centerGbc.insets = new Insets(0, 6, 8, 6);

    boolean showTopTimeControls = !onlineMode;
    if (showTopTimeControls) {
        centerPanel.add(timeControlPanel, centerGbc);
    }

    settingsPanel = new SettingsPanel(board);
    boolean spellChess = "Spell Chess".equalsIgnoreCase(gameVariant);
    int nextRow = showTopTimeControls ? 1 : 0;
    int fillerRow = nextRow;
    if (spellChess) {
        centerGbc.fill = GridBagConstraints.HORIZONTAL;
        centerGbc.weightx = 1.0;
        centerGbc.gridy = nextRow;
        centerGbc.insets = new Insets(6, 6, 10, 6);
        blackSpellPanel = createSpellPanel(false);
        centerPanel.add(blackSpellPanel, centerGbc);
        fillerRow = nextRow + 1;
    }

    // â”€â”€ Filler row absorbs spare space, pinning buttons to the bottom â”€â”€â”€â”€â”€
    centerGbc.gridy = fillerRow;
    centerGbc.weighty = 1.0;
    centerGbc.fill = GridBagConstraints.VERTICAL;
    centerSpellFiller = Box.createVerticalGlue();
    centerPanel.add(centerSpellFiller, centerGbc);
    centerGbc.weighty = 0;
    centerGbc.fill = GridBagConstraints.NONE;

    // â”€â”€ Bottom action buttons: Reset Board + Exit to Menu â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    JPanel bottomBtnPanel = new JPanel(new GridLayout(onlineMode ? 3 : 2, 1, 0, 10));
    bottomBtnPanel.setOpaque(false);

    JButton resetBtn = createGlossyActionButton(
        "Reset Board",
        new Color(103, 145, 216),
        new Color(69, 106, 176),
        new Color(124, 168, 236),
        new Color(82, 122, 194),
        new Color(155, 196, 255),
        new Color(45, 74, 126)
    );
    resetBtn.addActionListener(e -> resetBoard());
    this.resetBoardBtn = resetBtn;

    JButton exitMenuBtn = createGlossyActionButton(
        "Exit to Menu",
        new Color(214, 88, 88),
        new Color(160, 54, 54),
        new Color(232, 108, 108),
        new Color(178, 66, 66),
        new Color(255, 152, 152),
        new Color(104, 30, 30)
    );
    exitMenuBtn.addActionListener(e -> {
        boolean confirm = showGameConfirm("Exit Game", "Exit to main menu?", "Yes", "No");
        if (confirm) exitToMenu();
    });
    this.exitToMenuBtn = exitMenuBtn;

    JButton drawBtn = new JButton("Offer Draw");
    drawBtn.setPreferredSize(new Dimension(360, 48));
    drawBtn.setMaximumSize(new Dimension(360, 48));
    drawBtn.setFont(new Font("Arial", Font.BOLD, 18));
    drawBtn.setForeground(Color.WHITE);
    drawBtn.setBackground(new Color(80, 205, 70));
    drawBtn.setFocusPainted(false);
    drawBtn.setBorder(BorderFactory.createLineBorder(new Color(120, 235, 110), 2));
    drawBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    drawBtn.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent e) { drawBtn.setBackground(new Color(98, 225, 88)); }
        public void mouseExited (java.awt.event.MouseEvent e) { drawBtn.setBackground(new Color(80, 205, 70)); }
    });
    drawBtn.addActionListener(e -> onOfferDraw());

    JButton resignBtn = new JButton("Resign");
    resignBtn.setPreferredSize(new Dimension(360, 48));
    resignBtn.setMaximumSize(new Dimension(360, 48));
    resignBtn.setFont(new Font("Arial", Font.BOLD, 18));
    resignBtn.setForeground(Color.WHITE);
    resignBtn.setBackground(new Color(66, 120, 205));
    resignBtn.setFocusPainted(false);
    resignBtn.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 232), 2));
    resignBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    resignBtn.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent e) { resignBtn.setBackground(new Color(86, 140, 225)); }
        public void mouseExited (java.awt.event.MouseEvent e) { resignBtn.setBackground(new Color(66, 120, 205)); }
    });
    resignBtn.addActionListener(e -> onLocalResignRequest());

    if (!onlineMode) {
        bottomBtnPanel.add(resetBtn);
    } else {
        bottomBtnPanel.add(drawBtn);
        bottomBtnPanel.add(resignBtn);
    }
    bottomBtnPanel.add(exitMenuBtn);

    if (spellChess) {
        centerGbc.gridy = fillerRow + 1;
        centerGbc.anchor = GridBagConstraints.SOUTH;
        centerGbc.fill = GridBagConstraints.HORIZONTAL;
        centerGbc.weightx = 1.0;
        centerGbc.insets = new Insets(6, 6, 8, 6);
        whiteSpellPanel = createSpellPanel(true);
        centerPanel.add(whiteSpellPanel, centerGbc);
        centerGbc.anchor = GridBagConstraints.NORTH;
        centerGbc.fill = GridBagConstraints.NONE;
        centerGbc.weightx = 0;
    }

    if (spellChess) {
        if (!onlineMode && !isBotGame) {
            applySpellChessPreStartLayout();
        } else {
            setSpellPanelsVisible(true);
        }
    }

            moveHistoryPanel = null;
            openingNamePanel = null;

            JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
            rightPanel.setBackground(new Color(64, 64, 64));
            rightPanel.setPreferredSize(new Dimension(BASE_RIGHT_WIDTH, BASE_GAME_HEIGHT));
            rightPanel.setBorder(BorderFactory.createEmptyBorder(18, 13, 18, 12));
            rightPanel.add(settingsPanel, BorderLayout.NORTH);
            rightPanel.add(bottomBtnPanel, BorderLayout.SOUTH);
            rightPanel.setComponentZOrder(settingsPanel, 0);

            centerPanel.setMinimumSize(new Dimension(MIN_CENTER_WIDTH, 0));
            rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));

            centerRightSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                centerPanel,
                rightPanel
            );
            centerRightSplitPane.setBorder(BorderFactory.createEmptyBorder());
            centerRightSplitPane.setContinuousLayout(true);
            centerRightSplitPane.setOneTouchExpandable(false);
            centerRightSplitPane.setDividerSize(SPLIT_DIVIDER_WIDTH);
            centerRightSplitPane.setResizeWeight(
                BASE_CENTER_WIDTH / (double) (BASE_CENTER_WIDTH + BASE_RIGHT_WIDTH)
            );
            centerRightSplitPane.setBackground(new Color(47, 47, 47));
            centerRightSplitPane.setUI(createSinglePixelDividerUI(centerPanel, rightPanel, null));
            installResponsiveControlScaling(centerPanel, rightPanel);

            leftPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));
            centerRightSplitPane.setMinimumSize(new Dimension(
                MIN_CENTER_WIDTH + SPLIT_DIVIDER_WIDTH + MIN_RIGHT_WIDTH,
                0
            ));
            leftMainSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                centerRightSplitPane
            );
            leftMainSplitPane.setBorder(BorderFactory.createEmptyBorder());
            leftMainSplitPane.setContinuousLayout(true);
            leftMainSplitPane.setOneTouchExpandable(false);
            leftMainSplitPane.setDividerSize(SPLIT_DIVIDER_WIDTH);
            leftMainSplitPane.setResizeWeight(
                BASE_LEFT_WIDTH / (double) BASE_LAYOUT_WIDTH
            );
            leftMainSplitPane.setBackground(new Color(47, 47, 47));
            leftMainSplitPane.setUI(createSinglePixelDividerUI(
                evalBarLanePanel,
                centerPanel,
                () -> getMaximumLeftDividerLocation(leftPanel)
            ));
            installLeftPanelScaling(leftPanel);

            // The outer divider sits on the original right edge of the board/player area.
            mainPanel.add(leftMainSplitPane, BorderLayout.CENTER);
            
            JPanel windowPanel = new JPanel(new GridBagLayout());
            windowPanel.setBackground(new Color(64, 64, 64));
            windowPanel.putClientProperty(
                    ProportionalUiScaler.SKIP_SUBTREE_PROPERTY,
                    Boolean.TRUE);
            GridBagConstraints windowGbc = new GridBagConstraints();
            windowGbc.gridx = 0;
            windowGbc.gridy = 0;
            windowGbc.weightx = 1.0;
            windowGbc.weighty = 1.0;
            windowGbc.anchor = GridBagConstraints.CENTER;
            windowPanel.add(mainPanel, windowGbc);
            embeddedHotkeyTarget = windowPanel;
            if (appHost == null) {
                setContentPane(windowPanel);
            }
            installFitToScreenLayout(
                windowPanel,
                mainPanel,
                leftPanel,
                centerPanel,
                rightPanel,
                centerRightSplitPane,
                leftMainSplitPane
            );
            SwingUtilities.invokeLater(() -> centerRightSplitPane.setDividerLocation(
                BASE_CENTER_WIDTH / (double) (BASE_CENTER_WIDTH + BASE_RIGHT_WIDTH)
            ));
            bindGameHotkeys();
            if (appHost != null) installAltF4Hotkey();
            syncBoardEnterHotkeyMode();
            // In online mode we start immediately; in bot mode we start immediately; in local mode we start after pressing Start Game.
            if ((onlineMode || isBotGame) && whiteTimeRemaining > 0 && blackTimeRemaining > 0) {
                startTimer();
            }
            if (appHost == null) {
                setUndecorated(true);
                pack();
                applyMinimumContentSize(BASE_GAME_WIDTH, BASE_GAME_HEIGHT);
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                setResizable(false);
                setVisible(true);
            } else {
                appHost.showEmbeddedScreen(windowPanel, this::cleanupEmbeddedGame);
                appHost.finishGameLaunchOverlay(windowPanel);
            }
            SwingUtilities.invokeLater(() -> setLeftDividerToMaximum(leftPanel));
            setupPlayerLabels();
            refreshSpellUI();
            refreshCapturedMaterialUI();
            if (isBotGame && spectateMode) {
                SwingUtilities.invokeLater(this::makeBotMove);
            }
	    }
	    
        private void installAltF4Hotkey() {
            JComponent target = appHost == null ? getRootPane() : embeddedHotkeyTarget;
            if (target == null) return;
            InputMap im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = target.getActionMap();
            im.put(KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_F4,
                java.awt.event.InputEvent.ALT_DOWN_MASK
            ), "altF4CloseGame");
            am.put("altF4CloseGame", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    exitApplication();
                }
            });
        }
	    
	    
	        private void installFitToScreenLayout(
            JPanel viewport,
            JPanel mainPanel,
            JPanel leftPanel,
            JPanel centerPanel,
            JPanel rightPanel,
            JSplitPane centerRightSplitPane,
            JSplitPane leftMainSplitPane
        ) {
            java.awt.event.ComponentAdapter adapter = new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    applyFitToScreenScale(
                        viewport,
                        mainPanel,
                        leftPanel,
                        centerPanel,
                        rightPanel,
                        centerRightSplitPane,
                        leftMainSplitPane
                    );
                }
            };
            viewport.addComponentListener(adapter);
            SwingUtilities.invokeLater(() -> applyFitToScreenScale(
                viewport,
                mainPanel,
                leftPanel,
                centerPanel,
                rightPanel,
                centerRightSplitPane,
                leftMainSplitPane
            ));
        }

        private void applyFitToScreenScale(
            JPanel viewport,
            JPanel mainPanel,
            JPanel leftPanel,
            JPanel centerPanel,
            JPanel rightPanel,
            JSplitPane centerRightSplitPane,
            JSplitPane leftMainSplitPane
        ) {
            int availableW = Math.max(1, viewport.getWidth());
            int availableH = Math.max(1, viewport.getHeight());
            double scale = Math.min(availableW / (double) BASE_LAYOUT_WIDTH, availableH / (double) BASE_GAME_HEIGHT);
            if (!Double.isFinite(scale) || scale <= 0) scale = 1.0;

            // Fill the host viewport. The board column keeps its own aspect ratio,
            // while the center and settings columns absorb the remaining width.
            int leftHeight = availableH;
            int mainWidth = availableW;

            setFixedSize(mainPanel, mainWidth, leftHeight);
            setFixedSize(leftMainSplitPane, mainWidth, leftHeight);
            int minimumCenterWidth = scaled(MIN_CENTER_WIDTH, scale);
            int minimumRightWidth = scaled(MIN_RIGHT_WIDTH, scale);
            setFlexibleSplitChildSize(leftPanel, scaled(MIN_LEFT_WIDTH, scale), leftHeight);
            setFlexibleSplitChildSize(
                centerRightSplitPane,
                minimumCenterWidth + SPLIT_DIVIDER_WIDTH + minimumRightWidth,
                leftHeight
            );
            setFlexibleSplitChildSize(centerPanel, minimumCenterWidth, leftHeight);
            setFlexibleSplitChildSize(rightPanel, minimumRightWidth, leftHeight);

            if (leftMainSplitPane.getDividerLocation() <= 0) {
                leftMainSplitPane.setDividerLocation(scaled(BASE_LEFT_WIDTH, scale));
            }
            int leftMaximum = getMaximumLeftDividerLocation(leftPanel);
            if (leftMaximum > 0 && leftMainSplitPane.getDividerLocation() > leftMaximum) {
                leftMainSplitPane.setDividerLocation(leftMaximum);
            }

            int centerLocation = centerRightSplitPane.getDividerLocation();
            int centerMinimum = centerRightSplitPane.getMinimumDividerLocation();
            int centerMaximum = centerRightSplitPane.getMaximumDividerLocation();
            if (centerLocation > 0) {
                centerRightSplitPane.setDividerLocation(
                    Math.max(centerMinimum, Math.min(centerMaximum, centerLocation))
                );
            }
            applyLeftPanelScale(leftPanel);

            viewport.revalidate();
            viewport.repaint();
        }

        private BasicSplitPaneUI createSinglePixelDividerUI(
            Component leftPanel,
            Component rightPanel,
            java.util.function.IntSupplier maximumLocation
        ) {
            return new BasicSplitPaneUI() {
                private int clampDividerLocation(int location) {
                    if (splitPane == null) return location;
                    int minimum = splitPane.getMinimumDividerLocation();
                    int maximum = splitPane.getMaximumDividerLocation();
                    if (maximumLocation != null) {
                        int customMaximum = maximumLocation.getAsInt();
                        if (customMaximum > 0) maximum = Math.min(maximum, customMaximum);
                    }
                    return Math.max(minimum, Math.min(maximum, location));
                }

                @Override
                protected void dragDividerTo(int location) {
                    super.dragDividerTo(clampDividerLocation(location));
                }

                @Override
                protected void finishDraggingTo(int location) {
                    super.finishDraggingTo(clampDividerLocation(location));
                }

                @Override
                public BasicSplitPaneDivider createDefaultDivider() {
                    return new BasicSplitPaneDivider(this) {
                        {
                            setBorder(BorderFactory.createEmptyBorder());
                            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                        }

                        @Override
                        public void paint(Graphics g) {
                            int midX = getWidth() / 2;
                            g.setColor(leftPanel.getBackground());
                            g.fillRect(0, 0, midX, getHeight());
                            g.setColor(rightPanel.getBackground());
                            g.fillRect(midX + 1, 0, getWidth() - midX - 1, getHeight());
                            g.setColor(new Color(80, 80, 80));
                            g.drawLine(midX, 0, midX, getHeight());
                        }
                    };
                }
            };
        }

        private void installLeftPanelScaling(JPanel leftPanel) {
            captureLeftPanelFonts(leftPanel);
            leftPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    applyLeftPanelScale(leftPanel);
                }
            });
            leftMainSplitPane.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                e -> applyLeftPanelScale(leftPanel)
            );
        }

        private int getMaximumLeftPanelWidth(JPanel leftPanel) {
            if (leftPanel == null || leftPanel.getHeight() <= 0) return 0;
            return (int) Math.floor(
                leftPanel.getHeight() * (BASE_LEFT_WIDTH / (double) BASE_GAME_HEIGHT)
            );
        }

        private int getMaximumLeftDividerLocation(JPanel leftPanel) {
            if (leftMainSplitPane == null) return getMaximumLeftPanelWidth(leftPanel);
            int aspectMaximum = getMaximumLeftPanelWidth(leftPanel);
            int reservedRightWidth = centerRightSplitPane == null
                ? MIN_CENTER_WIDTH + SPLIT_DIVIDER_WIDTH + MIN_RIGHT_WIDTH
                : centerRightSplitPane.getMinimumSize().width;
            int spaceMaximum = leftMainSplitPane.getWidth()
                - leftMainSplitPane.getDividerSize()
                - reservedRightWidth;
            if (aspectMaximum <= 0) return spaceMaximum;
            return Math.max(leftMainSplitPane.getMinimumDividerLocation(), Math.min(aspectMaximum, spaceMaximum));
        }

        private void setLeftDividerToMaximum(JPanel leftPanel) {
            int maximumLeftWidth = getMaximumLeftDividerLocation(leftPanel);
            if (maximumLeftWidth > 0 && leftMainSplitPane != null) {
                leftMainSplitPane.setDividerLocation(maximumLeftWidth);
                applyLeftPanelScale(leftPanel);
            }
        }

        private void captureLeftPanelFonts(Component component) {
            if (component == null) return;
            if (component.getFont() != null) {
                leftPanelBaseFonts.putIfAbsent(component, component.getFont());
            }
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    captureLeftPanelFonts(child);
                }
            }
        }

        private void applyLeftPanelScale(JPanel leftPanel) {
            int panelWidth = leftPanel.getWidth();
            if (panelWidth <= 0) return;

            double scale = panelWidth / (double) BASE_LEFT_WIDTH;
            int sideCompartmentWidth = scaled(BOARD_SIDE_COMPARTMENT_WIDTH, scale);
            int boardPixels = Math.max(1, panelWidth - sideCompartmentWidth);
            int barHeight = Math.max(1, (int) Math.round(
                ((BASE_GAME_HEIGHT - (BASE_LEFT_WIDTH - BOARD_SIDE_COMPARTMENT_WIDTH)) / 2.0) * scale
            ));
            int contentHeight = boardPixels + 2 * barHeight;

            if (board != null) board.setDisplaySize(boardPixels);
            setFixedSize(topPanel, boardPixels, barHeight);
            setFixedSize(bottomPanel, boardPixels, barHeight);
            setFixedSize(boardStackPanel, boardPixels, contentHeight);
            setFixedSize(evalBarLanePanel, sideCompartmentWidth, contentHeight);
            setFixedSize(evalBarColumnPanel, sideCompartmentWidth, contentHeight);
            setFixedSize(leftContentPanel, panelWidth, contentHeight);

            scaleLeftPanelComponents(leftPanel, scale);
            topPanel.setBorder(BorderFactory.createEmptyBorder(
                scaled(8, scale), 0, scaled(8, scale), scaled(10, scale)
            ));
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(
                scaled(8, scale), 0, scaled(8, scale), scaled(10, scale)
            ));

            leftPanel.revalidate();
            leftPanel.repaint();
        }

        private void scaleLeftPanelComponents(Component component, double scale) {
            Font baseFont = leftPanelBaseFonts.get(component);
            if (baseFont != null) {
                component.setFont(baseFont.deriveFont(
                    Math.max(8f, (float) (baseFont.getSize2D() * scale))
                ));
            }

            if (component instanceof PlayerAvatarCard) {
                setFixedSize((JComponent) component, scaled(72, scale), scaled(72, scale));
            } else if (component instanceof RoundedClockLabel) {
                setFixedSize((JComponent) component, scaled(156, scale), scaled(62, scale));
                ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(
                    scaled(2, scale), scaled(16, scale),
                    scaled(2, scale), scaled(16, scale)
                ));
            } else if (component instanceof CapturedMaterialStrip) {
                ((CapturedMaterialStrip) component).setVisualScale(scale);
            }

            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    scaleLeftPanelComponents(child, scale);
                }
            }
        }

        private int scaled(int value, double scale) {
            return Math.max(1, (int) Math.round(value * scale));
        }

        private void setFixedSize(JComponent component, int width, int height) {
            if (component == null) return;
            Dimension d = new Dimension(width, height);
            component.setPreferredSize(d);
            component.setMinimumSize(d);
            component.setMaximumSize(d);
        }

        private void setFlexibleSplitChildSize(JComponent component, int minimumWidth, int height) {
            if (component == null) return;
            component.setMinimumSize(new Dimension(minimumWidth, height));
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        private void installResponsiveControlScaling(JPanel centerPanel, JPanel rightPanel) {
            captureResponsiveControlMetrics(centerPanel);
            captureResponsiveControlMetrics(rightPanel);

            java.awt.event.ComponentAdapter resizeListener = new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    ensurePreGameTimeControlsVisible();
                    applyResponsiveControlScaling(centerPanel, rightPanel);
                }
            };
            centerPanel.addComponentListener(resizeListener);
            rightPanel.addComponentListener(resizeListener);
            centerRightSplitPane.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                e -> {
                    ensurePreGameTimeControlsVisible();
                    applyResponsiveControlScaling(centerPanel, rightPanel);
                }
            );
            SwingUtilities.invokeLater(() -> {
                ensurePreGameTimeControlsVisible();
                applyResponsiveControlScaling(centerPanel, rightPanel);
            });
        }

        private void ensurePreGameTimeControlsVisible() {
            if (gameStarted || onlineMode || isBotGame || timeControlPanel == null || centerColumnPanel == null) {
                return;
            }
            if (timeControlPanel.getParent() == null) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(0, 6, 8, 6);
                centerColumnPanel.add(timeControlPanel, gbc);
            }
            timeControlPanel.setVisible(true);
            timeSelectBtn.setVisible(true);
            startGameBtn.setVisible(true);
            timeControlPanel.revalidate();
            centerColumnPanel.revalidate();
        }

        private void applyResponsiveControlScaling(JPanel centerPanel, JPanel rightPanel) {
            if (applyingResponsiveControlScale) return;
            applyingResponsiveControlScale = true;
            try {
                synchronizeSpellPanelSizes(centerPanel);
                scaleResponsiveControls(
                    centerPanel,
                    responsiveScale(centerPanel.getWidth(), BASE_CENTER_WIDTH)
                );
                scaleResponsiveControls(
                    rightPanel,
                    responsiveScale(rightPanel.getWidth(), BASE_RIGHT_WIDTH)
                );
                syncExpandedTimeDetailsHeight();
                updateTimeControlPanelSize();
                centerPanel.revalidate();
                rightPanel.revalidate();
                centerPanel.repaint();
                rightPanel.repaint();
            } finally {
                applyingResponsiveControlScale = false;
            }
        }

        private void synchronizeSpellPanelSizes(JPanel centerPanel) {
            if (centerPanel == null || whiteSpellPanel == null || blackSpellPanel == null) return;
            int width = Math.max(1, centerPanel.getWidth() - 12);
            for (JPanel spellPanel : new JPanel[]{blackSpellPanel, whiteSpellPanel}) {
                if (spellPanel instanceof SpellDeckPanel) {
                    ((SpellDeckPanel) spellPanel).setResponsiveWidth(width);
                }
            }
        }

        private double responsiveScale(int currentWidth, int designWidth) {
            if (currentWidth <= 0 || designWidth <= 0) return 1.0;
            double widthRatio = currentWidth / (double) designWidth;
            return 1.0 + (widthRatio - 1.0) * 0.40;
        }

        private void captureResponsiveControlMetrics(Component component) {
            if (component == null || responsiveControlMetrics.containsKey(component)) return;
            responsiveControlMetrics.put(component, new ResponsiveControlMetrics(component));
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    captureResponsiveControlMetrics(child);
                }
            }
        }

        private void scaleResponsiveControls(Component component, double scale) {
            captureResponsiveControlMetrics(component);
            ResponsiveControlMetrics metrics = responsiveControlMetrics.get(component);
            if (metrics == null) return;

            boolean dropdown = component instanceof JComboBox<?>;
            boolean responsiveDropdown = dropdown
                && settingsPanel != null
                && settingsPanel.isDividerResponsiveDropdown(component);
            if (dropdown && !responsiveDropdown) {
                return;
            }

            if (metrics.font != null) {
                component.setFont(metrics.font.deriveFont(
                    Math.max(8f, (float) (metrics.font.getSize2D() * scale))
                ));
            }
            if (component instanceof AbstractButton || responsiveDropdown) {
                component.setPreferredSize(scaleResponsiveDimension(metrics.preferredSize, scale, false));
                component.setMinimumSize(scaleResponsiveDimension(metrics.minimumSize, scale, false));
                component.setMaximumSize(scaleResponsiveDimension(metrics.maximumSize, scale, true));
            } else if (component instanceof JComponent
                    && Boolean.TRUE.equals(((JComponent) component).getClientProperty("responsivePresetIcon"))) {
                component.setPreferredSize(scaleResponsiveDimension(metrics.preferredSize, scale, false));
                component.setMinimumSize(scaleResponsiveDimension(metrics.minimumSize, scale, false));
                component.setMaximumSize(scaleResponsiveDimension(metrics.maximumSize, scale, false));
            }

            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    scaleResponsiveControls(child, scale);
                }
            }
        }

        private Dimension scaleResponsiveDimension(
            Dimension source,
            double scale,
            boolean preserveUnbounded
        ) {
            if (source == null) return null;
            int width = preserveUnbounded && source.width >= Integer.MAX_VALUE / 4
                    ? Integer.MAX_VALUE
                    : Math.max(0, (int) Math.round(source.width * scale));
            int height = preserveUnbounded && source.height >= Integer.MAX_VALUE / 4
                    ? Integer.MAX_VALUE
                    : Math.max(0, (int) Math.round(source.height * scale));
            return new Dimension(width, height);
        }

        private void applyMinimumContentSize(int contentWidth, int contentHeight) {
            Insets insets = getInsets();
            int frameWidth = contentWidth + insets.left + insets.right;
            int frameHeight = contentHeight + insets.top + insets.bottom;
            setMinimumSize(new Dimension(frameWidth, frameHeight));
        }
        
        private void startTimer() {
            if (gameOver) return;
            timer = new Timer(1000, e -> { // FIXED: Update every 1000ms (1 second)
                if (gameOver) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
                if (whitesTurn) {
                    whiteTimeRemaining--;
                    
                    // Update the correct label (might be top or bottom depending on flip)
                    JLabel whiteLabel = getWhiteClockLabel();

                    whiteLabel.setText(formatTime(whiteTimeRemaining));
                    
                    // Check if time ran out
                    if (whiteTimeRemaining <= 0) {
                handleTimeout(true);
    }

                    
                    // Warning color when under 30 seconds
                    if (whiteTimeRemaining <= 30 && whiteTimeRemaining > 0) {
                        whiteLabel.setForeground(Color.RED);
                    } else if (whiteTimeRemaining > 30) {
                        whiteLabel.setForeground(Color.WHITE);
                    }
                } else {
                    blackTimeRemaining--;
                    
                    // Update the correct label (might be top or bottom depending on flip)
                    JLabel blackLabel = getBlackClockLabel();
                    blackLabel.setText(formatTime(blackTimeRemaining));
                    
                    // Check if time ran out
                    if (blackTimeRemaining <= 0) {
                    handleTimeout(false);
    }

                    
                    // Warning color when under 30 seconds
                    if (blackTimeRemaining <= 30 && blackTimeRemaining > 0) {
                        blackLabel.setForeground(Color.RED);
                    } else if (blackTimeRemaining > 30) {
                        blackLabel.setForeground(Color.WHITE);
                    }
                }
            });
            timer.start();
        }
        
        // Called by Board when a move is made
        public void switchTurn() {
            if (gameOver) return;
            whitesTurn = !whitesTurn;
            
            if (!timerStarted && whiteTimeRemaining > 0 && blackTimeRemaining > 0) {
        timerStarted = true;
        startTimer();
    }
            
            // Add increment to the player who just moved
            if (!whitesTurn && incrementSeconds > 0) {
                // White just moved, add increment to white's time
                whiteTimeRemaining += incrementSeconds;
                JLabel whiteLabel = getWhiteClockLabel();
                whiteLabel.setText(formatTime(whiteTimeRemaining));
            } else if (whitesTurn && incrementSeconds > 0) {
                // Black just moved, add increment to black's time
                blackTimeRemaining += incrementSeconds;
                JLabel blackLabel = getBlackClockLabel();
                blackLabel.setText(formatTime(blackTimeRemaining));
            }
            if (isBotGame && (spectateMode || isBotTurnToMove())) {
                makeBotMove();
            }
            refreshSpellUI();
            refreshCapturedMaterialUI();
            refreshOpeningTitle();
        }

        private boolean isBotTurnToMove() {
            return whitesTurn != playerIsWhite;
        }

        private void makeBotMove() {
        if (stockfishEngine == null || gameOver) return;
        if (!spectateMode && !isBotTurnToMove()) return;
        synchronized (this) {
            if (botMoveInProgress) return;
            botMoveInProgress = true;
        }

        new Thread(() -> {
            try {
                if (gameOver) return;
                if (!spectateMode && !isBotTurnToMove()) return;
                ChessBot activeBot = selectedBot;
                if (spectateMode) {
                    activeBot = whitesTurn ? spectateWhiteBot : spectateBlackBot;
                }
                int activeDepth = botDepth;
                if (activeBot != null) {
                    activeDepth = activeBot.getDepth();
                    stockfishEngine.setSkillLevel(activeBot.getSkillLevel());
                }

                // Add minimum 1 second thinking time for more natural feel
                long startTime = System.currentTimeMillis();
                
                String bestMove;
                
                // If UCI moves are empty (after takeback), use FEN position instead
                if (uciMoves.length() == 0 && board != null) {
                    String fen = board.getCurrentFEN();
                    EngineAnalysis analysis = stockfishEngine.analyzePosition(fen, activeDepth);
                    bestMove = analysis.bestMove;
                } else {
                    // Ask Stockfish using UCI move list
                    bestMove = stockfishEngine.getBestMoveFromMoves(
                        uciMoves.toString().trim(),
                        activeDepth
                    );
                }

                if (bestMove == null || bestMove.equals("(none)")) return;

                // Convert UCI â†’ board coordinates
                int[] move = UCIConverter.toMove(bestMove);
                if (move == null) return;

                // Ensure at least 1 second has passed (more natural thinking time)
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed < 1000) {
                    Thread.sleep(1000 - elapsed);
                }

                // Apply move on EDT
                // Note: The move will be added to uciMoves by onLocalMove() 
                // when commitMove() is called from applyBotMove()
                SwingUtilities.invokeLater(() -> {
                    if (gameOver) return;
                    if (!spectateMode && !isBotTurnToMove()) return;
                    board.applyBotMove(
                        move[0], move[1], move[2], move[3]
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, 
                        "Bot error: " + e.getMessage(), 
                        "Engine Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                botMoveInProgress = false;
            }
        }).start();
    }






        
        // Format seconds into MM:SS
        private String formatTime(int totalSeconds) {
            if (totalSeconds < 0) totalSeconds = 0;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        private void refreshOpeningTitle() {
            if (moveHistoryPanel == null || openingDetector == null || board == null) return;
            if (!openingNamesEnabled) {
                moveHistoryPanel.setOpeningTitle("");
                return;
            }
            String name = openingDetector.getOpeningName(board.getCurrentFEN());
            if (name.isEmpty()) {
                // Walk history backward to find the most recent known opening.
                java.util.List<String> history = board.getMoveHistoryFEN();
                for (int i = history.size() - 1; i >= 0; i--) {
                    String candidate = openingDetector.getOpeningName(history.get(i));
                    if (!candidate.isEmpty()) {
                        name = candidate;
                        break;
                    }
                }
            }
            if (!name.isEmpty()) {
                lastOpeningTitle = name;
            }
            String variantTitle = (gameVariant == null || gameVariant.trim().isEmpty()) ? "Classic" : gameVariant.trim();
            String title = lastOpeningTitle.isEmpty() ? variantTitle : lastOpeningTitle;
            moveHistoryPanel.setOpeningTitle(title);
        }

        private void resetOpeningTitleToVariant() {
            lastOpeningTitle = "";
            if (moveHistoryPanel == null) return;
            if (!openingNamesEnabled) {
                moveHistoryPanel.setOpeningTitle("");
                return;
            }
            String variantTitle = (gameVariant == null || gameVariant.trim().isEmpty()) ? "Classic" : gameVariant.trim();
            moveHistoryPanel.setOpeningTitle(variantTitle);
        }

        public void setOpeningNamesEnabled(boolean enabled) {
            openingNamesEnabled = enabled;
            if (moveHistoryPanel == null) return;
            if (enabled) {
                refreshOpeningTitle();
            } else {
                moveHistoryPanel.setOpeningTitle("");
            }
        }

        public void setUiTheme(String themeName) {
            timeUiTheme = TimeUiTheme.forName(themeName);
            if (startGameBtn != null) startGameBtn.repaint();
            if (timeSelectBtn != null) timeSelectBtn.repaint();
            if (timeDetailsPanel != null) timeDetailsPanel.repaint();
            updatePresetButtonStyles();
        }

        private void showMoveHistoryPanel() {
            if (centerColumnPanel == null || board == null) return;
            if (moveHistoryPanel == null) {
                moveHistoryPanel = new MoveHistoryPanel();
                moveHistoryPanel.setBoardReference(board);
                moveHistoryPanel.setGameController(this);
            }

            if (timeControlPanel != null && timeControlPanel.getParent() == centerColumnPanel) {
                centerColumnPanel.remove(timeControlPanel);
            }
            if (centerSpellFiller != null && centerSpellFiller.getParent() == centerColumnPanel) {
                centerColumnPanel.remove(centerSpellFiller);
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(0, 6, 0, 6);
            centerColumnPanel.add(moveHistoryPanel, gbc);
            centerColumnPanel.revalidate();
            centerColumnPanel.repaint();
            refreshOpeningTitle();
        }

        private void refreshCapturedMaterialUI() {
            if (board == null || topCapturedStrip == null || bottomCapturedStrip == null) return;
            topCapturedStrip.setSummary(computeCaptureSummaryForSide(false));
            bottomCapturedStrip.setSummary(computeCaptureSummaryForSide(true));
        }

        private CaptureSummary computeCaptureSummaryForSide(boolean sideWhite) {
            CaptureSummary s = new CaptureSummary();
            // PlayerState stores pieces lost by that side, so to show pieces
            // captured by `sideWhite`, read the opponent side's lost list.
            List<CapturedPieceRecord> sideCaptured = board.getPlayerState(!sideWhite).getCapturedPieces();
            for (CapturedPieceRecord rec : sideCaptured) {
                if (rec == null || rec.getPieceType() == null) continue;
                String type = rec.getPieceType();
                if ("Pawn".equalsIgnoreCase(type)) s.pawns++;
                else if ("Knight".equalsIgnoreCase(type)) s.knights++;
                else if ("Bishop".equalsIgnoreCase(type)) s.bishops++;
                else if ("Rook".equalsIgnoreCase(type)) s.rooks++;
                else if ("Queen".equalsIgnoreCase(type)) s.queens++;
                else if ("Zoglin".equalsIgnoreCase(type)) s.zoglins++;
            }

            int sideValue = computeCapturedValue(sideCaptured);
            int otherValue = computeCapturedValue(board.getPlayerState(sideWhite).getCapturedPieces());
            s.advantage = sideValue - otherValue;
            return s;
        }

        private int computeCapturedValue(List<CapturedPieceRecord> captured) {
            int total = 0;
            if (captured == null) return 0;
            for (CapturedPieceRecord rec : captured) {
                if (rec == null || rec.getPieceType() == null) continue;
                String type = rec.getPieceType();
                if ("Pawn".equalsIgnoreCase(type)) total += 1;
                else if ("Knight".equalsIgnoreCase(type)) total += 3;
                else if ("Bishop".equalsIgnoreCase(type)) total += 3;
                else if ("Rook".equalsIgnoreCase(type)) total += 5;
                else if ("Queen".equalsIgnoreCase(type)) total += 9;
                else if ("Zoglin".equalsIgnoreCase(type)) total += 12;
            }
            return total;
        }

        private static class CaptureSummary {
            int pawns;
            int knights;
            int bishops;
            int rooks;
            int queens;
            int zoglins;
            int advantage;
        }
        
        public void stopTimer() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            timerStarted = false;
            cancelSpellResolutionLock();
        }

        private void setGameOverState() {
            gameOver = true;
            stopTimer();
        }

        private void clearGameOverState() {
            gameOver = false;
            ratingResultRecorded = false;
        }
        
        // Timer-related public methods for Board to access
        public boolean isTimerStarted() {
            return timerStarted;
        }
        
        public void setTimerStarted(boolean started) {
            this.timerStarted = started;
        }
        
        public int getWhiteTimeRemaining() {
            return whiteTimeRemaining;
        }
        
        public int getBlackTimeRemaining() {
            return blackTimeRemaining;
        }
        
        public int getIncrementSeconds() {
            return incrementSeconds;
        }
        
        public void ensureTimerStarted() {
            if (!timerStarted && whiteTimeRemaining > 0 && blackTimeRemaining > 0) {
                timerStarted = true;
                startTimer();
            }
        }
        
        // Toggle turn without triggering bot move (used by applyBotMove)
        public void toggleTurnOnly() {
            whitesTurn = !whitesTurn;
        }
        
        public void addTimeIncrement(boolean forWhite) {
            if (incrementSeconds > 0) {
                if (forWhite) {
                    whiteTimeRemaining += incrementSeconds;
                    JLabel whiteLabel = getWhiteClockLabel();

                    whiteLabel.setText(formatTime(whiteTimeRemaining));
                } else {
                    blackTimeRemaining += incrementSeconds;
                    JLabel blackLabel = getBlackClockLabel();
                    blackLabel.setText(formatTime(blackTimeRemaining));
                }
            }
        }

        public boolean isOnlineMode() {
            return onlineMode;
        }

        // Called by Board when local move completed
        public void onLocalMove(int fr, int fc, int tr, int tc, String promotion) {

        // Convert move to UCI
        String uci = UCIConverter.fromMove(fr, fc, tr, tc, promotion);

        if (uci != null) {
            if (uciMoves.length() > 0) uciMoves.append(" ");
            uciMoves.append(uci);
        }

        if (onlineMode && networkManager != null && !applyingRemoteNetworkEvent) {
            networkManager.sendMove(fr, fc, tr, tc, promotion == null ? "" : promotion);
        }
    }


        // Called by NetworkManager listener when remote move arrives
        public void onRemoteMove(int fr, int fc, int tr, int tc, String promotion) {
            if (board != null) {
                applyingRemoteNetworkEvent = true;
                try {
                    board.applyRemoteMove(fr, fc, tr, tc, promotion == null ? "" : promotion);
                } finally {
                    applyingRemoteNetworkEvent = false;
                }
            }
        }

        public void onLocalSpellCast(String spellId, boolean casterWhite, SpellTarget target) {
            if (onlineMode && networkManager != null && !applyingRemoteNetworkEvent) {
                networkManager.sendSpellCast(spellId, casterWhite, target);
            }
        }

        public void onLocalSpellPhase(String phaseId, boolean casterWhite, int row, int col) {
            if (onlineMode && networkManager != null && !applyingRemoteNetworkEvent) {
                networkManager.sendSpellPhase(phaseId, casterWhite, row, col);
            }
        }

        public void onRemoteSpellCast(String spellId, boolean casterWhite, SpellTarget target) {
            if (board != null) {
                applyingRemoteNetworkEvent = true;
                try {
                    board.applyRemoteSpellCast(spellId, casterWhite, target);
                } finally {
                    applyingRemoteNetworkEvent = false;
                }
            }
        }

        public void onRemoteSpellPhase(String phaseId, boolean casterWhite, int row, int col) {
            if (board != null) {
                applyingRemoteNetworkEvent = true;
                try {
                    board.applyRemoteSpellPhase(phaseId, casterWhite, row, col);
                } finally {
                    applyingRemoteNetworkEvent = false;
                }
            }
        }

        public void onRemoteGameResult(String title, String message) {
            if (gameOver) return;
            applyingRemoteNetworkEvent = true;
            try {
                showInGameInfoPopup(title, message);
            } finally {
                applyingRemoteNetworkEvent = false;
            }
        }

        private void handleNetworkDisconnect(String message) {
            if (!onlineMode || gameOver) return;
            setGameOverState();
            botMoveInProgress = false;
            if (board != null) board.setInputEnabled(false);
            if (networkManager != null) {
                networkManager.close();
                networkManager = null;
            }
            String detail = (message == null || message.isBlank())
                    ? "Opponent disconnected."
                    : message;
            boolean opponentDisconnected = detail.toLowerCase().contains("opponent disconnected")
                    || detail.toLowerCase().contains("forfeit")
                    || detail.toLowerCase().contains("disconnected.");
            if (opponentDisconnected) {
                showGamePopup("Your opponent disconnected. You win by forfeit", "Your opponent has disconnected from the game. You win by forfeit.");
                returnToApplicationMenu();
                return;
            }
            boolean returnToMenu = showGameConfirm(
                    "Connection Lost",
                    detail + "\n\nReturn to the multiplayer menu?",
                    "Return",
                    "Stay");
            if (returnToMenu) {
                returnToApplicationMenu();
            }
        }

        // Called by Board when the visible position index changes (during animated navigation)
        public void onPositionChanged(int idx) {
            if (moveHistoryPanel != null) {
                // Board state index (idx) corresponds to half-move index = idx - 1
                int half = idx - 1;
                moveHistoryPanel.setCurrentMoveIndexExternal(half);
            }
            refreshOpeningTitle();
        }
        
        public MoveHistoryPanel getMoveHistoryPanel() {
            return moveHistoryPanel;
        }
        
        // Rebuild UCI move list from move history (used after takeback)
        public void rebuildUCIMoves() {
            // Clear UCI moves - they'll rebuild as the game continues
            // This works because Stockfish will be called with the current position
            uciMoves.setLength(0);
        }
        
        public OpeningDetector getOpeningDetector() {
            return openingDetector;
        }
        
        public OpeningNamePanel getOpeningNamePanel() {
            return openingNamePanel;
        }
        
        // NEW: Update opening name display
        public void updateOpeningName() {
            String currentFEN = board.getCurrentFEN();
            String openingName = openingDetector.getOpeningName(currentFEN);
            openingNamePanel.setOpeningName(openingName);
        }

        public void onLocalResignRequest() {
            boolean confirm = showGameConfirm("Resign", "Are you sure you want to resign?", "Resign", "Cancel");
            if (confirm) {
                onLocalResign();
            }
        }

        // Called when local player resigns via UI
        public void onLocalResign() {
            setGameOverState();
            botMoveInProgress = false;
            if (networkManager != null) {
                networkManager.sendResign();
            }
            String winner = localIsWhite ? "Black" : "White";
            recordTerminalResult("Resign", "You resigned. " + winner + " wins.");
            showGamePopup("Resign", "You resigned. " + winner + " wins.");
            // Clean up and close the game window
            if (networkManager != null) networkManager.close();
            returnToApplicationMenu();
        }

        // Called when the user clicks Offer Draw
        public void onOfferDraw() {
            if (networkManager != null) {
                networkManager.sendOfferDraw();
                showGamePopup("Offer Draw", "Draw offer sent to opponent.");
            } else {
                boolean confirm = showGameConfirm("Offer Draw", "Offer draw to local opponent?", "Offer", "Cancel");
                if (confirm) {
                    // For local play, immediately accept â€” stop and close
                    setGameOverState();
                    recordRatingResult("draw");
                    showGamePopup("Draw", "Draw agreed.");
                    if (networkManager != null) networkManager.close();
                    returnToApplicationMenu();
                }
            }
        }

        // Network callbacks
        public void onRemoteResign() {
            setGameOverState();
            recordRatingResult("win");
            showGamePopup("Resign", "Opponent resigned. You win!");
            if (networkManager != null) networkManager.close();
            returnToApplicationMenu();
        }

        public void onRemoteOfferDraw() {
            boolean accept = showGameConfirm("Draw Offer", "Opponent offers a draw. Accept?", "Accept", "Decline");
            if (accept) {
                if (networkManager != null) networkManager.sendDrawResponse(true);
                setGameOverState();
                recordRatingResult("draw");
                showGamePopup("Draw", "Draw agreed.");
                if (networkManager != null) networkManager.close();
                returnToApplicationMenu();
            } else {
                if (networkManager != null) networkManager.sendDrawResponse(false);
            }
        }

        public void onRemoteDrawAccepted() {
            setGameOverState();
            recordRatingResult("draw");
            showGamePopup("Draw", "Opponent accepted the draw.");
            if (networkManager != null) networkManager.close();
            returnToApplicationMenu();
        }

        public void onRemoteDrawDeclined() {
            showGamePopup("Draw", "Opponent declined the draw.");
        }

    private class SpellDeckPanel extends JPanel {
        private JPanel titlePanel;
        private JLabel titleLabel;
        private JPanel cardsPanel;
        private ElixirBar elixirBar;
        private List<SpellCardButton> cardButtons;
        private double uiScale = 1.0;
        private int lastLayoutWidth = -1;
        private int responsiveWidth = SPELL_PANEL_W;

        SpellDeckPanel() {
            setOpaque(false);
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    int width = getWidth();
                    if (width <= 0 || width == lastLayoutWidth) return;
                    lastLayoutWidth = width;
                    Container parent = getParent();
                    if (parent != null) {
                        SwingUtilities.invokeLater(() -> {
                            parent.revalidate();
                            parent.repaint();
                        });
                    }
                }
            });
        }

        void configure(
                JPanel titlePanel,
                JLabel titleLabel,
                JPanel cardsPanel,
                ElixirBar elixirBar,
                List<SpellCardButton> cardButtons) {
            this.titlePanel = titlePanel;
            this.titleLabel = titleLabel;
            this.cardsPanel = cardsPanel;
            this.elixirBar = elixirBar;
            this.cardButtons = cardButtons;
        }

        double getUiScale() {
            return uiScale;
        }

        void setResponsiveWidth(int width) {
            responsiveWidth = Math.max(1, width);
            int height = Math.max(1,
                    (int) Math.round(responsiveWidth * SPELL_PANEL_H
                            / (double) SPELL_PANEL_W));
            Dimension responsiveSize = new Dimension(responsiveWidth, height);
            super.setMinimumSize(responsiveSize);
            super.setPreferredSize(responsiveSize);
            super.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(responsiveWidth, Math.max(1,
                    (int) Math.round(responsiveWidth * SPELL_PANEL_H
                            / (double) SPELL_PANEL_W)));
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public void doLayout() {
            uiScale = Math.max(0.01, getWidth() / (double) SPELL_PANEL_W);
            if (titleLabel != null) {
                titleLabel.setFont(new Font(
                        "Arial", Font.BOLD, Math.max(8, spellScaled(16, uiScale))));
            }
            if (titlePanel != null) {
                titlePanel.setBounds(
                        spellScaled(8, uiScale),
                        spellScaled(7, uiScale),
                        spellScaled(484, uiScale),
                        spellScaled(25, uiScale));
            }
            if (elixirBar != null) {
                Dimension barSize = new Dimension(
                        spellScaled(SPELL_BAR_W, uiScale),
                        spellScaled(SPELL_BAR_H, uiScale));
                elixirBar.setPreferredSize(barSize);
                elixirBar.setMinimumSize(barSize);
                elixirBar.setMaximumSize(barSize);
                elixirBar.setBounds(
                        spellScaled(20, uiScale),
                        spellScaled(186, uiScale),
                        barSize.width,
                        barSize.height);
            }
            if (cardsPanel != null) {
                Dimension cardsSize = new Dimension(
                        spellScaled(SPELL_CARDS_W, uiScale),
                        spellScaled(SPELL_CARDS_H, uiScale));
                cardsPanel.setPreferredSize(cardsSize);
                cardsPanel.setMinimumSize(cardsSize);
                cardsPanel.setMaximumSize(cardsSize);
                cardsPanel.setBounds(
                        spellScaled(13, uiScale),
                        spellScaled(36, uiScale),
                        cardsSize.width,
                        cardsSize.height);
            }
            if (cardButtons != null && !spellCardTransitionAnimating) {
                for (int i = 0; i < cardButtons.size(); i++) {
                    cardButtons.get(i).setBounds(getSpellSlotBounds(i, uiScale));
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double sx = getWidth() / (double) SPELL_PANEL_W;
            double sy = getHeight() / (double) SPELL_PANEL_H;
            g2.scale(sx, sy);
            GradientPaint gp = new GradientPaint(0, 0, new Color(17, 92, 168), 0, SPELL_PANEL_H, new Color(10, 45, 114));
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, SPELL_PANEL_W, SPELL_PANEL_H, 14, 14);
            g2.setColor(new Color(115, 181, 248));
            g2.drawRoundRect(1, 1, SPELL_PANEL_W - 3, SPELL_PANEL_H - 3, 14, 14);
            g2.setColor(new Color(4, 23, 63, 145));
            g2.fillRoundRect(8, 8, SPELL_PANEL_W - 16, SPELL_PANEL_H - 16, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class PlayerInfoBarPanel extends JPanel {
        PlayerInfoBarPanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            GradientPaint bg = new GradientPaint(0, 0, new Color(43, 45, 49), 0, h, new Color(30, 32, 36));
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 36), 0, h / 2, new Color(255, 255, 255, 0)));
            g2.fillRect(0, 0, w, Math.max(1, h / 2));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedClockLabel extends JLabel {
        private float pauseOverlayAlpha = 0f;

        RoundedClockLabel(String text) {
            super(text);
            setOpaque(false);
        }

        void setPauseOverlayAlpha(float alpha) {
            pauseOverlayAlpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = 18;
            GradientPaint gp = new GradientPaint(0, 0, new Color(28, 34, 46, 210), 0, h, new Color(20, 25, 36, 210));
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(new Color(150, 156, 168, 220));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(new Color(230, 235, 245, 40));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);

            String text = getText();
            if (text != null && !text.isEmpty()) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 95));
                g2.drawString(text, tx + 1, ty + 1);
                g2.setColor(getForeground());
                g2.drawString(text, tx, ty);
            }
            if (pauseOverlayAlpha > 0f) {
                int alpha = Math.round(178f * pauseOverlayAlpha);
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(155, 158, 164, alpha),
                    0, h, new Color(92, 96, 104, alpha)
                ));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(225, 227, 232, Math.round(90f * pauseOverlayAlpha)));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);
            }
            g2.dispose();
        }
    }

    private static class PlayerAvatarCard extends JComponent {
        PlayerAvatarCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(72, 72));
            setMinimumSize(new Dimension(72, 72));
            setMaximumSize(new Dimension(72, 72));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            double scale = Math.min(w, h) / 72.0;
            int arc = Math.max(4, (int) Math.round(8 * scale));

            g2.setColor(new Color(228, 228, 228));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(new Color(188, 188, 188));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            g2.setColor(new Color(214, 214, 214));
            int cx = w / 2;
            int head = Math.max(8, (int) Math.round(28 * scale));
            int headY = (int) Math.round(11 * scale);
            int bodyW = Math.max(10, (int) Math.round(38 * scale));
            int bodyH = Math.max(8, (int) Math.round(23 * scale));
            int bodyY = (int) Math.round(37 * scale);
            int bodyArc = Math.max(4, (int) Math.round(13 * scale));
            g2.fillOval(cx - head / 2, headY, head, head);
            g2.fillRoundRect(cx - bodyW / 2, bodyY, bodyW, bodyH, bodyArc, bodyArc);
            g2.dispose();
        }
    }

    private static class CapturedMaterialStrip extends JComponent {
        private CaptureSummary summary = new CaptureSummary();
        private Font pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, 21);
        private Font advFont = new Font("Arial", Font.BOLD, 19);
        private double visualScale = 1.0;

        CapturedMaterialStrip() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 26));
            setMinimumSize(new Dimension(160, 24));
            setMaximumSize(new Dimension(340, 28));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setSummary(CaptureSummary s) {
            if (s == null) s = new CaptureSummary();
            summary = s;
            repaint();
        }

        void setVisualScale(double scale) {
            visualScale = Math.max(0.01, scale);
            pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, Math.max(8, (int) Math.round(21 * visualScale)));
            advFont = new Font("Arial", Font.BOLD, Math.max(8, (int) Math.round(19 * visualScale)));
            setPreferredSize(new Dimension(
                Math.max(1, (int) Math.round(280 * visualScale)),
                Math.max(1, (int) Math.round(26 * visualScale))
            ));
            setMinimumSize(new Dimension(
                Math.max(1, (int) Math.round(160 * visualScale)),
                Math.max(1, (int) Math.round(24 * visualScale))
            ));
            setMaximumSize(new Dimension(
                Math.max(1, (int) Math.round(340 * visualScale)),
                Math.max(1, (int) Math.round(28 * visualScale))
            ));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(pieceFont);

            int x = 0;
            int baseline = Math.max(1, (int) Math.round(20 * visualScale));
            x = drawGroup(g2, '\u265F', summary.pawns, x);
            x = drawGroup(g2, '\u265E', summary.knights, x);
            x = drawGroup(g2, '\u265D', summary.bishops, x);
            x = drawGroup(g2, '\u265C', summary.rooks, x);
            x = drawGroup(g2, '\u265B', summary.queens, x);
            x = drawGroup(g2, '\u2739', summary.zoglins, x);

            if (summary.advantage > 0) {
                g2.setFont(advFont);
                g2.setColor(new Color(230, 230, 230));
                g2.drawString("+" + summary.advantage, x + Math.max(2, (int) Math.round(10 * visualScale)), baseline);
            }
            g2.dispose();
        }

        private int drawGroup(Graphics2D g2, char symbol, int count, int xStart) {
            if (count <= 0) return xStart;
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.charWidth(symbol);
            int overlap = Math.max(1, w / 3);
            int x = xStart;
            for (int i = 0; i < count; i++) {
                g2.drawString(String.valueOf(symbol), x, Math.max(1, (int) Math.round(20 * visualScale)));
                x += (w - overlap);
            }
            return x + Math.max(2, (int) Math.round(7 * visualScale));
        }
    }

    private class ElixirBar extends JComponent {
        private int value;
        void setValue(int v) {
            value = Math.max(0, Math.min(10, v));
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(
                    getWidth() / (double) SPELL_BAR_W,
                    getHeight() / (double) SPELL_BAR_H);
            int w = SPELL_BAR_W, h = SPELL_BAR_H;
            int badgeW = 104;
            int badgeH = 28;
            int barH = h - badgeH + 4;
            int barY = 0;
            int badgeX = (w - badgeW) / 2;
            int badgeY = h - badgeH;

            // Outer blue shell
            g2.setPaint(new GradientPaint(0, 0, new Color(45, 108, 216), 0, barH, new Color(24, 67, 160)));
            g2.fillRoundRect(0, barY, w, barH, 20, 20);
            g2.setColor(new Color(166, 213, 255));
            g2.drawRoundRect(1, barY + 1, w - 3, barH - 3, 20, 20);

            // Inner track
            int pad = 8;
            int trackX = pad;
            int trackY = barY + 7;
            int trackW = w - pad * 2;
            int trackH = barH - 14;
            g2.setPaint(new GradientPaint(0, trackY, new Color(18, 52, 126), 0, trackY + trackH, new Color(9, 34, 94)));
            g2.fillRoundRect(trackX, trackY, trackW, trackH, 14, 14);
            g2.setColor(new Color(120, 178, 255, 170));
            g2.drawRoundRect(trackX, trackY, trackW - 1, trackH - 1, 14, 14);

            // 10 segmented elixir cells
            int segGap = 3;
            int segW = (trackW - segGap * 11) / 10;
            int segH = trackH - 6;
            int segY = trackY + 3;
            for (int i = 0; i < 10; i++) {
                int segX = trackX + segGap + i * (segW + segGap);
                boolean filled = i < value;
                if (filled) {
                    g2.setPaint(new GradientPaint(segX, segY, new Color(203, 126, 255), segX, segY + segH, new Color(126, 45, 217)));
                    g2.fillRoundRect(segX, segY, segW, segH, 8, 8);
                    int shimmer = (int) ((spellAnimPhase % 1.0f) * Math.max(1, segW));
                    g2.setColor(new Color(255, 255, 255, 55));
                    g2.fillRect(segX + shimmer, segY + 1, Math.min(5, segW), segH - 2);
                } else {
                    g2.setPaint(new GradientPaint(segX, segY, new Color(36, 66, 138), segX, segY + segH, new Color(20, 50, 118)));
                    g2.fillRoundRect(segX, segY, segW, segH, 8, 8);
                }
                g2.setColor(new Color(14, 29, 77, 160));
                g2.drawRoundRect(segX, segY, segW - 1, segH - 1, 8, 8);
            }

            // Bottom value badge
            g2.setPaint(new GradientPaint(0, badgeY, new Color(45, 108, 216), 0, badgeY + badgeH, new Color(24, 67, 160)));
            g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 14, 14);
            g2.setColor(new Color(166, 213, 255));
            g2.drawRoundRect(badgeX, badgeY, badgeW - 1, badgeH - 1, 14, 14);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String txt = value + " / 10";
            FontMetrics fm = g2.getFontMetrics();
            int tx = badgeX + (badgeW - fm.stringWidth(txt)) / 2;
            int ty = badgeY + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(txt, tx, ty);
            g2.dispose();
        }
    }

    private class SpellCardButton extends JButton {
        private float visualLift = 0f;
        private float targetLift = 0f;
        private boolean selectedVisual = false;
        private long selectedSinceMs = 0L;
        private boolean glazeActive = false;
        private long glazeStartedAtMs = 0L;
        private Timer liftTimer;
        private Timer fxTimer;
        private boolean previewOnly = false;
        private String spellId;
        private String spellName = "";
        private int spellCost = 0;
        private BufferedImage artImage;
        private BufferedImage artGrayImage;
        SpellCardButton() {
            setFont(new Font("Arial", Font.BOLD, 12));
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(90, 135));
            setMinimumSize(new Dimension(90, 135));
            setMaximumSize(new Dimension(90, 135));
            setHorizontalTextPosition(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.CENTER);
            liftTimer = AnimationTiming.createUiTimer(e -> {
                float diff = targetLift - visualLift;
                if (Math.abs(diff) < 0.4f) {
                    visualLift = targetLift;
                    SpellCardButton.this.liftTimer.stop();
                } else {
                    double frameAdjustedEase = 1.0 - Math.pow(
                            1.0 - 0.35,
                            AnimationTiming.FRAME_SCALE_FROM_16_MS);
                    visualLift += diff * (float) frameAdjustedEase;
                }
                repaint();
            });
            fxTimer = AnimationTiming.createUiTimer(e -> {
                if (isShowing()) repaint();
            });
            addHierarchyListener(event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
                if (isShowing()) {
                    fxTimer.start();
                } else {
                    fxTimer.stop();
                    liftTimer.stop();
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    if (!previewOnly && !selectedVisual) setLift(-4f);
                }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (!previewOnly && !selectedVisual) setLift(0f);
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!previewOnly && !selectedVisual) setLift(0f);
                }
            });
        }
        void setSpellData(String spellId, Spell spell, boolean previewOnly) {
            this.spellId = spellId;
            this.previewOnly = previewOnly;
            if (spell != null) {
                this.spellName = spell.getName();
                this.spellCost = spell.getCost();
                // Keep art visible for both active cards and NEXT preview.
                this.artImage = loadSpellArt(spellId);
                this.artGrayImage = loadSpellArtGray(spellId);
            } else {
                this.spellName = "";
                this.spellCost = 0;
                this.artImage = null;
                this.artGrayImage = null;
            }
            if (previewOnly) {
                setCursor(Cursor.getDefaultCursor());
            } else {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            repaint();
        }
        void setLift(float lift) {
            targetLift = lift;
            if (!liftTimer.isRunning()) liftTimer.start();
        }
        void pulsePop() {
            setLift(-8f);
            Timer t = new Timer(120, e -> setLift(0f));
            t.setRepeats(false);
            t.start();
        }
        void setSelectedVisual(boolean selected) {
            boolean wasSelected = this.selectedVisual;
            this.selectedVisual = selected;
            if (previewOnly) selected = false;
            if (selected) {
                if (!wasSelected) selectedSinceMs = System.currentTimeMillis();
                setLift(-8f);
            } else {
                selectedSinceMs = 0L;
                glazeActive = false;
                glazeStartedAtMs = 0L;
                setLift(0f);
            }
        }
        void setGlazeActive(boolean active) {
            if (previewOnly) active = false;
            if (active && !glazeActive) glazeStartedAtMs = System.currentTimeMillis();
            if (!active) glazeStartedAtMs = 0L;
            glazeActive = active;
            repaint();
        }
        private Color[] getSpellColors() {
            if (spellId == null) return new Color[]{new Color(56, 88, 148), new Color(26, 56, 116)};
            switch (spellId) {
                case SpellManager.FIREBALL: return new Color[]{new Color(255, 120, 38), new Color(172, 48, 13)};   // Molten red-orange
                case SpellManager.FREEZE: return new Color[]{new Color(94, 230, 255), new Color(22, 124, 230)};    // Electric ice blue
                case SpellManager.SHIELD: return new Color[]{new Color(67, 220, 125), new Color(24, 137, 76)};     // Emerald green
                case SpellManager.ENDERMAN: return new Color[]{new Color(157, 100, 255), new Color(81, 32, 172)};  // Void purple
                case SpellManager.URIEL: return new Color[]{new Color(255, 217, 96), new Color(207, 141, 24)};     // Radiant gold
                case SpellManager.FOG: return new Color[]{new Color(103, 214, 199), new Color(36, 126, 132)};      // Storm teal
                case SpellManager.BOMBER: return new Color[]{new Color(120, 236, 82), new Color(48, 154, 42)};     // Creeper green
                case SpellManager.ZOGLIN: return new Color[]{new Color(227, 72, 82), new Color(134, 20, 30)};      // Blood red
                default: return new Color[]{new Color(56, 88, 148), new Color(26, 56, 116)};
            }
        }
        private Color[] getBorderTheme() {
            if (spellId == null) return new Color[]{new Color(160, 96, 38), new Color(72, 22, 10), new Color(255, 185, 74)};
            switch (spellId) {
                case SpellManager.FIREBALL: return new Color[]{new Color(255, 146, 44), new Color(130, 34, 10), new Color(255, 194, 78)}; // Orange
                case SpellManager.FREEZE: return new Color[]{new Color(97, 183, 255), new Color(22, 68, 150), new Color(160, 220, 255)}; // Blue
                case SpellManager.SHIELD: return new Color[]{new Color(74, 213, 118), new Color(17, 102, 56), new Color(150, 241, 183)}; // Emerald Green
                case SpellManager.ENDERMAN: return new Color[]{new Color(169, 106, 255), new Color(66, 26, 138), new Color(212, 166, 255)}; // Purple
                case SpellManager.URIEL: return new Color[]{new Color(255, 214, 95), new Color(136, 84, 14), new Color(255, 234, 160)}; // Gold
                case SpellManager.FOG: return new Color[]{new Color(87, 206, 190), new Color(18, 102, 111), new Color(164, 235, 226)}; // Storm Teal
                case SpellManager.BOMBER: return new Color[]{new Color(119, 227, 85), new Color(32, 110, 24), new Color(188, 255, 166)}; // Creeper Green
                case SpellManager.ZOGLIN: return new Color[]{new Color(228, 70, 80), new Color(108, 16, 24), new Color(255, 160, 170)}; // Blood red
                default: return new Color[]{new Color(160, 96, 38), new Color(72, 22, 10), new Color(255, 185, 74)};
            }
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int designW = previewOnly ? SPELL_NEXT_W : SPELL_CARD_W;
            int designH = previewOnly ? SPELL_NEXT_H : SPELL_CARD_H;
            g2.scale(
                    getWidth() / (double) designW,
                    getHeight() / (double) designH);
            long nowMs = System.currentTimeMillis();
            boolean selectedCard = selectedVisual && !previewOnly;
            float selectedFloat = selectedCard
                ? 1.1f * (float) Math.sin((nowMs - selectedSinceMs) * (2.0 * Math.PI / 2100.0))
                : 0f;
            g2.translate(0, visualLift + selectedFloat);
            int w = designW;
            int h = designH;
            Color[] borderTheme = getBorderTheme();
            float pulse = 0.5f + 0.5f * (float) Math.sin(nowMs * (2.0 * Math.PI / 1800.0));
            boolean disabledCard = !isEnabled() && !previewOnly;

            // Fireball-style molten outer frame for all cards
            if (disabledCard) {
                int gTop = Math.round(120 + 22 * pulse);
                int gBot = Math.round(72 + 16 * pulse);
                g2.setPaint(new GradientPaint(0, 0, new Color(gTop, gTop, gTop), 0, h, new Color(gBot, gBot, gBot)));
            } else {
                g2.setPaint(new GradientPaint(0, 0, borderTheme[0], 0, h, borderTheme[1]));
            }
            g2.fillRoundRect(0, 0, w, h, 18, 18);
            if (disabledCard) {
                int edge = Math.round(132 + 26 * pulse);
                g2.setColor(new Color(edge, edge, edge, 235));
            } else {
                g2.setColor(selectedVisual ? new Color(255, 231, 132) : new Color(30, 18, 22));
            }
            g2.setStroke(new BasicStroke(selectedVisual ? 3.2f : 2.2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 18, 18);

            // Inner dark border line
            if (disabledCard) {
                int inner = Math.round(94 + 20 * pulse);
                g2.setColor(new Color(inner, inner, inner, 210));
            } else {
                g2.setColor(new Color(borderTheme[2].getRed(), borderTheme[2].getGreen(), borderTheme[2].getBlue(), 190));
            }
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(4, 4, w - 9, h - 9, 14, 14);

            // Art area placeholder
            int artPad = 8;
            int artX = artPad, artY = 8, artW = w - artPad * 2, artH = h - 40;
            Color[] spellColors = getSpellColors();
            if (!isEnabled() && !previewOnly) {
                spellColors = new Color[]{new Color(114, 126, 145), new Color(79, 90, 108)};
            }
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(artX, artY, artW, artH, 10, 10));
            if (artImage != null) {
                BufferedImage imgToDraw = (!isEnabled() && !previewOnly && artGrayImage != null) ? artGrayImage : artImage;
                drawImageCover(g2, imgToDraw, artX, artY, artW, artH);
                if (!isEnabled() && !previewOnly) {
                    g2.setColor(new Color(30, 30, 30, 70));
                    g2.fillRect(artX, artY, artW, artH);
                }
            } else {
                g2.setPaint(new GradientPaint(artX, artY, spellColors[0], artX, artY + artH, spellColors[1]));
                g2.fillRoundRect(artX, artY, artW, artH, 10, 10);
                g2.setColor(new Color(255, 255, 255, 42));
                g2.fillOval(artX + 6, artY + 4, artW - 12, artH / 2);
            }
            g2.setClip(oldClip);
            g2.setColor(new Color(19, 30, 53, 130));
            g2.drawRoundRect(artX, artY, artW - 1, artH - 1, 10, 10);

            // Title ribbon
            int ribbonH = 24;
            int ribbonY = h - ribbonH - 12; // slight overlap into art area
            g2.setPaint(new GradientPaint(0, ribbonY, new Color(180, 86, 255), 0, ribbonY + ribbonH, new Color(88, 34, 176)));
            if (previewOnly) {
                g2.setPaint(new GradientPaint(0, ribbonY, new Color(96, 122, 169), 0, ribbonY + ribbonH, new Color(66, 88, 128)));
            }
            g2.fillRoundRect(8, ribbonY, w - 16, ribbonH, 10, 10);
            g2.setColor(new Color(214, 158, 255));
            g2.drawRoundRect(8, ribbonY, w - 17, ribbonH - 1, 10, 10);

            // Name text
            g2.setColor((!isEnabled() && !previewOnly) ? new Color(205, 205, 205) : Color.WHITE);
            int baseSize = previewOnly ? 13 : 14;
            Font nameFont = new Font("Arial", Font.BOLD, baseSize);
            g2.setFont(nameFont);
            String nm = (spellName == null || spellName.isEmpty()) ? "Spell" : spellName;
            int maxW = w - 22;
            FontMetrics fm = g2.getFontMetrics();
            while (fm.stringWidth(nm) > maxW && baseSize > 10) {
                baseSize--;
                nameFont = new Font("Arial", Font.BOLD, baseSize);
                g2.setFont(nameFont);
                fm = g2.getFontMetrics();
            }
            while (fm.stringWidth(nm) > maxW && nm.length() > 3) nm = nm.substring(0, nm.length() - 1);
            int nx = (w - fm.stringWidth(nm)) / 2;
            g2.drawString(nm, nx, ribbonY + 17);

            // Cost gem (top-left)
            if (!previewOnly) {
                int gemR = 16;
                g2.setPaint(new GradientPaint(0, 0, new Color(181, 99, 255), 0, gemR * 2, new Color(117, 45, 209)));
                g2.fillOval(-3, -3, gemR * 2, gemR * 2);
                g2.setColor(new Color(71, 27, 131));
                g2.drawOval(-3, -3, gemR * 2 - 1, gemR * 2 - 1);

                g2.setColor((!isEnabled() && !previewOnly) ? new Color(218, 218, 218) : Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                String costTxt = String.valueOf(spellCost);
                FontMetrics cfm = g2.getFontMetrics();
                g2.drawString(costTxt, 13 - cfm.stringWidth(costTxt) / 2, 18);
            }

            if (previewOnly) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                String nextTxt = "NEXT";
                FontMetrics nfm = g2.getFontMetrics();
                int nx2 = (w - nfm.stringWidth(nextTxt)) / 2;
                // subtle shadow + centered label
                g2.setColor(new Color(14, 31, 72, 210));
                g2.drawString(nextTxt, nx2 + 1, 15);
                g2.setColor(new Color(235, 244, 255));
                g2.drawString(nextTxt, nx2, 14);
            }

            long glazeCycleElapsedMs = glazeActive ? (nowMs - glazeStartedAtMs) % 10000L : 0L;
            if (selectedCard && glazeActive && isEnabled() && glazeCycleElapsedMs < 1400L) {
                Shape cardClip = new java.awt.geom.RoundRectangle2D.Float(1, 1, w - 2, h - 2, 18, 18);
                Shape previousClip = g2.getClip();
                g2.clip(cardClip);

                float glazePhase = glazeCycleElapsedMs / 1400f;
                float diagonalLength = w + h;
                float center = -42f + glazePhase * (diagonalLength + 84f);
                float halfBand = 18f;
                float s1 = center - halfBand;
                float s2 = center + halfBand;
                float reach = Math.max(w, h) + 60f;

                java.awt.geom.Path2D.Float band = new java.awt.geom.Path2D.Float();
                band.moveTo(s1 + reach, -reach);
                band.lineTo(s2 + reach, -reach);
                band.lineTo(s2 - reach, reach);
                band.lineTo(s1 - reach, reach);
                band.closePath();

                g2.setPaint(new LinearGradientPaint(
                    s1 / 2f, s1 / 2f,
                    s2 / 2f, s2 / 2f,
                    new float[]{0f, 0.48f, 0.52f, 1f},
                    new Color[]{
                        new Color(255, 255, 255, 0),
                        new Color(borderTheme[2].getRed(), borderTheme[2].getGreen(), borderTheme[2].getBlue(), 100),
                        new Color(255, 255, 255, 145),
                        new Color(255, 255, 255, 0)
                    }
                ));
                g2.fill(band);
                g2.setClip(previousClip);
            }
            g2.dispose();
        }

        private void drawImageCover(
                Graphics2D graphics,
                BufferedImage image,
                int x,
                int y,
                int width,
                int height) {
            double scale = Math.max(
                    width / (double) image.getWidth(),
                    height / (double) image.getHeight());
            int drawWidth = Math.max(1, (int) Math.ceil(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.ceil(image.getHeight() * scale));
            int drawX = x + (width - drawWidth) / 2;
            int drawY = y + (height - drawHeight) / 2;
            Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            if (previousInterpolation != null) {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
            }
        }
    }

    private void ensureSpellDeckInitialized() {
        if (!allSpellIds.isEmpty()) return;
        for (Spell s : board.getSpellDefinitions()) {
            allSpellIds.add(s.getId());
            spellById.put(s.getId(), s);
        }
        resetSpellDeckForSide(true);
        resetSpellDeckForSide(false);
    }

    private void resetSpellDeckForSide(boolean white) {
        List<String> active = white ? whiteActiveSpells : blackActiveSpells;
        active.clear();
        // Deterministic opening hand for both sides:
        // Enderman, Fireball, Uriel. First NEXT card: Fog.
        String[] opening = new String[] {
            SpellManager.ENDERMAN,
            SpellManager.FIREBALL,
            SpellManager.URIEL
        };
        for (String id : opening) {
            if (allSpellIds.contains(id) && !active.contains(id) && active.size() < PLAYABLE_SPELL_SLOTS) {
                active.add(id);
            }
        }
        // Fallback if any expected spell is unavailable.
        for (String id : allSpellIds) {
            if (active.size() >= PLAYABLE_SPELL_SLOTS) break;
            if (!active.contains(id)) active.add(id);
        }
        ensureHandSize(active);
        String next = SpellManager.FOG;
        if (next == null || !allSpellIds.contains(next) || active.contains(next)) {
            next = drawRandomCandidate(active, null);
        }
        if (white) whiteNextSpellId = next;
        else blackNextSpellId = next;
    }

    private void ensureHandSize(List<String> active) {
        while (active.size() < PLAYABLE_SPELL_SLOTS) {
            String candidate = drawRandomCandidate(active, null);
            if (candidate == null) break;
            if (!active.contains(candidate)) {
                active.add(candidate);
            } else {
                break;
            }
        }
        while (active.size() > PLAYABLE_SPELL_SLOTS) {
            active.remove(active.size() - 1);
        }
    }

    private BufferedImage loadSpellArt(String spellId) {
        if (spellId == null) return null;
        BufferedImage cached = spellArtCache.get(spellId);
        if (cached != null) return cached;

        String base = spellId.toLowerCase();
        String[] candidates = new String[] {
            "/assets/spells/" + base + "_inside.png",
            "/assets/spells/" + base + ".png",
            "/assets/spells/" + base + "_inside.jpg",
            "/assets/spells/" + base + ".jpg"
        };

        BufferedImage img = null;
        for (String p : candidates) {
            try {
                URL u = ChessGame.class.getResource(p);
                if (u != null) {
                    img = ImageIO.read(u);
                    break;
                }
            } catch (Exception ignored) { }
        }

        if (img == null) {
            String[] fileCandidates = new String[] {
                "Scaccomatto_final/Scaccomatto/src/assets/spells/" + base + "_inside.png",
                "Scaccomatto_final/Scaccomatto/src/assets/spells/" + base + ".png",
                "Scaccomatto_final/Scaccomatto/src/assets/spells/" + base + "_inside.jpg",
                "Scaccomatto_final/Scaccomatto/src/assets/spells/" + base + ".jpg",
                "src/assets/spells/" + base + "_inside.png",
                "src/assets/spells/" + base + ".png",
                "assets/spells/" + base + "_inside.png",
                "assets/spells/" + base + ".png"
            };
            for (String fp : fileCandidates) {
                try {
                    File f = new File(fp);
                    if (f.exists()) {
                        img = ImageIO.read(f);
                        break;
                    }
                } catch (Exception ignored) { }
            }
        }

        // Only cache successful loads so newly added files appear without restarting.
        if (img != null) spellArtCache.put(spellId, img);
        return img;
    }

    private BufferedImage loadSpellArtGray(String spellId) {
        if (spellId == null) return null;
        BufferedImage cached = spellArtGrayCache.get(spellId);
        if (cached != null) return cached;
        BufferedImage base = loadSpellArt(spellId);
        if (base == null) return null;
        try {
            BufferedImage gray = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = gray.createGraphics();
            g2.drawImage(base, 0, 0, null);
            g2.dispose();
            ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
            op.filter(gray, gray);
            spellArtGrayCache.put(spellId, gray);
            return gray;
        } catch (Exception ignored) {
            return base;
        }
    }

    private JPanel createSpellPanel(boolean forWhite) {
        ensureSpellDeckInitialized();
        SpellDeckPanel panel = new SpellDeckPanel();
        panel.setLayout(null);
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setPreferredSize(new Dimension(SPELL_PANEL_W, SPELL_PANEL_H));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        String boardLabel = forWhite ? "White's Elixir Board" : "Black's Elixir Board";
        JLabel elixirLabel = new JLabel(boardLabel, SwingConstants.CENTER);
        elixirLabel.setFont(new Font("Arial", Font.BOLD, 16));
        elixirLabel.setForeground(Color.WHITE);
        top.add(elixirLabel, BorderLayout.CENTER);
        panel.add(top);

        ElixirBar elixirBar = new ElixirBar();
        elixirBar.setPreferredSize(new Dimension(SPELL_BAR_W, SPELL_BAR_H));
        panel.add(elixirBar);

        JPanel cards = new JPanel();
        cards.setLayout(null);
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        cards.setPreferredSize(new Dimension(SPELL_CARDS_W, SPELL_CARDS_H));
        cards.setMinimumSize(new Dimension(SPELL_CARDS_W, SPELL_CARDS_H));
        cards.setMaximumSize(new Dimension(SPELL_CARDS_W, SPELL_CARDS_H));
        List<SpellCardButton> cardButtons = forWhite ? whiteCardButtons : blackCardButtons;
        cardButtons.clear();
        for (int i = 0; i < TOTAL_SPELL_SLOTS; i++) {
            SpellCardButton b = new SpellCardButton();
            b.setSpellData(null, null, i == NEXT_SLOT_INDEX);
            b.setFocusable(false);
            final int idx = i;
            if (i != NEXT_SLOT_INDEX) {
                b.addActionListener(e -> onSpellCardClicked(forWhite, idx));
            } else {
                b.setEnabled(false);
            }
            if (i == NEXT_SLOT_INDEX) {
                b.setPreferredSize(new Dimension(SPELL_NEXT_W, SPELL_NEXT_H));
                b.setMinimumSize(new Dimension(SPELL_NEXT_W, SPELL_NEXT_H));
                b.setMaximumSize(new Dimension(SPELL_NEXT_W, SPELL_NEXT_H));
            }
            b.setBounds(getSpellSlotBounds(i));
            cards.add(b);
            cardButtons.add(b);
        }
        panel.configure(top, elixirLabel, cards, elixirBar, cardButtons);
        if (forWhite) whiteNextCardPreview = null;
        else blackNextCardPreview = null;
        panel.add(cards);

        if (forWhite) {
            whiteSpellElixirLabel = elixirLabel;
            whiteElixirBar = elixirBar;
            whiteSpellPanel = panel;
        } else {
            blackSpellElixirLabel = elixirLabel;
            blackElixirBar = elixirBar;
            blackSpellPanel = panel;
        }

        startSpellUiAnimation();
        refreshSpellUI();
        return panel;
    }

    private void onSpellCardClicked(boolean forWhite, int cardIndex) {
        if (spellCardTransitionAnimating || spellResolutionLocked) return;
        List<String> active = forWhite ? whiteActiveSpells : blackActiveSpells;
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        if (cardIndex == NEXT_SLOT_INDEX) return;
        if (cardIndex < 0 || cardIndex >= active.size()) return;
        if (hasPendingSpellConfirmation()
                && !isPendingSpellCard(forWhite, cardIndex)) return;
        int selectedIndex = forWhite ? whiteSelectedCardIndex : blackSelectedCardIndex;

        if (selectedIndex != cardIndex) {
            setSelectedSpellCard(forWhite, cardIndex);
            return;
        }

        String spellId = active.get(cardIndex);
        SpellCardButton selectedButton = cardIndex < buttons.size() ? buttons.get(cardIndex) : null;
        if (selectedButton != null) selectedButton.setGlazeActive(true);
        String error = board.castSpellInteractiveForSide(spellId, forWhite);
        if (Board.SPELL_TARGETING_PENDING.equals(error)) {
            if (forWhite) {
                whitePendingTargetCardIndex = cardIndex;
                whitePendingTargetSpellId = spellId;
            } else {
                blackPendingTargetCardIndex = cardIndex;
                blackPendingTargetSpellId = spellId;
            }
            refreshSpellUI();
            board.repaint();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
            board.requestFocus();
            board.requestFocusInWindow();
            board.grabFocus();
            SwingUtilities.invokeLater(board::requestFocusInWindow);
            return;
        }
        if (error != null) {
            if (selectedButton != null) selectedButton.setGlazeActive(false);
            if (forWhite) {
                whitePendingTargetCardIndex = -1;
                whitePendingTargetSpellId = null;
            } else {
                blackPendingTargetCardIndex = -1;
                blackPendingTargetSpellId = null;
            }
            if (!"Cancelled.".equals(error)) {
                JOptionPane.showMessageDialog(dialogOwner(), error, "Spell", JOptionPane.WARNING_MESSAGE);
            }
            refreshSpellUI();
            return;
        }

        beginSpellResolutionLock(spellId);
        startSpellTravelAnimation(selectedButton);
        if (forWhite) {
            whitePendingTargetCardIndex = -1;
            whitePendingTargetSpellId = null;
        } else {
            blackPendingTargetCardIndex = -1;
            blackPendingTargetSpellId = null;
        }
        animateAndApplySpellCardRotation(forWhite, spellId, cardIndex, true);
        clearSelectedSpellCards();
    }

    private boolean hasPendingSpellConfirmation() {
        return whitePendingTargetCardIndex >= 0 || blackPendingTargetCardIndex >= 0;
    }

    private boolean isPendingSpellCard(boolean white, int cardIndex) {
        return white
            ? whitePendingTargetCardIndex == cardIndex
            : blackPendingTargetCardIndex == cardIndex;
    }

    private void beginSpellResolutionLock(String spellId) {
        if (spellResolutionLocked) return;
        spellResolutionLocked = true;
        spellResolutionWaitsForFireballTimer = SpellManager.FIREBALL.equals(spellId);
        spellResolutionHoldMs = SpellManager.FREEZE.equals(spellId)
                ? SPELL_RESOLUTION_HOLD_MS + FREEZE_TRAVEL_HOLD_MS - SPELL_TRAVEL_HOLD_MS
                : SPELL_RESOLUTION_HOLD_MS;
        resumeClockAfterSpellResolution = timer != null && timer.isRunning();
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (spellResolutionReleaseTimer != null) {
            spellResolutionReleaseTimer.stop();
            spellResolutionReleaseTimer = null;
        }
        if (board != null) board.setInputEnabled(false, false);
        animateClockPauseOverlay(1f, CLOCK_OVERLAY_FADE_MS, null);
        refreshSpellUI();
        if (spellResolutionWaitsForFireballTimer) {
            spellResolutionReleaseTimer = new Timer(FIREBALL_ANIMATION_LOCK_MS, e -> {
                ((Timer) e.getSource()).stop();
                spellResolutionReleaseTimer = null;
                spellResolutionWaitsForFireballTimer = false;
                finishSpellResolutionAfterCardAnimation();
            });
            spellResolutionReleaseTimer.setRepeats(false);
            spellResolutionReleaseTimer.start();
        }
    }

    private void finishSpellResolutionAfterCardAnimation() {
        if (!spellResolutionLocked) return;
        if (spellResolutionWaitsForFireballTimer) return;
        if (spellResolutionReleaseTimer != null) spellResolutionReleaseTimer.stop();

        int fadeDelay = Math.max(0, spellResolutionHoldMs - CLOCK_OVERLAY_FADE_MS);
        spellResolutionReleaseTimer = new Timer(fadeDelay, e -> {
            ((Timer) e.getSource()).stop();
            animateClockPauseOverlay(0f, CLOCK_OVERLAY_FADE_MS, this::releaseSpellResolutionLock);
        });
        spellResolutionReleaseTimer.setRepeats(false);
        spellResolutionReleaseTimer.start();
    }

    private void releaseSpellResolutionLock() {
        spellResolutionReleaseTimer = null;
        spellResolutionLocked = false;
        spellResolutionWaitsForFireballTimer = false;
        spellResolutionHoldMs = SPELL_RESOLUTION_HOLD_MS;
        if (board != null && gameStarted && !spectateMode && !gameOver) {
            board.setInputEnabled(true);
        }
        if (resumeClockAfterSpellResolution && !gameOver
                && whiteTimeRemaining > 0 && blackTimeRemaining > 0) {
            timerStarted = true;
            startTimer();
        }
        resumeClockAfterSpellResolution = false;
        refreshSpellUI();
    }

    private void cancelSpellResolutionLock() {
        stopSpellTravelAnimation();
        if (spellResolutionReleaseTimer != null) {
            spellResolutionReleaseTimer.stop();
            spellResolutionReleaseTimer = null;
        }
        if (clockOverlayFadeTimer != null) {
            clockOverlayFadeTimer.stop();
            clockOverlayFadeTimer = null;
        }
        spellResolutionLocked = false;
        spellResolutionWaitsForFireballTimer = false;
        spellResolutionHoldMs = SPELL_RESOLUTION_HOLD_MS;
        resumeClockAfterSpellResolution = false;
        clockPauseOverlayAlpha = 0f;
        setClockPauseOverlayAlpha(0f);
    }

    private void animateClockPauseOverlay(float target, int durationMs, Runnable done) {
        if (clockOverlayFadeTimer != null) clockOverlayFadeTimer.stop();
        float startAlpha = clockPauseOverlayAlpha;
        long startedAtNanos = System.nanoTime();
        clockOverlayFadeTimer = AnimationTiming.createUiTimer(null);
        clockOverlayFadeTimer.addActionListener(e -> {
            float t = AnimationTiming.progress(startedAtNanos, durationMs);
            float eased = t * t * (3f - 2f * t);
            clockPauseOverlayAlpha = startAlpha + (target - startAlpha) * eased;
            setClockPauseOverlayAlpha(clockPauseOverlayAlpha);
            if (t >= 1f) {
                clockOverlayFadeTimer.stop();
                clockOverlayFadeTimer = null;
                clockPauseOverlayAlpha = target;
                setClockPauseOverlayAlpha(target);
                if (done != null) done.run();
            }
        });
        clockOverlayFadeTimer.start();
    }

    private void setClockPauseOverlayAlpha(float alpha) {
        if (topClockLabel instanceof RoundedClockLabel) {
            ((RoundedClockLabel) topClockLabel).setPauseOverlayAlpha(alpha);
        }
        if (bottomClockLabel instanceof RoundedClockLabel) {
            ((RoundedClockLabel) bottomClockLabel).setPauseOverlayAlpha(alpha);
        }
    }

    private void applySpellCardRotation(boolean forWhite, String spellId, int cardIndex, boolean preserveCard, boolean pulse) {
        List<String> active = forWhite ? whiteActiveSpells : blackActiveSpells;
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        if (cardIndex < 0 || cardIndex >= active.size()) return;
        if (!preserveCard) {
            String replacement = forWhite ? whiteNextSpellId : blackNextSpellId;
            if (replacement == null || active.contains(replacement)) {
                replacement = drawRandomCandidate(active, spellId);
            }
            active.remove(cardIndex);
            active.add(replacement);
            String next = drawRandomCandidate(active, null);
            if (forWhite) whiteNextSpellId = next;
            else blackNextSpellId = next;
        }
        if (pulse && (PLAYABLE_SPELL_SLOTS - 1) < buttons.size()) {
            buttons.get(PLAYABLE_SPELL_SLOTS - 1).pulsePop();
        }
    }

    private static int spellScaled(int value, double scale) {
        return Math.max(1, (int) Math.round(value * scale));
    }

    private Rectangle getSpellSlotBounds(int slotIndex) {
        return getSpellSlotBounds(slotIndex, 1.0);
    }

    private Rectangle getSpellSlotBounds(int slotIndex, double scale) {
        int x = SPELL_CARDS_START_X + slotIndex * (SPELL_CARD_W + SPELL_CARD_GAP);
        int y = SPELL_CARD_Y;
        int w = SPELL_CARD_W;
        int h = SPELL_CARD_H;
        if (slotIndex == NEXT_SLOT_INDEX) {
            w = SPELL_NEXT_W;
            h = SPELL_NEXT_H;
            x += (SPELL_CARD_W - SPELL_NEXT_W) / 2;
            y += (SPELL_CARD_H - SPELL_NEXT_H) / 2;
        }
        return new Rectangle(
                spellScaled(x, scale),
                spellScaled(y, scale),
                spellScaled(w, scale),
                spellScaled(h, scale));
    }

    private double getSpellPanelScale(boolean forWhite) {
        JPanel panel = forWhite ? whiteSpellPanel : blackSpellPanel;
        return panel instanceof SpellDeckPanel
                ? ((SpellDeckPanel) panel).getUiScale()
                : 1.0;
    }

    private void animateAndApplySpellCardRotation(boolean forWhite, String spellId, int usedIndex, boolean pulse) {
        List<String> active = forWhite ? whiteActiveSpells : blackActiveSpells;
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        boolean preserveCard = board.consumeSpellCardPreserveRequest();
        if (usedIndex < 0 || usedIndex >= active.size()) {
            applySpellCardRotation(forWhite, spellId, usedIndex, preserveCard, pulse);
            refreshSpellUI();
            board.repaint();
            finishSpellResolutionAfterCardAnimation();
            return;
        }
        if (spellCardTransitionAnimating || usedIndex >= PLAYABLE_SPELL_SLOTS || buttons.size() <= NEXT_SLOT_INDEX) {
            applySpellCardRotation(forWhite, spellId, usedIndex, preserveCard, pulse);
            refreshSpellUI();
            board.repaint();
            finishSpellResolutionAfterCardAnimation();
            return;
        }

        if (preserveCard) {
            applySpellCardRotation(forWhite, spellId, usedIndex, true, pulse);
            refreshSpellUI();
            board.repaint();
            finishSpellResolutionAfterCardAnimation();
            return;
        }

        spellCardTransitionAnimating = true;
        SpellCardButton used = buttons.get(usedIndex);
        used.setVisible(false);

        List<SpellCardButton> movers = new ArrayList<>();
        List<Rectangle> from = new ArrayList<>();
        List<Rectangle> to = new ArrayList<>();
        double panelScale = getSpellPanelScale(forWhite);
        for (int i = usedIndex + 1; i < PLAYABLE_SPELL_SLOTS; i++) {
            SpellCardButton b = buttons.get(i);
            movers.add(b);
            from.add(b.getBounds());
            to.add(getSpellSlotBounds(i - 1, panelScale));
        }
        SpellCardButton nextBtn = buttons.get(NEXT_SLOT_INDEX);
        movers.add(nextBtn);
        from.add(nextBtn.getBounds());
        to.add(getSpellSlotBounds(PLAYABLE_SPELL_SLOTS - 1, panelScale));

        final long startNanos = System.nanoTime();
        final int durationMs = SPELL_CARD_TRANSITION_MS;
        Timer t = new Timer(AnimationTiming.FRAME_DELAY_MS, null);
        t.addActionListener(e -> {
            float elapsedMs = (System.nanoTime() - startNanos) / 1_000_000f;
            float p = Math.min(1f, elapsedMs / durationMs);
            float eased = 1f - (float) Math.pow(1f - p, 3.0);
            for (int i = 0; i < movers.size(); i++) {
                Rectangle a = from.get(i), b = to.get(i);
                int x = Math.round(a.x + (b.x - a.x) * eased);
                int y = Math.round(a.y + (b.y - a.y) * eased);
                int w = Math.round(a.width + (b.width - a.width) * eased);
                int h = Math.round(a.height + (b.height - a.height) * eased);
                movers.get(i).setBounds(x, y, w, h);
            }
            if (p >= 1f) {
                t.stop();
                double finalScale = getSpellPanelScale(forWhite);
                for (int i = 0; i < buttons.size(); i++) {
                    buttons.get(i).setBounds(getSpellSlotBounds(i, finalScale));
                }
                used.setVisible(true);
                applySpellCardRotation(forWhite, spellId, usedIndex, false, pulse);
                spellCardTransitionAnimating = false;
                refreshSpellUI();
                board.repaint();
                finishSpellResolutionAfterCardAnimation();
            } else {
                board.repaint();
            }
        });
        t.setCoalesce(true);
        t.setInitialDelay(0);
        t.start();
    }

    public void onBoardSpellCastResolved(String spellId, boolean forWhite) {
        if (!"Spell Chess".equalsIgnoreCase(gameVariant)) return;
        beginSpellResolutionLock(spellId);
        List<String> active = forWhite ? whiteActiveSpells : blackActiveSpells;
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        int selectedIndex = forWhite ? whitePendingTargetCardIndex : blackPendingTargetCardIndex;
        String pendingSpellId = forWhite ? whitePendingTargetSpellId : blackPendingTargetSpellId;
        if (selectedIndex < 0 || selectedIndex >= active.size() || pendingSpellId == null || !spellId.equals(pendingSpellId)) {
            selectedIndex = forWhite ? whiteSelectedCardIndex : blackSelectedCardIndex;
            if (selectedIndex < 0 || selectedIndex >= active.size() || !spellId.equals(active.get(selectedIndex))) {
                selectedIndex = -1;
                for (int i = 0; i < active.size(); i++) {
                    if (spellId.equals(active.get(i))) {
                        selectedIndex = i;
                        break;
                    }
                }
                if (selectedIndex < 0) {
                    if (forWhite) {
                        whitePendingTargetCardIndex = -1;
                        whitePendingTargetSpellId = null;
                    } else {
                        blackPendingTargetCardIndex = -1;
                        blackPendingTargetSpellId = null;
                    }
                    refreshSpellUI();
                    board.repaint();
                    finishSpellResolutionAfterCardAnimation();
                    return;
                }
            }
        }
        SpellCardButton resolvedButton = selectedIndex >= 0 && selectedIndex < buttons.size()
                ? buttons.get(selectedIndex) : null;
        startSpellTravelAnimation(resolvedButton);
        animateAndApplySpellCardRotation(forWhite, spellId, selectedIndex, true);
        if (forWhite) {
            whitePendingTargetCardIndex = -1;
            whitePendingTargetSpellId = null;
        } else {
            blackPendingTargetCardIndex = -1;
            blackPendingTargetSpellId = null;
        }
        clearSelectedSpellCards();
    }

    private void startSpellTravelAnimation(SpellCardButton card) {
        if (card == null || board == null || !card.isShowing() || !board.isShowing()) {
            if (board != null) board.releaseDeferredSpellVisualEffects();
            return;
        }
        JLayeredPane layered = appHost != null ? appHost.getLayeredPane() : getLayeredPane();
        if (layered == null || layered.getWidth() <= 0 || layered.getHeight() <= 0) {
            board.releaseDeferredSpellVisualEffects();
            return;
        }

        stopSpellTravelAnimation();
        BufferedImage snapshot = new BufferedImage(
                Math.max(1, card.getWidth()), Math.max(1, card.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D snapshotGraphics = snapshot.createGraphics();
        snapshotGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        card.paint(snapshotGraphics);
        snapshotGraphics.dispose();

        Point start = SwingUtilities.convertPoint(
                card, card.getWidth() / 2, card.getHeight() / 2, layered);
        Point center = SwingUtilities.convertPoint(
                board, board.getWidth() / 2, board.getHeight() / 2, layered);
        Point boardTopLeft = SwingUtilities.convertPoint(board, 0, 0, layered);
        boolean cardStartsBelowBoard = start.y > center.y;
        Point exit = cardStartsBelowBoard
                ? new Point(boardTopLeft.x + 24, boardTopLeft.y + 24)
                : new Point(boardTopLeft.x + 24, boardTopLeft.y + board.getHeight() - 24);
        long startedAtNanos = System.nanoTime();
        boolean freezeAtCenter = SpellManager.FREEZE.equals(card.spellId);
        boolean fogRipple = SpellManager.FOG.equals(card.spellId);
        boolean fireballCard = SpellManager.FIREBALL.equals(card.spellId);
        boolean endermanCard = SpellManager.ENDERMAN.equals(card.spellId);
        Point endermanBoardTarget = endermanCard ? board.getEndermanCardTargetPoint() : null;
        Point endermanTarget = endermanBoardTarget == null
                ? center
                : SwingUtilities.convertPoint(
                        board, endermanBoardTarget.x, endermanBoardTarget.y, layered);
        boolean releaseAtCenter = freezeAtCenter || fogRipple;
        boolean fixedAtCenter = releaseAtCenter || endermanCard;
        boolean[] spellVisualsReleased = {false};
        boolean[] fireballPlaneStarted = {false};
        boolean[] endermanTeleportCompleted = {false};
        boolean[] endermanWhooshPlayed = {false};
        int travelHoldMs = freezeAtCenter
                ? FREEZE_TRAVEL_HOLD_MS
                : fogRipple ? FOG_RIPPLE_HOLD_MS
                : endermanCard ? ENDERMAN_TRAVEL_HOLD_MS
                : SPELL_TRAVEL_HOLD_MS;
        int travelOutMs = fogRipple
                ? FOG_CARD_SHRINK_MS
                : endermanCard ? ENDERMAN_ABSORB_MS
                : SPELL_TRAVEL_OUT_MS;
        int travelTotalMs = SPELL_TRAVEL_IN_MS + travelHoldMs + travelOutMs;
        int repaintPadding = Math.round(Math.max(snapshot.getWidth(), snapshot.getHeight()) * 1.3f) + 32;
        int minAnimationX = Math.min(
                Math.min(Math.min(start.x, center.x - 165), exit.x), endermanTarget.x) - repaintPadding;
        int minAnimationY = Math.min(
                Math.min(Math.min(start.y, center.y - 150), exit.y), endermanTarget.y) - repaintPadding;
        int maxAnimationX = Math.max(
                Math.max(Math.max(start.x, center.x + 20), exit.x), endermanTarget.x) + repaintPadding;
        int maxAnimationY = Math.max(
                Math.max(Math.max(start.y, center.y + 150), exit.y), endermanTarget.y) + repaintPadding;
        Rectangle animationRepaintBounds = new Rectangle(
                Math.max(0, minAnimationX),
                Math.max(0, minAnimationY),
                Math.min(layered.getWidth(), maxAnimationX) - Math.max(0, minAnimationX),
                Math.min(layered.getHeight(), maxAnimationY) - Math.max(0, minAnimationY));

        spellTravelOverlay = new JComponent() {
            @Override
            public boolean contains(int x, int y) {
                return false;
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                float elapsed = (System.nanoTime() - startedAtNanos) / 1_000_000f;
                if (elapsed < 0 || elapsed >= travelTotalMs) return;

                float x;
                float y;
                float scale;
                float horizontalFlip = 1f;
                float alpha = 1f;
                float fogCharge = 0f;
                if (elapsed < SPELL_TRAVEL_IN_MS) {
                    float t = elapsed / SPELL_TRAVEL_IN_MS;
                    float eased = smootherStep(t);
                    float approachControlY = cardStartsBelowBoard
                            ? center.y + 115f : center.y - 115f;
                    x = quadraticCurve(start.x, center.x - 125f, center.x, eased);
                    y = quadraticCurve(start.y, approachControlY, center.y, eased);
                    float waveEnvelope = (float) Math.sin(Math.PI * t);
                    float wave = (float) Math.sin(t * Math.PI * 2.0) * 17f * waveEnvelope;
                    x += wave;
                    y += wave * (cardStartsBelowBoard ? -0.42f : 0.42f);
                    scale = 1f + 1.05f * smootherStep(t);
                    float entranceFlipT = smootherStep(Math.min(1f, t / 0.88f));
                    horizontalFlip = (float) Math.cos(entranceFlipT * Math.PI * 2.0);
                } else if (elapsed < SPELL_TRAVEL_IN_MS + travelHoldMs) {
                    float t = (elapsed - SPELL_TRAVEL_IN_MS) / travelHoldMs;
                    x = center.x;
                    y = fixedAtCenter
                            ? center.y
                            : center.y + (float) Math.sin(t * Math.PI * 2.0) * 4f;
                    scale = 2.05f + (float) Math.sin(t * Math.PI) * 0.05f;
                    if (fogRipple) {
                        float centerElapsed = elapsed - SPELL_TRAVEL_IN_MS;
                        float chargeT = Math.min(1f, centerElapsed / FOG_CHARGE_MS);
                        fogCharge = smootherStep(chargeT);
                        float growthDurationMs = FOG_CHARGE_MS - FOG_GROWTH_HOLD_BEFORE_RIPPLE_MS;
                        float growthT = Math.min(1f, centerElapsed / growthDurationMs);
                        float growthStrength = smootherStep(growthT);
                        scale += growthStrength * 0.28f;
                        if (centerElapsed < FOG_CHARGE_MS) {
                            float shakeRadius = 0.20f + growthStrength * 1.85f;
                            float shakeSpeed = 0.16f + growthStrength * 0.22f;
                            x += (float) Math.sin(centerElapsed * shakeSpeed) * shakeRadius;
                            y += (float) Math.sin(
                                    centerElapsed * (shakeSpeed * 1.37f) + 0.8f)
                                    * shakeRadius * 0.78f;
                        }
                    }
                    if (freezeAtCenter) {
                        float firingWindow = Math.min(1f, t / 0.66f);
                        float shotPhase = firingWindow * 5f;
                        float withinShot = shotPhase - (float) Math.floor(shotPhase);
                        float shotEnvelope = 0f;
                        if (firingWindow < 1f) {
                            if (withinShot < 0.16f) {
                                shotEnvelope = smootherStep(withinShot / 0.16f);
                            } else if (withinShot < 0.42f) {
                                shotEnvelope = 1f - smootherStep((withinShot - 0.16f) / 0.26f);
                            }
                        }
                        scale += shotEnvelope * 0.24f;
                    }
                } else {
                    float t = (elapsed - SPELL_TRAVEL_IN_MS - travelHoldMs)
                            / travelOutMs;
                    if (fogRipple) {
                        x = center.x;
                        y = center.y;
                        fogCharge = 1f;
                        scale = 2.33f + (0.02f - 2.33f) * smootherStep(t);
                        alpha = 1f - smootherStep(Math.max(0f, (t - 0.35f) / 0.65f));
                    } else if (endermanCard) {
                        float eased = smootherStep(t);
                        float dx = endermanTarget.x - center.x;
                        float dy = endermanTarget.y - center.y;
                        float length = Math.max(1f, (float) Math.hypot(dx, dy));
                        float bend = Math.min(95f, length * 0.24f);
                        float controlX = (center.x + endermanTarget.x) * 0.5f - dy / length * bend;
                        float controlY = (center.y + endermanTarget.y) * 0.5f + dx / length * bend;
                        x = quadraticCurve(center.x, controlX, endermanTarget.x, eased);
                        y = quadraticCurve(center.y, controlY, endermanTarget.y, eased);
                        float waveEnvelope = (float) Math.sin(Math.PI * t);
                        x += (float) Math.sin(t * Math.PI * 3.0) * 7f * waveEnvelope;
                        y += (float) Math.cos(t * Math.PI * 3.0) * 5f * waveEnvelope;
                        float shrinkT = smootherStep(Math.max(0f, (t - 0.12f) / 0.88f));
                        scale = 2.05f + (0.08f - 2.05f) * shrinkT;
                        if (t > 0.82f) {
                            alpha = 1f - smootherStep((t - 0.82f) / 0.18f);
                        }
                    } else {
                        float eased = smootherStep(t);
                        float exitControlY = cardStartsBelowBoard
                                ? center.y - 150f : center.y + 150f;
                        x = quadraticCurve(center.x, center.x - 165f, exit.x, eased);
                        y = quadraticCurve(center.y, exitControlY, exit.y, eased);
                        float waveEnvelope = (float) Math.sin(Math.PI * t);
                        float wave = (float) Math.sin(t * Math.PI * 2.0) * 19f * waveEnvelope;
                        x += wave * 0.55f;
                        y += wave * (cardStartsBelowBoard ? -1f : 1f);
                        scale = 2.05f + (0.16f - 2.05f) * smootherStep(t);
                        horizontalFlip = (float) Math.cos(smootherStep(t) * Math.PI * 2.0);
                        if (t > 0.72f) alpha = 1f - smootherStep((t - 0.72f) / 0.28f);
                    }
                }

                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if (endermanCard && elapsed >= SPELL_TRAVEL_IN_MS + travelHoldMs) {
                    float absorbT = Math.max(0f, Math.min(1f,
                            (elapsed - SPELL_TRAVEL_IN_MS - travelHoldMs) / travelOutMs));
                    drawEndermanAbsorptionFx(g2, endermanTarget.x, endermanTarget.y, absorbT);
                }
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.translate(x, y);
                g2.scale(scale * horizontalFlip, scale);

                int w = snapshot.getWidth();
                int h = snapshot.getHeight();
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(-w / 2 + 6, -h / 2 + 9, w, h, 18, 18);
                g2.drawImage(snapshot, -w / 2, -h / 2, null);
                if (fogCharge > 0f) {
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_ATOP, Math.min(0.88f, fogCharge * 0.88f)));
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(-w / 2, -h / 2, w, h, 18, 18);
                }
                g2.dispose();
            }
        };
        spellTravelOverlay.setOpaque(false);
        spellTravelOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(spellTravelOverlay, JLayeredPane.DRAG_LAYER);
        layered.moveToFront(spellTravelOverlay);

        spellTravelTimer = new Timer(SPELL_TRAVEL_FRAME_MS, e -> {
            float elapsed = (System.nanoTime() - startedAtNanos) / 1_000_000f;
            if (spellTravelOverlay != null) {
                if (spellTravelOverlay.getWidth() != layered.getWidth()
                        || spellTravelOverlay.getHeight() != layered.getHeight()) {
                    spellTravelOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
                }
                spellTravelOverlay.repaint(
                        animationRepaintBounds.x,
                        animationRepaintBounds.y,
                        animationRepaintBounds.width,
                        animationRepaintBounds.height);
            }
            if (releaseAtCenter && !spellVisualsReleased[0] && elapsed >= SPELL_TRAVEL_IN_MS) {
                spellVisualsReleased[0] = true;
                board.releaseDeferredSpellVisualEffects();
            }
            if (fireballCard && !fireballPlaneStarted[0] && elapsed >= travelTotalMs - 500f) {
                fireballPlaneStarted[0] = true;
                board.prestartFireballPlaneSound();
            }
            if (endermanCard && !endermanWhooshPlayed[0] && elapsed >= travelTotalMs - 600f) {
                endermanWhooshPlayed[0] = true;
                board.playEndermanAbsorbWhoosh();
            }
            if (elapsed >= travelTotalMs) {
                stopSpellTravelAnimation();
                if (endermanCard && !endermanTeleportCompleted[0]) {
                    endermanTeleportCompleted[0] = true;
                    board.completePendingEndermanTeleport();
                }
                if (!spellVisualsReleased[0]) {
                    spellVisualsReleased[0] = true;
                    board.releaseDeferredSpellVisualEffects();
                }
            }
        });
        spellTravelTimer.setCoalesce(true);
        spellTravelTimer.setInitialDelay(0);
        spellTravelTimer.start();
    }

    private static void drawEndermanAbsorptionFx(
            Graphics2D source, float targetX, float targetY, float progress) {
        if (progress < 0.52f) return;
        float impactT = Math.max(0f, Math.min(1f, (progress - 0.52f) / 0.48f));
        float pulse = (float) Math.sin(Math.PI * impactT);
        Graphics2D g = (Graphics2D) source.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float outerRadius = 18f + pulse * 54f;
        g.setStroke(new BasicStroke(3f + pulse * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(174, 91, 255, Math.round(150f * pulse)));
        g.drawOval(
                Math.round(targetX - outerRadius),
                Math.round(targetY - outerRadius),
                Math.round(outerRadius * 2f),
                Math.round(outerRadius * 2f));

        for (int i = 0; i < 18; i++) {
            double angle = i * Math.PI * 2.0 / 18.0 + impactT * 1.8;
            float distance = (1f - impactT) * (32f + (i % 5) * 7f);
            int size = 3 + i % 4;
            int px = Math.round(targetX + (float) Math.cos(angle) * distance);
            int py = Math.round(targetY + (float) Math.sin(angle) * distance);
            g.setColor(new Color(
                    135 + (i % 3) * 24,
                    52,
                    225 + (i % 2) * 25,
                    Math.round(205f * pulse)));
            g.fillRect(px - size / 2, py - size / 2, size, size);
        }
        g.dispose();
    }

    private void stopSpellTravelAnimation() {
        if (spellTravelTimer != null) {
            spellTravelTimer.stop();
            spellTravelTimer = null;
        }
        if (spellTravelOverlay != null) {
            Container parent = spellTravelOverlay.getParent();
            if (parent != null) {
                parent.remove(spellTravelOverlay);
                parent.revalidate();
                parent.repaint();
            }
            spellTravelOverlay = null;
        }
    }

    private static float quadraticCurve(float start, float control, float end, float t) {
        float inverse = 1f - t;
        return inverse * inverse * start + 2f * inverse * t * control + t * t * end;
    }

    private static float smootherStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    public void cancelPendingSpellSelection(boolean forWhite, String spellId) {
        List<String> active = forWhite ? whiteActiveSpells : blackActiveSpells;
        int selectedIndex = forWhite ? whiteSelectedCardIndex : blackSelectedCardIndex;
        if (selectedIndex >= 0 && selectedIndex < active.size() && spellId.equals(active.get(selectedIndex))) {
            clearSelectedSpellCards();
        }
        if (forWhite) {
            if (spellId.equals(whitePendingTargetSpellId)) {
                whitePendingTargetCardIndex = -1;
                whitePendingTargetSpellId = null;
            }
        } else {
            if (spellId.equals(blackPendingTargetSpellId)) {
                blackPendingTargetCardIndex = -1;
                blackPendingTargetSpellId = null;
            }
        }
        refreshSpellUI();
        board.repaint();
    }

    public void cancelAllSpellSelections() {
        whitePendingTargetCardIndex = -1;
        blackPendingTargetCardIndex = -1;
        whitePendingTargetSpellId = null;
        blackPendingTargetSpellId = null;
        clearSelectedSpellCards();
        refreshSpellUI();
        if (board != null) board.repaint();
    }

    private void bindGameHotkeys() {
        if (board == null) return;
        JComponent hotkeyTarget = appHost == null ? getRootPane() : embeddedHotkeyTarget;
        if (hotkeyTarget == null) hotkeyTarget = board;
        InputMap im = hotkeyTarget.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = hotkeyTarget.getActionMap();
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1, 0), "spellCard1Hotkey");
        am.put("spellCard1Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) {
                    selectTimePresetByHotkey(0);
                    return;
                }
                selectSpellCardByHotkey(0);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_2, 0), "spellCard2Hotkey");
        am.put("spellCard2Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) {
                    selectTimePresetByHotkey(1);
                    return;
                }
                selectSpellCardByHotkey(1);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_3, 0), "spellCard3Hotkey");
        am.put("spellCard3Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) {
                    selectTimePresetByHotkey(2);
                    return;
                }
                selectSpellCardByHotkey(2);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_4, 0), "timePreset4Hotkey");
        am.put("timePreset4Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(3);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_5, 0), "timePreset5Hotkey");
        am.put("timePreset5Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(4);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_6, 0), "timePreset6Hotkey");
        am.put("timePreset6Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(5);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_7, 0), "timePreset7Hotkey");
        am.put("timePreset7Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(6);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_8, 0), "timePreset8Hotkey");
        am.put("timePreset8Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(7);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_9, 0), "timePreset9Hotkey");
        am.put("timePreset9Hotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) selectTimePresetByHotkey(8);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0), "presetGridUpHotkey");
        am.put("presetGridUpHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) navigateTimePresetGrid(-1, 0);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0), "presetGridDownHotkey");
        am.put("presetGridDownHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) navigateTimePresetGrid(1, 0);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0), "presetGridLeftHotkey");
        am.put("presetGridLeftHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) navigateTimePresetGrid(0, -1);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0), "presetGridRightHotkey");
        am.put("presetGridRightHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isTimeControlHotkeyScreenActive()) navigateTimePresetGrid(0, 1);
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "startGameHotkey");
        am.put("startGameHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (confirmSelectedSpellByHotkey()) return;
                if (startGameBtn != null && startGameBtn.isVisible() && startGameBtn.isEnabled()) {
                    startGameBtn.doClick();
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK), "resetBoardHotkey");
        am.put("resetBoardHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (resetBoardBtn != null && resetBoardBtn.isVisible() && resetBoardBtn.isEnabled()) {
                    resetBoardBtn.doClick();
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_A,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK
        ), "toggleAnimationsHotkey");
        am.put("toggleAnimationsHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.toggleAnimations();
            }
        });
        im.put(KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_F,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK
        ), "toggleAutoFlipHotkey");
        am.put("toggleAutoFlipHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.toggleAutoFlip();
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "flipBoardHotkey");
        am.put("flipBoardHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.flipBoard();
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK), "toggleCoordinatesHotkey");
        am.put("toggleCoordinatesHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.toggleShowCoordinates();
            }
        });
        im.put(KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_DOWN,
            java.awt.event.InputEvent.SHIFT_DOWN_MASK
        ), "expandSettingsDropdownHotkey");
        am.put("expandSettingsDropdownHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.expandDropdown();
            }
        });
        im.put(KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_UP,
            java.awt.event.InputEvent.SHIFT_DOWN_MASK
        ), "collapseSettingsDropdownHotkey");
        am.put("collapseSettingsDropdownHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (settingsPanel != null) settingsPanel.collapseDropdown();
            }
        });
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancelCardConfirmHotkey");
        am.put("cancelCardConfirmHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (board != null && board.cancelKeyboardTabSelection()) return;
                if (!"Spell Chess".equalsIgnoreCase(gameVariant)) return;
                if (board != null) board.cancelActiveSpellInteraction();
                cancelAllSpellSelections();
            }
        });
        im.put(KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_ESCAPE,
            java.awt.event.InputEvent.SHIFT_DOWN_MASK
        ), "exitToMenuHotkey");
        am.put("exitToMenuHotkey", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (exitToMenuBtn != null && exitToMenuBtn.isVisible() && exitToMenuBtn.isEnabled()) {
                    exitToMenuBtn.doClick();
                }
            }
        });
    }

    private void syncBoardEnterHotkeyMode() {
        if (board == null) return;
        InputMap boardMap = board.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap boardActions = board.getActionMap();
        if (boardMap == null || boardActions == null) return;

        boardActions.put("preGameStartHotkeyForward", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!gameStarted && !onlineMode && !isBotGame
                        && startGameBtn != null && startGameBtn.isVisible() && startGameBtn.isEnabled()) {
                    startGameBtn.doClick();
                }
            }
        });

        if (!gameStarted && !onlineMode && !isBotGame) {
            boardMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "preGameStartHotkeyForward");
        } else {
            boardMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "legalMoveEnterActivate");
        }
    }

    private void selectSpellCardByHotkey(int cardIndex) {
        if (board == null || cardIndex < 0 || cardIndex >= PLAYABLE_SPELL_SLOTS) return;
        if (!"Spell Chess".equalsIgnoreCase(gameVariant)) return;
        boolean forWhite = board.isWhiteTurn();
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        if (cardIndex >= buttons.size()) return;
        SpellCardButton btn = buttons.get(cardIndex);
        if (btn == null || !btn.isEnabled()) return;
        onSpellCardClicked(forWhite, cardIndex);
    }

    private boolean isTimeControlHotkeyScreenActive() {
        return !gameStarted
            && !onlineMode
            && !isBotGame
            && startGameBtn != null
            && startGameBtn.isVisible()
            && startGameBtn.isEnabled();
    }

    private void selectTimePresetByHotkey(int presetIndex) {
        if (!isTimeControlHotkeyScreenActive()) return;
        if (timePresetButtons == null || presetIndex < 0 || presetIndex >= timePresetButtons.size()) return;
        JButton btn = timePresetButtons.get(presetIndex);
        if (btn == null || !btn.isEnabled() || !btn.isVisible()) return;
        Object presetObj = btn.getClientProperty("preset");
        if (!(presetObj instanceof TimePreset)) return;
        selectPreset((TimePreset) presetObj);
        btn.requestFocusInWindow();
    }

    private boolean confirmSelectedSpellByHotkey() {
        if (board == null) return false;
        if (!"Spell Chess".equalsIgnoreCase(gameVariant)) return false;
        boolean forWhite = board.isWhiteTurn();
        int selectedIndex = forWhite ? whiteSelectedCardIndex : blackSelectedCardIndex;
        if (selectedIndex < 0 || selectedIndex >= PLAYABLE_SPELL_SLOTS) return false;
        List<SpellCardButton> buttons = forWhite ? whiteCardButtons : blackCardButtons;
        if (selectedIndex >= buttons.size()) return false;
        SpellCardButton btn = buttons.get(selectedIndex);
        if (btn == null || !btn.isEnabled()) return false;
        onSpellCardClicked(forWhite, selectedIndex);
        return true;
    }

    private int getSelectedPresetIndex() {
        if (timePresetButtons == null || timePresetButtons.isEmpty() || selectedPreset == null) return -1;
        for (int i = 0; i < timePresetButtons.size(); i++) {
            Object p = timePresetButtons.get(i).getClientProperty("preset");
            if (!(p instanceof TimePreset)) continue;
            TimePreset tp = (TimePreset) p;
            if (tp.initialSeconds == selectedPreset.initialSeconds
                && tp.incrementSeconds == selectedPreset.incrementSeconds
                && tp.display.equals(selectedPreset.display)) {
                return i;
            }
        }
        return -1;
    }

    private void navigateTimePresetGrid(int rowDelta, int colDelta) {
        if (gameStarted) return;
        if ((rowDelta == 0 && colDelta == 0) || timePresetButtons == null || timePresetButtons.isEmpty()) return;

        final int cols = 3;
        final int count = timePresetButtons.size();

        int currentIndex = getSelectedPresetIndex();
        if (currentIndex < 0) currentIndex = 0;

        int row = currentIndex / cols;
        int col = currentIndex % cols;

        int nextRow = row + rowDelta;
        int nextCol = col + colDelta;

        int maxRow = (count - 1) / cols;
        if (nextRow < 0) nextRow = 0;
        if (nextRow > maxRow) nextRow = maxRow;
        if (nextCol < 0) nextCol = 0;
        if (nextCol >= cols) nextCol = cols - 1;

        int nextIndex = nextRow * cols + nextCol;
        if (nextIndex >= count) {
            nextIndex = count - 1;
        }

        JButton nextBtn = timePresetButtons.get(nextIndex);
        if (nextBtn != null && nextBtn.isEnabled()) {
            nextBtn.doClick();
        }
    }

    private void setSelectedSpellCard(boolean white, int cardIndex) {
        List<SpellCardButton> buttons = white ? whiteCardButtons : blackCardButtons;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setSelectedVisual(i == cardIndex);
        }
        if (white) {
            whiteSelectedCardIndex = cardIndex;
            blackSelectedCardIndex = -1;
            for (SpellCardButton b : blackCardButtons) b.setSelectedVisual(false);
        } else {
            blackSelectedCardIndex = cardIndex;
            whiteSelectedCardIndex = -1;
            for (SpellCardButton b : whiteCardButtons) b.setSelectedVisual(false);
        }
    }

    private void clearSelectedSpellCards() {
        whiteSelectedCardIndex = -1;
        blackSelectedCardIndex = -1;
        for (SpellCardButton b : whiteCardButtons) b.setSelectedVisual(false);
        for (SpellCardButton b : blackCardButtons) b.setSelectedVisual(false);
    }

    private String drawRandomCandidate(List<String> active, String usedSpell) {
        List<String> candidates = new ArrayList<>();
        for (String id : allSpellIds) {
            if (!active.contains(id) && !id.equals(usedSpell)) {
                candidates.add(id);
            }
        }
        if (candidates.isEmpty()) {
            for (String id : allSpellIds) {
                if (!active.contains(id)) candidates.add(id);
            }
        }
        if (candidates.isEmpty()) return usedSpell;
        StringBuilder key = new StringBuilder();
        if (usedSpell != null) key.append(usedSpell);
        key.append('|');
        for (String id : active) key.append(id).append(',');
        int idx = Math.floorMod(key.toString().hashCode(), candidates.size());
        return candidates.get(idx);
    }

    private void startSpellUiAnimation() {
        if (spellAnimTimer != null && spellAnimTimer.isRunning()) return;
        final long animationStartedAtNanos = System.nanoTime();
        final float startingPhase = spellAnimPhase;
        spellAnimTimer = new Timer(AnimationTiming.FRAME_DELAY_MS, e -> {
            double elapsedMs = (System.nanoTime() - animationStartedAtNanos) / 1_000_000.0;
            spellAnimPhase = (float) ((startingPhase + elapsedMs * (0.02 / 40.0)) % 1.0);
            if (whiteElixirBar != null) whiteElixirBar.repaint();
            if (blackElixirBar != null) blackElixirBar.repaint();
        });
        spellAnimTimer.start();
    }

    private void syncSideCards(boolean white, boolean canInteract) {
        List<String> active = white ? whiteActiveSpells : blackActiveSpells;
        ensureHandSize(active);
        List<SpellCardButton> buttons = white ? whiteCardButtons : blackCardButtons;
        int selected = white ? whiteSelectedCardIndex : blackSelectedCardIndex;
        if (spellCardTransitionAnimating) return;
        for (int i = 0; i < buttons.size(); i++) {
            SpellCardButton b = buttons.get(i);
            if (i == NEXT_SLOT_INDEX) {
                String nextId = white ? whiteNextSpellId : blackNextSpellId;
                if (nextId == null || active.contains(nextId)) {
                    nextId = drawRandomCandidate(active, null);
                    if (white) whiteNextSpellId = nextId;
                    else blackNextSpellId = nextId;
                }
                b.setSpellData(nextId, spellById.get(nextId), true);
                b.setEnabled(false);
                b.setSelectedVisual(false);
                continue;
            }
            if (i >= active.size()) {
                b.setSpellData(null, null, false);
                b.setEnabled(false);
                b.setSelectedVisual(false);
                continue;
            }
            String spellId = active.get(i);
            b.setSpellData(spellId, spellById.get(spellId), false);
            boolean pendingConfirmation = hasPendingSpellConfirmation();
            boolean pendingCard = isPendingSpellCard(white, i);
            boolean enabled = pendingConfirmation
                ? pendingCard
                : canInteract && board.canSideCastAny(spellId, white);
            b.setEnabled(enabled);
            b.setSelectedVisual(pendingCard || (enabled && i == selected));
        }

    }

    private void refreshSpellUI() {
        if (board == null) return;
        if (!"Spell Chess".equalsIgnoreCase(gameVariant)) return;
        if (board.isWhiteTurn() && blackSelectedCardIndex != -1) clearSelectedSpellCards();
        if (!board.isWhiteTurn() && whiteSelectedCardIndex != -1) clearSelectedSpellCards();

        int whiteElixir = board.getElixirForSide(true);
        int blackElixir = board.getElixirForSide(false);
        if (whiteSpellElixirLabel != null) whiteSpellElixirLabel.setText("White's Elixir Board");
        if (blackSpellElixirLabel != null) blackSpellElixirLabel.setText("Black's Elixir Board");
        if (whiteElixirBar != null) whiteElixirBar.setValue(whiteElixir);
        if (blackElixirBar != null) blackElixirBar.setValue(blackElixir);

        syncSideCards(true, board.isWhiteTurn());
        syncSideCards(false, !board.isWhiteTurn());
    }

    private JButton createTimeSelectButton() {
        JButton btn = new JButton(selectedPreset.display) {
            private boolean hover;
            private final Timer breatheTimer = AnimationTiming.createUiTimer(e -> {
                if (isShowing()) repaint();
            });
            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setFont(new Font("Arial", Font.BOLD, 28));
                setForeground(Color.WHITE);
                addHierarchyListener(event -> {
                    if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
                    if (isShowing()) {
                        breatheTimer.start();
                    } else {
                        breatheTimer.stop();
                    }
                });
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        if (isEnabled()) {
                            hover = true;
                            repaint();
                        }
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 31;
                Color top = hover ? new Color(66, 66, 66) : new Color(58, 58, 58);
                Color bottom = hover ? new Color(49, 49, 49) : new Color(42, 42, 42);
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                float breathe = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * (2.0 * Math.PI / 2100.0));
                int outerAlpha = Math.round(150 + 45 * breathe);
                int innerAlpha = Math.round(130 + 35 * breathe);
                int gray = Math.round(124 + 14 * breathe);
                g2.setColor(new Color(gray, gray, gray, Math.max(0, Math.min(255, outerAlpha))));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(new Color(20, 20, 20, Math.max(0, Math.min(255, innerAlpha))));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);

                float controlScale = Math.max(0.55f, getFont().getSize2D() / 28f);
                int iconX = Math.round(28 * controlScale);
                int iconSize = Math.round(24 * controlScale);
                int iconY = (h - iconSize) / 2;
                drawPresetIcon(g2, iconX, iconY, getPresetCategory(selectedPreset), true, controlScale);

                // Label with subtle drop shadow
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = iconX + iconSize + Math.round(27 * controlScale);
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 140));
                g2.drawString(getText(), textX + 1, textY + 1);
                g2.setColor(new Color(245, 245, 245));
                g2.drawString(getText(), textX, textY);

                // Chevron
                int cx = w - 40;
                int cy = h / 2;
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(168, 168, 168));
                if (timeDetailsExpanded) {
                    g2.drawLine(cx - 6, cy + 3, cx, cy - 3);
                    g2.drawLine(cx, cy - 3, cx + 6, cy + 3);
                } else {
                    g2.drawLine(cx - 6, cy - 3, cx, cy + 3);
                    g2.drawLine(cx, cy + 3, cx + 6, cy - 3);
                }

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(TIME_CONTROL_WIDTH, TIME_SELECT_HEIGHT));
        btn.setMinimumSize(new Dimension(0, TIME_SELECT_HEIGHT));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, TIME_SELECT_HEIGHT));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> toggleTimeDetails());
        applyTimeControl(selectedPreset.initialSeconds, selectedPreset.incrementSeconds);
        return btn;
    }

    private JButton createStartGameButton() {
        JButton btn = new JButton("Start Game") {
            private boolean hover;
            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setFont(new Font("Arial", Font.BOLD, 34));
                setForeground(Color.WHITE);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        if (isEnabled()) {
                            hover = true;
                            repaint();
                        }
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 31;
                Color top = hover ? timeUiTheme.startHoverTop : timeUiTheme.startTop;
                Color bottom = hover ? timeUiTheme.startHoverBottom : timeUiTheme.startBottom;

                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(timeUiTheme.border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(timeUiTheme.borderDark);
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(timeUiTheme.textShadow.getRed(), timeUiTheme.textShadow.getGreen(), timeUiTheme.textShadow.getBlue(), 170));
                g2.drawString(getText(), tx + 1, ty + 2);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(TIME_CONTROL_WIDTH, TIME_START_HEIGHT));
        btn.setMinimumSize(new Dimension(0, TIME_START_HEIGHT));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, TIME_START_HEIGHT));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> startLocalGame());
        return btn;
    }

    private JButton createGlossyActionButton(
        String text,
        Color top,
        Color bottom,
        Color hoverTop,
        Color hoverBottom,
        Color borderBright,
        Color borderDark
    ) {
        JButton btn = new JButton(text) {
            private boolean hover;
            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setFont(new Font("Arial", Font.BOLD, 18));
                setForeground(Color.WHITE);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        hover = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        hover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 14;
                Color t = hover ? hoverTop : top;
                Color b = hover ? hoverBottom : bottom;

                g2.setPaint(new GradientPaint(0, 0, t, 0, h, b));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // glossy highlight band
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, hover ? 88 : 64), 0, h / 2, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(2, 2, w - 4, h / 2, arc - 3, arc - 3);

                g2.setColor(borderBright);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(borderDark);
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);

                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 140));
                g2.drawString(getText(), tx + 1, ty + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(360, 48));
        btn.setMaximumSize(new Dimension(360, 48));
        return btn;
    }

    private JPanel createTimeDetailsPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(39, 39, 39), getWidth(), getHeight(), new Color(29, 29, 29));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMinimumSize(new Dimension(0, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(43, 43, 43), 1),
            BorderFactory.createEmptyBorder(17, 20, 17, 20)
        ));

        TimePreset[] bullet = new TimePreset[] {
            new TimePreset("1 min (Bullet)", 60, 0),
            new TimePreset("1 | 1 (Bullet)", 60, 1),
            new TimePreset("2 | 1 (Bullet)", 120, 1),
        };
        TimePreset[] blitz = new TimePreset[] {
            new TimePreset("3 min (Blitz)", 180, 0),
            new TimePreset("3 | 2 (Blitz)", 180, 2),
            new TimePreset("5 min (Blitz)", 300, 0),
        };
        TimePreset[] rapid = new TimePreset[] {
            new TimePreset("10 min (Rapid)", 600, 0),
            new TimePreset("15 | 10 (Rapid)", 900, 10),
            new TimePreset("30 min (Rapid)", 1800, 0),
        };

        panel.add(createPresetSection("Bullet", bullet, "bullet"));
        panel.add(Box.createVerticalStrut(14));
        panel.add(createPresetSection("Blitz", blitz, "blitz"));
        panel.add(Box.createVerticalStrut(14));
        panel.add(createPresetSection("Rapid", rapid, "rapid"));

        updatePresetButtonStyles();
        return panel;
    }

    private JPanel createPresetSection(String title, TimePreset[] presets, String iconKind) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.add(createSectionHeader(title, iconKind));
        section.add(Box.createVerticalStrut(11));

        JPanel row = new JPanel(new GridLayout(1, presets.length, 14, 0));
        row.setOpaque(false);
        for (TimePreset preset : presets) {
            JButton b = createPresetButton(preset);
            row.add(b);
        }
        section.add(row);
        return section;
    }

    private JComponent createSectionHeader(String title, String iconKind) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 11, 0));
        row.setOpaque(false);

        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float scale = Math.max(0.55f, Math.min(getWidth(), getHeight()) / 25f);
                drawPresetIcon(g2, 0, 0, iconKind, false, scale);
                g2.dispose();
            }
        };
        Dimension iconSize = new Dimension(25, 25);
        icon.setPreferredSize(iconSize);
        icon.setMinimumSize(iconSize);
        icon.setMaximumSize(iconSize);
        icon.putClientProperty("responsivePresetIcon", Boolean.TRUE);
        row.add(icon);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 21));
        label.setForeground(new Color(235, 235, 235));
        row.add(label);
        return row;
    }

    private JButton createPresetButton(TimePreset preset) {
        JButton btn = new JButton(compactPresetLabel(preset)) {
            private boolean hover;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (isEnabled()) {
                            hover = true;
                            repaint();
                        }
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 14;
                boolean selected = isPresetSelected(this);
                Color top = selected ? new Color(76, 80, 76) : (hover ? new Color(76, 76, 76) : new Color(67, 67, 67));
                Color bottom = selected ? new Color(58, 61, 58) : (hover ? new Color(56, 56, 56) : new Color(49, 49, 49));
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                if (selected) {
                    g2.setColor(timeUiTheme.accent);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
                } else {
                    g2.setColor(new Color(89, 89, 89));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    g2.setColor(new Color(31, 31, 31));
                    g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 1, arc - 1);
                }

                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 150));
                g2.drawString(getText(), tx + 1, ty + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.putClientProperty("preset", preset);
        btn.setPreferredSize(new Dimension(TIME_PRESET_WIDTH, TIME_PRESET_HEIGHT));
        btn.setMinimumSize(new Dimension(TIME_PRESET_WIDTH, TIME_PRESET_HEIGHT));
        btn.setMaximumSize(new Dimension(TIME_PRESET_WIDTH, TIME_PRESET_HEIGHT));
        btn.setFont(new Font("Arial", Font.BOLD, 23));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.addActionListener(e -> selectPreset(preset));
        timePresetButtons.add(btn);
        return btn;
    }

    private String compactPresetLabel(TimePreset preset) {
        int idx = preset.display.lastIndexOf(" (");
        return idx > 0 ? preset.display.substring(0, idx) : preset.display;
    }

    private boolean isPresetSelected(JButton btn) {
        Object p = btn.getClientProperty("preset");
        if (!(p instanceof TimePreset)) return false;
        TimePreset tp = (TimePreset) p;
        return selectedPreset != null && selectedPreset.initialSeconds == tp.initialSeconds
            && selectedPreset.incrementSeconds == tp.incrementSeconds
            && selectedPreset.display.equals(tp.display);
    }

    private void updatePresetButtonStyles() {
        for (JButton b : timePresetButtons) {
            boolean selected = isPresetSelected(b);
            b.setForeground(selected ? new Color(245, 255, 241) : Color.WHITE);
            b.repaint();
        }
    }

    private String getPresetCategory(TimePreset preset) {
        if (preset == null || preset.display == null) return "rapid";
        String s = preset.display.toLowerCase();
        if (s.contains("(bullet)")) return "bullet";
        if (s.contains("(blitz)")) return "blitz";
        return "rapid";
    }

    private void drawPresetIcon(
        Graphics2D g2,
        int x,
        int y,
        String iconKind,
        boolean large,
        float scale
    ) {
        Graphics2D gg = (Graphics2D) g2.create();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.translate(x, y);
        gg.scale(scale, scale);
        x = 0;
        y = 0;
        if ("blitz".equals(iconKind)) {
            gg.setColor(new Color(248, 215, 77));
            Polygon p = new Polygon();
            p.addPoint(x + 7, y + 1); p.addPoint(x + 12, y + 1); p.addPoint(x + 9, y + 8);
            p.addPoint(x + 14, y + 8); p.addPoint(x + 6, y + 18); p.addPoint(x + 8, y + 11);
            p.addPoint(x + 4, y + 11);
            gg.fillPolygon(p);
        } else if ("rapid".equals(iconKind)) {
            gg.setColor(new Color(133, 199, 78));
            gg.setStroke(new BasicStroke(large ? 3f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int d = large ? 20 : 14;
            int ox = x + (large ? 0 : 2);
            int oy = y + (large ? 0 : 3);
            gg.drawOval(ox, oy, d, d);
            gg.drawLine(ox + d / 2, oy - 2, ox + d / 2 - 2, oy - 5);
            gg.drawLine(ox + d / 2, oy - 2, ox + d / 2 + 2, oy - 5);
            gg.drawLine(ox + d / 2, oy + d / 2, ox + d / 2, oy + d / 2 - 5);
            gg.drawLine(ox + d / 2, oy + d / 2, ox + d / 2 + 4, oy + d / 2 + 3);
        } else {
            // Bullet uses a rocket-like glyph similar to chess.com.
            java.awt.geom.AffineTransform old = gg.getTransform();
            int cx = x + (large ? 12 : 9);
            int cy = y + (large ? 10 : 9);
            gg.rotate(Math.toRadians(-35), cx, cy);

            gg.setColor(new Color(255, 205, 72));
            java.awt.geom.RoundRectangle2D.Float hull =
                new java.awt.geom.RoundRectangle2D.Float(cx - 2, cy - 8, 6, 13, 4, 4);
            gg.fill(hull);

            Polygon nose = new Polygon();
            nose.addPoint(cx + 1, cy - 11);
            nose.addPoint(cx - 2, cy - 7);
            nose.addPoint(cx + 4, cy - 7);
            gg.fillPolygon(nose);

            gg.setColor(new Color(33, 33, 33));
            gg.fillOval(cx, cy - 5, 2, 2);

            gg.setColor(new Color(255, 216, 110));
            Polygon finL = new Polygon();
            finL.addPoint(cx - 2, cy + 2);
            finL.addPoint(cx - 5, cy + 5);
            finL.addPoint(cx - 1, cy + 4);
            gg.fillPolygon(finL);
            Polygon finR = new Polygon();
            finR.addPoint(cx + 4, cy + 2);
            finR.addPoint(cx + 6, cy + 5);
            finR.addPoint(cx + 3, cy + 4);
            gg.fillPolygon(finR);

            gg.setColor(new Color(255, 237, 169));
            Polygon flame = new Polygon();
            flame.addPoint(cx + 1, cy + 5);
            flame.addPoint(cx - 1, cy + 9);
            flame.addPoint(cx + 3, cy + 7);
            gg.fillPolygon(flame);

            gg.setTransform(old);
        }
        gg.dispose();
    }

    private void toggleTimeDetails() {
        if (gameStarted) return;

        if (timeDetailsAnimTimer != null && timeDetailsAnimTimer.isRunning()) {
            timeDetailsAnimTimer.stop();
        }
        timeDetailsExpanded = !timeDetailsExpanded;
        if (timeDetailsExpanded) updateTimeDetailsExpandedHeight();
        animateTimeDetailsHeight(timeDetailsExpanded ? timeDetailsExpandedHeight : 0);
        if (timeSelectBtn != null) timeSelectBtn.repaint();
    }

    private void animateTimeDetailsHeight(int targetHeight) {
        int startHeight = timeDetailsCurrentHeight;
        int target = Math.max(0, Math.min(timeDetailsExpandedHeight, targetHeight));
        if (startHeight == target) {
            applyTimeDetailsHeight(target);
            return;
        }

        float distanceRatio = Math.abs(target - startHeight)
                / (float) Math.max(1, timeDetailsExpandedHeight);
        int durationMs = Math.max(120, Math.round(TIME_DETAILS_ANIMATION_MS * distanceRatio));
        long startedAtNanos = System.nanoTime();

        timeDetailsAnimTimer = AnimationTiming.createUiTimer(null);
        timeDetailsAnimTimer.addActionListener(e -> {
            float progress = AnimationTiming.progress(startedAtNanos, durationMs);
            float eased = progress * progress * (3f - 2f * progress);
            int height = Math.round(startHeight + (target - startHeight) * eased);
            applyTimeDetailsHeight(height);

            if (progress >= 1f) {
                ((Timer) e.getSource()).stop();
                timeDetailsAnimTimer = null;
                applyTimeDetailsHeight(target);
            }
        });
        timeDetailsAnimTimer.start();
    }

    private void syncExpandedTimeDetailsHeight() {
        int preferredHeight = updateTimeDetailsExpandedHeight();
        if (timeDetailsExpanded
                && (timeDetailsAnimTimer == null || !timeDetailsAnimTimer.isRunning())
                && timeDetailsCurrentHeight != preferredHeight) {
            applyTimeDetailsHeight(preferredHeight);
        }
    }

    private int updateTimeDetailsExpandedHeight() {
        if (timeDetailsPanel == null) return Math.max(1, timeDetailsExpandedHeight);
        int preferredHeight = Math.max(1, timeDetailsPanel.getPreferredSize().height);
        timeDetailsExpandedHeight = preferredHeight;
        if (timeDetailsHostPanel != null) {
            timeDetailsHostPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
        }
        return preferredHeight;
    }

    private void applyTimeDetailsHeight(int h) {
        if (timeDetailsHostPanel == null) return;
        int clamped = Math.max(0, Math.min(timeDetailsExpandedHeight, h));
        timeDetailsCurrentHeight = clamped;
        timeDetailsHostPanel.setMinimumSize(new Dimension(0, clamped));
        timeDetailsHostPanel.setPreferredSize(new Dimension(TIME_CONTROL_WIDTH, clamped));
        timeDetailsHostPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, clamped));
        updateTimeControlPanelSize();
        timeDetailsHostPanel.revalidate();
        if (timeControlPanel != null) {
            timeControlPanel.revalidate();
            timeControlPanel.repaint();
        }
        Container p = timeControlPanel == null ? null : timeControlPanel.getParent();
        if (p != null) {
            p.revalidate();
            p.repaint();
        }
        if (timeSelectBtn != null) {
            timeSelectBtn.repaint();
        }
    }

    private void updateTimeControlPanelSize() {
        if (timeControlPanel == null) return;
        int selectHeight = timeSelectBtn == null ? 0 : timeSelectBtn.getPreferredSize().height;
        int detailsHeight = timeDetailsHostPanel == null
                ? 0
                : timeDetailsHostPanel.getPreferredSize().height;
        int startHeight = startGameBtn == null ? 0 : startGameBtn.getPreferredSize().height;
        Insets insets = timeControlPanel.getInsets();
        int requiredHeight = selectHeight + detailsHeight + 14 + startHeight
                + insets.top + insets.bottom;
        int preferredWidth = Math.max(
                MIN_TIME_CONTROL_WIDTH,
                timeSelectBtn == null ? TIME_CONTROL_WIDTH
                        : timeSelectBtn.getPreferredSize().width);
        timeControlPanel.setPreferredSize(new Dimension(preferredWidth, requiredHeight));
        timeControlPanel.setMinimumSize(new Dimension(MIN_TIME_CONTROL_WIDTH, requiredHeight));
    }

    private void selectPreset(TimePreset preset) {
        if (gameStarted) return; // don't allow changing mid-game for now
        selectedPreset = preset;

        if (timeSelectBtn != null) {
            timeSelectBtn.setText(selectedPreset.display);
            timeSelectBtn.repaint();
        }
        updatePresetButtonStyles();

        applyTimeControl(selectedPreset.initialSeconds, selectedPreset.incrementSeconds);
    }

    private TimePreset promptCustomTime() {
        JSpinner minutes = new JSpinner(new SpinnerNumberModel(10, 1, 180, 1));
        JSpinner increment = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));

        JPanel p = new JPanel(new GridLayout(2, 2, 10, 10));
        p.add(new JLabel("Minutes:"));
        p.add(minutes);
        p.add(new JLabel("Increment (sec):"));
        p.add(increment);

        int result = JOptionPane.showConfirmDialog(dialogOwner(), p, "Custom Time Control", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return null;

        int m = (Integer) minutes.getValue();
        int inc = (Integer) increment.getValue();
        String display = (inc > 0) ? (m + " | " + inc + " (Custom)") : (m + " min (Custom)");
        return new TimePreset(display, m * 60, inc);
    }

    private void applyTimeControl(int initialSeconds, int increment) {
        stopTimer();

        this.whiteTimeRemaining = initialSeconds;
        this.blackTimeRemaining = initialSeconds;
        this.incrementSeconds = increment;
        this.whitesTurn = true;

        // Reset clock label colors/text
        topClockLabel.setForeground(Color.WHITE);
        bottomClockLabel.setForeground(Color.WHITE);
        topClockLabel.setText(formatTime(blackTimeRemaining));
        bottomClockLabel.setText(formatTime(whiteTimeRemaining));
    }

    private void startLocalGame() {
        if (gameStarted) return;
        clearGameOverState();
        gameStarted = true;
        syncBoardEnterHotkeyMode();

        if ("Spell Chess".equalsIgnoreCase(gameVariant) && !onlineMode && !isBotGame) {
            applySpellChessStartedLayout();
        }

        // Enable board interaction
        if (board != null) board.setInputEnabled(true);

        if (shouldShowMoveHistory()) {
            resetOpeningTitleToVariant();
            showMoveHistoryPanel();
        }
        refreshSpellUI();
        
    }

    private void setSpellPanelsVisible(boolean visible) {
        if (whiteSpellPanel != null) whiteSpellPanel.setVisible(visible);
        if (blackSpellPanel != null) blackSpellPanel.setVisible(visible);
    }

    private void applySpellChessPreStartLayout() {
        if (!"Spell Chess".equalsIgnoreCase(gameVariant) || onlineMode || isBotGame) return;
        if (timeControlPanel != null) timeControlPanel.setVisible(true);
        setSpellPanelsVisible(false);
        if (centerColumnPanel != null) {
            centerColumnPanel.revalidate();
            centerColumnPanel.repaint();
        }
    }

    private void applySpellChessStartedLayout() {
        if (!"Spell Chess".equalsIgnoreCase(gameVariant) || onlineMode || isBotGame) return;
        if (centerColumnPanel != null && timeControlPanel != null && timeControlPanel.getParent() == centerColumnPanel) {
            centerColumnPanel.remove(timeControlPanel);
        }
        if (timeControlPanel != null) timeControlPanel.setVisible(false);
        setSpellPanelsVisible(true);
        if (centerColumnPanel != null) {
            centerColumnPanel.revalidate();
            centerColumnPanel.repaint();
        }
    }


    private void setupPlayerLabels() {
    if (isBotGame && selectedBot != null) {
        if (spectateMode) {
            ChessBot whiteBot = (spectateWhiteBot == null) ? selectedBot : spectateWhiteBot;
            ChessBot blackBot = (spectateBlackBot == null) ? selectedBot : spectateBlackBot;
            bottomPlayerLabel.setText(whiteBot.getName() + " (ELO " + whiteBot.getElo() + ")");
            topPlayerLabel.setText(blackBot.getName() + " (ELO " + blackBot.getElo() + ")");
        } else {
            topPlayerLabel.setText(
                selectedBot.getName() + " (ELO " + selectedBot.getElo() + ")"
            );
            bottomPlayerLabel.setText("White");
        }
        return;
    }

    // Online / local
    if (youAreWhite) {
        bottomPlayerLabel.setText("White");
        topPlayerLabel.setText("Black");
    } else {
        bottomPlayerLabel.setText("Black");
        topPlayerLabel.setText("White");
    }
    refreshCapturedMaterialUI();
}


    // â”€â”€ Called by Board popup Exit button and the in-game Exit button â”€â”€â”€â”€â”€
    public void exitToMenu() {
        returnToApplicationMenu();
    }

    private void returnToApplicationMenu() {
        cleanupEmbeddedGame();
        if (appHost != null) {
            appHost.returnToMenuFromEmbeddedScreen();
        } else {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenu());
        }
    }

    private void cleanupEmbeddedGame() {
        stopTimer();
        cancelSpellResolutionLock();
        if (clockOverlayFadeTimer != null) {
            clockOverlayFadeTimer.stop();
            clockOverlayFadeTimer = null;
        }
        if (timeDetailsAnimTimer != null) {
            timeDetailsAnimTimer.stop();
            timeDetailsAnimTimer = null;
        }
        if (networkManager != null) {
            networkManager.close();
            networkManager = null;
        }
        if (stockfishEngine != null) {
            try {
                stockfishEngine.close();
            } catch (Exception ignored) {
            }
            stockfishEngine = null;
        }
    }

    private void exitApplication() {
        cleanupEmbeddedGame();
        if (appHost != null) {
            appHost.dispose();
        }
        dispose();
        System.exit(0);
    }

    private void showExitConfirmationDialog() {
        cleanupEmbeddedGame();
        if (appHost != null) {
            appHost.returnToMenuFromEmbeddedScreen();
        } else {
            dispose();
            System.exit(0);
        }
    }

    private JButton buildExitDialogButton(String text, Color fg, Color bg, Color border) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 2, true),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(
                    Math.max(0, bg.getRed() - 10),
                    Math.max(0, bg.getGreen() - 10),
                    Math.max(0, bg.getBlue() - 10)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private BufferedImage loadExitDialogImage(String resourcePath, String filePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) return ImageIO.read(url);
        } catch (Exception ignored) {}
        try {
            File f = new File(filePath);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {}
        return null;
    }

    private BufferedImage loadDoodleUnderlay() {
        String[] resourcePaths = {
            "/assets/doodle.png"
        };
        for (String path : resourcePaths) {
            try {
                URL url = getClass().getResource(path);
                if (url != null) {
                    BufferedImage image = ImageIO.read(url);
                    if (image != null) return image;
                }
            } catch (Exception ignored) {
            }
        }

        String[] filePaths = {
            "Scaccomatto_final/Scaccomatto/src/assets/doodle.png",
            "src/assets/doodle.png",
            "assets/doodle.png"
        };
        for (String path : filePaths) {
            try {
                File file = new File(path);
                if (file.isFile()) {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) return image;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // â”€â”€ Reset the board to starting position and restart the game â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void resetBoard() {
        stopTimer();
        SoundManager.stopAllSounds();
        clearGameOverState();
        cancelAllSpellSelections();
        botMoveInProgress = false;
        uciMoves.setLength(0);
        timerStarted = false;
        whitesTurn   = true;
        gameStarted  = false;
        syncBoardEnterHotkeyMode();

        // Restore original time from the selected preset
        whiteTimeRemaining = selectedPreset.initialSeconds;
        blackTimeRemaining = selectedPreset.initialSeconds;

        // Reset clock display
        topClockLabel.setForeground(Color.WHITE);
        bottomClockLabel.setForeground(Color.WHITE);
        topClockLabel.setText(formatTime(blackTimeRemaining));
        bottomClockLabel.setText(formatTime(whiteTimeRemaining));

        // Reset the board pieces and move history
        if (board != null) {
            board.resetToStartPosition();
            if (!onlineMode && !isBotGame) board.setInputEnabled(false);
        }
        SoundManager.stopAllSounds();
        if ("Spell Chess".equalsIgnoreCase(gameVariant)) {
            resetSpellDeckForSide(true);
            resetSpellDeckForSide(false);
        }
        if (moveHistoryPanel != null) moveHistoryPanel.clearHistory();
        lastOpeningTitle = "";
        if (openingNamePanel != null) openingNamePanel.setOpeningName("");

        // Re-show time control panel for local games
        if (!onlineMode && !isBotGame && timeControlPanel != null) {
            if (timeDetailsAnimTimer != null && timeDetailsAnimTimer.isRunning()) {
                timeDetailsAnimTimer.stop();
            }
            timeDetailsExpanded = false;
            applyTimeDetailsHeight(0);

            if (centerColumnPanel != null && moveHistoryPanel != null) {
                centerColumnPanel.remove(moveHistoryPanel);
                moveHistoryPanel = null;
            }
            if (centerColumnPanel != null && timeControlPanel.getParent() == null) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(0, 6, 8, 6);
                centerColumnPanel.add(timeControlPanel, gbc);
            }
            if (centerColumnPanel != null && centerSpellFiller != null
                    && centerSpellFiller.getParent() == null) {
                GridBagConstraints fillerGbc = new GridBagConstraints();
                fillerGbc.gridx = 0;
                fillerGbc.gridy = 1;
                fillerGbc.weighty = 1.0;
                fillerGbc.fill = GridBagConstraints.VERTICAL;
                centerColumnPanel.add(centerSpellFiller, fillerGbc);
            }
            timeControlPanel.setVisible(true);
            if (centerColumnPanel != null) { centerColumnPanel.revalidate(); centerColumnPanel.repaint(); }
        }
        if ("Spell Chess".equalsIgnoreCase(gameVariant) && !onlineMode && !isBotGame) {
            applySpellChessPreStartLayout();
        }

        // Bot/online games restart immediately
        if (isBotGame || onlineMode) {
            gameStarted = true;
            syncBoardEnterHotkeyMode();
            if (board != null) board.setInputEnabled(true);
            if (whiteTimeRemaining > 0 && blackTimeRemaining > 0) startTimer();
            if (shouldShowMoveHistory()) {
                showMoveHistoryPanel();
            }
        }
        refreshSpellUI();
        refreshCapturedMaterialUI();
        refreshOpeningTitle();
    }

    private boolean shouldShowMoveHistory() {
        return !"Spell Chess".equalsIgnoreCase(gameVariant);
    }

        // Called by Board whenever board orientation flips.
        public void onBoardFlipped() {
            updateUIForFlip();
        }

        // Update UI when board is flipped
        private void updateUIForFlip() {
            whiteAtBottom = !whiteAtBottom;

            if (boardStackPanel != null && topPanel != null && bottomPanel != null && board != null) {
                boardStackPanel.removeAll();
                if (whiteAtBottom) {
                    boardStackPanel.add(topPanel);
                    boardStackPanel.add(board);
                    boardStackPanel.add(bottomPanel);
                } else {
                    boardStackPanel.add(bottomPanel);
                    boardStackPanel.add(board);
                    boardStackPanel.add(topPanel);
                }
                boardStackPanel.revalidate();
                boardStackPanel.repaint();
            }

            if (centerColumnPanel != null && blackSpellPanel != null && whiteSpellPanel != null) {
                centerColumnPanel.remove(blackSpellPanel);
                centerColumnPanel.remove(whiteSpellPanel);

                GridBagConstraints topSpell = new GridBagConstraints();
                topSpell.gridx = 0;
                topSpell.gridy = 1;
                topSpell.fill = GridBagConstraints.HORIZONTAL;
                topSpell.weightx = 1.0;
                topSpell.anchor = GridBagConstraints.NORTH;
                topSpell.insets = new Insets(6, 6, 10, 6);

                GridBagConstraints bottomSpell = new GridBagConstraints();
                bottomSpell.gridx = 0;
                bottomSpell.gridy = 3;
                bottomSpell.fill = GridBagConstraints.HORIZONTAL;
                bottomSpell.weightx = 1.0;
                bottomSpell.anchor = GridBagConstraints.SOUTH;
                bottomSpell.insets = new Insets(6, 6, 8, 6);

                if (whiteAtBottom) {
                    centerColumnPanel.add(blackSpellPanel, topSpell);
                    centerColumnPanel.add(whiteSpellPanel, bottomSpell);
                } else {
                    centerColumnPanel.add(whiteSpellPanel, topSpell);
                    centerColumnPanel.add(blackSpellPanel, bottomSpell);
                }
                centerColumnPanel.revalidate();
                centerColumnPanel.repaint();
            }

            // Refresh clock values immediately for the new orientation.
            JLabel whiteLabel = getWhiteClockLabel();
            JLabel blackLabel = getBlackClockLabel();
            if (whiteLabel != null) whiteLabel.setText(formatTime(whiteTimeRemaining));
            if (blackLabel != null) blackLabel.setText(formatTime(blackTimeRemaining));

            // Preserve warning coloring according to each side's actual remaining time.
            if (whiteLabel != null) {
                whiteLabel.setForeground(whiteTimeRemaining <= 30 && whiteTimeRemaining > 0 ? Color.RED : Color.WHITE);
            }
            if (blackLabel != null) {
                blackLabel.setForeground(blackTimeRemaining <= 30 && blackTimeRemaining > 0 ? Color.RED : Color.WHITE);
            }
        }
    }
