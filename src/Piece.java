import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public abstract class Piece {
    protected boolean white;
    protected static BufferedImage[][] pieceImages = new BufferedImage[2][6];
    protected int imageIndex;
    private int frozenTurnsRemaining = 0;
    private int shieldedTurnsRemaining = 0;
    private int bombRookTurnsRemaining = 0;
    private int resurrectCooldownTurnsRemaining = 0;
    private int zoglinMoveCount = 0;
    private boolean zoglin = false;

    static {
    try {
        pieceImages[1][0] = ImageIO.read(Piece.class.getResource("/assets/pieces/wk.png"));
        pieceImages[1][1] = ImageIO.read(Piece.class.getResource("/assets/pieces/wq.png"));
        pieceImages[1][2] = ImageIO.read(Piece.class.getResource("/assets/pieces/wr.png"));
        pieceImages[1][3] = ImageIO.read(Piece.class.getResource("/assets/pieces/wb.png"));
        pieceImages[1][4] = ImageIO.read(Piece.class.getResource("/assets/pieces/wn.png"));
        pieceImages[1][5] = ImageIO.read(Piece.class.getResource("/assets/pieces/wp.png"));

        pieceImages[0][0] = ImageIO.read(Piece.class.getResource("/assets/pieces/bk.png"));
        pieceImages[0][1] = ImageIO.read(Piece.class.getResource("/assets/pieces/bq.png"));
        pieceImages[0][2] = ImageIO.read(Piece.class.getResource("/assets/pieces/br.png"));
        pieceImages[0][3] = ImageIO.read(Piece.class.getResource("/assets/pieces/bb.png"));
        pieceImages[0][4] = ImageIO.read(Piece.class.getResource("/assets/pieces/bn.png"));
        pieceImages[0][5] = ImageIO.read(Piece.class.getResource("/assets/pieces/bp.png"));

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public Piece(boolean white, int imageIndex) {
    this.white = white;
    this.imageIndex = imageIndex;
    }


    public boolean isWhite() {
        return white;
    }

    public abstract boolean isValidMove(
        int sr, int sc, int er, int ec, Piece[][] board
    );

    public abstract Piece copy();

    protected void copyStatusTo(Piece target) {
        target.frozenTurnsRemaining = this.frozenTurnsRemaining;
        target.shieldedTurnsRemaining = this.shieldedTurnsRemaining;
        target.bombRookTurnsRemaining = this.bombRookTurnsRemaining;
        target.resurrectCooldownTurnsRemaining = this.resurrectCooldownTurnsRemaining;
        target.zoglinMoveCount = this.zoglinMoveCount;
        target.zoglin = this.zoglin;
    }

    public int getFrozenTurnsRemaining() {
        return frozenTurnsRemaining;
    }

    public void setFrozenTurnsRemaining(int frozenTurnsRemaining) {
        this.frozenTurnsRemaining = Math.max(0, frozenTurnsRemaining);
    }

    public int getShieldedTurnsRemaining() {
        return shieldedTurnsRemaining;
    }

    public void setShieldedTurnsRemaining(int shieldedTurnsRemaining) {
        this.shieldedTurnsRemaining = Math.max(0, shieldedTurnsRemaining);
    }

    public int getBombRookTurnsRemaining() {
        return bombRookTurnsRemaining;
    }

    public void setBombRookTurnsRemaining(int bombRookTurnsRemaining) {
        this.bombRookTurnsRemaining = Math.max(0, bombRookTurnsRemaining);
    }

    public int getResurrectCooldownTurnsRemaining() {
        return resurrectCooldownTurnsRemaining;
    }

    public void setResurrectCooldownTurnsRemaining(int resurrectCooldownTurnsRemaining) {
        this.resurrectCooldownTurnsRemaining = Math.max(0, resurrectCooldownTurnsRemaining);
    }

    public int getZoglinMoveCount() {
        return zoglinMoveCount;
    }

    public void incrementZoglinMoveCount() {
        zoglinMoveCount++;
    }

    public void setZoglinMoveCount(int zoglinMoveCount) {
        this.zoglinMoveCount = Math.max(0, zoglinMoveCount);
    }

    public boolean isZoglin() {
        return zoglin;
    }

    public void setZoglin(boolean zoglin) {
        this.zoglin = zoglin;
    }

    public void draw(Graphics g, int x, int y) {
    int colorIndex = white ? 1 : 0;
    BufferedImage img = pieceImages[colorIndex][imageIndex];

    if (img != null) {
        g.drawImage(img, x, y, 70, 70, null);
    }
}
}   
