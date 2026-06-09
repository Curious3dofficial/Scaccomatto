public class King extends Piece {

    private boolean moved = false;
    private int col; // track column

    public King(boolean white) {
        super(white,0);
    }

    @Override
    public boolean isValidMove(int sr, int sc, int er, int ec, Piece[][] board) {
        if (er < 0 || er > 7 || ec < 0 || ec > 7) {
            return false;
        }
        
        int dr = Math.abs(sr - er);
        int dc = Math.abs(sc - ec);

        // Normal king move (one square in any direction)
        if ((dr <= 1 && dc <= 1) && !(dr == 0 && dc == 0)) {
            if (board[er][ec] instanceof Duck) return false;
            // Check destination square - can't capture own piece
            if (board[er][ec] != null && board[er][ec].isWhite() == this.isWhite()) {
                return false;
            }
            return true;
        }

        // Castling move (king moves 2 squares horizontally)
        if (!moved && dr == 0 && dc == 2) {
            int row = sr;
            int rookCol = (ec == 6) ? 7 : 0; // king-side or queen-side
            Piece rook = board[row][rookCol];

            if (rook instanceof Rook && !((Rook) rook).hasMoved()) {
                // Check empty squares between king and rook
                int step = (rookCol - sc) > 0 ? 1 : -1;
                for (int c = sc + step; c != rookCol; c += step) {
                    if (board[row][c] != null) return false;
                }
                return true;
            }
        }

        return false;
    }

    public boolean hasMoved() { return moved; }
    public void setMoved(boolean moved) { this.moved = moved; }

    @Override
    public Piece copy() {
        King k = new King(isWhite());
        k.moved = this.moved;
        k.col = this.col;
        copyStatusTo(k);
        return k;
    }
}
