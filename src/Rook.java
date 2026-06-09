public class Rook extends Piece {

    private boolean moved = false;
    private int col;

    public Rook(boolean white) {
        super(white,2);
    }

    @Override
    public boolean isValidMove(int sr, int sc, int er, int ec, Piece[][] board) {
        // Bounds check
        if (er < 0 || er > 7 || ec < 0 || ec > 7) {
            return false;
        }
        
        // Must move in straight line (horizontal or vertical)
        if (sr != er && sc != ec) {
            return false;
        }
        
        // Same square
        if (sr == er && sc == ec) {
            return false;
        }
        
        // Check if path is clear
        int rowStep = (er == sr) ? 0 : ((er > sr) ? 1 : -1);
        int colStep = (ec == sc) ? 0 : ((ec > sc) ? 1 : -1);
        
        int r = sr + rowStep;
        int c = sc + colStep;
        
        while (r != er || c != ec) {
            // Extra safety check
            if (r < 0 || r > 7 || c < 0 || c > 7) {
                return false;
            }
            if (board[r][c] != null) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }
        
        // Check destination square
        if (board[er][ec] instanceof Duck) return false;
        return board[er][ec] == null || board[er][ec].isWhite() != this.isWhite();
    }

    public boolean hasMoved() { return moved; }
    public void setMoved(boolean moved) { this.moved = moved; }

    @Override
    public Piece copy() {
        Rook r = new Rook(isWhite());
        r.moved = this.moved;
        r.col = this.col;
        copyStatusTo(r);
        return r;
    }
}
