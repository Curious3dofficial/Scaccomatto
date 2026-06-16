import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OnlinePanel extends JPanel {
    private static final Color GOLD = new Color(230, 174, 67);
    private static final Color GOLD_BRIGHT = new Color(255, 215, 115);
    private static final Color GOLD_MUTED = new Color(186, 143, 78);
    private static final Color TEXT = new Color(245, 232, 200);
    private static final Color SUBTEXT = new Color(202, 165, 105);
    private static final Color PANEL_FILL = new Color(2, 9, 12, 226);
    private static final Color ROW_FILL = new Color(5, 12, 15, 239);
    private static final Color BORDER = new Color(180, 123, 43, 185);
    private static final Color PURPLE = new Color(48, 17, 63, 242);
    private static final Color GREEN = new Color(4, 47, 20, 244);
    private static final Color BLUE = new Color(3, 35, 58, 244);

    private static final String[] VARIANT_NAMES = {
            "CLASSIC",
            "CHESS960",
            "KING OF THE HILL",
            "THREE-CHECK",
            "ATOMIC",
            "SPELL CHESS",
            "FOG OF WAR",
            "DUCK CHESS"
    };
    private static final String[] VARIANT_DESCRIPTIONS = {
            "Standard chess rules",
            "Randomized back rank setup!",
            "Reach the center with your king",
            "Win by giving three checks",
            "Captures explode nearby pieces!",
            "Cast spells and see the magic uncover!",
            "Limited vision outside piece range",
            "Place the duck, chaos ensues"
    };
    private static final boolean[] VARIANT_SUPPORTED = {
            true, false, true, true, true, true, true, false
    };

    private final MainMenu parent;
    private final List<VariantButton> variantButtons = new ArrayList<>();
    private final List<TimeButton> timeButtons = new ArrayList<>();
    private final Image backgroundImage;
    private final Image headerPawn;

    private JTextField ipField;
    private JTextField portField;
    private JButton hostBtn;
    private JButton joinBtn;
    private JLabel statusLabel;
    private NetworkManager net;
    private LocalLobbyDiscovery.Advertiser lobbyAdvertiser;
    private String selectedVariant = "Classic";
    private TimePreset selectedPreset = new TimePreset("10 MIN", 600, 0);

    private static final class TimePreset {
        final String label;
        final int initialSeconds;
        final int incrementSeconds;

        TimePreset(String label, int initialSeconds, int incrementSeconds) {
            this.label = label;
            this.initialSeconds = initialSeconds;
            this.incrementSeconds = incrementSeconds;
        }
    }

    public OnlinePanel(MainMenu parent) {
        this.parent = parent;
        backgroundImage = loadImage("/assets/bgmulti.png", "src/assets/bgmulti.png");
        headerPawn = tintImage(loadImage("/assets/pieces/wp.png", "src/assets/pieces/wp.png"));

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(28, 62, 22, 62));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        updateVariantUI();
        updateTimeUI();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (backgroundImage != null) {
            int iw = Math.max(1, backgroundImage.getWidth(this));
            int ih = Math.max(1, backgroundImage.getHeight(this));
            double scale = Math.max(getWidth() / (double) iw, getHeight() / (double) ih);
            int dw = Math.max(1, (int) Math.ceil(iw * scale));
            int dh = Math.max(1, (int) Math.ceil(ih * scale));
            g2.drawImage(backgroundImage, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, this);
        }
        g2.setPaint(new GradientPaint(
                0, 0, new Color(0, 4, 6, 205),
                getWidth(), 0, new Color(0, 3, 5, 116)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(new GradientPaint(
                0, 0, new Color(0, 0, 0, 30),
                0, getHeight(), new Color(0, 0, 0, 150)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(graphics);
    }

    @Override
    public void removeNotify() {
        closeCurrentConnection();
        super.removeNotify();
    }

    private JComponent buildHeader() {
        JPanel header = transparent(new BorderLayout());
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JPanel identity = transparent(new BorderLayout(18, 0));
        identity.add(new HeaderEmblem(), BorderLayout.WEST);

        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("MULTIPLAYER");
        title.setFont(UiFonts.trackedTitle(42f, 0.08f));
        title.setForeground(GOLD_BRIGHT);
        JLabel subtitle = new JLabel("CHOOSE YOUR FORMAT, SET THE CLOCK, THEN HOST OR JOIN INSTANTLY");
        subtitle.setFont(UiFonts.subtext(14f));
        subtitle.setForeground(new Color(224, 190, 126));
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        identity.add(copy, BorderLayout.CENTER);

        LobbyBadge badge = new LobbyBadge();
        badge.setPreferredSize(new Dimension(190, 58));
        JPanel badgeWrap = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        badgeWrap.add(badge);

        header.add(identity, BorderLayout.WEST);
        header.add(badgeWrap, BorderLayout.EAST);
        return header;
    }

    private JComponent buildMainContent() {
        JPanel content = transparent(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 4);
        content.add(buildVariantPanel(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 4, 0, 0);
        content.add(buildLobbyPanel(), gbc);
        return content;
    }

    private JComponent buildVariantPanel() {
        RoyalPanel panel = new RoyalPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(14, 26, 0, 14));

        JPanel heading = transparent(new BorderLayout(18, 0));
        heading.setBorder(new EmptyBorder(0, 10, 8, 12));
        heading.add(new VariantEmblem(), BorderLayout.WEST);

        JPanel titleCopy = transparent();
        titleCopy.setLayout(new BoxLayout(titleCopy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CHESS VARIANTS");
        title.setFont(UiFonts.title(25f));
        title.setForeground(GOLD_BRIGHT);
        JLabel subtitle = new JLabel("PICK A MODE FOR YOUR ONLINE MATCH");
        subtitle.setFont(UiFonts.subtext(14f));
        subtitle.setForeground(SUBTEXT);
        titleCopy.add(title);
        titleCopy.add(Box.createVerticalStrut(4));
        titleCopy.add(subtitle);
        heading.add(titleCopy, BorderLayout.CENTER);
        panel.add(heading, BorderLayout.NORTH);

        JPanel list = transparent();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int i = 0; i < VARIANT_NAMES.length; i++) {
            VariantButton button = new VariantButton(
                    VARIANT_NAMES[i],
                    VARIANT_DESCRIPTIONS[i],
                    VARIANT_SUPPORTED[i],
                    loadVariantImage(VARIANT_NAMES[i]));
            variantButtons.add(button);
            list.add(button);
            if (i < VARIANT_NAMES.length - 1) list.add(Box.createVerticalStrut(7));
        }
        list.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(13, 0));
        scrollPane.getVerticalScrollBar().setUI(new GoldScrollBarUI());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = transparent(new BorderLayout());
        footer.setBorder(new EmptyBorder(7, 6, 8, 14));
        JLabel ready = new JLabel("\u25cf   READY");
        ready.setFont(UiFonts.subtext(13f));
        ready.setForeground(new Color(193, 217, 119));
        JLabel players = new JLabel("\u265f  45,231 players online");
        players.setFont(UiFonts.uiRegular(13f));
        players.setForeground(SUBTEXT);
        footer.add(ready, BorderLayout.WEST);
        footer.add(players, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildLobbyPanel() {
        final int contentWidth = 760;
        RoyalPanel panel = new RoyalPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(contentWidth + 60, 720));
        panel.setBorder(new EmptyBorder(22, 30, 24, 30));

        JPanel body = transparent();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setPreferredSize(new Dimension(contentWidth, 720));
        body.setMaximumSize(new Dimension(contentWidth, Integer.MAX_VALUE));

        SectionDivider timeDivider = new SectionDivider("TIME CONTROL");
        timeDivider.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(timeDivider);
        body.add(Box.createVerticalStrut(18));

        JPanel timeRow = transparent(new GridLayout(1, 4, 16, 0));
        timeRow.setPreferredSize(new Dimension(contentWidth, 112));
        timeRow.setMinimumSize(new Dimension(420, 92));
        timeRow.setMaximumSize(new Dimension(contentWidth, 112));
        timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeRow.add(createTimeButton(new TimePreset("3 | 2", 180, 2)));
        timeRow.add(createTimeButton(new TimePreset("5 MIN", 300, 0)));
        timeRow.add(createTimeButton(new TimePreset("10 MIN", 600, 0)));
        timeRow.add(createTimeButton(new TimePreset("15 | 10", 900, 10)));
        body.add(timeRow);
        body.add(Box.createVerticalStrut(30));

        BannerButton local = new BannerButton();
        local.setMaximumSize(new Dimension(contentWidth, 108));
        local.setPreferredSize(new Dimension(contentWidth, 108));
        local.setMinimumSize(new Dimension(420, 96));
        local.setAlignmentX(Component.LEFT_ALIGNMENT);
        local.addActionListener(event -> showLocalLobbyDialog());
        body.add(local);
        body.add(Box.createVerticalStrut(33));
        SectionDivider connectionDivider = new SectionDivider("DIRECT CONNECTION");
        connectionDivider.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(connectionDivider);
        body.add(Box.createVerticalStrut(16));

        ipField = new ConnectionField("localhost", false);
        portField = new ConnectionField("5000", true);
        ipField.setPreferredSize(new Dimension(contentWidth, 54));
        portField.setPreferredSize(new Dimension(contentWidth, 54));
        ipField.setMinimumSize(new Dimension(420, 48));
        portField.setMinimumSize(new Dimension(420, 48));
        ipField.setMaximumSize(new Dimension(contentWidth, 54));
        portField.setMaximumSize(new Dimension(contentWidth, 54));
        ipField.setAlignmentX(Component.LEFT_ALIGNMENT);
        portField.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(ipField);
        body.add(Box.createVerticalStrut(10));
        body.add(portField);
        body.add(Box.createVerticalStrut(34));

        JPanel actions = transparent(new GridLayout(1, 2, 24, 0));
        actions.setPreferredSize(new Dimension(contentWidth, 132));
        actions.setMinimumSize(new Dimension(520, 116));
        actions.setMaximumSize(new Dimension(contentWidth, 132));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        hostBtn = new ActionCard(
                "HOST GAME",
                "CREATE A GAME AND INVITE",
                GREEN,
                new Color(64, 176, 47),
                false);
        joinBtn = new ActionCard(
                "JOIN GAME",
                "JOIN AN EXISTING GAME",
                BLUE,
                new Color(38, 139, 213),
                true);
        hostBtn.addActionListener(event -> doHost());
        joinBtn.addActionListener(event -> doJoin());
        actions.add(hostBtn);
        actions.add(joinBtn);
        body.add(actions);
        body.add(Box.createVerticalGlue());

        JPanel bodyPositioner = transparent(new GridBagLayout());
        GridBagConstraints bodyGbc = new GridBagConstraints();
        bodyGbc.gridx = 0;
        bodyGbc.gridy = 0;
        bodyGbc.weightx = 1.0;
        bodyGbc.weighty = 1.0;
        bodyGbc.anchor = GridBagConstraints.NORTHEAST;
        bodyGbc.fill = GridBagConstraints.VERTICAL;
        bodyPositioner.add(body, bodyGbc);
        panel.add(bodyPositioner, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildBottomBar() {
        JPanel footer = transparent(new BorderLayout());
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JButton back = new BackButton();
        back.addActionListener(event ->
                parent.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "BACK_TO_MENU")));
        statusLabel = new JLabel("Ready", SwingConstants.RIGHT);
        statusLabel.setFont(UiFonts.uiRegular(13f));
        statusLabel.setForeground(SUBTEXT);
        footer.add(back, BorderLayout.WEST);
        footer.add(statusLabel, BorderLayout.EAST);
        return footer;
    }

    private TimeButton createTimeButton(TimePreset preset) {
        TimeButton button = new TimeButton(preset);
        button.addActionListener(event -> {
            selectedPreset = preset;
            updateTimeUI();
            setStatus("Time selected: " + preset.label);
        });
        timeButtons.add(button);
        return button;
    }

    private void updateVariantUI() {
        for (VariantButton button : variantButtons) {
            button.setSelectedVariant(
                    button.variantName.equalsIgnoreCase(selectedVariant));
        }
    }

    private void updateTimeUI() {
        for (TimeButton button : timeButtons) {
            button.setSelectedPreset(button.preset.label.equals(selectedPreset.label));
        }
    }

    private Integer parsePort() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException exception) {
            setStatus("Invalid port. Use 1-65535.");
            return null;
        }
    }

    private void doHost() {
        Integer port = parsePort();
        if (port == null) return;
        boolean supported = isSupportedVariant();
        if (!showLaunchDialog("Host Game", supported)) return;
        if (!supported) {
            setStatus(selectedVariant + " is coming soon in multiplayer.");
            return;
        }

        closeCurrentConnection();
        net = new NetworkManager();
        lobbyAdvertiser = LocalLobbyDiscovery.advertise(LocalLobbyDiscovery.Lobby.create(
                localHostName(),
                port,
                selectedVariant,
                selectedPreset.label,
                selectedPreset.initialSeconds,
                selectedPreset.incrementSeconds));
        final ChessGame[] gameRef = new ChessGame[1];
        final List<Runnable> pendingEvents = new ArrayList<>();
        setStatus("Hosting " + selectedVariant + " on port " + port + "... waiting for opponent");
        net.host(port, createNetworkListener(gameRef, pendingEvents, true));
    }

    private void doJoin() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            setStatus("IP / Host cannot be empty.");
            return;
        }
        Integer port = parsePort();
        if (port == null) return;
        boolean supported = isSupportedVariant();
        if (!showLaunchDialog("Join Game", supported)) return;
        if (!supported) {
            setStatus(selectedVariant + " is coming soon in multiplayer.");
            return;
        }

        closeCurrentConnection();
        net = new NetworkManager();
        final ChessGame[] gameRef = new ChessGame[1];
        final List<Runnable> pendingEvents = new ArrayList<>();
        setStatus("Connecting to " + ip + ":" + port + "...");
        net.join(ip, port, createNetworkListener(gameRef, pendingEvents, false));
    }

    private NetworkManager.Listener createNetworkListener(
            ChessGame[] gameRef,
            List<Runnable> pendingEvents,
            boolean hosting) {
        return new NetworkManager.Listener() {
            private void dispatch(Runnable action) {
                SwingUtilities.invokeLater(() -> {
                    synchronized (pendingEvents) {
                        if (gameRef[0] == null) {
                            pendingEvents.add(action);
                            return;
                        }
                    }
                    action.run();
                });
            }

            @Override
            public void onConnected(boolean amWhite) {
                stopLobbyAdvertiser();
                setStatus((hosting ? "Opponent connected. " : "Connected. ")
                        + "You are " + (amWhite ? "White" : "Black"));
                parent.launchGameWithOverlay(() -> {
                    ChessGame game = new ChessGame(
                            parent,
                            selectedPreset.initialSeconds,
                            selectedPreset.incrementSeconds,
                            net,
                            amWhite,
                            selectedVariant);
                    List<Runnable> replay;
                    synchronized (pendingEvents) {
                        gameRef[0] = game;
                        replay = new ArrayList<>(pendingEvents);
                        pendingEvents.clear();
                    }
                    for (Runnable event : replay) event.run();
                });
            }

            @Override
            public void onMoveReceived(int fr, int fc, int tr, int tc, String promotion) {
                dispatch(() -> gameRef[0].onRemoteMove(fr, fc, tr, tc, promotion));
            }

            @Override
            public void onSpellCastReceived(String spellId, boolean casterWhite, SpellTarget target) {
                dispatch(() -> gameRef[0].onRemoteSpellCast(spellId, casterWhite, target));
            }

            @Override
            public void onSpellPhaseReceived(String phaseId, boolean casterWhite, int row, int col) {
                dispatch(() -> gameRef[0].onRemoteSpellPhase(phaseId, casterWhite, row, col));
            }

            @Override
            public void onGameResultReceived(String title, String message) {
                dispatch(() -> gameRef[0].onRemoteGameResult(title, message));
            }

            @Override public void onError(String message) {
                if (hosting) stopLobbyAdvertiser();
                setStatus("Error: " + message);
            }
            @Override public void onDrawOffered() { dispatch(() -> gameRef[0].onRemoteOfferDraw()); }
            @Override public void onDrawAccepted() { dispatch(() -> gameRef[0].onRemoteDrawAccepted()); }
            @Override public void onDrawDeclined() { dispatch(() -> gameRef[0].onRemoteDrawDeclined()); }
            @Override public void onResign() { dispatch(() -> gameRef[0].onRemoteResign()); }
        };
    }

    private boolean isSupportedVariant() {
        return "Classic".equals(selectedVariant)
                || "King Of The Hill".equals(selectedVariant)
                || "King of the Hill".equals(selectedVariant)
                || "Fog of War".equals(selectedVariant)
                || "Spell Chess".equals(selectedVariant)
                || "Atomic".equals(selectedVariant)
                || "Three-check".equals(selectedVariant);
    }

    private void showLocalLobbyDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Join Local", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        RoyalPanel card = new RoyalPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel header = transparent();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("JOIN LOCAL");
        title.setFont(UiFonts.title(28f));
        title.setForeground(GOLD_BRIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("DISCOVER HOSTED GAMES ON YOUR NETWORK");
        subtitle.setFont(UiFonts.subtext(13f));
        subtitle.setForeground(SUBTEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        card.add(header, BorderLayout.NORTH);

        DefaultListModel<LocalLobbyDiscovery.Lobby> model = new DefaultListModel<>();
        JList<LocalLobbyDiscovery.Lobby> list = new JList<>(model);
        list.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(68);
        list.setCellRenderer(new LobbyCellRenderer());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(720, 300));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new GoldScrollBarUI());
        card.add(scrollPane, BorderLayout.CENTER);

        JLabel scanStatus = new JLabel("Scanning local network...", SwingConstants.LEFT);
        scanStatus.setFont(UiFonts.uiRegular(13f));
        scanStatus.setForeground(SUBTEXT);

        JButton refresh = compactDialogButton("REFRESH");
        JButton cancel = compactDialogButton("CANCEL");
        JButton join = compactDialogButton("JOIN SELECTED");
        join.setEnabled(false);
        list.addListSelectionListener(event -> join.setEnabled(!list.isSelectionEmpty()));

        Runnable scan = () -> {
            refresh.setEnabled(false);
            join.setEnabled(false);
            scanStatus.setText("Scanning local network...");
            model.clear();
            new SwingWorker<List<LocalLobbyDiscovery.Lobby>, Void>() {
                @Override
                protected List<LocalLobbyDiscovery.Lobby> doInBackground() throws Exception {
                    return LocalLobbyDiscovery.scan(2600);
                }

                @Override
                protected void done() {
                    try {
                        List<LocalLobbyDiscovery.Lobby> lobbies = get();
                        for (LocalLobbyDiscovery.Lobby lobby : lobbies) model.addElement(lobby);
                        scanStatus.setText(lobbies.isEmpty()
                                ? "No local games found. Ask the host to click Host Game."
                                : lobbies.size() + " local game" + (lobbies.size() == 1 ? "" : "s") + " found.");
                    } catch (Exception exception) {
                        scanStatus.setText("Could not scan local games: " + exception.getMessage());
                    } finally {
                        refresh.setEnabled(true);
                        join.setEnabled(!list.isSelectionEmpty());
                    }
                }
            }.execute();
        };

        refresh.addActionListener(event -> scan.run());
        cancel.addActionListener(event -> dialog.dispose());
        join.addActionListener(event -> {
            LocalLobbyDiscovery.Lobby lobby = list.getSelectedValue();
            if (lobby == null) return;
            applyLobbySelection(lobby);
            dialog.dispose();
            doJoin();
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && !list.isSelectionEmpty()) {
                    join.doClick();
                }
            }
        });

        JPanel actions = transparent(new BorderLayout(12, 0));
        actions.add(scanStatus, BorderLayout.CENTER);
        JPanel buttons = transparent(new GridLayout(1, 3, 10, 0));
        buttons.add(refresh);
        buttons.add(cancel);
        buttons.add(join);
        actions.add(buttons, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.setSize(790, 460);
        dialog.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(scan);
        dialog.setVisible(true);
    }

    private void applyLobbySelection(LocalLobbyDiscovery.Lobby lobby) {
        selectedVariant = lobby.variant();
        selectedPreset = new TimePreset(
                lobby.timeLabel(),
                lobby.initialSeconds(),
                lobby.incrementSeconds());
        ipField.setText(lobby.hostAddress());
        portField.setText(String.valueOf(lobby.port()));
        updateVariantUI();
        updateTimeUI();
        setStatus("Selected local game: " + lobby.hostAddress() + ":" + lobby.port());
    }

    private void closeCurrentConnection() {
        stopLobbyAdvertiser();
        if (net != null) {
            net.close();
            net = null;
        }
    }

    private void stopLobbyAdvertiser() {
        if (lobbyAdvertiser != null) {
            lobbyAdvertiser.close();
            lobbyAdvertiser = null;
        }
    }

    private static String localHostName() {
        try {
            String name = java.net.InetAddress.getLocalHost().getHostName();
            return name == null || name.isBlank() ? "Scaccomatto Host" : name;
        } catch (Exception ignored) {
            return "Scaccomatto Host";
        }
    }

    private boolean showLaunchDialog(String mode, boolean supported) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, mode, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        RoyalPanel card = new RoyalPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(selectedVariant + (supported ? "" : " (SOON)"));
        title.setFont(UiFonts.title(28f));
        title.setForeground(GOLD_BRIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detail = new JLabel(
                mode.toUpperCase() + "  |  " + selectedPreset.label
                        + "  |  " + ipField.getText().trim() + ":" + portField.getText().trim());
        detail.setFont(UiFonts.uiRegular(14f));
        detail.setForeground(SUBTEXT);
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(12));
        copy.add(detail);
        card.add(copy, BorderLayout.CENTER);

        JPanel actions = transparent(new GridLayout(1, 2, 12, 0));
        actions.setBorder(new EmptyBorder(20, 0, 0, 0));
        final boolean[] accepted = { false };
        JButton cancel = compactDialogButton("CANCEL");
        JButton confirm = compactDialogButton(supported ? mode.toUpperCase() : "CLOSE");
        cancel.addActionListener(event -> dialog.dispose());
        confirm.addActionListener(event -> {
            accepted[0] = supported;
            dialog.dispose();
        });
        actions.add(cancel);
        actions.add(confirm);
        card.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.setSize(560, 220);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return accepted[0];
    }

    private JButton compactDialogButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiFonts.title(15f));
        button.setForeground(GOLD_BRIGHT);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private final class VariantButton extends JButton {
        private final String variantName;
        private final String description;
        private final boolean supported;
        private final Image iconImage;
        private boolean selected;
        private boolean hovered;

        VariantButton(String name, String description, boolean supported, Image iconImage) {
            this.variantName = titleCase(name);
            this.description = description;
            this.supported = supported;
            this.iconImage = iconImage;
            setPreferredSize(new Dimension(590, 74));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addActionListener(event -> {
                selectedVariant = variantName;
                updateVariantUI();
                setStatus(supported
                        ? "Selected variant: " + selectedVariant
                        : selectedVariant + " is coming soon.");
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
            });
        }

        void setSelectedVariant(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(selected
                    ? new Color(25, 20, 10, 246)
                    : hovered ? new Color(15, 17, 15, 244) : ROW_FILL);
            g2.fillRoundRect(0, 0, w, h, 8, 8);

            if (selected) {
                g2.setPaint(new LinearGradientPaint(
                        0, 0, w, h,
                        new float[] { 0f, 0.55f, 1f },
                        new Color[] {
                                new Color(133, 85, 14, 95),
                                new Color(17, 15, 9, 15),
                                new Color(244, 171, 42, 74)
                        }));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(GOLD_BRIGHT);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(115, 82, 43, 145));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);

            int tile = 58;
            int tileX = 18;
            int tileY = (h - tile) / 2;
            Color tileTop = variantTileColor(variantName);
            g2.setPaint(new GradientPaint(
                    tileX, tileY, tileTop,
                    tileX, tileY + tile, new Color(5, 10, 13)));
            g2.fillRoundRect(tileX, tileY, tile, tile, 6, 6);
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 125));
            g2.drawRoundRect(tileX, tileY, tile, tile, 6, 6);
            if (iconImage != null) {
                g2.drawImage(iconImage, tileX + 7, tileY + 7, tile - 14, tile - 14, this);
            }

            int textX = 100;
            g2.setFont(UiFonts.title(17f));
            g2.setColor(selected ? GOLD_BRIGHT : new Color(234, 189, 91));
            String title = variantName.toUpperCase() + (supported ? "" : " (SOON)");
            g2.drawString(title, textX, 31);
            g2.setFont(UiFonts.uiRegular(14f));
            g2.setColor(SUBTEXT);
            g2.drawString(description, textX, 55);
            if (selected) {
                g2.setColor(GOLD_BRIGHT);
                g2.fillPolygon(
                        new int[] { 7, 12, 7 },
                        new int[] { h / 2 - 5, h / 2, h / 2 + 5 },
                        3);
            }
            g2.dispose();
        }
    }

    private static final class TimeButton extends JButton {
        private final TimePreset preset;
        private boolean selected;
        private boolean hovered;

        TimeButton(TimePreset preset) {
            super(preset.label);
            this.preset = preset;
            setFont(UiFonts.title(20f));
            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
            });
        }

        void setSelectedPreset(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            if (selected) {
                for (int i = 10; i >= 2; i -= 2) {
                    g2.setStroke(new BasicStroke(i));
                    g2.setColor(new Color(240, 169, 49, 10));
                    g2.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 6, 6);
                }
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(79, 54, 20, 235),
                        0, getHeight(), new Color(35, 22, 8, 245)));
            } else {
                g2.setColor(hovered ? new Color(13, 20, 22, 245) : new Color(3, 11, 14, 240));
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
            g2.setColor(selected ? GOLD_BRIGHT : new Color(116, 81, 43, 190));
            g2.setStroke(new BasicStroke(selected ? 1.6f : 1f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 7, 7);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ConnectionField extends JTextField {
        private final boolean port;

        ConnectionField(String text, boolean port) {
            super(text);
            this.port = port;
            setFont(UiFonts.uiRegular(18f));
            setForeground(new Color(234, 232, 223));
            setCaretColor(GOLD_BRIGHT);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 74, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            g2.setColor(new Color(2, 9, 12, 245));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.setColor(new Color(129, 87, 37, 190));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 6, 6);
            g2.drawLine(60, 1, 60, getHeight() - 2);
            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (port) {
                g2.drawRoundRect(23, 15, 17, 22, 3, 3);
                g2.drawLine(29, 15, 29, 9);
                g2.drawLine(35, 15, 35, 9);
                g2.fillOval(29, 24, 5, 5);
            } else {
                g2.drawRect(20, 12, 20, 10);
                g2.drawRect(20, 31, 20, 10);
                g2.drawLine(24, 25, 24, 29);
                g2.drawLine(36, 25, 36, 29);
                g2.drawLine(24, 27, 36, 27);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoyalPanel extends JPanel {
        RoyalPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            g2.setColor(PANEL_FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 7, 7);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class SectionDivider extends JComponent {
        private final String text;

        SectionDivider(String text) {
            this.text = text;
            setPreferredSize(new Dimension(500, 26));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            g2.setFont(UiFonts.title(16f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int cx = getWidth() / 2;
            int y = getHeight() / 2;
            g2.setColor(new Color(117, 80, 36, 165));
            g2.drawLine(0, y, cx - tw / 2 - 36, y);
            g2.drawLine(cx + tw / 2 + 36, y, getWidth(), y);
            g2.setColor(GOLD);
            diamond(g2, cx - tw / 2 - 28, y, 5);
            diamond(g2, cx + tw / 2 + 28, y, 5);
            g2.setColor(GOLD_BRIGHT);
            g2.drawString(text, cx - tw / 2, y + fm.getAscent() / 2 - 1);
            g2.dispose();
        }
    }

    private static final class BannerButton extends JButton {
        private boolean hovered;

        BannerButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Color fill = new Color(48, 17, 63, 242);
            Color accent = new Color(190, 92, 224);
            Shape outer = ActionCard.clippedCard(2, 2, w - 4, h - 4, 7);
            Shape inner = ActionCard.clippedCard(6, 6, w - 12, h - 12, 5);

            Color activeFill = hovered ? brighten(fill, 1.12f) : fill;
            Color deepTone = new Color(
                    Math.max(1, activeFill.getRed() / 2),
                    Math.max(8, activeFill.getGreen() / 2),
                    Math.max(4, activeFill.getBlue() / 2));
            g2.setPaint(new LinearGradientPaint(
                    0, 0, 0, h,
                    new float[] { 0f, 0.16f, 0.52f, 1f },
                    new Color[] {
                            new Color(22, 12, 26),
                            deepTone,
                            activeFill,
                            new Color(10, 3, 15)
                    }));
            g2.fill(outer);

            g2.setClip(inner);
            g2.setPaint(new RadialGradientPaint(
                    new Point((int) (w * 0.53), (int) (h * 0.18)),
                    Math.max(1f, w * 0.58f),
                    new float[] { 0f, 0.18f, 0.58f, 1f },
                    new Color[] {
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    hovered ? 38 : 24),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12),
                            new Color(0, 0, 0, 0)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new LinearGradientPaint(
                    0, 0, w, 0,
                    new float[] { 0f, 0.16f, 0.52f, 0.84f, 1f },
                    new Color[] {
                            new Color(0, 0, 0, 150),
                            new Color(0, 0, 0, 45),
                            new Color(0, 0, 0, 0),
                            new Color(0, 0, 0, 45),
                            new Color(0, 0, 0, 150)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new LinearGradientPaint(
                    0, 0, 0, h,
                    new float[] { 0f, 0.08f, 0.36f, 0.72f, 1f },
                    new Color[] {
                            new Color(255, 219, 119, 36),
                            new Color(255, 180, 44, 16),
                            new Color(0, 0, 0, 0),
                            new Color(0, 0, 0, 28),
                            new Color(0, 0, 0, 115)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(230, 185, 83, 12));
            for (int x = -h; x < w; x += 22) {
                g2.drawLine(x, h, x + h, 0);
            }
            g2.setClip(null);

            for (int stroke = 8; stroke >= 2; stroke -= 2) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 11));
                g2.setStroke(new BasicStroke(stroke));
                g2.draw(outer);
            }
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
            g2.setStroke(new BasicStroke(1.35f));
            g2.draw(outer);
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 145));
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(inner);

            g2.setPaint(new GradientPaint(
                    18, 0, new Color(255, 214, 106, 35),
                    w - 18, 0, new Color(255, 214, 106, 210)));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawLine(17, 5, w - 17, 5);
            ActionCard.drawCardCorners(g2, w, h, accent);

            int cx = w / 2;
            g2.setColor(GOLD_BRIGHT);
            drawPeopleIcon(g2, cx - 118, 39);
            g2.setFont(UiFonts.title(25f));
            String title = "JOIN LOCAL";
            FontMetrics titleFm = g2.getFontMetrics();
            g2.drawString(title, cx - titleFm.stringWidth(title) / 2 + 18, 48);
            g2.setFont(UiFonts.uiRegular(13f));
            g2.setColor(new Color(222, 190, 137));
            String subtitle = "PLAY WITH PEOPLE ON YOUR NETWORK";
            FontMetrics subFm = g2.getFontMetrics();
            g2.drawString(subtitle, cx - subFm.stringWidth(subtitle) / 2, 76);
            g2.dispose();
        }

        private static void drawPeopleIcon(Graphics2D g2, int x, int y) {
            g2.fillOval(x - 5, y - 12, 10, 10);
            g2.fillOval(x - 17, y - 9, 8, 8);
            g2.fillOval(x + 9, y - 9, 8, 8);
            g2.fillRoundRect(x - 10, y, 20, 13, 8, 8);
            g2.fillRoundRect(x - 22, y + 2, 12, 11, 6, 6);
            g2.fillRoundRect(x + 10, y + 2, 12, 11, 6, 6);
        }
    }

    private static final class ActionCard extends JButton {
        private final String title;
        private final String subtitle;
        private final Color fill;
        private final Color accent;
        private final boolean crossedSwords;
        private boolean hovered;

        ActionCard(String title, String subtitle, Color fill, Color accent, boolean crossedSwords) {
            this.title = title;
            this.subtitle = subtitle;
            this.fill = fill;
            this.accent = accent;
            this.crossedSwords = crossedSwords;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(360, 132));
            setMinimumSize(new Dimension(250, 116));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Shape outer = clippedCard(2, 2, w - 4, h - 4, 7);
            Shape inner = clippedCard(6, 6, w - 12, h - 12, 5);

            Color activeFill = hovered ? brighten(fill, 1.12f) : fill;
            Color nearBlack = crossedSwords
                    ? new Color(1, 10, 17)
                    : new Color(1, 13, 6);
            Color deepTone = new Color(
                    Math.max(1, activeFill.getRed() / 2),
                    Math.max(8, activeFill.getGreen() / 2),
                    Math.max(4, activeFill.getBlue() / 2));
            g2.setPaint(new LinearGradientPaint(
                    0, 0, 0, h,
                    new float[] { 0f, 0.16f, 0.52f, 1f },
                    new Color[] {
                            new Color(18, 21, 10),
                            deepTone,
                            activeFill,
                            nearBlack
                    }));
            g2.fill(outer);

            g2.setClip(inner);
            g2.setPaint(new RadialGradientPaint(
                    new Point((int) (w * 0.53), (int) (h * 0.18)),
                    Math.max(1f, w * 0.58f),
                    new float[] { 0f, 0.18f, 0.58f, 1f },
                    new Color[] {
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    hovered ? 38 : 24),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12),
                            new Color(0, 0, 0, 0)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new LinearGradientPaint(
                    0, 0, w, 0,
                    new float[] { 0f, 0.16f, 0.52f, 0.84f, 1f },
                    new Color[] {
                            new Color(0, 0, 0, 150),
                            new Color(0, 0, 0, 45),
                            new Color(0, 0, 0, 0),
                            new Color(0, 0, 0, 45),
                            new Color(0, 0, 0, 150)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new LinearGradientPaint(
                    0, 0, 0, h,
                    new float[] { 0f, 0.08f, 0.36f, 0.72f, 1f },
                    new Color[] {
                            new Color(255, 219, 119, 36),
                            new Color(255, 180, 44, 16),
                            new Color(0, 0, 0, 0),
                            new Color(0, 0, 0, 28),
                            new Color(0, 0, 0, 115)
                    }));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(230, 185, 83, 12));
            for (int x = -h; x < w; x += 22) {
                g2.drawLine(x, h, x + h, 0);
            }
            g2.setClip(null);

            for (int stroke = 8; stroke >= 2; stroke -= 2) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 11));
                g2.setStroke(new BasicStroke(stroke));
                g2.draw(outer);
            }
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
            g2.setStroke(new BasicStroke(1.35f));
            g2.draw(outer);
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 145));
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(inner);

            g2.setPaint(new GradientPaint(
                    18, 0, new Color(255, 214, 106, 35),
                    w - 18, 0, new Color(255, 214, 106, 210)));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawLine(17, 5, w - 17, 5);
            drawCardCorners(g2, w, h, accent);

            int iconX = 57;
            int cy = getHeight() / 2;
            g2.setColor(GOLD_BRIGHT);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (crossedSwords) {
                drawCrossedSwords(g2, iconX, cy);
            } else {
                drawKingEmblem(g2, iconX, cy);
            }

            int textX = 103;
            g2.setFont(UiFonts.title(20f));
            g2.setColor(GOLD_BRIGHT);
            g2.drawString(title, textX, cy - 5);
            g2.setFont(UiFonts.uiRegular(11f));
            g2.setColor(new Color(219, 190, 139));
            g2.drawString(subtitle, textX, cy + 18);
            g2.dispose();
        }

        private static Shape clippedCard(int x, int y, int w, int h, int cut) {
            Path2D path = new Path2D.Float();
            path.moveTo(x + cut, y);
            path.lineTo(x + w - cut, y);
            path.lineTo(x + w, y + cut);
            path.lineTo(x + w, y + h - cut);
            path.lineTo(x + w - cut, y + h);
            path.lineTo(x + cut, y + h);
            path.lineTo(x, y + h - cut);
            path.lineTo(x, y + cut);
            path.closePath();
            return path;
        }

        private static void drawCardCorners(Graphics2D g2, int w, int h, Color accent) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 230));
            int[][] corners = {
                    { 7, 7, 1, 1 },
                    { w - 8, 7, -1, 1 },
                    { 7, h - 8, 1, -1 },
                    { w - 8, h - 8, -1, -1 }
            };
            for (int[] corner : corners) {
                int x = corner[0];
                int y = corner[1];
                int dx = corner[2];
                int dy = corner[3];
                g2.drawLine(x, y, x + 7 * dx, y);
                g2.drawLine(x, y, x, y + 7 * dy);
                diamond(g2, x + 2 * dx, y + 2 * dy, 2);
            }
        }

        private static void drawKingEmblem(Graphics2D g2, int x, int y) {
            Path2D crown = new Path2D.Float();
            crown.moveTo(x - 16, y - 16);
            crown.lineTo(x - 10, y - 5);
            crown.lineTo(x - 3, y - 17);
            crown.lineTo(x + 3, y - 5);
            crown.lineTo(x + 11, y - 16);
            crown.lineTo(x + 15, y + 2);
            crown.lineTo(x - 14, y + 2);
            crown.closePath();
            g2.draw(crown);
            g2.drawLine(x, y - 28, x, y - 18);
            g2.drawLine(x - 4, y - 24, x + 4, y - 24);
            g2.drawOval(x - 10, y + 2, 20, 18);
            g2.drawLine(x - 18, y + 20, x + 18, y + 20);
            g2.drawLine(x - 14, y + 26, x + 14, y + 26);
            g2.drawLine(x - 9, y + 31, x + 9, y + 31);
        }

        private static void drawCrossedSwords(Graphics2D g2, int x, int y) {
            g2.drawLine(x - 18, y - 23, x + 17, y + 20);
            g2.drawLine(x + 18, y - 23, x - 17, y + 20);
            g2.drawLine(x - 22, y - 19, x - 14, y - 27);
            g2.drawLine(x + 22, y - 19, x + 14, y - 27);
            g2.drawLine(x + 11, y + 13, x + 22, y + 22);
            g2.drawLine(x - 11, y + 13, x - 22, y + 22);
        }
    }

    private static final class BackButton extends JButton {
        BackButton() {
            super("\u2039  BACK TO MENU");
            setFont(UiFonts.title(15f));
            setForeground(GOLD_BRIGHT);
            setPreferredSize(new Dimension(205, 48));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        }
    }

    private static final class LobbyCellRenderer extends JPanel
            implements ListCellRenderer<LocalLobbyDiscovery.Lobby> {
        private final JLabel title = new JLabel();
        private final JLabel subtitle = new JLabel();

        LobbyCellRenderer() {
            super(new BorderLayout(12, 3));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));

            JPanel copy = transparent();
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            title.setFont(UiFonts.title(17f));
            title.setForeground(GOLD_BRIGHT);
            subtitle.setFont(UiFonts.uiRegular(13f));
            subtitle.setForeground(SUBTEXT);
            copy.add(title);
            copy.add(Box.createVerticalStrut(4));
            copy.add(subtitle);
            add(copy, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends LocalLobbyDiscovery.Lobby> list,
                LocalLobbyDiscovery.Lobby lobby,
                int index,
                boolean selected,
                boolean focus) {
            title.setText(lobby.hostName());
            subtitle.setText(lobby.variant() + "  |  " + lobby.timeLabel()
                    + "  |  " + lobby.hostAddress() + ":" + lobby.port());
            putClientProperty("selected", selected);
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            boolean selected = Boolean.TRUE.equals(getClientProperty("selected"));
            g2.setColor(selected ? new Color(39, 29, 12, 244) : ROW_FILL);
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 6, 7, 7);
            g2.setColor(selected ? GOLD_BRIGHT : new Color(115, 82, 43, 145));
            g2.drawRoundRect(2, 3, getWidth() - 5, getHeight() - 7, 7, 7);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class LobbyBadge extends JComponent {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            g2.setColor(new Color(3, 10, 13, 235));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
            g2.setColor(BORDER);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 5, 5);
            g2.setFont(UiFonts.title(15f));
            g2.setColor(TEXT);
            g2.drawString("LIVE LOBBY", 23, 35);
            g2.setColor(GOLD);
            g2.fillOval(getWidth() - 47, 20, 9, 9);
            g2.fillOval(getWidth() - 35, 22, 7, 7);
            g2.fillRoundRect(getWidth() - 50, 31, 17, 10, 5, 5);
            g2.setColor(new Color(57, 204, 82));
            g2.fillOval(getWidth() - 23, 19, 7, 7);
            g2.dispose();
        }
    }

    private final class HeaderEmblem extends JComponent {
        HeaderEmblem() {
            setPreferredSize(new Dimension(82, 82));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            for (int r : new int[] { 38, 32, 26 }) {
                g2.setColor(new Color(231, 171, 58, r == 38 ? 180 : 115));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            }
            if (headerPawn != null) {
                g2.drawImage(headerPawn, cx - 25, cy - 29, 50, 58, this);
            }
            g2.dispose();
        }
    }

    private static final class VariantEmblem extends JComponent {
        VariantEmblem() {
            setPreferredSize(new Dimension(72, 72));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = quality(graphics);
            g2.translate(getWidth() / 2, getHeight() / 2);
            g2.rotate(Math.PI / 4);
            g2.setColor(new Color(5, 12, 15, 245));
            g2.fillRect(-24, -24, 48, 48);
            g2.setColor(BORDER);
            g2.drawRect(-24, -24, 48, 48);
            g2.rotate(-Math.PI / 4);
            g2.setColor(GOLD_BRIGHT);
            g2.fillOval(-5, -18, 10, 10);
            g2.fillRect(-4, -8, 8, 22);
            g2.fillOval(-12, 10, 24, 8);
            g2.dispose();
        }
    }

    private static final class GoldScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = GOLD;
            trackColor = new Color(2, 8, 10);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            Graphics2D g2 = quality(graphics);
            g2.setColor(new Color(3, 10, 12));
            g2.fillRoundRect(bounds.x + 3, bounds.y, bounds.width - 6, bounds.height, 8, 8);
            g2.setColor(new Color(131, 89, 35));
            g2.drawRoundRect(bounds.x + 3, bounds.y, bounds.width - 7, bounds.height - 1, 8, 8);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty()) return;
            Graphics2D g2 = quality(graphics);
            g2.setPaint(new GradientPaint(
                    bounds.x, bounds.y, GOLD_BRIGHT,
                    bounds.x + bounds.width, bounds.y, new Color(139, 88, 21)));
            g2.fillRoundRect(bounds.x + 4, bounds.y + 2, bounds.width - 8, bounds.height - 4, 8, 8);
            g2.dispose();
        }
    }

    private Image loadVariantImage(String name) {
        String key = name.toLowerCase();
        String file;
        if (key.contains("classic")) file = "classic.png";
        else if (key.contains("960")) file = "960.png";
        else if (key.contains("king")) file = "kinghill.png";
        else if (key.contains("three")) file = "3checks.png";
        else if (key.contains("atomic")) file = "atomic.png";
        else if (key.contains("spell")) file = "spellchess.png";
        else if (key.contains("fog")) file = "fogofwar.png";
        else file = "duck.png";
        return loadImage("/assets/multiplayer/" + file, "src/assets/multiplayer/" + file);
    }

    private static Image loadImage(String resourcePath, String filePath) {
        URL resource = OnlinePanel.class.getResource(resourcePath);
        if (resource != null) return new ImageIcon(resource).getImage();
        ImageIcon icon = new ImageIcon(filePath);
        return icon.getIconWidth() > 0 ? icon.getImage() : null;
    }

    private static Image tintImage(Image source) {
        if (source == null) return null;
        int width = source.getWidth(null);
        int height = source.getHeight(null);
        if (width <= 0 || height <= 0) return source;
        BufferedImage tinted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tinted.createGraphics();
        g2.drawImage(source, 0, 0, null);
        g2.setComposite(AlphaComposite.SrcIn);
        g2.setPaint(new GradientPaint(0, 0, GOLD_BRIGHT, 0, height, new Color(153, 88, 14)));
        g2.fillRect(0, 0, width, height);
        g2.dispose();
        return tinted;
    }

    private static JPanel transparent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static Graphics2D quality(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g2;
    }

    private static void diamond(Graphics2D g2, int x, int y, int radius) {
        g2.fillPolygon(
                new int[] { x, x + radius, x, x - radius },
                new int[] { y - radius, y, y + radius, y },
                4);
    }

    private static Color brighten(Color color, float factor) {
        return new Color(
                Math.min(255, Math.round(color.getRed() * factor)),
                Math.min(255, Math.round(color.getGreen() * factor)),
                Math.min(255, Math.round(color.getBlue() * factor)),
                color.getAlpha());
    }

    private static Color variantTileColor(String name) {
        String key = name.toLowerCase();
        if (key.contains("three")) return new Color(7, 75, 81);
        if (key.contains("spell")) return new Color(22, 77, 24);
        if (key.contains("duck")) return new Color(78, 48, 12);
        if (key.contains("fog")) return new Color(49, 43, 31);
        return new Color(50, 17, 57);
    }

    private static String titleCase(String value) {
        String[] words = value.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
