import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.function.Consumer;

public class MainWindow extends JFrame {

  public MainWindow() {
    setTitle("MainWindow");
    setSize(1920, 1080);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setUndecorated(true);
    setContentPane(createContentPanel());
    installAltF4Close(this);
    setLocationRelativeTo(null);
  }

  static void installAltF4Close(JFrame frame) {
    JRootPane rootPane = frame.getRootPane();
    InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = rootPane.getActionMap();
    inputMap.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK),
        "closeApplication");
    actionMap.put("closeApplication", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
      }
    });
  }

  static JPanel createContentPanel() {
    return createContentPanel(null);
  }

  static JPanel createContentPanel(Consumer<String> actionHandler) {
    JPanel content = new JPanel(new BorderLayout());
    content.add(new AtmosphereCanvas(
        loadBackgroundImage(),
        loadImage("/assets/icon.png", "src/assets/icon.png"),
        loadImage("/assets/headers/ScaccoText.png", "src/assets/headers/ScaccoText.png"),
        loadFont("src/assets/fonts/Cinzel.ttf", new Font("Serif", Font.PLAIN, 12)),
        loadFont("src/assets/fonts/Inter.ttf", new Font("SansSerif", Font.PLAIN, 12)),
        actionHandler),
        BorderLayout.CENTER);
    return content;
  }

  private static Image loadImage(String resourcePath, String filePath) {
    URL resource = MainWindow.class.getResource(resourcePath);
    if (resource != null) return new ImageIcon(resource).getImage();
    ImageIcon icon = new ImageIcon(filePath);
    return icon.getIconWidth() > 0 ? icon.getImage() : null;
  }

  private static Font loadFont(String path, Font fallback) {
    try (FileInputStream input = new FileInputStream(new File(path))) {
      return Font.createFont(Font.TRUETYPE_FONT, input);
    } catch (FontFormatException | IOException ignored) {
      return fallback;
    }
  }

  private static Image loadBackgroundImage() {
    URL resource = MainWindow.class.getResource("/assets/bg2.png");
    if (resource != null) {
      return new ImageIcon(resource).getImage();
    }

    String[] paths = {
        "src/assets/bg2.png",
        "Scaccomatto_final/Scaccomatto/src/assets/bg2.png",
        "assets/bg2.png"
    };
    for (String path : paths) {
      ImageIcon icon = new ImageIcon(path);
      if (icon.getIconWidth() > 0) {
        return icon.getImage();
      }
    }
    return null;
  }

  private static final class AtmosphereCanvas extends Canvas {
    private static final int ANIMATION_FPS = 60;
    private static final long FRAME_DURATION_NANOS = 1_000_000_000L / ANIMATION_FPS;
    private static final int FIREFLY_COUNT = 24;
    private static final int MENU_ITEM_COUNT = 8;
    private static final String[] MENU_TITLES = {
        "Play with Friend",
        "Play with Bot",
        "Online",
        "Analysis",
        "Puzzles",
        "Lessons"
    };
    private static final String[] MENU_SUBTITLES = {
        "Local 2-player",
        "Choose difficulty",
        "Matchmaking & rooms",
        "Review games, best moves",
        "Tactics & puzzle rush",
        "Guided training paths"
    };
    private final Image backgroundImage;
    private final Image logoImage;
    private final Image headerImage;
    private final Font titleFont;
    private final Font bodyFont;
    private final Font fallbackHeaderFont;
    private final Font menuTitleFont;
    private final Font menuSubtitleFont;
    private final Font menuFooterFont;
    private final Consumer<String> actionHandler;
    private final long animationStartedAt = System.nanoTime();
    private final FireflyLifecycle[] fireflyLifecycles = createFireflyLifecycles();
    private final float[] hoverAmount = new float[MENU_ITEM_COUNT];
    private volatile boolean rendering;
    private volatile boolean resetBufferStrategy = true;
    private volatile int hoveredItem = -1;
    private Thread renderThread;
    private BufferedImage cachedBackground;
    private BufferedImage atmosphereBuffer;
    private int cachedWidth;
    private int cachedHeight;

    AtmosphereCanvas(
        Image backgroundImage,
        Image logoImage,
        Image headerImage,
        Font titleFont,
        Font bodyFont,
        Consumer<String> actionHandler) {
      this.backgroundImage = backgroundImage;
      this.logoImage = logoImage;
      this.headerImage = headerImage;
      this.titleFont = titleFont;
      this.bodyFont = bodyFont;
      this.fallbackHeaderFont = titleFont.deriveFont(Font.PLAIN, 58f);
      this.menuTitleFont = bodyFont.deriveFont(Font.BOLD, 24f);
      this.menuSubtitleFont = bodyFont.deriveFont(Font.PLAIN, 17f);
      this.menuFooterFont = bodyFont.deriveFont(Font.PLAIN, 18f);
      this.actionHandler = actionHandler;
      setBackground(Color.BLACK);
      setIgnoreRepaint(true);
      addHierarchyListener(event -> {
        if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
        if (isShowing()) {
          startRendering();
        } else {
          stopRendering();
        }
      });
      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent event) {
          hoveredItem = menuItemAt(event.getX(), event.getY());
          setCursor(hoveredItem >= 0
              ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
              : Cursor.getDefaultCursor());
        }
      });
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseExited(MouseEvent event) {
          hoveredItem = -1;
          setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mousePressed(MouseEvent event) {
          int item = menuItemAt(event.getX(), event.getY());
          if (item >= 0 && item < MENU_TITLES.length) {
            openMenuSection(item);
          } else if (item == 6) {
            dispatchMenuCommand("ACCOUNT");
          } else if (item == 7) {
            Window window = SwingUtilities.getWindowAncestor(AtmosphereCanvas.this);
            if (window != null) {
              window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
          }
        }
      });
    }

    private void openMenuSection(int item) {
      String[] commands = {
          "PLAY_FRIEND",
          "PLAY_BOT",
          "ONLINE",
          "ANALYSIS",
          "PUZZLES",
          "LESSONS"
      };
      if (item < 0 || item >= commands.length) return;

      dispatchMenuCommand(commands[item]);
    }

    private void dispatchMenuCommand(String command) {
      SwingUtilities.invokeLater(() -> {
        if (actionHandler != null) {
          actionHandler.accept(command);
        }
      });
    }

    @Override
    public void addNotify() {
      super.addNotify();
      if (isShowing()) {
        startRendering();
      }
    }

    @Override
    public void removeNotify() {
      stopRendering();
      super.removeNotify();
    }

    private synchronized void startRendering() {
      if (rendering) return;
      resetBufferStrategy = true;
      rendering = true;
      renderThread = new Thread(this::renderLoop, "main-window-atmosphere");
      renderThread.setDaemon(true);
      renderThread.start();
    }

    private synchronized void stopRendering() {
      rendering = false;
      resetBufferStrategy = true;
      if (renderThread != null) {
        renderThread.interrupt();
        renderThread = null;
      }
    }

    private void renderLoop() {
      long nextFrameAt = System.nanoTime();
      while (rendering && isDisplayable() && isShowing()) {
        try {
          if (resetBufferStrategy) {
            BufferStrategy existing = getBufferStrategy();
            if (existing != null) existing.dispose();
            resetBufferStrategy = false;
          }
          if (getBufferStrategy() == null) {
            createBufferStrategy(3);
          }
          renderFrame();

          nextFrameAt += FRAME_DURATION_NANOS;
          long remainingNanos = nextFrameAt - System.nanoTime();
          if (remainingNanos > 0L) {
            long sleepMillis = remainingNanos / 1_000_000L;
            int sleepNanos = (int) (remainingNanos % 1_000_000L);
            Thread.sleep(sleepMillis, sleepNanos);
          } else if (remainingNanos < -FRAME_DURATION_NANOS * 2L) {
            nextFrameAt = System.nanoTime();
          }
        } catch (IllegalStateException ignored) {
          sleepBriefly();
          nextFrameAt = System.nanoTime();
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    private void renderFrame() {
      BufferStrategy strategy = getBufferStrategy();
      if (strategy == null) return;

      do {
        do {
          Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
          try {
            g.setComposite(AlphaComposite.Src);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            drawScene(g);
          } finally {
            g.dispose();
          }
        } while (strategy.contentsRestored());
        strategy.show();
        Toolkit.getDefaultToolkit().sync();
      } while (strategy.contentsLost());
    }

    private void drawScene(Graphics2D g) {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      drawBackground(g);
      double seconds = (System.nanoTime() - animationStartedAt) / 1_000_000_000.0;
      drawAtmosphere(g, seconds);
      drawMenu(g);
    }

    private void drawAtmosphere(Graphics2D g, double seconds) {
      int width = Math.max(1, getWidth() / 2);
      int height = Math.max(1, getHeight() / 2);
      if (atmosphereBuffer == null
          || atmosphereBuffer.getWidth() != width
          || atmosphereBuffer.getHeight() != height) {
        atmosphereBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      }

      Graphics2D fx = atmosphereBuffer.createGraphics();
      fx.setComposite(AlphaComposite.Clear);
      fx.fillRect(0, 0, width, height);
      fx.setComposite(AlphaComposite.SrcOver);
      fx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      drawFog(fx, seconds, width, height);
      drawFireflies(fx, seconds, width, height);
      fx.dispose();

      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(atmosphereBuffer, 0, 0, getWidth(), getHeight(), null);
    }

    private void drawMenu(Graphics2D graphics) {
      int width = getWidth();
      int height = getHeight();
      if (width <= 0 || height <= 0) return;

      double scale = Math.min(width / 1920.0, height / 1080.0);
      double offsetX = (width - 1920.0 * scale) / 2.0;
      double offsetY = (height - 1080.0 * scale) / 2.0;
      Graphics2D g = (Graphics2D) graphics.create();
      g.translate(offsetX, offsetY);
      g.scale(scale, scale);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      g.setPaint(new GradientPaint(
          0, 0, new Color(1, 9, 13, 218),
          880, 0, new Color(1, 8, 11, 18)));
      g.fillRect(0, 0, 920, 1080);

      drawHeader(g);
      drawMenuCard(g);
      g.dispose();
    }

    private void drawHeader(Graphics2D g) {
      if (logoImage != null) {
        g.drawImage(logoImage, 43, 25, 132, 142, this);
      }

      if (headerImage != null) {
        g.drawImage(headerImage, 190, -22, 650, 240, this);
      } else {
        g.setColor(new Color(232, 185, 104));
        g.setFont(fallbackHeaderFont);
        g.drawString("SCACCOMATTO", 190, 112);
      }
    }

    private void drawMenuCard(Graphics2D g) {
      int x = 78;
      int y = 205;
      int width = 680;
      int height = 795;
      Shape card = new RoundRectangle2D.Float(x, y, width, height, 22, 22);

      g.setColor(new Color(1, 8, 12, 208));
      g.fill(card);
      g.setStroke(new BasicStroke(1.3f));
      g.setColor(new Color(198, 145, 61, 190));
      g.draw(card);

      for (int i = 0; i < MENU_ITEM_COUNT; i++) {
        float target = hoveredItem == i ? 1f : 0f;
        hoverAmount[i] += (target - hoverAmount[i]) * 0.62f;
        if (Math.abs(target - hoverAmount[i]) < 0.01f) {
          hoverAmount[i] = target;
        }
      }

      int rowY = 226;
      int rowHeight = 111;
      for (int i = 0; i < MENU_TITLES.length; i++) {
        drawMenuRow(g, i, x, rowY + i * rowHeight, width, rowHeight);
      }
      drawFooter(g, x, y + 681, width, height - 681);
    }

    private void drawMenuRow(
        Graphics2D g,
        int index,
        int x,
        int y,
        int width,
        int height) {
      float hover = hoverAmount[index];
      if (hover > 0.002f) {
        g.setPaint(new GradientPaint(
            x, y, new Color(220, 166, 76, Math.round(48 * hover)),
            x + width, y, new Color(220, 166, 76, 0)));
        g.fillRoundRect(x + 1, y, width - 2, height, 18, 18);
        g.setColor(new Color(239, 182, 76, Math.round(255 * hover)));
        g.fillRoundRect(x + 1, y + 8, 7, height - 16, 7, 7);
      }

      int iconX = x + 88;
      int centerY = y + 53;
      drawMenuIcon(g, index, iconX, centerY, new Color(226, 177, 91));

      g.setColor(new Color(91, 90, 84, 155));
      g.drawLine(x + 147, y + 28, x + 147, y + 77);

      g.setFont(menuTitleFont);
      g.setColor(new Color(242, 238, 229));
      g.drawString(MENU_TITLES[index], x + 180, y + 47);
      g.setFont(menuSubtitleFont);
      g.setColor(new Color(174, 151, 115));
      g.drawString(MENU_SUBTITLES[index], x + 180, y + 76);

      g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(new Color(190, 158, 106));
      int arrowX = x + width - 62 + Math.round(4 * hover);
      g.drawLine(arrowX - 7, centerY - 9, arrowX + 2, centerY);
      g.drawLine(arrowX + 2, centerY, arrowX - 7, centerY + 9);

      if (index < MENU_TITLES.length - 1) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(118, 117, 108, 72));
        g.drawLine(x + 38, y + height - 1, x + width - 38, y + height - 1);
      }
    }

    private void drawFooter(Graphics2D g, int x, int y, int width, int height) {
      g.setColor(new Color(118, 117, 108, 82));
      g.drawLine(x + 38, y, x + width - 38, y);

      drawAccountButton(g, x + 38, y + 32, 185, 48, hoverAmount[6]);
      drawExitButton(g, x + width - 223, y + 32, 185, 48, hoverAmount[7]);
    }

    private void drawAccountButton(Graphics2D g, int x, int y, int width, int height, float hover) {
      Graphics2D button = (Graphics2D) g.create();
      button.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if (hover > 0.002f) {
        for (int i = 4; i >= 1; i--) {
          button.setStroke(new BasicStroke(i * 3f));
          button.setColor(new Color(235, 178, 56, Math.round(10f * hover)));
          button.drawRoundRect(x, y, width, height, height, height);
        }
      }

      button.setPaint(new GradientPaint(
          x, y, blend(new Color(13, 17, 18), new Color(39, 31, 13), hover),
          x + width, y + height, new Color(4, 7, 8, 244)));
      button.fillRoundRect(x, y, width, height, height, height);
      button.setStroke(new BasicStroke(2f));
      button.setColor(blend(
          new Color(222, 166, 48),
          new Color(255, 205, 82),
          hover));
      button.drawRoundRect(x + 1, y + 1, width - 3, height - 3, height, height);

      int iconX = x + 37;
      int centerY = y + height / 2;
      button.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      button.drawOval(iconX - 6, centerY - 14, 12, 12);
      button.drawArc(iconX - 12, centerY, 24, 17, 0, 180);
      button.drawLine(iconX - 12, centerY + 8, iconX + 12, centerY + 8);

      int dividerX = x + 63;
      button.setStroke(new BasicStroke(1f));
      button.setColor(new Color(196, 148, 49, 170));
      button.drawLine(dividerX, y + 17, dividerX, y + height - 17);

      button.setFont(bodyFont.deriveFont(Font.BOLD, 17f));
      button.setColor(blend(
          new Color(232, 179, 62),
          new Color(255, 215, 104),
          hover));
      FontMetrics metrics = button.getFontMetrics();
      int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
      button.drawString("Account", dividerX + 15, textY);
      button.dispose();
    }

    private void drawExitButton(Graphics2D g, int x, int y, int width, int height, float hover) {
      Graphics2D button = (Graphics2D) g.create();
      button.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if (hover > 0.002f) {
        for (int i = 4; i >= 1; i--) {
          button.setStroke(new BasicStroke(i * 3f));
          button.setColor(new Color(255, 17, 42, Math.round(13f * hover)));
          button.drawRoundRect(x, y, width, height, height, height);
        }
      }

      button.setPaint(new GradientPaint(
          x, y, blend(new Color(32, 4, 8), new Color(68, 5, 12), hover),
          x + width, y + height, new Color(7, 3, 5, 248)));
      button.fillRoundRect(x, y, width, height, height, height);

      Color red = blend(new Color(241, 18, 42), new Color(255, 55, 70), hover);
      button.setColor(red);
      button.setStroke(new BasicStroke(2f));
      button.drawRoundRect(x + 1, y + 1, width - 3, height - 3, height, height);

      int iconX = x + 39;
      int centerY = y + height / 2;
      button.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      button.drawRoundRect(iconX - 13, centerY - 14, 19, 28, 2, 2);
      button.drawLine(iconX - 1, centerY, iconX + 19, centerY);
      button.drawLine(iconX + 12, centerY - 7, iconX + 19, centerY);
      button.drawLine(iconX + 19, centerY, iconX + 12, centerY + 7);

      int dividerX = x + 71;
      button.setStroke(new BasicStroke(1f));
      button.setColor(new Color(231, 21, 43, 190));
      button.drawLine(dividerX, y + 10, dividerX, y + height - 10);

      button.setFont(bodyFont.deriveFont(Font.BOLD, 19f));
      button.setColor(red);
      FontMetrics metrics = button.getFontMetrics();
      int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
      button.drawString("Exit", dividerX + 27, textY);
      button.dispose();
    }

    private int menuItemAt(int mouseX, int mouseY) {
      double scale = Math.min(getWidth() / 1920.0, getHeight() / 1080.0);
      if (scale <= 0.0) return -1;
      double offsetX = (getWidth() - 1920.0 * scale) / 2.0;
      double offsetY = (getHeight() - 1080.0 * scale) / 2.0;
      double x = (mouseX - offsetX) / scale;
      double y = (mouseY - offsetY) / scale;
      if (x >= 78 && x <= 758 && y >= 226 && y < 892) {
        return Math.min(5, (int) ((y - 226) / 111));
      }
      if (y >= 918 && y <= 966 && x >= 116 && x <= 301) return 6;
      if (y >= 918 && y <= 966 && x >= 535 && x <= 720) return 7;
      return -1;
    }

    private void drawMenuIcon(Graphics2D g, int type, int x, int y, Color color) {
      Graphics2D icon = (Graphics2D) g.create();
      icon.setColor(color);
      icon.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      switch (type) {
        case 0:
          icon.fillOval(x - 20, y - 20, 19, 19);
          icon.fillOval(x + 4, y - 17, 16, 16);
          icon.fillRoundRect(x - 24, y + 2, 29, 20, 10, 10);
          icon.fillRoundRect(x + 4, y + 4, 22, 18, 9, 9);
          break;
        case 1:
          icon.drawLine(x, y - 24, x, y - 15);
          icon.drawLine(x - 5, y - 20, x, y - 15);
          icon.drawLine(x + 5, y - 20, x, y - 15);
          icon.fillRoundRect(x - 22, y - 13, 44, 34, 8, 8);
          icon.setColor(new Color(7, 14, 16));
          icon.fillOval(x - 11, y - 3, 6, 6);
          icon.fillOval(x + 5, y - 3, 6, 6);
          icon.fillRoundRect(x - 8, y + 9, 16, 4, 2, 2);
          icon.setColor(color);
          icon.drawLine(x - 27, y - 4, x - 27, y + 12);
          icon.drawLine(x + 27, y - 4, x + 27, y + 12);
          break;
        case 2:
          icon.drawOval(x - 21, y - 22, 42, 44);
          icon.drawOval(x - 10, y - 22, 20, 44);
          icon.drawLine(x - 21, y, x + 21, y);
          icon.drawArc(x - 19, y - 14, 38, 28, 0, 180);
          icon.drawArc(x - 19, y - 14, 38, 28, 180, 180);
          break;
        case 3:
          icon.drawRoundRect(x - 21, y - 20, 42, 40, 5, 5);
          icon.drawLine(x - 14, y + 10, x - 4, y);
          icon.drawLine(x - 4, y, x + 4, y + 6);
          icon.drawLine(x + 4, y + 6, x + 15, y - 10);
          icon.drawLine(x + 10, y - 10, x + 15, y - 10);
          icon.drawLine(x + 15, y - 10, x + 15, y - 5);
          break;
        case 4:
          icon.fillRoundRect(x - 18, y - 18, 36, 36, 5, 5);
          icon.fillOval(x - 7, y - 26, 14, 16);
          icon.fillOval(x + 11, y - 7, 16, 14);
          icon.setColor(new Color(4, 12, 15));
          icon.fillOval(x - 7, y + 11, 14, 16);
          break;
        case 5:
          Polygon cap = new Polygon(
              new int[]{x - 25, x, x + 25, x},
              new int[]{y - 8, y - 21, y - 8, y + 5},
              4);
          icon.fillPolygon(cap);
          icon.drawLine(x - 17, y, x - 17, y + 14);
          icon.draw(new QuadCurve2D.Float(
              x - 17, y + 14,
              x, y + 27,
              x + 17, y + 14));
          icon.drawLine(x + 17, y, x + 17, y + 14);
          icon.drawLine(x + 25, y - 8, x + 25, y + 12);
          icon.fillOval(x + 22, y + 10, 6, 6);
          break;
        default:
          break;
      }
      icon.dispose();
    }

    private Color blend(Color from, Color to, float amount) {
      float t = Math.max(0f, Math.min(1f, amount));
      return new Color(
          Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
          Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
          Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
          Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }

    private void sleepBriefly() {
      try {
        Thread.sleep(50L);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    }

    private void drawBackground(Graphics2D g) {
      int width = getWidth();
      int height = getHeight();
      if (backgroundImage == null) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        return;
      }
      if (cachedBackground == null || cachedWidth != width || cachedHeight != height) {
        cachedBackground = new BufferedImage(
            Math.max(1, width),
            Math.max(1, height),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D backgroundGraphics = cachedBackground.createGraphics();
        backgroundGraphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        backgroundGraphics.setColor(Color.BLACK);
        backgroundGraphics.fillRect(0, 0, width, height);

        int imageWidth = Math.max(1, backgroundImage.getWidth(this));
        int imageHeight = Math.max(1, backgroundImage.getHeight(this));
        double scale = Math.max(width / (double) imageWidth, height / (double) imageHeight);
        int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
        int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
        int drawX = (width - drawWidth) / 2;
        int drawY = (height - drawHeight) / 2;
        backgroundGraphics.drawImage(
            backgroundImage,
            drawX,
            drawY,
            drawWidth,
            drawHeight,
            this);
        backgroundGraphics.dispose();
        cachedWidth = width;
        cachedHeight = height;
      }

      g.drawImage(cachedBackground, 0, 0, null);
    }

    private void drawFog(Graphics2D g, double seconds, int width, int height) {
      double fogSeconds = seconds - 1.0;
      if (fogSeconds < 0.0) return;

      drawFogBank(g, width, height, fogSeconds, 0.08, 0.24, 1.12, 0.48, 145, 0.00, 1.00);
      drawFogBank(g, width, height, fogSeconds, 0.38, 0.43, 1.02, 0.43, 132, 1.75, -1.28);
      drawFogBank(g, width, height, fogSeconds, 0.76, 0.24, 0.92, 0.39, 118, 3.40, 1.58);
      drawFogBank(g, width, height, fogSeconds, 0.59, 0.70, 1.10, 0.46, 128, 5.10, -1.12);
      drawFogBank(g, width, height, fogSeconds, 0.90, 0.53, 0.82, 0.38, 110, 7.20, 1.82);
      drawFogBank(g, width, height, fogSeconds, 0.30, 0.84, 0.96, 0.36, 104, 8.60, -2.00);
      drawFogBank(g, width, height, fogSeconds, 0.12, 0.60, 0.76, 0.31, 96, 10.10, 2.25);
      drawFogBank(g, width, height, fogSeconds, 0.82, 0.88, 0.88, 0.34, 92, 11.70, -2.40);
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
      double direction = Math.signum(speed);
      double movementSpeed = Math.abs(speed);
      int fogWidth = Math.max(1, (int) Math.round(width * widthRatio));
      int fogHeight = Math.max(1, (int) Math.round(height * heightRatio));
      double entranceDelay = (phase % 3.0) * 0.12;
      double entranceProgress = smoothStep((float) (
          Math.max(0.0, seconds - entranceDelay) / 2.4));
      double entranceStartX = direction > 0.0
          ? -fogWidth * 0.52
          : width + fogWidth * 0.52;
      double entranceTargetX = width * baseX;

      double movementSeconds = Math.max(0.0, seconds - entranceDelay - 2.4);
      double travel = (movementSeconds * 0.035 * movementSpeed + baseX) % 1.4;
      if (travel < 0.0) travel += 1.4;
      double travelX = direction > 0.0 ? travel - 0.2 : 1.2 - travel;
      double normalX = movementSeconds <= 0.0
          ? entranceTargetX
          : travelX * width;
      normalX += Math.sin(seconds * 0.34 * movementSpeed + phase) * width * 0.09;
      double centerXValue = entranceStartX
          + (normalX - entranceStartX) * entranceProgress;
      double driftY = Math.cos(seconds * 0.23 * movementSpeed + phase * 0.7)
          * height * 0.10;
      float centerX = (float) centerXValue;
      float centerY = (float) (height * baseY + driftY);
      float radius = Math.max(fogWidth, fogHeight) * 0.52f;
      double densitySeed = Math.sin(phase * 12.9898 + speed * 78.233) * 43758.5453;
      double randomDensity = 0.55 + (densitySeed - Math.floor(densitySeed)) * 0.45;
      float opacityPulse = (float) (0.82
          + 0.18 * Math.sin(seconds * 0.65 * movementSpeed + phase));
      int animatedAlpha = Math.max(
          1,
          Math.min(190, Math.round(
              maxAlpha
                  * 0.5f
                  * (float) randomDensity
                  * opacityPulse
                  * (float) entranceProgress)));

      Graphics2D fog = (Graphics2D) g.create();
      fog.scale(1.0, fogHeight / (double) fogWidth);
      float scaledCenterY = (float) (centerY * fogWidth / (double) fogHeight);
      fog.setPaint(new RadialGradientPaint(
          new Point2D.Float(centerX, scaledCenterY),
          Math.max(1f, radius),
          new float[]{0f, 0.42f, 1f},
          new Color[]{
              new Color(174, 184, 183, animatedAlpha),
              new Color(112, 124, 124, Math.max(1, Math.round(animatedAlpha * 0.68f))),
              new Color(45, 55, 57, 0)
          }));
      int drawSize = Math.round(radius * 2f);
      fog.fillOval(
          Math.round(centerX - radius),
          Math.round(scaledCenterY - radius),
          drawSize,
          drawSize);
      fog.dispose();
    }

    private void drawFireflies(Graphics2D g, double seconds, int width, int height) {
      float effectScale = width / (float) Math.max(1, getWidth());
      for (int i = 0; i < FIREFLY_COUNT; i++) {
        float visibility = fireflyLifecycles[i].visibilityAt(seconds);
        if (visibility <= 0.001f) continue;

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
        int coreSize = Math.max(1, Math.round((2 + i % 3) * effectScale));
        int glowRadius = Math.max(2, Math.round((8 + i % 7) * effectScale));
        int glowAlpha = Math.round((28 + 72 * pulse) * visibility);
        int coreAlpha = Math.round((115 + 140 * pulse) * visibility);
        drawFirefly(g, x, y, glowRadius, coreSize, glowAlpha, coreAlpha);
      }
    }

    private FireflyLifecycle[] createFireflyLifecycles() {
      Random random = new Random(0x5CA77E);
      FireflyLifecycle[] lifecycles = new FireflyLifecycle[FIREFLY_COUNT];
      for (int i = 0; i < lifecycles.length; i++) {
        lifecycles[i] = new FireflyLifecycle(
            1.0 + random.nextDouble() * 4.5,
            0.55 + random.nextDouble() * 0.85,
            3.5 + random.nextDouble() * 7.0,
            0.65 + random.nextDouble() * 1.10,
            1.5 + random.nextDouble() * 5.5);
      }
      return lifecycles;
    }

    private static final class FireflyLifecycle {
      private final double initialDelay;
      private final double fadeInDuration;
      private final double visibleDuration;
      private final double fadeOutDuration;
      private final double hiddenDuration;

      FireflyLifecycle(
          double initialDelay,
          double fadeInDuration,
          double visibleDuration,
          double fadeOutDuration,
          double hiddenDuration) {
        this.initialDelay = initialDelay;
        this.fadeInDuration = fadeInDuration;
        this.visibleDuration = visibleDuration;
        this.fadeOutDuration = fadeOutDuration;
        this.hiddenDuration = hiddenDuration;
      }

      float visibilityAt(double seconds) {
        if (seconds < initialDelay) return 0f;

        double cycleDuration = fadeInDuration
            + visibleDuration
            + fadeOutDuration
            + hiddenDuration;
        double cycleTime = (seconds - initialDelay) % cycleDuration;
        if (cycleTime < fadeInDuration) {
          return smoothStep((float) (cycleTime / fadeInDuration));
        }

        cycleTime -= fadeInDuration;
        if (cycleTime < visibleDuration) return 1f;

        cycleTime -= visibleDuration;
        if (cycleTime < fadeOutDuration) {
          return 1f - smoothStep((float) (cycleTime / fadeOutDuration));
        }
        return 0f;
      }
    }

    private static float smoothStep(float value) {
      float t = Math.max(0f, Math.min(1f, value));
      return t * t * (3f - 2f * t);
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
          new Point2D.Float(x, y),
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
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Intro::new);
  }
}
