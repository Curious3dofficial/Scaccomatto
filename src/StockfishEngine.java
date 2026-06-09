import java.io.*;

public class StockfishEngine {

    private Process process;
    private BufferedWriter input;
    private BufferedReader output;

    public StockfishEngine() throws IOException {
        this(resolveEnginePath());
    }

    public StockfishEngine(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileNotFoundException(
                "stockfish.exe not found. Tried: " + String.join(", ", getEngineCandidates())
            );
        }
        File engineFile = new File(path);
        if (!engineFile.exists()) {
            throw new FileNotFoundException("Stockfish not found at: " + engineFile.getAbsolutePath());
        }

        process = new ProcessBuilder(engineFile.getAbsolutePath())
                .directory(engineFile.getParentFile())
                .redirectErrorStream(true)
                .start();

        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        output = new BufferedReader(new InputStreamReader(process.getInputStream()));

        send("uci");
        waitFor("uciok");

        send("isready");
        waitFor("readyok");
    }

    private void send(String cmd) throws IOException {
        input.write(cmd);
        input.newLine();
        input.flush();
    }

    private void waitFor(String keyword) throws IOException {
        String line;
        while ((line = output.readLine()) != null) {
            if (line.contains(keyword)) break;
        }
    }

    /**
     * Get best move using FEN position
     * @param fen FEN string of current position
     * @param depth Search depth
     * @return UCI move string (e.g., "e2e4")
     */
    public synchronized String getBestMoveFromFEN(String fen, int depth) throws IOException {
        send("position fen " + fen);
        send("go depth " + depth);

        String line;
        while ((line = output.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                return line.split(" ")[1];
            }
        }
        return null;
    }

    /**
     * Get best move using UCI move list from starting position
     * @param uciMoves Space-separated UCI moves (e.g., "e2e4 e7e5 g1f3")
     * @param depth Search depth
     * @return UCI move string (e.g., "b8c6")
     */
    public synchronized String getBestMoveFromMoves(String uciMoves, int depth) throws IOException {
        // Send position command with move list
        if (uciMoves == null || uciMoves.trim().isEmpty()) {
            send("position startpos");
        } else {
            send("position startpos moves " + uciMoves.trim());
        }
        
        send("go depth " + depth);

        String line;
        while ((line = output.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    return parts[1];
                }
            }
        }
        return null;
    }
    
    /**
     * Analyze a position and return evaluation + best move
     * @param fen FEN string of position
     * @param depth Search depth
     * @return EngineAnalysis object with evaluation and best move
     */
    public synchronized EngineAnalysis analyzePosition(String fen, int depth) throws IOException {
        send("position fen " + fen);
        send("go depth " + depth);
        
        EngineAnalysis analysis = new EngineAnalysis();
        String line;
        
        while ((line = output.readLine()) != null) {
            // Parse evaluation from info lines
            if (line.contains("score cp")) {
                String[] parts = line.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("cp")) {
                        try {
                            analysis.evaluation = Integer.parseInt(parts[i + 1]);
                        } catch (NumberFormatException e) {
                            // Ignore parse errors
                        }
                        break;
                    }
                }
            }
            
            // Parse mate scores
            if (line.contains("score mate")) {
                String[] parts = line.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("mate")) {
                        try {
                            int mateIn = Integer.parseInt(parts[i + 1]);
                            // Convert mate to large evaluation
                            analysis.evaluation = mateIn > 0 ? 10000 : -10000;
                            analysis.isMate = true;
                            analysis.mateIn = Math.abs(mateIn);
                        } catch (NumberFormatException e) {
                            // Ignore parse errors
                        }
                        break;
                    }
                }
            }
            
            // Get best move
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    analysis.bestMove = parts[1];
                }
                break;
            }
        }
        
        return analysis;
    }

    /**
     * Set Stockfish skill level (0-20)
     * 0 = weakest, 20 = strongest
     */
    public synchronized void setSkillLevel(int level) throws IOException {
        if (level < 0) level = 0;
        if (level > 20) level = 20;
        
        send("setoption name Skill Level value " + level);
        
        // For lower skill levels, also limit strength
        if (level < 20) {
            send("setoption name UCI_LimitStrength value true");
            // Approximate ELO mapping: level 0 ≈ 1320, level 20 ≈ 3190
            int elo = 1320 + (level * 93); // Linear interpolation
            send("setoption name UCI_Elo value " + elo);
        } else {
            send("setoption name UCI_LimitStrength value false");
        }
        
        send("isready");
        waitFor("readyok");
    }

    public void close() throws IOException {
        send("quit");
        process.destroy();
    }

    public static String resolveEnginePath() {
        for (String candidate : getEngineCandidates()) {
            File f = new File(candidate);
            if (f.exists() && f.isFile()) {
                return f.getPath();
            }
        }
        return null;
    }

    private static String[] getEngineCandidates() {
        return new String[] {
            "Scaccomatto_final/Scaccomatto/engines/stockfish.exe",
            "Scaccomatto/engines/stockfish.exe",
            "engines/stockfish.exe",
            "stockfish.exe"
        };
    }
}
