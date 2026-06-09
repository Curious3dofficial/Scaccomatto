public class Knight extends Piece {

    public Knight(boolean white) {
        super(white,4);
    }

    @Override
    public boolean isValidMove(int sr, int sc, int er, int ec, Piece[][] board) {
        // Bounds check first!
        if (er < 0 || er > 7 || ec < 0 || ec > 7) {
            return false;
        }
        
        int dr = Math.abs(sr - er);
        int dc = Math.abs(sc - ec);
        
        // Valid knight move pattern (L-shape)
        if (dr * dc != 2) {
            return false;
        }
        
        // Check destination square - can't capture own piece
        if (board[er][ec] instanceof Duck) return false;
        if (board[er][ec] != null && board[er][ec].isWhite() == this.isWhite()) {
            return false;
        }
        
        return true;
    }

    @Override
    public Piece copy() {
        Knight n = new Knight(isWhite());
        copyStatusTo(n);
        return n;
    }
}
