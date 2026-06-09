import java.io.*;
import java.util.*;

/**
 * Review
 * ======
 * Classifies chess moves as Brilliant, Great, Best, Excellent,
 * Good, Inaccuracy, Mistake, Blunder, or Miss — exactly like Chess.com.
 *
 * Depends on: PositionEvaluator.java (must be compiled together)
 *
 * HOW IT WORKS:
 *   1. Asks Stockfish for its top 3 candidate moves BEFORE the played move
 *      (MultiPV) — this is what enables Brilliant detection
 *   2. Gets the score AFTER the played move
 *   3. Converts centipawns → win probability (sigmoid curve)
 *   4. Classifies the move based on win% loss + whether alternatives existed
 *
 * USAGE — classify a single move:
 *   Review review = new Review();
 *   MoveResult result = review.classifyMove(fenBefore, "e2e4", true);
 *   System.out.println(result);   // "e2e4  →  BEST  (+0.17)"
 *   review.close();
 *
 * USAGE — review a full game:
 *   GameSummary summary = review.reviewGame(fenList, moveList);
 *   System.out.println(summary);
 */
public class Review {

    // -------------------------------------------------------------------------
    // Stockfish settings
    // -------------------------------------------------------------------------
    private static final int    DEPTH          = 20;  // Chess.com uses 20-22
    private static final int    MULTI_PV       = 3;   // fetch top 3 candidate moves

    // -------------------------------------------------------------------------
    // Classification thresholds (win% loss)
    // -------------------------------------------------------------------------
    private static final double BRILLIANT_GAP  = 10.0; // 2nd-best must lose 10% more win%
    private static final double GREAT_GAP      =  5.0;

    private Process        process;
    private BufferedReader reader;
    private PrintWriter    writer;
    private boolean        ready = false;

    // =========================================================================
    // Public enums and result classes
    // =========================================================================

    /**
     * Move classification labels, matching Chess.com's system.
     */
    public enum MoveClass {
        BRILLIANT  ("!!", "◆◆", "An incredible, non-obvious move. All alternatives were much worse."),
        GREAT      ("!",  "◆",  "A strong, non-obvious move."),
        BEST       ("",   "✓",  "The top engine move."),
        EXCELLENT  ("",   "★",  "Very close to the best move."),
        GOOD       ("",   "●",  "A solid move with only a minor imprecision."),
        INACCURACY ("?!", "▲",  "A noticeable slip that gives away some advantage."),
        MISTAKE    ("?",  "✗",  "A significant error."),
        BLUNDER    ("??", "✗✗", "A major error that changes the game."),
        MISS       ("!",  "◉",  "You missed a forced mate or a huge winning tactic.");

        public final String symbol;      // e.g. "??"
        public final String icon;        // e.g. "✗✗"
        public final String description;

        MoveClass(String symbol, String icon, String description) {
            this.symbol      = symbol;
            this.icon        = icon;
            this.description = description;
        }
    }

    /** Result for a single classified move */
    public static class MoveResult {
        public final String    move;            // UCI move e.g. "e2e4"
        public final MoveClass classification;
        public final String    scoreAfter;      // formatted score e.g. "+0.35"
        public final double    accuracy;        // 0–100
        public final String    bestMove;        // what engine recommended
        public final boolean   playedBest;      // did player find the top move?

        MoveResult(String move, MoveClass cls, String scoreAfter,
                   double accuracy, String bestMove) {
            this.move           = move;
            this.classification = cls;
            this.scoreAfter     = scoreAfter;
            this.accuracy       = accuracy;
            this.bestMove       = bestMove;
            this.playedBest     = move.equalsIgnoreCase(bestMove);
        }

        @Override
        public String toString() {
            String tag = classification.symbol.isEmpty() ? "  " : classification.symbol;
            return String.format(
                "%-6s %s %-11s | Score: %6s | Accuracy: %5.1f%% | Best was: %s%s",
                move, tag,
                "[" + classification + "]",
                scoreAfter, accuracy,
                bestMove,
                playedBest ? " ✓" : ""
            );
        }
    }

    /** Summary for a full game review */
    public static class GameSummary {
        public final List<MoveResult> moves;
        public final double           whiteAccuracy;
        public final double           blackAccuracy;

