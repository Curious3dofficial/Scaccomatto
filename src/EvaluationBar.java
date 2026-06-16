import javax.swing.*;
import java.awt.*;

public class EvaluationBar extends JPanel {

    private int evaluation = 0;
    private static final int MAX_EVAL = 1000;            // ±10 pawns full-scale

    // animated fill
    private double currentPct = 0.5;
    private double targetPct  = 0.5;
    private Timer  animTimer;

    // mate overlay
    private boolean isMate      = false;
    private int     mateIn      = 0;

    public EvaluationBar() {
        setPreferredSize(new Dimension(30, 560));
        setBackground(new Color(64, 64, 64));

        animTimer = AnimationTiming.createUiTimer(e -> {
            double diff = targetPct - currentPct;
            if (Math.abs(diff) < 0.002) {
                currentPct = targetPct;
                animTimer.stop();
            } else {
                double frameAdjustedEase = 1.0 - Math.pow(
                        1.0 - 0.14,
                        AnimationTiming.FRAME_SCALE_FROM_16_MS);
                currentPct += diff * frameAdjustedEase;
            }
            repaint();
        });
    }

    // ── public setters ──────────────────────────────────────────────────────
    public void setEvaluation(int centipawns) {
        this.evaluation = centipawns;
        isMate = false;

        int clamped = Math.max(-MAX_EVAL, Math.min(MAX_EVAL, centipawns));
        targetPct = 0.5 + (clamped / (2.0 * MAX_EVAL));

        if (!animTimer.isRunning()) animTimer.start();
    }

    public void setMate(int mateInMoves, boolean whiteIsMating) {
        this.isMate      = true;
        this.mateIn      = mateInMoves;
        targetPct  = whiteIsMating ? 0.95 : 0.05;
        evaluation = whiteIsMating ? 10000 : -10000;
        if (!animTimer.isRunning()) animTimer.start();
    }

    // ── painting ────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int whiteH = (int) Math.round(h * currentPct);
        int blackH = h - whiteH;

        // fills
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(0, 0, w, blackH);
        g2.setColor(new Color(240, 240, 240));
        g2.fillRect(0, blackH, w, whiteH);

        // border
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(0, 0, w - 1, h - 1);

        // label
        String label;
        if (isMate) {
            label = "M" + mateIn;
        } else if (evaluation == 0) {
            label = "0.0";
        } else {
            label = String.format("%+.1f", evaluation / 100.0);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(label)) / 2;

        int textY;
        Color textColor;
        int sectionH;

        if (currentPct >= 0.5) {
            // white section (bottom) is bigger – centre text inside it
            sectionH  = whiteH;
            textY     = blackH + whiteH / 2 + fm.getAscent() / 2 - 1;
            textColor = Color.BLACK;
        } else {
            // black section (top) is bigger – centre text inside it
            sectionH  = blackH;
            textY     = blackH / 2 + fm.getAscent() / 2 - 1;
            textColor = Color.WHITE;
        }

        if (sectionH >= fm.getHeight() + 6) {
            g2.setColor(textColor);
            g2.drawString(label, textX, textY);
        }
    }
}
