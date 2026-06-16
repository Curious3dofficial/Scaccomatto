public class Pawn extends Piece {

    private boolean justMovedTwo = false;

    public Pawn(boolean isWhite) {
        super(isWhite,5);
    }

    @Override
    public boolean isValidMove(
            int startRow, int startCol,
            int endRow, int endCol,
            Piece[][] board) {

        // Bounds check first!
        if (endRow < 0 || endRow > 7 || endCol < 0 || endCol > 7) {
            return false;
        }
        if (board[endRow][endCol] instanceof Duck) {
            return false;
        }

        int direction = isWhite() ? -1 : 1;

        // 1. Normal forward move (1 square)
        if (startCol == endCol &&
            endRow == startRow + direction &&
            board[endRow][endCol] == null) {
            return true;
        }

        // 2. First move: 2 squares forward
        if (startCol == endCol) {
            if (isWhite() && startRow == 6 &&
                endRow == 4 &&
                board[5][startCol] == null &&
                board[4][startCol] == null) {
                return true;
            }

            if (!isWhite() && startRow == 1 &&
                endRow == 3 &&
                board[2][startCol] == null &&
                board[3][startCol] == null) {
                return true;
            }
        }

        // 3. Diagonal capture (normal) - FIXED: Check for enemy piece
        if (Math.abs(endCol - startCol) == 1 &&
            endRow == startRow + direction &&
            board[endRow][endCol] != null &&
            board[endRow][endCol].isWhite() != this.isWhite()) { // Must be enemy piece
            return true;
        }

        // 4. En passant capture
        if (Math.abs(endCol - startCol) == 1 &&
            endRow == startRow + direction &&
            board[endRow][endCol] == null) {
            
            // Check if there's an enemy pawn beside us that just moved 2 squares
            Piece adjacentPiece = board[startRow][endCol];
            
            if (adjacentPiece instanceof Pawn &&
                adjacentPiece.isWhite() != isWhite() &&
                ((Pawn) adjacentPiece).justMovedTwo()) {
                return true;
            }
        }

        return false;
    }

    public void onMove(int startRow, int startCol, int endRow, int endCol) {
        // Set flag if pawn moved 2 squares
        justMovedTwo = (Math.abs(endRow - startRow) == 2);
    }

    public boolean justMovedTwo() {
        return justMovedTwo;
    }

    public void clearJustMovedTwo() {
        justMovedTwo = false;
    }

    @Override
    public Piece copy() {
        Pawn p = new Pawn(isWhite());
        p.justMovedTwo = this.justMovedTwo;
        copyStatusTo(p);
        return p;
    }
}