        GameSummary(List<MoveResult> moves, double white, double black) {
            this.moves         = moves;
            this.whiteAccuracy = white;
            this.blackAccuracy = black;
        }

        /** Count how many of each classification White or Black had */
        public Map<MoveClass, Integer> counts(boolean forWhite) {
            Map<MoveClass, Integer> map = new LinkedHashMap<>();
            for (MoveClass mc : MoveClass.values()) map.put(mc, 0);
            for (int i = 0; i < moves.size(); i++)
                if ((i % 2 == 0) == forWhite)
                    map.merge(moves.get(i).classification, 1, Integer::sum);
            return map;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔════════════════════════════════════════════════════════════╗\n");
            sb.append("║                      GAME REVIEW                           ║\n");
            sb.append("╠════════════════════════════════════════════════════════════╣\n");

            for (int i = 0; i < moves.size(); i++) {
                String side = (i % 2 == 0) ? "White" : "Black";
                sb.append(String.format("  #%2d [%s] %s%n", i + 1, side, moves.get(i)));
            }

            sb.append("╠════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("  White Accuracy: %.1f%%%n", whiteAccuracy));
            sb.append(String.format("  Black Accuracy: %.1f%%%n", blackAccuracy));

            sb.append("\n  White move breakdown:\n");
            counts(true).forEach((k, v) -> {
                if (v > 0) sb.append(String.format("    %s %-11s : %d%n", k.icon, k, v));
            });
            sb.append("\n  Black move breakdown:\n");
            counts(false).forEach((k, v) -> {
                if (v > 0) sb.append(String.format("    %s %-11s : %d%n", k.icon, k, v));
            });
            sb.append("╚════════════════════════════════════════════════════════════╝\n");
            return sb.toString();
        }
    }

    // =========================================================================
    // Constructor
    // =========================================================================
    public Review() throws IOException {
        String enginePath = resolveEngineCommand();
        ProcessBuilder pb = new ProcessBuilder(enginePath);
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IOException("Cannot start Stockfish at: " + enginePath, e);
        }

        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);

        writer.println("uci");
        waitFor("uciok", 5000);

        // MultiPV lets Stockfish return its top N moves — essential for Brilliant detection
        writer.println("setoption name MultiPV value " + MULTI_PV);

        writer.println("isready");
        waitFor("readyok", 5000);

        ready = true;
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Classify a single move.
     *
     * @param fenBefore   FEN of the position BEFORE the move
     * @param playedMove  The move in UCI format e.g. "e2e4", "g1f3", "e1g1"
     * @param isWhite     true if it was White's move
     * @return            MoveResult with classification, score, accuracy
     */
    public MoveResult classifyMove(String fenBefore, String playedMove, boolean isWhite)
            throws IOException {
        if (!ready) throw new IllegalStateException("Engine not initialised.");

        // Step 1 — Get top 3 moves BEFORE the played move
        List<PVLine> linesBefore = getMultiPV(fenBefore);
        if (linesBefore.isEmpty()) throw new IOException("No analysis for: " + fenBefore);

        double cpBefore = linesBefore.get(0).score;
        String bestMove = linesBefore.get(0).bestMove;

        // Step 2 — Get score AFTER the played move (flip: now opponent's turn)
        String fenAfter  = applyMove(fenBefore, playedMove);
        List<PVLine> linesAfter = getMultiPV(fenAfter);
        double cpAfter   = linesAfter.isEmpty() ? cpBefore : -linesAfter.get(0).score;

        // Step 3 — Convert to win percentages (sigmoid, not raw centipawns)
        double wpBefore  = winPercent(isWhite ? cpBefore : -cpBefore);
        double wpAfter   = winPercent(isWhite ? cpAfter  : -cpAfter);
        double wpLoss    = Math.max(0, wpBefore - wpAfter);

        // Step 4 — Calculate accuracy for this move
        double acc       = moveAccuracy(wpLoss);

        // Step 5 — Classify
        MoveClass cls    = classify(playedMove, bestMove, wpLoss, linesBefore, isWhite);

        return new MoveResult(playedMove, cls, formatCp(cpAfter), acc, bestMove);
    }

