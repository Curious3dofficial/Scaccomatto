import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class CreditsPanel extends JPanel {
    private MainMenu parent;
    private Image backgroundImage;
    private JLabel creditsTitle;
    private JLabel leadProgrammerTitle;
    private JLabel leadProgrammerName;
    private JLabel leadGraphicsTitle;
    private JLabel leadGraphicsName;
    private JButton backButton;
    private JComponent sharedThemeToggle;
    
    public CreditsPanel(MainMenu parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        
        backgroundImage = loadBackgroundImage();
        
        setupUI();
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
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Fallback to light gray if no image
            g.setColor(new Color(230, 230, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        boolean dark = parent != null && parent.isMainMenuDarkMode();
        if (dark) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(7, 10, 16, 148));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        applyTheme();
    }
    
    private void setupUI() {
        // Left container for credits content
        JPanel leftContainer = new JPanel();
        leftContainer.setOpaque(false);
        leftContainer.setLayout(new BoxLayout(leftContainer, BoxLayout.Y_AXIS));
        leftContainer.setBorder(BorderFactory.createEmptyBorder(100, 80, 40, 0));
        
        // Credits title
        creditsTitle = new JLabel("Credits");
        creditsTitle.setFont(new Font("Georgia", Font.BOLD, 72));
        creditsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftContainer.add(creditsTitle);
        leftContainer.add(Box.createVerticalStrut(60));
        
        // Lead Programmer section
        leadProgrammerTitle = new JLabel("Lead Programmer");
        leadProgrammerTitle.setFont(new Font("Arial", Font.BOLD, 36));
        leadProgrammerTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leadProgrammerName = new JLabel("Arpan Gayen XI A");
        leadProgrammerName.setFont(new Font("Serif", Font.ITALIC, 32));
        leadProgrammerName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftContainer.add(leadProgrammerTitle);
        leftContainer.add(Box.createVerticalStrut(10));
        leftContainer.add(leadProgrammerName);
        leftContainer.add(Box.createVerticalStrut(40));
        
        // Lead Graphics Designer section
        leadGraphicsTitle = new JLabel("Lead Graphics Designer");
        leadGraphicsTitle.setFont(new Font("Arial", Font.BOLD, 36));
        leadGraphicsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leadGraphicsName = new JLabel("Aniruddha Mallick XI C");
        leadGraphicsName.setFont(new Font("Serif", Font.ITALIC, 32));
        leadGraphicsName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftContainer.add(leadGraphicsTitle);
        leftContainer.add(Box.createVerticalStrut(10));
        leftContainer.add(leadGraphicsName);
        leftContainer.add(Box.createVerticalStrut(80));
        
        // Back button
        backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.PLAIN, 24));
        backButton.setPreferredSize(new Dimension(150, 50));
        backButton.setMaximumSize(new Dimension(150, 50));
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (parent != null && parent.isMainMenuDarkMode()) {
                    backButton.setBackground(new Color(85, 92, 108));
                } else {
                    backButton.setBackground(new Color(200, 200, 200));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (parent != null && parent.isMainMenuDarkMode()) {
                    backButton.setBackground(new Color(101, 108, 124));
                } else {
                    backButton.setBackground(new Color(220, 220, 220));
                }
            }
        });
        
        backButton.addActionListener(e -> {
            if (parent != null) {
                parent.showMenu();
            }
        });
        
        leftContainer.add(backButton);
        
        // Add some space at the bottom
        leftContainer.add(Box.createVerticalGlue());
        
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(leftContainer, BorderLayout.WEST);

        sharedThemeToggle = (parent != null) ? parent.createSharedThemeToggleControl() : null;
        JLayeredPane overlayPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                content.setBounds(0, 0, getWidth(), getHeight());
                if (sharedThemeToggle != null) {
                    int s = sharedThemeToggle.getPreferredSize().width;
                    int margin = 18; // same as main menu
                    sharedThemeToggle.setBounds(getWidth() - s - margin, getHeight() - s - margin, s, s);
                }
            }
        };
        overlayPane.setOpaque(false);
        overlayPane.add(content, JLayeredPane.DEFAULT_LAYER);
        if (sharedThemeToggle != null) overlayPane.add(sharedThemeToggle, JLayeredPane.PALETTE_LAYER);
        add(overlayPane, BorderLayout.CENTER);

        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleCreditsTheme");
        am.put("toggleCreditsTheme", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (parent == null) return;
                parent.toggleThemeFromComponent(sharedThemeToggle);
                repaint();
            }
        });
        applyTheme();
    }

    private void applyTheme() {
        boolean dark = parent != null && parent.isMainMenuDarkMode();
        Color text = dark ? new Color(232, 239, 248) : Color.BLACK;
        Color accent = dark ? new Color(205, 214, 228) : Color.BLACK;
        if (creditsTitle != null) creditsTitle.setForeground(text);
        if (leadProgrammerTitle != null) leadProgrammerTitle.setForeground(text);
        if (leadProgrammerName != null) leadProgrammerName.setForeground(accent);
        if (leadGraphicsTitle != null) leadGraphicsTitle.setForeground(text);
        if (leadGraphicsName != null) leadGraphicsName.setForeground(accent);
        if (backButton != null) {
            backButton.setBackground(dark ? new Color(101, 108, 124) : new Color(220, 220, 220));
            backButton.setForeground(dark ? new Color(241, 245, 252) : Color.BLACK);
        }
    }
}
