public class Bishop extends Piece {

    public Bishop(boolean white) {
        super(white,3);
    }

    @Override
    public boolean isValidMove(int sr, int sc, int er, int ec, Piece[][] board) {
        // Bounds check
        if (er < 0 || er > 7 || ec < 0 || ec > 7) {
            return false;
        }
        
        // Must move diagonally
        if (Math.abs(sr - er) != Math.abs(sc - ec)) {
            return false;
        }
        
        // Same square
        if (sr == er && sc == ec) {
            return false;
        }
        
        // Check if path is clear
        int rowStep = (er > sr) ? 1 : -1;
        int colStep = (ec > sc) ? 1 : -1;
        
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

    @Override
    public Piece copy() {
        Bishop b = new Bishop(isWhite());
        copyStatusTo(b);
        return b;
    }
}