    /**
     * Review a full game, returning a summary with all move classifications
     * and overall White/Black accuracy scores.
     *
     * @param fens   One FEN per position, BEFORE each move is played
     * @param moves  UCI moves — same length as fens (or fens.size()-1)
     */
    public GameSummary reviewGame(List<String> fens, List<String> moves) throws IOException {
        List<MoveResult> results = new ArrayList<>();
        List<Double> whiteAcc   = new ArrayList<>();
        List<Double> blackAcc   = new ArrayList<>();

        for (int i = 0; i < moves.size() && i < fens.size(); i++) {
            boolean isWhite   = (i % 2 == 0);
            MoveResult result = classifyMove(fens.get(i), moves.get(i), isWhite);
            results.add(result);
            (isWhite ? whiteAcc : blackAcc).add(result.accuracy);
        }

        return new GameSummary(results, average(whiteAcc), average(blackAcc));
    }

    public void close() {
        if (process != null && process.isAlive()) {
            writer.println("quit");
            process.destroyForcibly();
        }
        ready = false;
    }

    private String resolveEngineCommand() {
        String resolved = StockfishEngine.resolveEnginePath();
        if (resolved != null && !resolved.isBlank()) return resolved;
        return "stockfish";
    }

    // =========================================================================
    // CLASSIFICATION LOGIC
    // =========================================================================

    /**
     * Classify a move using win% loss + MultiPV gap for Brilliant detection.
     *
     * BRILLIANT requires:
     *   (a) Move IS the engine's best move
     *   (b) At least 2 candidate moves existed (not a forced/only move)
     *   (c) The 2nd-best alternative loses ≥10% more win% → move was non-obvious
     *
     * GREAT requires same as Brilliant but with a smaller gap (5–10%).
     *
     * MISS: there was a forced mate available but the player missed it.
     */
    private MoveClass classify(String playedMove, String bestMove,
                               double wpLoss, List<PVLine> linesBefore, boolean isWhite) {
        boolean playedBest = playedMove.equalsIgnoreCase(bestMove);

        // --- Brilliant / Great detection ---
        if (playedBest && linesBefore.size() >= 2) {
            double wpBest   = winPercent(isWhite ?  linesBefore.get(0).score : -linesBefore.get(0).score);
            double wpSecond = winPercent(isWhite ?  linesBefore.get(1).score : -linesBefore.get(1).score);
            double gap      = wpBest - wpSecond;

            if (gap >= BRILLIANT_GAP) return MoveClass.BRILLIANT;
            if (gap >= GREAT_GAP)     return MoveClass.GREAT;
        }

        // --- Missed forced mate ---
        if (!playedBest && !linesBefore.isEmpty()
                && linesBefore.get(0).isMate && linesBefore.get(0).score > 0) {
            return MoveClass.MISS;
        }

        // --- Standard classification by win% loss ---
        if (wpLoss <= 0)   return MoveClass.BEST;
        if (wpLoss <= 2)   return MoveClass.EXCELLENT;
        if (wpLoss <= 5)   return MoveClass.GOOD;
        if (wpLoss <= 10)  return MoveClass.INACCURACY;
        if (wpLoss <= 20)  return MoveClass.MISTAKE;
        return MoveClass.BLUNDER;
    }

    // =========================================================================
    // CHESS.COM WIN-PROBABILITY FORMULAS
    // =========================================================================

    /**
     * Converts centipawns to win probability (0–100%).
     *
     * Uses a sigmoid curve so that:
     *   +1.0 pawn from equality is huge (~65% win chance)
     *   +5.0 vs +6.0 makes almost no difference (both ~95%)
     *
     * This is why Chess.com's accuracy doesn't just use raw centipawn loss.
     */
    private double winPercent(double centipawns) {
        return 50 + 50 * (2.0 / (1.0 + Math.exp(-0.00368208 * centipawns)) - 1.0);
    }

    /**
     * Per-move accuracy from win% loss.
     * 0 loss = 100%, large loss approaches 0%.
     */
    private double moveAccuracy(double winPercentLoss) {
        if (winPercentLoss <= 0) return 100.0;
        double acc = 103.1668 * Math.exp(-0.04354 * winPercentLoss) - 3.1669;
        return Math.max(0.0, Math.min(100.0, acc));
    }

