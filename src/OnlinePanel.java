import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class OnlinePanel extends JPanel {

    private static class TimePreset {
        final String label;
        final int initialSeconds;
        final int incrementSeconds;

        TimePreset(String label, int initialSeconds, int incrementSeconds) {
            this.label = label;
            this.initialSeconds = initialSeconds;
            this.incrementSeconds = incrementSeconds;
        }
    }

    private final MainMenu parent;
    private JTextField ipField;
    private JTextField portField;
    private JButton hostBtn;
    private JButton joinBtn;
    private JLabel statusLabel;
    private NetworkManager net;

    private String selectedVariant = "Classic";
    private TimePreset selectedPreset = new TimePreset("10 min", 600, 0);

    private final List<JButton> variantButtons = new ArrayList<>();
    private final List<JButton> timeButtons = new ArrayList<>();

    private final Color BG = new Color(23, 26, 31);
    private final Color CARD = new Color(34, 38, 45);
    private final Color CARD_BORDER = new Color(78, 84, 97);
    private final Color CARD_BORDER_INNER = new Color(48, 54, 65);
    private final Color TEXT_MAIN = Color.WHITE;
    private final Color TEXT_SUB = new Color(184, 191, 205);
    private final Color SELECTED = new Color(78, 109, 156);
    private final Color SELECTED_BORDER = new Color(116, 153, 210);
    private final Color BTN_BG = new Color(50, 55, 66);
    private final Color BTN_HOVER = new Color(66, 73, 87);
    private final Color BTN_DISABLED = new Color(62, 65, 72);
    private final Font TITLE_FONT = new Font("Bahnschrift", Font.BOLD, 50);
    private final Font SECTION_FONT = new Font("Bahnschrift", Font.BOLD, 34);
    private final Font LABEL_FONT = new Font("Bahnschrift", Font.BOLD, 19);
    private final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 18);

    public OnlinePanel(MainMenu parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 34, 22, 34));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        updateVariantUI();
        updateTimeUI();
        updateActionAvailability();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        JLabel title = new JLabel("Multiplayer");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_MAIN);

        JLabel subtitle = new JLabel("Choose your format, set the clock, then host or join instantly");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(TEXT_SUB);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        JLabel badge = new JLabel("LIVE LOBBY");
        badge.setFont(new Font("Bahnschrift", Font.BOLD, 13));
        badge.setForeground(new Color(16, 23, 35));
        badge.setOpaque(true);
        badge.setBackground(new Color(130, 166, 214));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(157, 190, 232), 1, true),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        header.add(text, BorderLayout.WEST);
        header.add(badgeWrap, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMainContent() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2, 24, 0));
        wrapper.setOpaque(false);

        wrapper.add(buildVariantPanel());
        wrapper.add(buildLobbyPanel());
        return wrapper;
    }

    private JPanel buildVariantPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel sectionTitle = new JLabel("Chess Variants");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setForeground(TEXT_MAIN);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(leftAlignedRow(sectionTitle));
        body.add(Box.createVerticalStrut(4));

        JLabel sectionSub = new JLabel("Pick a mode for your online match");
        sectionSub.setFont(BODY_FONT);
        sectionSub.setForeground(TEXT_SUB);
        sectionSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(leftAlignedRow(sectionSub));
        body.add(Box.createVerticalStrut(12));

        body.add(createVariantButton("Classic", "Standard chess rules", true));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Chess960", "Randomized back rank setup!", false));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("King of the Hill", "Reach the center with your king", false));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Three-check", "Win by giving three checks", true));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Atomic", "Captures explode nearby pieces!", true));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Spell Chess", "Cast spells and see the magic uncover!", true));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Fog of War", "Limited vision outside piece range", true));
        body.add(Box.createVerticalStrut(10));
        body.add(createVariantButton("Duck Chess", "Place the duck after each move", false));

        body.add(Box.createVerticalGlue());

        JScrollPane scroller = new JScrollPane(body);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(14);
        scroller.getVerticalScrollBar().setBackground(new Color(34, 38, 45));
        scroller.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(90, 96, 109);
                trackColor = new Color(30, 34, 41);
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
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 10, 10);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.dispose();
            }
        });

        panel.add(scroller, BorderLayout.CENTER);
        return panel;
    }

    private JButton createVariantButton(String name, String desc, boolean supported) {
        RoundedButton btn = new RoundedButton("<html><div style='text-align:left;'><b>" + name + "</b><br>" + desc + "</div></html>", 16);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setIconTextGap(22);
        btn.setPreferredSize(new Dimension(320, 86));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        btn.setFont(BODY_FONT);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(BTN_BG);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 26, 4, 8));
        btn.setBorderColor(CARD_BORDER);

        if (!supported) {
            btn.setText("<html><div style='text-align:left;'><b>" + name + " (Soon)</b><br>" + desc + "</div></html>");
        }

        Icon icon = loadVariantIcon(name);
        if (icon != null) {
            btn.setIcon(icon);
        }

        btn.addActionListener(e -> {
            selectedVariant = name;
            updateVariantUI();
            if (!supported) {
                setStatus(name + " is coming soon. Stay Tuned!");
            } else {
                setStatus("Selected variant: " + selectedVariant);
            }
            updateActionAvailability();
        });

        addHover(btn);
        variantButtons.add(btn);
        return btn;
    }

    private Icon loadVariantIcon(String name) {
        String file = null;
        String key = name.toLowerCase();
        if (key.contains("classic")) {
            file = "classic.png";
        } else if (key.contains("three-check") || key.contains("three check") || key.contains("3check") || key.contains("3-check")) {
            file = "3checks.png";
        } else if (key.contains("960")) {
            file = "960.png";
        } else if (key.contains("atomic")) {
            file = "atomic.png";
        } else if (key.contains("duck")) {
            file = "duck.png";
        } else if (key.contains("fog")) {
            file = "fogofwar.png";
        } else if (key.contains("king")) {
            file = "kinghill.png";
        } else if (key.contains("spell")) {
            file = "spellchess.png";
        }
        if (file == null) return null;

        int size = 68;
        String resPath = "/assets/multiplayer/" + file;
        try {
            URL url = getClass().getResource(resPath);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    return new RoundedImageIcon(img, size);
                }
            }
        } catch (IOException ignored) {
        }

        String[] fs = {
            "Scaccomatto_final/Scaccomatto/src/assets/multiplayer/" + file,
            "src/assets/multiplayer/" + file,
            "assets/multiplayer/" + file
        };
        for (String path : fs) {
            try {
                File f = new File(path);
                if (!f.exists()) continue;
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    return new RoundedImageIcon(img, size);
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }

    private static class RoundedImageIcon implements Icon {
        private final Image image;
        private final int imageSize;
        private final int size;

        RoundedImageIcon(BufferedImage img, int size) {
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

            g2.setColor(new Color(200, 170, 240, 0));
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

    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fill;
        private final Color border;
        private final Color borderInner;

        RoundedPanel(int arc, Color fill, Color border, Color borderInner) {
            this.arc = arc;
            this.fill = fill;
            this.border = border;
            this.borderInner = borderInner;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(borderInner);
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final int arc;
        private Color borderColor;

        RoundedButton(String text, int arc) {
            super(text);
            this.arc = arc;
            setContentAreaFilled(false);
            setOpaque(false);
        }

        void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (borderColor == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }

    private static class RoundedTextField extends JTextField {
        private final int arc;
        private final Color borderColor = new Color(90, 96, 109);

        RoundedTextField(String text, int arc) {
            super(text);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }

    private JPanel buildLobbyPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel sectionTitle = new JLabel("Create / Join Game");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setForeground(TEXT_MAIN);
        sectionTitle.setHorizontalAlignment(SwingConstants.LEFT);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, sectionTitle.getPreferredSize().height));
        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        JLabel logo = loadLogoLabel(40);
        if (logo != null) {
            logo.setAlignmentY(Component.CENTER_ALIGNMENT);
            logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            titleRow.add(logo);
            titleRow.add(Box.createHorizontalStrut(10));
        }
        sectionTitle.setAlignmentY(Component.CENTER_ALIGNMENT);
        titleRow.add(sectionTitle);
        titleRow.add(Box.createHorizontalGlue());
        body.add(titleRow);
        body.add(Box.createVerticalStrut(0));

        JLabel sectionSub = new JLabel("Classic matchmaking and direct invites");
        sectionSub.setFont(BODY_FONT);
        sectionSub.setForeground(TEXT_SUB);
        sectionSub.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setOpaque(false);
        subRow.setBorder(BorderFactory.createEmptyBorder(0, 56, 0, 0));
        subRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, sectionSub.getPreferredSize().height));
        subRow.add(sectionSub, BorderLayout.WEST);
        body.add(subRow);
        body.add(Box.createVerticalStrut(12));

        JLabel timeLabel = new JLabel("Time Control", SwingConstants.CENTER);
        timeLabel.setFont(LABEL_FONT);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeLabel.getPreferredSize().height));
        body.add(timeLabel);
        body.add(Box.createVerticalStrut(8));

        JPanel timeRow = new JPanel(new GridLayout(1, 4, 10, 0));
        timeRow.setOpaque(false);
        timeRow.add(createTimeButton(new TimePreset("3|2", 180, 2)));
        timeRow.add(createTimeButton(new TimePreset("5 min", 300, 0)));
        timeRow.add(createTimeButton(new TimePreset("10 min", 600, 0)));
        timeRow.add(createTimeButton(new TimePreset("15|10", 900, 10)));
        body.add(timeRow);
        body.add(Box.createVerticalStrut(18));

        JLabel connLabel = new JLabel("Direct Connection", SwingConstants.CENTER);
        connLabel.setFont(LABEL_FONT);
        connLabel.setForeground(Color.WHITE);
        connLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connLabel.setHorizontalAlignment(SwingConstants.CENTER);
        connLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, connLabel.getPreferredSize().height));
        body.add(connLabel);
        body.add(Box.createVerticalStrut(8));   

        ipField = new RoundedTextField("localhost", 12);
        portField = new RoundedTextField("5000", 12);
        styleField(ipField, "IP / Host");
        styleField(portField, "Port");
        body.add(ipField);
        body.add(Box.createVerticalStrut(8));
        body.add(portField);
        body.add(Box.createVerticalStrut(18));

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 12, 0));
        actionRow.setOpaque(false);
        hostBtn = createActionButton("Host Game", new Color(118, 150, 86), new Color(140, 175, 105));
        joinBtn = createActionButton("Join Game", new Color(62, 96, 139), new Color(82, 118, 166));
        hostBtn.addActionListener(e -> doHost());
        joinBtn.addActionListener(e -> doJoin());
        actionRow.add(hostBtn);
        actionRow.add(joinBtn);
        body.add(actionRow);
        body.add(Box.createVerticalGlue());

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JLabel loadLogoLabel(int size) {
        String resPath = "/assets/logo.png";
        try {
            URL url = getClass().getResource(resPath);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    JLabel label = new JLabel(new ImageIcon(scaled));
                    label.setOpaque(false);
                    return label;
                }
            }
        } catch (IOException ignored) {
        }

        String[] fs = {
            "Scaccomatto_final/Scaccomatto/src/assets/logo.png",
            "src/assets/logo.png",
            "assets/logo.png"
        };
        for (String path : fs) {
            try {
                File f = new File(path);
                if (!f.exists()) continue;
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    JLabel label = new JLabel(new ImageIcon(scaled));
                    label.setOpaque(false);
                    return label;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_MAIN);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        RoundedButton back = new RoundedButton("Back to Menu", 14);
        back.setFont(new Font("Bahnschrift", Font.BOLD, 16));
        back.setForeground(TEXT_MAIN);
        back.setBackground(new Color(66, 71, 83));
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        back.setBorderColor(new Color(108, 114, 126));
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                back.setBackground(new Color(78, 84, 98));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                back.setBackground(new Color(66, 71, 83));
            }
        });
        back.addActionListener(e -> parent.actionPerformed(new ActionEvent(this, 0, "BACK_TO_MENU")));

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    private JPanel createCardPanel() {
        RoundedPanel panel = new RoundedPanel(18, CARD, CARD_BORDER, CARD_BORDER_INNER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }

    private JPanel leftAlignedRow(JComponent component) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        row.add(component, BorderLayout.WEST);
        return row;
    }

    private JButton createTimeButton(TimePreset preset) {
        RoundedButton btn = new RoundedButton(preset.label, 12);
        btn.setFont(new Font("Bahnschrift", Font.BOLD, 20));
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(BTN_BG);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 62));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btn.setBorderColor(CARD_BORDER);
        btn.addActionListener(e -> {
            selectedPreset = preset;
            updateTimeUI();
            setStatus("Time selected: " + preset.label);
        });
        addHover(btn);
        timeButtons.add(btn);
        return btn;
    }

    private JButton createActionButton(String text, Color base, Color hover) {
        RoundedButton btn = new RoundedButton(text, 18);
        btn.setFont(new Font("Bahnschrift", Font.BOLD, 38));
        btn.setForeground(Color.WHITE);
        btn.setBackground(base);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 96));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setBorderColor(hover);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(base);
            }
        });
        return btn;
    }

    private void styleField(JTextField field, String tooltip) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 19));
        field.setBackground(new Color(30, 34, 41));
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(TEXT_MAIN);
        field.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        field.setToolTipText(tooltip);
    }

    private void addHover(JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelectedButton(btn)) btn.setBackground(BTN_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelectedButton(btn)) btn.setBackground(BTN_BG);
            }
        });
    }

    private boolean isSelectedButton(JButton btn) {
        return btn.getBackground().equals(SELECTED);
    }

    private void updateVariantUI() {
        for (JButton b : variantButtons) {
            if (b.getText().contains("<b>" + selectedVariant)) {
                b.setBackground(SELECTED);
                if (b instanceof RoundedButton) {
                    ((RoundedButton) b).setBorderColor(SELECTED_BORDER);
                } else {
                    b.setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 2, true));
                }
            } else {
                b.setBackground(BTN_BG);
                if (b instanceof RoundedButton) {
                    ((RoundedButton) b).setBorderColor(CARD_BORDER);
                } else {
                    b.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
                }
            }
        }
    }

    private void updateTimeUI() {
        for (JButton b : timeButtons) {
            if (b.getText().equals(selectedPreset.label)) {
                b.setBackground(SELECTED);
                if (b instanceof RoundedButton) {
                    ((RoundedButton) b).setBorderColor(SELECTED_BORDER);
                } else {
                    b.setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 2, true));
                }
            } else {
                b.setBackground(BTN_BG);
                if (b instanceof RoundedButton) {
                    ((RoundedButton) b).setBorderColor(CARD_BORDER);
                } else {
                    b.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
                }
            }
        }
    }

    private void updateActionAvailability() {
        hostBtn.setEnabled(true);
        joinBtn.setEnabled(true);
        hostBtn.setBackground(new Color(118, 150, 86));
        joinBtn.setBackground(new Color(62, 96, 139));
    }

    private void setStatus(String s) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(s));
    }

    private Integer parsePort() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException ex) {
            setStatus("Invalid port. Use 1-65535.");
            return null;
        }
    }

    private void doHost() {
        Integer port = parsePort();
        if (port == null) return;
        boolean supported = "Classic".equals(selectedVariant) || "Fog of War".equals(selectedVariant) || "Spell Chess".equals(selectedVariant) || "Atomic".equals(selectedVariant) || "Three-check".equals(selectedVariant);
        if (!showLaunchDialog("Host Game", supported)) return;
        if (!supported) {
            setStatus(selectedVariant + " is coming soon in multiplayer.");
            return;
        }

        net = new NetworkManager();
        final ChessGame[] gameRef = new ChessGame[1];
        final List<Runnable> pendingEvents = new ArrayList<>();
        setStatus("Hosting " + selectedVariant + " on port " + port + "... waiting for opponent");
        net.host(port, new NetworkManager.Listener() {
            private void dispatchToGame(Runnable action) {
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
            @Override public void onConnected(boolean amWhite) {
                setStatus("Opponent connected. You are " + (amWhite ? "White" : "Black"));
                parent.launchGameWithOverlay(() -> {
                    ChessGame cg = new ChessGame(parent, selectedPreset.initialSeconds, selectedPreset.incrementSeconds, net, amWhite, selectedVariant);
                    List<Runnable> replay;
                    synchronized (pendingEvents) {
                        gameRef[0] = cg;
                        replay = new ArrayList<>(pendingEvents);
                        pendingEvents.clear();
                    }
                    for (Runnable r : replay) r.run();
                });
            }
            @Override public void onMoveReceived(int fromRow, int fromCol, int toRow, int toCol, String promotion) {
                dispatchToGame(() -> gameRef[0].onRemoteMove(fromRow, fromCol, toRow, toCol, promotion));
            }
            @Override public void onSpellCastReceived(String spellId, boolean casterWhite, SpellTarget target) {
                dispatchToGame(() -> gameRef[0].onRemoteSpellCast(spellId, casterWhite, target));
            }
            @Override public void onSpellPhaseReceived(String phaseId, boolean casterWhite, int row, int col) {
                dispatchToGame(() -> gameRef[0].onRemoteSpellPhase(phaseId, casterWhite, row, col));
            }
            @Override public void onError(String msg) { setStatus("Error: " + msg); }
            @Override public void onDrawOffered() { dispatchToGame(() -> gameRef[0].onRemoteOfferDraw()); }
            @Override public void onDrawAccepted() { dispatchToGame(() -> gameRef[0].onRemoteDrawAccepted()); }
            @Override public void onDrawDeclined() { dispatchToGame(() -> gameRef[0].onRemoteDrawDeclined()); }
            @Override public void onResign() { dispatchToGame(() -> gameRef[0].onRemoteResign()); }
        });
    }

    private void doJoin() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            setStatus("IP / Host cannot be empty.");
            return;
        }

        Integer port = parsePort();
        if (port == null) return;
        boolean supported = "Classic".equals(selectedVariant) || "Fog of War".equals(selectedVariant) || "Spell Chess".equals(selectedVariant) || "Atomic".equals(selectedVariant) || "Three-check".equals(selectedVariant);
        if (!showLaunchDialog("Join Game", supported)) return;
        if (!supported) {
            setStatus(selectedVariant + " is coming soon in multiplayer.");
            return;
        }

        net = new NetworkManager();
        final ChessGame[] gameRef = new ChessGame[1];
        final List<Runnable> pendingEvents = new ArrayList<>();
        setStatus("Connecting to " + ip + ":" + port + "...");
        net.join(ip, port, new NetworkManager.Listener() {
            private void dispatchToGame(Runnable action) {
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
            @Override public void onConnected(boolean amWhite) {
                setStatus("Connected. You are " + (amWhite ? "White" : "Black"));
                parent.launchGameWithOverlay(() -> {
                    ChessGame cg = new ChessGame(parent, selectedPreset.initialSeconds, selectedPreset.incrementSeconds, net, amWhite, selectedVariant);
                    List<Runnable> replay;
                    synchronized (pendingEvents) {
                        gameRef[0] = cg;
                        replay = new ArrayList<>(pendingEvents);
                        pendingEvents.clear();
                    }
                    for (Runnable r : replay) r.run();
                });
            }
            @Override public void onMoveReceived(int fromRow, int fromCol, int toRow, int toCol, String promotion) {
                dispatchToGame(() -> gameRef[0].onRemoteMove(fromRow, fromCol, toRow, toCol, promotion));
            }
            @Override public void onSpellCastReceived(String spellId, boolean casterWhite, SpellTarget target) {
                dispatchToGame(() -> gameRef[0].onRemoteSpellCast(spellId, casterWhite, target));
            }
            @Override public void onSpellPhaseReceived(String phaseId, boolean casterWhite, int row, int col) {
                dispatchToGame(() -> gameRef[0].onRemoteSpellPhase(phaseId, casterWhite, row, col));
            }
            @Override public void onError(String msg) { setStatus("Error: " + msg); }
            @Override public void onDrawOffered() { dispatchToGame(() -> gameRef[0].onRemoteOfferDraw()); }
            @Override public void onDrawAccepted() { dispatchToGame(() -> gameRef[0].onRemoteDrawAccepted()); }
            @Override public void onDrawDeclined() { dispatchToGame(() -> gameRef[0].onRemoteDrawDeclined()); }
            @Override public void onResign() { dispatchToGame(() -> gameRef[0].onRemoteResign()); }
        });
    }

    private boolean showLaunchDialog(String mode, boolean supported) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, mode, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(183, 189, 198), 1, true),
            BorderFactory.createEmptyBorder(18, 18, 14, 18)
        ));

        JLabel title = new JLabel(selectedVariant + (supported ? "" : " (Soon)"), SwingConstants.CENTER);
        title.setFont(new Font("Bahnschrift", Font.BOLD, 38));
        title.setForeground(new Color(43, 47, 56));

        JLabel sub = new JLabel(
            supported ? "Ready to start your online game" : "This variant is not playable yet",
            SwingConstants.CENTER
        );
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 21));
        sub.setForeground(new Color(95, 102, 113));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);
        center.add(Box.createVerticalStrut(10));
        center.add(sub);
        center.add(Box.createVerticalStrut(16));

        String details = mode + "  |  " + selectedPreset.label + "  |  " + ipField.getText().trim() + ":" + portField.getText().trim();
        JLabel detail = new JLabel(details, SwingConstants.CENTER);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        detail.setForeground(new Color(108, 114, 124));
        detail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(205, 210, 218), 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(detail);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        cancel.setFocusPainted(false);
        cancel.setBackground(new Color(223, 226, 232));
        cancel.setForeground(new Color(50, 54, 62));
        cancel.setBorder(BorderFactory.createLineBorder(new Color(194, 199, 208), 1, true));

        JButton play = new JButton(supported ? (mode.startsWith("Host") ? "Host" : "Join") : "Coming Soon");
        play.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        play.setFocusPainted(false);
        play.setBackground(supported ? new Color(132, 184, 84) : new Color(170, 174, 182));
        play.setForeground(Color.WHITE);
        play.setEnabled(supported);
        play.setBorder(BorderFactory.createLineBorder(supported ? new Color(106, 151, 66) : new Color(145, 149, 158), 1, true));

        final boolean[] accepted = new boolean[] { false };
        cancel.addActionListener(e -> dialog.dispose());
        play.addActionListener(e -> {
            accepted[0] = true;
            dialog.dispose();
        });

        actions.add(cancel);
        actions.add(play);

        root.add(center, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setSize(540, 330);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return accepted[0];
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(26, 24, getWidth() - 52, 6, 6, 6);

        g2.dispose();
        super.paintComponent(g);
    }
}
