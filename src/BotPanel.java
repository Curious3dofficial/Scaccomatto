import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class BotPanel extends JPanel {

    public interface BotSelectionListener {
        void onBotSelected(ChessBot bot);
    }

    private ChessBot selectedBot = ChessBot.CASUAL_CARL;
    private ActionListener startGameListener;
    private ActionListener spectateListener;
    private BotSelectionListener botSelectionListener;

    private final Color PANEL_BG = new Color(5, 10, 12, 128);
    private final Color CARD_BG = new Color(8, 13, 15, 142);
    private final Color CARD_HOVER = new Color(26, 24, 19, 170);
    private final Color SELECTED = new Color(50, 39, 24, 186);
    private final Color SELECTED_BORDER = new Color(247, 185, 65);
    private final Color BORDER = new Color(83, 70, 51, 190);
    private final Color TEXT_MAIN = new Color(244, 238, 225);
    private static final int PANEL_ARC = 18;
    private static final int CARD_ARC = 14;
    private static final int BUTTON_ARC = 12;

    public BotPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rebuild();
    }

    private void rebuild() {
        removeAll();

        JPanel content = new RoundedPanel(
                new BorderLayout(),
                PANEL_BG,
                new Color(76, 72, 65, 190),
                new Color(139, 103, 50, 70),
                PANEL_ARC);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(18, 16, 16, 16));

        content.add(Box.createVerticalStrut(4));

        ChessBot[] bots = ChessBot.getAllBots();
        for (ChessBot bot : bots) {
            JPanel card = createBotCard(bot);
            card.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(card);
            content.add(Box.createVerticalStrut(15));
        }

        content.add(Box.createVerticalGlue());
        content.add(Box.createVerticalStrut(8));
        JPanel actionRow = new JPanel(new GridLayout(1, 2, 14, 0));
        actionRow.setOpaque(false);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        actionRow.setPreferredSize(new Dimension(540, 64));
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = createActionButton("Start Game", new Color(151, 101, 34), new Color(232, 180, 88), new Color(177, 122, 43));
        JButton spectateBtn = createActionButton("Spectate", new Color(8, 13, 15, 150), new Color(151, 105, 46), new Color(29, 25, 18, 185));

        startBtn.addActionListener(e -> {
            if (startGameListener != null) {
                startGameListener.actionPerformed(e);
            }
        });
        spectateBtn.addActionListener(e -> {
            if (spectateListener != null) {
                spectateListener.actionPerformed(e);
            }
        });

        actionRow.add(startBtn);
        actionRow.add(spectateBtn);
        content.add(actionRow);

        add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createBotCard(ChessBot bot) {
        JPanel card = new RoundedPanel(new BorderLayout(), CARD_BG, BORDER, null, CARD_ARC);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
        card.setPreferredSize(new Dimension(560, 128));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel avatar = new JLabel(loadBotPortrait(bot, 92, 92));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(116, 108));
        avatar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 4));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(25, 14, 18, 10));

        JLabel nameLabel = new JLabel(bot.getName());
        boolean isSelected = (bot == selectedBot);
        nameLabel.setFont(UiFonts.title(isSelected ? 30f : 27f));
        nameLabel.setForeground(TEXT_MAIN);

        JLabel detailsLabel = new JLabel("ELO " + bot.getElo() + "  •  " + bot.getDescription());
        detailsLabel.setFont(UiFonts.subtext(18f));
        detailsLabel.setForeground(new Color(195, 153, 91));

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(detailsLabel);

        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 38));
        starsPanel.setOpaque(false);
        starsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        JLabel starsLabel = new JLabel(bot.getDifficultyStars());
        starsLabel.setFont(new Font("DejaVu Sans", Font.PLAIN, 31));
        starsLabel.setForeground(new Color(255, 190, 0));
        starsPanel.add(starsLabel);

        card.add(avatar, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(starsPanel, BorderLayout.EAST);

        if (isSelected) {
            updateCardSelection(card, true);
        }

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedBot = bot;
                if (botSelectionListener != null) {
                    botSelectionListener.onBotSelected(selectedBot);
                }
                rebuild();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (bot != selectedBot) {
                    card.setBackground(CARD_HOVER);
                    card.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (bot != selectedBot) {
                    card.setBackground(CARD_BG);
                    card.repaint();
                }
            }
        });

        return card;
    }

    private void updateCardSelection(JPanel card, boolean selected) {
        Color bgColor = selected ? SELECTED : CARD_BG;
        card.setBackground(bgColor);
        if (card instanceof RoundedPanel) {
            RoundedPanel roundedCard = (RoundedPanel) card;
            roundedCard.setOuterBorderColor(selected ? SELECTED_BORDER : BORDER);
            roundedCard.setInnerBorderColor(null);
            roundedCard.setBorderStroke(selected ? 1.35f : 1f);
            roundedCard.setSelectedGlow(selected);
        }
        card.repaint();
    }

    private JButton createActionButton(String text, Color baseColor, Color borderColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                boolean gold = "Start Game".equals(text);
                boolean hovered = getModel().isRollover();
                Path2D frame = createCutCornerFrame(1, 1, w - 1, h - 1, 8);

                if (gold) {
                    paintGoldButton(g2, frame, w, h, hovered);
                } else {
                    paintDarkButton(g2, frame, w, h, hovered);
                }
                paintActionButtonLabel(g2, text, gold, w, h);
                g2.dispose();
            }
        };
        btn.setFont(UiFonts.title(21f));
        btn.setForeground(new Color(247, 236, 214));
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(250, 64));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
                btn.repaint();
            }
        });

        return btn;
    }

    private static Path2D createCutCornerFrame(int x, int y, int w, int h, int cut) {
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

    private static void paintGoldButton(
            Graphics2D g2, Path2D frame, int width, int height, boolean hovered) {
        Shape oldClip = g2.getClip();
        g2.clip(frame);
        g2.setPaint(new GradientPaint(
                0, 0, hovered ? new Color(205, 147, 55) : new Color(178, 122, 39),
                0, height, hovered ? new Color(132, 76, 20) : new Color(113, 65, 18)));
        g2.fillRect(0, 0, width, height);

        g2.setPaint(new GradientPaint(
                0, 0, new Color(255, 224, 145, 70),
                0, height * 0.55f, new Color(255, 196, 73, 0)));
        g2.fillRect(0, 0, width, height);
        for (int i = 0; i < 34; i++) {
            int seed = i * 73 + 19;
            int x = Math.floorMod(seed * 29, Math.max(1, width));
            int y = Math.floorMod(seed * 47, Math.max(1, height));
            int length = 9 + Math.floorMod(seed, 25);
            int alpha = 9 + Math.floorMod(seed, 14);
            g2.setColor(new Color(255, 225, 154, alpha));
            g2.drawLine(x, y, Math.min(width, x + length), Math.max(0, y - 2));
        }
        g2.setClip(oldClip);

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(104, 58, 15));
        g2.draw(frame);
        Path2D inner = createCutCornerFrame(4, 4, width - 7, height - 7, 6);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(245, 190, 76));
        g2.draw(inner);
        g2.setColor(new Color(255, 238, 174, 185));
        g2.drawLine(14, 4, width - 14, 4);
        g2.setColor(new Color(67, 37, 10, 170));
        g2.drawLine(12, height - 4, width - 12, height - 4);

        int shineWidth = Math.min(90, width / 4);
        g2.setPaint(new GradientPaint(
                width / 2f - shineWidth, 0, new Color(255, 245, 196, 0),
                width / 2f, 0, new Color(255, 245, 196, 180), true));
        g2.fillRect(width / 2 - shineWidth, 2, shineWidth * 2, 3);
    }

    private static void paintDarkButton(
            Graphics2D g2, Path2D frame, int width, int height, boolean hovered) {
        Shape oldClip = g2.getClip();
        g2.clip(frame);
        g2.setPaint(new GradientPaint(
                0, 0, hovered ? new Color(30, 29, 25, 230) : new Color(17, 21, 22, 225),
                0, height, new Color(4, 8, 10, 235)));
        g2.fillRect(0, 0, width, height);
        g2.setPaint(new GradientPaint(
                0, 0, new Color(194, 145, 73, 20),
                width, height, new Color(0, 0, 0, 0)));
        g2.fillRect(0, 0, width, height);
        g2.setClip(oldClip);

        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(85, 62, 36));
        g2.draw(frame);
        g2.setColor(new Color(176, 124, 56, 90));
        g2.draw(createCutCornerFrame(4, 4, width - 7, height - 7, 6));
    }

    private static void paintActionButtonLabel(
            Graphics2D g2, String text, boolean gold, int width, int height) {
        g2.setFont(UiFonts.title(21f));
        FontMetrics metrics = g2.getFontMetrics();
        int iconWidth = 24;
        int gap = 13;
        int textWidth = metrics.stringWidth(text);
        int groupWidth = iconWidth + gap + textWidth;
        int iconX = (width - groupWidth) / 2;
        int centerY = height / 2;
        Color foreground = gold ? new Color(255, 244, 215) : new Color(211, 177, 116);

        g2.setColor(new Color(0, 0, 0, gold ? 95 : 55));
        g2.drawString(text, iconX + iconWidth + gap + 1,
                centerY + (metrics.getAscent() - metrics.getDescent()) / 2 + 1);
        g2.setColor(foreground);
        if (gold) {
            paintCrossedSwords(g2, iconX, centerY, foreground);
        } else {
            paintEye(g2, iconX, centerY, foreground);
        }
        g2.drawString(text, iconX + iconWidth + gap,
                centerY + (metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private static void paintCrossedSwords(Graphics2D g2, int x, int centerY, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 4, centerY - 9, x + 20, centerY + 8);
        g2.drawLine(x + 20, centerY - 9, x + 4, centerY + 8);
        g2.drawLine(x + 2, centerY - 10, x + 7, centerY - 8);
        g2.drawLine(x + 17, centerY - 8, x + 22, centerY - 10);
        g2.drawLine(x + 3, centerY + 5, x + 8, centerY + 10);
        g2.drawLine(x + 16, centerY + 10, x + 21, centerY + 5);
    }

    private static void paintEye(Graphics2D g2, int x, int centerY, Color color) {
        Path2D eye = new Path2D.Float();
        eye.moveTo(x + 1, centerY);
        eye.curveTo(x + 6, centerY - 8, x + 18, centerY - 8, x + 23, centerY);
        eye.curveTo(x + 18, centerY + 8, x + 6, centerY + 8, x + 1, centerY);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(eye);
        g2.fillOval(x + 9, centerY - 3, 6, 6);
    }

    private ImageIcon loadBotPortrait(ChessBot bot, int width, int height) {
        String base = bot == ChessBot.BOBBY_BEGINNER ? "bobby"
                : bot == ChessBot.CASUAL_CARL ? "Carl"
                : bot == ChessBot.TACTICAL_TOM ? "Tom"
                : bot == ChessBot.STRATEGIC_SARAH ? "Sarah"
                : "levy";
        String[] extensions = {"png", "jpg", "jpeg"};
        for (String extension : extensions) {
            try {
                URL url = getClass().getResource("/assets/avatars/" + base + "." + extension);
                if (url != null) {
                    BufferedImage image = ImageIO.read(url);
                    if (image != null) {
                        return new ImageIcon(image.getScaledInstance(
                                width, height, Image.SCALE_SMOOTH));
                    }
                }
            } catch (IOException ignored) {
            }
            try {
                File file = new File("src/assets/avatars/" + base + "." + extension);
                if (file.isFile()) {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        return new ImageIcon(image.getScaledInstance(
                                width, height, Image.SCALE_SMOOTH));
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public void setStartGameListener(ActionListener listener) {
        this.startGameListener = listener;
    }

    public void setSpectateListener(ActionListener listener) {
        this.spectateListener = listener;
    }

    public ChessBot getSelectedBot() {
        return selectedBot;
    }

    public void setBotSelectionListener(BotSelectionListener listener) {
        this.botSelectionListener = listener;
    }

    private static class RoundedPanel extends JPanel {
        private final int arc;
        private Color outerBorderColor;
        private Color innerBorderColor;
        private float borderStroke = 1f;
        private boolean selectedGlow;

        RoundedPanel(LayoutManager layout, Color bg, Color outerBorderColor, Color innerBorderColor, int arc) {
            super(layout);
            this.arc = arc;
            this.outerBorderColor = outerBorderColor;
            this.innerBorderColor = innerBorderColor;
            setBackground(bg);
            setOpaque(false);
        }

        void setOuterBorderColor(Color color) {
            this.outerBorderColor = color;
        }

        void setInnerBorderColor(Color color) {
            this.innerBorderColor = color;
        }

        void setBorderStroke(float stroke) {
            this.borderStroke = stroke;
        }

        void setSelectedGlow(boolean selectedGlow) {
            this.selectedGlow = selectedGlow;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(getBackground());
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
            if (selectedGlow) {
                paintSelectedGlow(g2, w, h);
            }
            if (outerBorderColor != null) {
                g2.setStroke(new BasicStroke(borderStroke));
                g2.setColor(outerBorderColor);
                g2.drawRoundRect(0, 0, w, h, arc, arc);
            }
            if (innerBorderColor != null) {
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(innerBorderColor);
                g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
            }
            g2.dispose();
        }

        private void paintSelectedGlow(Graphics2D g2, int w, int h) {
            for (int inset = 7; inset >= 1; inset--) {
                int alpha = 5 + (8 - inset) * 3;
                int insetArc = Math.max(2, arc - inset);
                g2.setStroke(new BasicStroke(1.4f));
                g2.setColor(new Color(255, 177, 43, alpha));
                g2.drawRoundRect(
                        inset,
                        inset,
                        Math.max(0, w - inset * 2),
                        Math.max(0, h - inset * 2),
                        insetArc,
                        insetArc);
            }

            int highlightWidth = Math.min(260, Math.max(80, w / 3));
            int highlightX = Math.max(8, w - highlightWidth - 8);
            g2.setPaint(new GradientPaint(
                    highlightX, 0, new Color(255, 211, 105, 0),
                    w - 5, 0, new Color(255, 238, 169, 220)));
            g2.fillRect(highlightX, 0, Math.max(1, w - highlightX - 4), 2);

            paintCornerFlare(g2, 4, 4);
            paintCornerFlare(g2, w - 4, 4);
            paintCornerFlare(g2, 4, h - 4);
            paintCornerFlare(g2, w - 4, h - 4);
        }

        private static void paintCornerFlare(Graphics2D g2, int x, int y) {
            float radius = 12f;
            g2.setPaint(new RadialGradientPaint(
                    new Point(x, y),
                    radius,
                    new float[] {0f, 0.24f, 1f},
                    new Color[] {
                        new Color(255, 247, 190, 235),
                        new Color(255, 185, 55, 115),
                        new Color(255, 153, 24, 0)
                    }));
            g2.fillOval(
                    Math.round(x - radius),
                    Math.round(y - radius),
                    Math.round(radius * 2f),
                    Math.round(radius * 2f));
        }
    }
}