    // =========================================================================
    // STOCKFISH COMMUNICATION
    // =========================================================================

    private static class PVLine {
        final int     pvIndex;
        final double  score;    // centipawns, always White's POV in UCI
        final boolean isMate;
        final String  bestMove;

        PVLine(int pvIndex, double score, boolean isMate, String bestMove) {
            this.pvIndex  = pvIndex;
            this.score    = score;
            this.isMate   = isMate;
            this.bestMove = bestMove;
        }
    }

    /** Request top MULTI_PV lines from Stockfish for a given FEN */
    private List<PVLine> getMultiPV(String fen) throws IOException {
        writer.println("position fen " + fen);
        writer.println("go depth " + DEPTH);

        Map<Integer, PVLine> pvMap   = new HashMap<>();
        long                 deadline = System.currentTimeMillis() + 15_000;
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info") && line.contains("score") && line.contains(" pv ")) {
                PVLine pv = parsePVLine(line);
                if (pv != null) pvMap.put(pv.pvIndex, pv);
            }
            if (line.startsWith("bestmove")) break;
            if (System.currentTimeMillis() > deadline) throw new IOException("Stockfish timed out.");
        }

        List<PVLine> result = new ArrayList<>(pvMap.values());
        result.sort(Comparator.comparingInt(pv -> pv.pvIndex));
        return result;
    }

    private PVLine parsePVLine(String line) {
        String[] tokens  = line.split("\\s+");
        int      pvIndex = 1;
        double   score   = 0;
        boolean  isMate  = false;
        String   move    = null;

        for (int i = 0; i < tokens.length - 1; i++) {
            switch (tokens[i]) {
                case "multipv":
                    pvIndex = Integer.parseInt(tokens[i + 1]);
                    break;
                case "score":
                    if (tokens[i + 1].equals("cp")) {
                        score = Double.parseDouble(tokens[i + 2]);
                    } else if (tokens[i + 1].equals("mate")) {
                        isMate    = true;
                        int mateN = Integer.parseInt(tokens[i + 2]);
                        score = mateN > 0 ? 100000 - mateN : -100000 - mateN;
                    }
                    break;
                case "pv":
                    if (i + 1 < tokens.length) move = tokens[i + 1];
                    break;
            }
        }
        return (move == null) ? null : new PVLine(pvIndex, score, isMate, move);
    }

    /** Use Stockfish's "d" command to get the FEN after a move is applied */
    private String applyMove(String fen, String move) throws IOException {
        writer.println("position fen " + fen + " moves " + move);
        writer.println("d");

        long   deadline = System.currentTimeMillis() + 3000;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Fen:")) return line.substring(5).trim();
            if (System.currentTimeMillis() > deadline) break;
        }
        return fen;
    }

    // =========================================================================
    // MISC HELPERS
    // =========================================================================

    private String formatCp(double cp) {
        if (Math.abs(cp) >= 99000) {
            int n = (int)(cp > 0 ? 100000 - cp : 100000 + cp);
            return (cp > 0 ? "+" : "-") + "M" + Math.abs(n);
        }
        double pawns = cp / 100.0;
        return String.format("%s%.2f", pawns >= 0 ? "+" : "", pawns);
    }

    private void waitFor(String token, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(token)) return;
            if (System.currentTimeMillis() > deadline)
                throw new IOException("Timed out waiting for: " + token);
        }
    }

    private double average(List<Double> values) {
        return values.isEmpty() ? 0
             : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    // =========================================================================
    // Demo
    // =========================================================================
    public static void main(String[] args) {
        // First 6 moves of the Ruy Lopez opening
        List<String> fens = Arrays.asList(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
        );
        List<String> moves = Arrays.asList("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6");

        Review review = null;
        try {
            review = new Review();
            System.out.println("=== Single Move Example ===");
            MoveResult single = review.classifyMove(fens.get(0), "e2e4", true);
            System.out.println(single);
            System.out.println("Classification : " + single.classification);
            System.out.println("Description    : " + single.classification.description);
            System.out.println();

            System.out.println("=== Full Game Review ===");
            GameSummary summary = review.reviewGame(fens, moves);
            System.out.println(summary);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Is Stockfish installed? Run: stockfish");
        } finally {
            if (review != null) review.close();
        }
    }
}
