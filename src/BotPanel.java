import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BotPanel extends JPanel {

    public interface BotSelectionListener {
        void onBotSelected(ChessBot bot);
    }

    private ChessBot selectedBot = ChessBot.CASUAL_CARL;
    private ActionListener startGameListener;
    private ActionListener spectateListener;
    private BotSelectionListener botSelectionListener;

    private final Color PANEL_BG = new Color(34, 38, 45);
    private final Color CARD_BG = new Color(50, 55, 66);
    private final Color CARD_HOVER = new Color(66, 73, 87);
    private final Color SELECTED = new Color(78, 109, 156);
    private final Color SELECTED_BORDER = new Color(116, 153, 210);
    private final Color BORDER = new Color(78, 84, 97);
    private final Color TEXT_MAIN = Color.WHITE;
    private final Color TEXT_SUB = new Color(184, 191, 205);
    private static final int PANEL_ARC = 22;
    private static final int CARD_ARC = 18;
    private static final int BUTTON_ARC = 16;

    public BotPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rebuild();
    }

    private void rebuild() {
        removeAll();

        JPanel content = new RoundedPanel(new BorderLayout(), PANEL_BG, new Color(78, 84, 97), new Color(48, 54, 65), PANEL_ARC);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        content.add(Box.createVerticalStrut(4));

        ChessBot[] bots = ChessBot.getAllBots();
        for (ChessBot bot : bots) {
            JPanel card = createBotCard(bot);
            card.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(card);
            content.add(Box.createVerticalStrut(12));
        }

        content.add(Box.createVerticalStrut(16));
        JPanel actionRow = new JPanel(new GridLayout(1, 2, 14, 0));
        actionRow.setOpaque(false);
        actionRow.setMaximumSize(new Dimension(460, 68));
        actionRow.setPreferredSize(new Dimension(460, 68));
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = createActionButton("Start Game", new Color(70, 105, 150), new Color(88, 126, 176), new Color(88, 126, 176));
        JButton spectateBtn = createActionButton("Spectate", new Color(78, 88, 106), new Color(95, 108, 129), new Color(102, 116, 138));

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
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));
        card.setPreferredSize(new Dimension(530, 94));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel nameLabel = new JLabel(bot.getName());
        boolean isSelected = (bot == selectedBot);
        nameLabel.setFont(new Font("Bahnschrift", Font.BOLD, isSelected ? 21 : 17));
        nameLabel.setForeground(TEXT_MAIN);

        JLabel detailsLabel = new JLabel("ELO " + bot.getElo() + " - " + bot.getDescription());
        detailsLabel.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, isSelected ? 14 : 12));
        detailsLabel.setForeground(new Color(208, 214, 226));

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(detailsLabel);

        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 16));
        starsPanel.setOpaque(false);
        starsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JLabel starsLabel = new JLabel(bot.getDifficultyStars());
        starsLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 20));
        starsLabel.setForeground(new Color(255, 215, 0));
        starsPanel.add(starsLabel);

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
            roundedCard.setBorderStroke(selected ? 2f : 1f);
        }
        card.repaint();
    }

    private JButton createActionButton(String text, Color baseColor, Color borderColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, BUTTON_ARC, BUTTON_ARC);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, w, h, BUTTON_ARC, BUTTON_ARC);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Bahnschrift", Font.BOLD, 18));
        btn.setForeground(Color.WHITE);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(223, 68));
        btn.setMaximumSize(new Dimension(223, 68));
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
    }
}
