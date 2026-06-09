import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class SettingsPanel extends JPanel {
    private static final int CARD_WIDTH = 420;
    private static final int BODY_WIDTH = 388;
    private static final int BODY_HOST_WIDTH = 388;
    private static final int BODY_VIEW_HEIGHT = 520;
    private static final int HEADER_HEIGHT = 105;
    private static final int BUTTON_HEIGHT = 52;
    private static final int TOGGLE_ROW_HEIGHT = 46;
    private static final int DROPDOWN_ROW_HEIGHT = 43;
    private static final int DROPDOWN_WIDTH = 244;
    private static final int DROPDOWN_HEIGHT = 41;
    private static final int TOGGLE_WIDTH = 76;
    private static final int TOGGLE_HEIGHT = 32;
    private static ThemePalette currentPalette = ThemePalette.forName("Blue");

    private Board board;
    private final List<JButton> themedButtons = new ArrayList<>();
    private final List<JLabel> sectionLabels = new ArrayList<>();
    private final List<JToggleButton> themedToggles = new ArrayList<>();
    private final List<JComboBox<String>> themedDropdowns = new ArrayList<>();
    private JToggleButton showLegalMovesToggle;
    private JToggleButton showCoordinatesToggle;
    private JToggleButton premovingToggle;
    private JToggleButton autoFlipToggle;
    private JToggleButton autoQueenToggle;
    private JToggleButton soundsToggle;
    private JToggleButton lowTimeAlertsToggle;
    private JToggleButton evalBarToggle;
    private JToggleButton bestMoveToggle;
    private JToggleButton openingNamesToggle;
    private JComboBox<String> animationsDropdown;
    private JComboBox<String> themesDropdown;
    private JComboBox<String> pieceStyleDropdown;
    private JPanel controlsCard;
    private JPanel controlsBodyPanel;
    private JPanel controlsBodyHostPanel;
    private JScrollPane controlsScrollPane;
    private JComponent expandChevronLabel;
    private boolean settingsExpanded = false;
    private int settingsBodyCurrentHeight = 0;
    private int settingsBodyExpandedHeight = 0;
    private Timer settingsAnimTimer;
    
    public SettingsPanel(Board board) {
        this.board = board;
        
        setLayout(new BorderLayout());
        setBackground(new Color(64, 64, 64));
        setOpaque(true);
        setPreferredSize(new Dimension(CARD_WIDTH, HEADER_HEIGHT));
        setMinimumSize(new Dimension(300, HEADER_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEADER_HEIGHT + BODY_VIEW_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        controlsCard = new JPanel() {
            private static final int CORNER_ARC = 28;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(58, 58, 58), 0, h, new Color(42, 42, 42)));
                g2.fillRoundRect(0, 0, w, h, CORNER_ARC, CORNER_ARC);
                g2.setColor(new Color(130, 130, 130, 170));
                g2.drawRoundRect(0, 0, w - 1, h - 1, CORNER_ARC, CORNER_ARC);
                g2.setColor(new Color(18, 18, 18, 150));
                g2.drawRoundRect(1, 1, w - 3, h - 3, CORNER_ARC - 2, CORNER_ARC - 2);

                if (controlsBodyHostPanel != null && settingsBodyCurrentHeight > 0) {
                    float reveal = Math.min(1f, settingsBodyCurrentHeight / 28f);
                    int alpha = Math.round(135 * reveal);
                    int y = controlsBodyHostPanel.getY() - 5;
                    int left = 16;
                    int right = w - 16;
                    int middle = (left + right) / 2;

                    g2.setColor(new Color(0, 0, 0, Math.round(75 * reveal)));
                    g2.drawLine(left, y + 1, right, y + 1);
                    g2.setPaint(new GradientPaint(
                        left, y, new Color(currentPalette.border.getRed(), currentPalette.border.getGreen(),
                                          currentPalette.border.getBlue(), 18),
                        middle, y, new Color(currentPalette.border.getRed(), currentPalette.border.getGreen(),
                                            currentPalette.border.getBlue(), alpha)
                    ));
                    g2.drawLine(left, y, middle, y);
                    g2.setPaint(new GradientPaint(
                        middle, y, new Color(currentPalette.border.getRed(), currentPalette.border.getGreen(),
                                            currentPalette.border.getBlue(), alpha),
                        right, y, new Color(currentPalette.border.getRed(), currentPalette.border.getGreen(),
                                           currentPalette.border.getBlue(), 18)
                    ));
                    g2.drawLine(middle, y, right, y);
                }
                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.clip(new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight(), CORNER_ARC, CORNER_ARC
                ));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        controlsCard.setLayout(new BoxLayout(controlsCard, BoxLayout.Y_AXIS));
        controlsCard.setOpaque(false);
        controlsCard.setBorder(BorderFactory.createEmptyBorder(18, 16, 20, 16));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleCol = new JPanel();
        titleCol.setOpaque(false);
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Settings");
        heading.setFont(new Font("Arial", Font.BOLD, 28));
        heading.setForeground(new Color(245, 245, 245));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleCol.add(heading);

        JLabel subtitle = new JLabel("Board preferences");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(new Color(180, 180, 180));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleCol.add(subtitle);

        expandChevronLabel = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2 + 1;
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(currentPalette.chevron);
                if (settingsExpanded) {
                    g2.drawLine(cx - 7, cy + 3, cx, cy - 4);
                    g2.drawLine(cx, cy - 4, cx + 7, cy + 3);
                } else {
                    g2.drawLine(cx - 7, cy - 3, cx, cy + 4);
                    g2.drawLine(cx, cy + 4, cx + 7, cy - 3);
                }
                g2.dispose();
            }
        };
        expandChevronLabel.setPreferredSize(new Dimension(24, 14));
        expandChevronLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        headerRow.add(titleCol, BorderLayout.WEST);
        headerRow.add(expandChevronLabel, BorderLayout.EAST);
        controlsCard.add(headerRow);
        controlsCard.add(Box.createVerticalStrut(18));

        controlsBodyPanel = new ScrollableBodyPanel();
        controlsBodyPanel.setOpaque(false);
        controlsBodyPanel.setLayout(new BoxLayout(controlsBodyPanel, BoxLayout.Y_AXIS));
        buildControlsBody();
        applySettingsTheme("Blue");

        settingsBodyExpandedHeight = Math.min(BODY_VIEW_HEIGHT, controlsBodyPanel.getPreferredSize().height);
        settingsBodyCurrentHeight = settingsExpanded ? settingsBodyExpandedHeight : 0;

        controlsBodyHostPanel = new JPanel(new BorderLayout());
        controlsBodyHostPanel.setOpaque(false);
        controlsBodyHostPanel.setPreferredSize(new Dimension(BODY_HOST_WIDTH, settingsBodyCurrentHeight));
        controlsBodyHostPanel.setMinimumSize(new Dimension(BODY_HOST_WIDTH, 0));
        controlsBodyHostPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, settingsBodyExpandedHeight));
        controlsBodyHostPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        controlsScrollPane = new JScrollPane(controlsBodyPanel);
        controlsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        controlsScrollPane.setOpaque(false);
        controlsScrollPane.getViewport().setOpaque(false);
        controlsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScrollPane.setWheelScrollingEnabled(true);
        JScrollBar verticalScrollBar = controlsScrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(14);
        verticalScrollBar.setPreferredSize(new Dimension(0, 0));
        verticalScrollBar.setOpaque(false);
        verticalScrollBar.setUI(new SettingsScrollBarUI());
        controlsScrollPane.setPreferredSize(new Dimension(BODY_HOST_WIDTH, settingsBodyCurrentHeight));
        controlsScrollPane.setMinimumSize(new Dimension(BODY_HOST_WIDTH, 0));
        controlsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, settingsBodyExpandedHeight));
        controlsBodyHostPanel.add(controlsScrollPane, BorderLayout.CENTER);
        controlsCard.add(controlsBodyHostPanel);

        java.awt.event.MouseAdapter headerClick = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleSettingsDropdown();
            }
        };
        headerRow.addMouseListener(headerClick);
        heading.addMouseListener(headerClick);
        subtitle.addMouseListener(headerClick);
        expandChevronLabel.addMouseListener(headerClick);

        updateChevron();
        add(controlsCard, BorderLayout.NORTH);
    }

    private void buildControlsBody() {
        addSection("Board");

        JButton flipBoardBtn = createButton("Flip Board");
        flipBoardBtn.addActionListener(e -> board.flipBoard());
        controlsBodyPanel.add(flipBoardBtn);
        addGap(10);

        autoFlipToggle = createToggle(false);
        autoFlipToggle.addActionListener(e -> board.setAutoFlipEnabled(autoFlipToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Auto Flip Board", autoFlipToggle));
        addGap(8);

        showCoordinatesToggle = createToggle(true);
        showCoordinatesToggle.addActionListener(e -> board.setShowCoordinates(showCoordinatesToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Coordinates", showCoordinatesToggle));
        addGap(8);

        themesDropdown = createDropdown(new String[] {
            "Blue", "Green", "Gray", "Purple", "Red", "Orange", "Yellow", "Pink"
        });
        themesDropdown.addActionListener(e -> {
            String theme = (String) themesDropdown.getSelectedItem();
            if (board != null) board.setBoardTheme(theme);
            applySettingsTheme(theme);
        });
        controlsBodyPanel.add(createDropdownRow("Themes", themesDropdown));
        addGap(8);

        pieceStyleDropdown = createDropdown(new String[] {
            "Default", "Classic", "Neo", "Minimal"
        });
        pieceStyleDropdown.addActionListener(e -> {
            if (board != null) board.repaint();
        });
        controlsBodyPanel.add(createDropdownRow("Piece Style", pieceStyleDropdown));

        addSection("Gameplay");

        showLegalMovesToggle = createToggle(true);
        showLegalMovesToggle.addActionListener(e -> board.setShowLegalMoves(showLegalMovesToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Legal Moves", showLegalMovesToggle));
        addGap(8);

        premovingToggle = createToggle(false);
        premovingToggle.addActionListener(e -> board.setPremovingEnabled(premovingToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Premoves", premovingToggle));
        addGap(8);

        autoQueenToggle = createToggle(false);
        autoQueenToggle.addActionListener(e -> board.setAutoQueenEnabled(autoQueenToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Auto Queen", autoQueenToggle));

        addSection("Audio and Visuals");

        soundsToggle = createToggle(SoundManager.isSoundEnabled());
        soundsToggle.addActionListener(e -> SoundManager.setSoundEnabled(soundsToggle.isSelected()));
        controlsBodyPanel.add(createToggleRow("Sounds", soundsToggle));
        addGap(8);

        animationsDropdown = createDropdown(new String[] {"Slow", "Normal", "Fast"});
        animationsDropdown.setSelectedItem("Normal");
        animationsDropdown.addActionListener(e -> board.setAnimationSpeed((String) animationsDropdown.getSelectedItem()));
        controlsBodyPanel.add(createDropdownRow("Animations", animationsDropdown));
        addGap(8);

        lowTimeAlertsToggle = createToggle(true);
        controlsBodyPanel.add(createToggleRow("Low Time Alerts", lowTimeAlertsToggle));

        addSection("Analysis");

        evalBarToggle = createToggle(true);
        controlsBodyPanel.add(createToggleRow("Eval Bar", evalBarToggle));
        addGap(8);

        bestMoveToggle = createToggle(false);
        controlsBodyPanel.add(createToggleRow("Indicate Best Move", bestMoveToggle));
        addGap(8);

        openingNamesToggle = createToggle(true);
        openingNamesToggle.addActionListener(e -> {
            if (board != null) board.setOpeningNamesEnabled(openingNamesToggle.isSelected());
        });
        controlsBodyPanel.add(createToggleRow("Opening Names", openingNamesToggle));
        addGap(16);

        JButton editHotkeysBtn = createButton("Edit Hotkeys");
        editHotkeysBtn.addActionListener(e -> showHotkeysPopup());
        controlsBodyPanel.add(editHotkeysBtn);
    }

    boolean isDividerResponsiveDropdown(Component component) {
        return component == themesDropdown || component == pieceStyleDropdown;
    }
    
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(BODY_WIDTH, BUTTON_HEIGHT));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFont(new Font("Arial", Font.BOLD, 21));
        btn.setForeground(Color.WHITE);
        btn.setBackground(currentPalette.button);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(currentPalette.border, 2, true));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themedButtons.add(btn);
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(currentPalette.buttonHover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(currentPalette.button);
            }
        });
        
        return btn;
    }

    private JPanel createToggleRow(String text, JToggleButton toggle) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, TOGGLE_ROW_HEIGHT));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel label = createRowLabel(text);
        row.add(label, BorderLayout.WEST);
        row.add(toggle, BorderLayout.EAST);

        java.awt.event.MouseAdapter clickToToggle = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggle.doClick(0);
            }
        };
        row.addMouseListener(clickToToggle);
        label.addMouseListener(clickToToggle);

        return row;
    }

    private JPanel createDropdownRow(String text, JComboBox<String> dropdown) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, DROPDOWN_ROW_HEIGHT));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.add(createRowLabel(text), BorderLayout.WEST);
        row.add(dropdown, BorderLayout.EAST);
        return row;
    }

    private JLabel createRowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(new Color(232, 232, 232));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JComboBox<String> createDropdown(String[] options) {
        JComboBox<String> dropdown = new SettingsComboBox(options);
        dropdown.setPreferredSize(new Dimension(DROPDOWN_WIDTH, DROPDOWN_HEIGHT));
        dropdown.setMinimumSize(new Dimension(DROPDOWN_WIDTH, DROPDOWN_HEIGHT));
        dropdown.setMaximumSize(new Dimension(DROPDOWN_WIDTH, DROPDOWN_HEIGHT));
        dropdown.setFont(new Font("Arial", Font.BOLD, 17));
        dropdown.setForeground(Color.WHITE);
        dropdown.setBackground(currentPalette.comboBottom);
        dropdown.setOpaque(false);
        dropdown.setFocusable(false);
        dropdown.setBorder(new RoundedComboBorder());
        dropdown.setUI(new SettingsComboBoxUI());
        themedDropdowns.add(dropdown);
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean selectedValue = index < 0;
                label.setFont(dropdown.getFont());
                label.setForeground(selectedValue ? new Color(245, 248, 255) : new Color(232, 238, 248));
                label.setBackground(isSelected ? currentPalette.popupSelection : currentPalette.popup);
                label.setOpaque(!selectedValue);
                label.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
                return label;
            }
        });
        return dropdown;
    }

    private void addSection(String title) {
        if (controlsBodyPanel.getComponentCount() > 0) {
            controlsBodyPanel.add(Box.createVerticalStrut(18));
        }
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 22));
        label.setForeground(currentPalette.section);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabels.add(label);
        controlsBodyPanel.add(label);
        controlsBodyPanel.add(Box.createVerticalStrut(10));
    }

    private void addGap(int height) {
        controlsBodyPanel.add(Box.createVerticalStrut(Math.round(height * 1.2f) + 7));
    }

    private void showHotkeysPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Edit Hotkeys", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(38, 42, 49), 0, getHeight(), new Color(25, 28, 34)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(currentPalette.border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(new Color(255, 255, 255, 24));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JLabel titleLabel = new JLabel("Edit Hotkeys", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 34));
        titleLabel.setForeground(new Color(245, 248, 252));
        content.add(titleLabel, BorderLayout.NORTH);

        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        addHotkeyHeader(table);

        String[][] hotkeys = {
            {"Start Game", "Enter"},
            {"Reset Board", "Ctrl + R"},
            {"Exit to Menu", "Shift + Esc"},
            {"Flip Board", "Ctrl + F"},
            {"Auto Flip Board", "Ctrl + Shift + F"},
            {"Coordinates", "Ctrl + C"},
            {"Animations", "Ctrl + Shift + A"},
            {"Open Settings", "Shift + Down"},
            {"Close Settings", "Shift + Up"},
            {"Cancel Selection", "Esc"}
        };
        for (int i = 0; i < hotkeys.length; i++) {
            addHotkeyRow(table, i + 1, hotkeys[i][0], hotkeys[i][1]);
        }
        content.add(table, BorderLayout.CENTER);

        JButton closeBtn = createPopupButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.add(closeBtn);
        content.add(footer, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.setContentPane(content);
        dialog.pack();
        applyRoundedDialogShape(dialog, 22);
        dialog.setLocationRelativeTo(owner == null ? this : owner);
        dialog.setVisible(true);
    }

    private void addHotkeyHeader(JPanel table) {
        String[] headers = {"Function", "Current Hotkey", "Edit Hotkey"};
        for (int i = 0; i < headers.length; i++) {
            JLabel label = createHotkeyCell(headers[i], true);
            GridBagConstraints gbc = hotkeyCellConstraints(i, 0);
            gbc.insets = new Insets(0, i == 0 ? 0 : 16, 10, 0);
            table.add(label, gbc);
        }
    }

    private void addHotkeyRow(JPanel table, int row, String function, String currentHotkey) {
        table.add(createHotkeyCell(function, false), hotkeyCellConstraints(0, row));
        table.add(createHotkeyCell(currentHotkey, false), hotkeyCellConstraints(1, row));

        JButton editBtn = createPopupButton("Edit");
        editBtn.setPreferredSize(new Dimension(92, 32));
        editBtn.setEnabled(false);
        GridBagConstraints gbc = hotkeyCellConstraints(2, row);
        table.add(editBtn, gbc);
    }

    private JLabel createHotkeyCell(String text, boolean header) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", header ? Font.BOLD : Font.PLAIN, header ? 16 : 15));
        label.setForeground(header ? currentPalette.section : new Color(232, 238, 248));
        label.setPreferredSize(new Dimension(header ? 150 : 150, header ? 28 : 34));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private GridBagConstraints hotkeyCellConstraints(int column, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = column == 0 ? 1.0 : 0.0;
        gbc.insets = new Insets(0, column == 0 ? 0 : 16, row == 0 ? 0 : 7, 0);
        return gbc;
    }

    private JButton createPopupButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(126, 40));
        button.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(currentPalette.button);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentPalette.border, 1, true),
            BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) button.setBackground(currentPalette.buttonHover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) button.setBackground(currentPalette.button);
            }
        });
        return button;
    }

    private void applyRoundedDialogShape(JDialog dialog, int arc) {
        try {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc));
        } catch (UnsupportedOperationException ignored) {
            // The painted panel still keeps the popup rounded when shaped windows are unavailable.
        }
    }

    private JToggleButton createToggle(boolean selected) {
        JToggleButton toggle = new AnimatedToggleButton(selected);
        toggle.setFocusPainted(false);
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setOpaque(false);
        toggle.setPreferredSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        toggle.setMinimumSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        toggle.setMaximumSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themedToggles.add(toggle);
        return toggle;
    }

    private void applySettingsTheme(String themeName) {
        currentPalette = ThemePalette.forName(themeName);
        for (JButton button : themedButtons) {
            button.setBackground(currentPalette.button);
            button.setBorder(BorderFactory.createLineBorder(currentPalette.border, 2, true));
            button.repaint();
        }
        for (JLabel label : sectionLabels) {
            label.setForeground(currentPalette.section);
            label.repaint();
        }
        for (JToggleButton toggle : themedToggles) {
            toggle.repaint();
        }
        for (JComboBox<String> dropdown : themedDropdowns) {
            dropdown.setBackground(currentPalette.comboBottom);
            dropdown.setBorder(new RoundedComboBorder());
            dropdown.repaint();
        }
        if (controlsCard != null) controlsCard.repaint();
        if (controlsBodyPanel != null) controlsBodyPanel.repaint();
    }

    private void toggleSettingsDropdown() {
        int from = settingsBodyCurrentHeight;
        int to = settingsExpanded ? 0 : settingsBodyExpandedHeight;
        if (from == to) {
            settingsExpanded = !settingsExpanded;
            updateChevron();
            return;
        }

        settingsExpanded = !settingsExpanded;
        updateChevron();

        if (settingsAnimTimer != null && settingsAnimTimer.isRunning()) {
            settingsAnimTimer.stop();
        }

        final long start = System.currentTimeMillis();
        final int durationMs = 200;
        settingsAnimTimer = new Timer(16, e -> {
            float t = (System.currentTimeMillis() - start) / (float) durationMs;
            if (t >= 1f) t = 1f;
            float eased = 1f - (float) Math.pow(1f - t, 3);
            int h = from + Math.round((to - from) * eased);
            applySettingsBodyHeight(h);
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
            }
        });
        settingsAnimTimer.start();
    }

    private void applySettingsBodyHeight(int h) {
        if (controlsBodyHostPanel == null) return;
        int clamped = Math.max(0, Math.min(settingsBodyExpandedHeight, h));
        settingsBodyCurrentHeight = clamped;
        int panelHeight = HEADER_HEIGHT + clamped;
        setPreferredSize(new Dimension(CARD_WIDTH, panelHeight));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, panelHeight));
        controlsBodyHostPanel.setPreferredSize(new Dimension(BODY_HOST_WIDTH, clamped));
        controlsBodyHostPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, clamped));
        controlsBodyHostPanel.revalidate();
        if (controlsScrollPane != null) {
            controlsScrollPane.setPreferredSize(new Dimension(BODY_HOST_WIDTH, clamped));
            controlsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, clamped));
            controlsScrollPane.revalidate();
        }
        controlsCard.revalidate();
        controlsCard.repaint();
        revalidate();
        repaint();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void updateChevron() {
        if (expandChevronLabel != null) {
            expandChevronLabel.repaint();
        }
    }

    public void flipBoard() {
        if (board != null) board.flipBoard();
    }

    public void toggleShowCoordinates() {
        if (showCoordinatesToggle != null) showCoordinatesToggle.doClick();
    }

    public void toggleAnimations() {
        if (animationsDropdown == null) return;
        String current = (String) animationsDropdown.getSelectedItem();
        if ("Slow".equals(current)) {
            animationsDropdown.setSelectedItem("Normal");
        } else if ("Normal".equals(current)) {
            animationsDropdown.setSelectedItem("Fast");
        } else {
            animationsDropdown.setSelectedItem("Slow");
        }
    }

    public void toggleAutoFlip() {
        if (autoFlipToggle != null) autoFlipToggle.doClick();
    }

    public void expandDropdown() {
        setDropdownExpanded(true);
    }

    public void collapseDropdown() {
        setDropdownExpanded(false);
    }

    private void setDropdownExpanded(boolean expanded) {
        if (settingsExpanded == expanded) return;
        toggleSettingsDropdown();
    }

    private static class ThemePalette {
        final Color section;
        final Color button;
        final Color buttonHover;
        final Color border;
        final Color borderBright;
        final Color comboTop;
        final Color comboBottom;
        final Color comboOpenTop;
        final Color comboOpenBottom;
        final Color popup;
        final Color popupSelection;
        final Color toggleOn;
        final Color chevron;

        ThemePalette(Color section, Color button, Color buttonHover, Color border, Color borderBright,
                     Color comboTop, Color comboBottom, Color comboOpenTop, Color comboOpenBottom,
                     Color popup, Color popupSelection, Color toggleOn, Color chevron) {
            this.section = section;
            this.button = button;
            this.buttonHover = buttonHover;
            this.border = border;
            this.borderBright = borderBright;
            this.comboTop = comboTop;
            this.comboBottom = comboBottom;
            this.comboOpenTop = comboOpenTop;
            this.comboOpenBottom = comboOpenBottom;
            this.popup = popup;
            this.popupSelection = popupSelection;
            this.toggleOn = toggleOn;
            this.chevron = chevron;
        }

        static ThemePalette forName(String name) {
            if ("Green".equals(name)) {
                return create(new Color(139, 206, 96));
            }
            if ("Gray".equals(name)) {
                return create(new Color(154, 166, 174));
            }
            if ("Purple".equals(name)) {
                return create(new Color(146, 104, 210));
            }
            if ("Red".equals(name)) {
                return create(new Color(214, 88, 88));
            }
            if ("Orange".equals(name)) {
                return create(new Color(214, 135, 61));
            }
            if ("Yellow".equals(name)) {
                return create(new Color(235, 181, 28));
            }
            if ("Pink".equals(name)) {
                return create(new Color(220, 105, 165));
            }
            return create(new Color(98, 165, 225));
        }

        private static ThemePalette create(Color accent) {
            Color dark = mix(accent, new Color(30, 34, 40), 0.58f);
            Color darker = mix(accent, new Color(24, 27, 32), 0.72f);
            Color hover = mix(dark, accent, 0.18f);
            Color border = mix(accent, Color.WHITE, 0.35f);
            Color bright = mix(accent, Color.WHITE, 0.55f);
            Color popupSelection = mix(accent, new Color(38, 42, 48), 0.30f);
            return new ThemePalette(
                mix(accent, Color.WHITE, 0.42f),
                dark,
                hover,
                border,
                bright,
                mix(accent, new Color(45, 55, 70), 0.36f),
                darker,
                mix(accent, new Color(48, 60, 78), 0.25f),
                mix(accent, new Color(35, 43, 55), 0.50f),
                new Color(36, 40, 47),
                popupSelection,
                accent,
                mix(accent, Color.WHITE, 0.70f)
            );
        }

        private static Color mix(Color a, Color b, float amountOfB) {
            float t = Math.max(0f, Math.min(1f, amountOfB));
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }
    }

    private static class AnimatedToggleButton extends JToggleButton {
        private static final Color OFF = new Color(84, 84, 84);
        private float progress;
        private float knobProgress;
        private float target;
        private float startProgress;
        private float startKnobProgress;
        private long animStartNanos;
        private static final int ANIM_MS = 145;
        private final Timer animTimer;

        AnimatedToggleButton(boolean initialSelected) {
            setSelected(initialSelected);
            progress = initialSelected ? 1f : 0f;
            knobProgress = progress;
            target = progress;

            animTimer = new Timer(16, e -> {
                float t = (System.nanoTime() - animStartNanos) / (ANIM_MS * 1_000_000f);
                if (t >= 1f) t = 1f;
                float trackEase = 1f - (float) Math.pow(1f - t, 3);
                float knobEase = easeOutBack(t);
                progress = startProgress + (target - startProgress) * trackEase;
                knobProgress = startKnobProgress + (target - startKnobProgress) * knobEase;
                if (t >= 1f) {
                    progress = target;
                    knobProgress = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });

            addItemListener(e -> {
                startProgress = progress;
                startKnobProgress = knobProgress;
                target = isSelected() ? 1f : 0f;
                animStartNanos = System.nanoTime();
                if (animTimer.isRunning()) animTimer.restart();
                else animTimer.start();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = h;

            g2.setColor(lerp(OFF, currentPalette.toggleOn, progress));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            int knob = h - 4;
            float clampedKnobProgress = Math.max(0f, Math.min(1f, knobProgress));
            int x = Math.round(2 + (w - knob - 4) * clampedKnobProgress);
            g2.setColor(new Color(236, 236, 236));
            g2.fillOval(x, 2, knob, knob);
            g2.setColor(new Color(0, 0, 0, 65));
            g2.drawOval(x, 2, knob, knob);
            g2.dispose();
        }

        private static Color lerp(Color a, Color b, float t) {
            float clamped = Math.max(0f, Math.min(1f, t));
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
            return new Color(r, g, bl);
        }

        private static float easeOutBack(float t) {
            float c1 = 1.9f;
            float c3 = c1 + 1f;
            float u = t - 1f;
            return 1f + c3 * u * u * u + c1 * u * u;
        }
    }

    private static class SettingsComboBox extends JComboBox<String> {
        SettingsComboBox(String[] options) {
            super(options);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean open = isPopupVisible();

            g2.setPaint(new GradientPaint(
                0, 0, open ? currentPalette.comboOpenTop : currentPalette.comboTop,
                0, h, open ? currentPalette.comboOpenBottom : currentPalette.comboBottom
            ));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

            g2.setColor(open ? currentPalette.borderBright : currentPalette.border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            g2.setColor(new Color(255, 255, 255, open ? 45 : 26));
            g2.drawLine(1, 1, w - 2, 1);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static class ScrollableBodyPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 14;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(14, visibleRect.height - 14);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class SettingsComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            return new ChevronButton();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            comboBox.setOpaque(false);
            comboBox.setBorder(new RoundedComboBorder());
        }

        @Override
        protected ComboPopup createPopup() {
            return new RoundedComboPopup(comboBox);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Background is painted by SettingsComboBox for a cleaner rounded shape.
        }
    }

    private static class ChevronButton extends JButton {
        ChevronButton() {
            setPreferredSize(new Dimension(42, DROPDOWN_HEIGHT));
            setMinimumSize(new Dimension(42, DROPDOWN_HEIGHT));
            setMaximumSize(new Dimension(42, DROPDOWN_HEIGHT));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(new Color(20, 26, 36, 95));
            g2.drawLine(0, 8, 0, h - 9);

            int cx = getWidth() / 2;
            int cy = h / 2;
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(currentPalette.chevron);
            g2.drawLine(cx - 7, cy - 3, cx, cy + 5);
            g2.drawLine(cx, cy + 5, cx + 7, cy - 3);
            g2.dispose();
        }
    }

    private static class RoundedComboBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(2, 2, 2, 2);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentPalette.border);
            g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g2.dispose();
        }
    }

    private static class RoundedComboPopup extends BasicComboPopup {
        private static final int OPEN_ANIMATION_MS = 150;
        private static final int CLOSE_ANIMATION_MS = 120;
        private static final float COLLAPSED_HEIGHT_RATIO = 0.90f;
        private Timer openTimer;
        private Timer closeTimer;
        private long openStartedAtNanos;
        private float openProgress = 1f;
        private int targetWidth;
        private int targetHeight;
        private boolean closing;
        private boolean forceHide;

        RoundedComboPopup(JComboBox combo) {
            super(combo);
            setOpaque(false);
            setBorder(new RoundedPopupBorder());
        }

        @Override
        protected JScrollPane createScroller() {
            JScrollPane scroller = super.createScroller();
            scroller.setOpaque(false);
            scroller.getViewport().setOpaque(false);
            scroller.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            JScrollBar bar = scroller.getVerticalScrollBar();
            bar.setPreferredSize(new Dimension(8, 0));
            bar.setOpaque(false);
            bar.setUI(new SettingsScrollBarUI());
            return scroller;
        }

        @Override
        protected JList createList() {
            JList list = super.createList();
            list.setOpaque(false);
            list.setBackground(currentPalette.popup);
            list.setSelectionBackground(currentPalette.popupSelection);
            list.setSelectionForeground(Color.WHITE);
            list.setFixedCellHeight(40);
            return list;
        }

        @Override
        public void show() {
            if (openTimer != null) openTimer.stop();
            if (closeTimer != null) {
                closeTimer.stop();
                closeTimer = null;
            }
            closing = false;
            Dimension naturalSize = getPreferredSize();
            targetWidth = Math.max(comboBox.getWidth(), DROPDOWN_WIDTH);
            targetHeight = naturalSize.height;
            openProgress = 0f;
            setPopupSize(new Dimension(
                    targetWidth,
                    Math.max(12, Math.round(targetHeight * COLLAPSED_HEIGHT_RATIO))));
            super.show();

            openStartedAtNanos = System.nanoTime();
            openTimer = new Timer(16, e -> {
                float elapsedMs = (System.nanoTime() - openStartedAtNanos) / 1_000_000f;
                float t = Math.max(0f, Math.min(1f, elapsedMs / OPEN_ANIMATION_MS));
                openProgress = 1f - (1f - t) * (1f - t) * (1f - t);
                int height = Math.round(targetHeight * (
                        COLLAPSED_HEIGHT_RATIO + (1f - COLLAPSED_HEIGHT_RATIO) * openProgress));
                setPopupSize(new Dimension(targetWidth, Math.max(12, height)));
                revalidate();
                repaint();
                if (t >= 1f) {
                    ((Timer) e.getSource()).stop();
                    openTimer = null;
                    openProgress = 1f;
                    setPopupSize(new Dimension(targetWidth, targetHeight));
                }
            });
            openTimer.setCoalesce(true);
            openTimer.setInitialDelay(0);
            openTimer.start();
        }

        @Override
        public void hide() {
            startCloseAnimation();
        }

        @Override
        public void setVisible(boolean visible) {
            if (!visible && !forceHide && isVisible()) {
                startCloseAnimation();
                return;
            }
            super.setVisible(visible);
        }

        private void startCloseAnimation() {
            if (closing || !isVisible()) return;
            if (openTimer != null) {
                openTimer.stop();
                openTimer = null;
            }
            closing = true;
            float startProgress = openProgress;
            long closeStartedAtNanos = System.nanoTime();
            closeTimer = new Timer(16, e -> {
                float elapsedMs = (System.nanoTime() - closeStartedAtNanos) / 1_000_000f;
                float t = Math.max(0f, Math.min(1f, elapsedMs / CLOSE_ANIMATION_MS));
                float eased = t * t * (3f - 2f * t);
                openProgress = startProgress * (1f - eased);
                int height = Math.round(targetHeight * (
                        COLLAPSED_HEIGHT_RATIO + (1f - COLLAPSED_HEIGHT_RATIO) * openProgress));
                setPopupSize(new Dimension(targetWidth, Math.max(12, height)));
                revalidate();
                repaint();
                if (t >= 1f) {
                    ((Timer) e.getSource()).stop();
                    closeTimer = null;
                    closing = false;
                    openProgress = 1f;
                    forceHide = true;
                    try {
                        RoundedComboPopup.super.hide();
                    } finally {
                        forceHide = false;
                    }
                }
            });
            closeTimer.setCoalesce(true);
            closeTimer.setInitialDelay(0);
            closeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.72f + 0.28f * openProgress));
            g2.setColor(currentPalette.popup);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.62f + 0.38f * openProgress));
            g2.translate(0, Math.round((1f - openProgress) * -4f));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    private static class RoundedPopupBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentPalette.border);
            g2.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            g2.dispose();
        }
    }

    private static class SettingsScrollBarUI extends BasicScrollBarUI {
        private static final Color THUMB = new Color(112, 145, 198, 220);
        private static final Color THUMB_HOVER = new Color(138, 170, 224, 235);
        private static final Color TRACK = new Color(28, 28, 28, 90);

        @Override
        protected void configureScrollBarColors() {
            thumbColor = THUMB;
            thumbHighlightColor = THUMB_HOVER;
            thumbDarkShadowColor = THUMB;
            thumbLightShadowColor = THUMB;
            trackColor = TRACK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TRACK);
            g2.fillRoundRect(trackBounds.x + 4, trackBounds.y, 4, trackBounds.height, 4, 4);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? THUMB_HOVER : THUMB);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
    }
}
