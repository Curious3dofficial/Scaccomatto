import java.io.*;
import java.util.*;

/**
 * PositionEvaluator
 * =================
 * Takes a FEN string and returns a position score like:
 *   "+0.35"  → White is slightly better
 *   "-1.20"  → Black is better by ~1.2 pawns
 *   "+M4"    → White has forced mate in 4
 *   "-M2"    → Black has forced mate in 2
 *
 * This class ONLY handles raw evaluation scores.
 * For move classification (Brilliant, Blunder, etc.) use Review.java
 *
 * REQUIREMENTS:
 *   Stockfish installed on your PATH.
 *     Mac:   brew install stockfish
 *     Linux: sudo apt install stockfish
 *     Win:   download from stockfishchess.org, add to PATH
 *
 * USAGE:
 *   PositionEvaluator ev = new PositionEvaluator();
 *   String score = ev.evaluate("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
 *   System.out.println(score); // "+0.17"
 *   ev.close();
 */
public class PositionEvaluator {

    private static final int    DEFAULT_DEPTH  = 20;          // higher = stronger, slower

    private Process        process;
    private BufferedReader reader;
    private PrintWriter    writer;
    private boolean        ready = false;

    // =========================================================================
    // Constructor — starts the Stockfish process
    // =========================================================================
    public PositionEvaluator() throws IOException {
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

        writer.println("isready");
        waitFor("readyok", 5000);

        ready = true;
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Evaluate a position from a FEN string.
     *
     * @param fen  The FEN string of the position to evaluate
     * @return     Score string e.g. "+0.35", "-1.20", "+M3", "-M1"
     */
    public String evaluate(String fen) throws IOException {
        return evaluate(fen, DEFAULT_DEPTH);
    }

    /**
     * Evaluate a position at a custom depth.
     *
     * @param fen   FEN string of the position
     * @param depth Search depth (15 = fast, 20 = strong, 22+ = very strong)
     * @return      Score string
     */
    public String evaluate(String fen, int depth) throws IOException {
        if (!ready)                          throw new IllegalStateException("Engine not ready.");
        if (fen == null || fen.isBlank())    throw new IllegalArgumentException("FEN cannot be empty.");

        writer.println("position fen " + fen);
        writer.println("go depth " + depth);

        RawScore raw = readBestScore(15_000);
        return format(raw);
    }

    /**
     * Evaluate a position and return the raw centipawn value.
     * Positive = White is better, negative = Black is better.
     * Useful if you want to do your own math with the score.
     */
    public double evaluateCentipawns(String fen) throws IOException {
        return evaluateCentipawns(fen, DEFAULT_DEPTH);
    }

    public double evaluateCentipawns(String fen, int depth) throws IOException {
        if (!ready)                          throw new IllegalStateException("Engine not ready.");
        if (fen == null || fen.isBlank())    throw new IllegalArgumentException("FEN cannot be empty.");

        writer.println("position fen " + fen);
        writer.println("go depth " + depth);

        return readBestScore(15_000).value;
    }

    /**
     * Shut down the Stockfish process. Always call this when done.
     */
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
    // PRIVATE HELPERS
    // =========================================================================

    /** Simple holder for raw Stockfish output */
    static class RawScore {
        boolean isMate;
        int     value;  // centipawns (White POV), or mate-in-N if isMate=true

        RawScore(boolean isMate, int value) {
            this.isMate = isMate;
            this.value  = value;
        }
    }

    /** Read Stockfish output until "bestmove", keeping the last score line */
    private RawScore readBestScore(long timeoutMs) throws IOException {
        RawScore last    = new RawScore(false, 0);
        long     deadline = System.currentTimeMillis() + timeoutMs;
        String   line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info") && line.contains("score"))
                last = parseScoreLine(line);
            if (line.startsWith("bestmove"))
                break;
            if (System.currentTimeMillis() > deadline)
                throw new IOException("Stockfish timed out.");
        }
        return last;
    }

    /** Parse "info … score cp 35 …" or "info … score mate 3 …" */
    private RawScore parseScoreLine(String line) {
        String[] tokens = line.split("\\s+");
        for (int i = 0; i < tokens.length - 2; i++) {
            if (tokens[i].equals("score")) {
                boolean isMate = tokens[i + 1].equals("mate");
                int     value  = Integer.parseInt(tokens[i + 2]);
                return new RawScore(isMate, value);
            }
        }
        return new RawScore(false, 0);
    }

    /** Format a RawScore into a human-readable string */
    private String format(RawScore raw) {
        if (raw.isMate) {
            String sign = raw.value > 0 ? "+" : "-";
            return sign + "M" + Math.abs(raw.value);
        }
        double pawns = raw.value / 100.0;
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

    public static void main(String[] args) {
        // No console demo output by default.
    }
}
