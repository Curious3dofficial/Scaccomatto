import java.io.*;
import java.net.*;

public class NetworkManager {

    public interface Listener {
        void onConnected(boolean amWhite);
        void onMoveReceived(int fromRow, int fromCol, int toRow, int toCol, String promotion);
        void onSpellCastReceived(String spellId, boolean casterWhite, SpellTarget target);
        void onSpellPhaseReceived(String phaseId, boolean casterWhite, int row, int col);
        void onError(String msg);
        void onDrawOffered();
        void onDrawAccepted();
        void onDrawDeclined();
        void onResign();
    }

    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Listener listener;
    private Thread listenThread;

    public void host(int port, Listener listener) {
        this.listener = listener;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
                setupStreams();
                // Host plays white
                if (listener != null) listener.onConnected(true);
                startListenLoop();
            } catch (IOException e) {
                if (listener != null) listener.onError("Host error: " + e.getMessage());
            }
        }).start();
    }

    public void join(String host, int port, Listener listener) {
        this.listener = listener;
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                setupStreams();
                // Client plays black
                if (listener != null) listener.onConnected(false);
                startListenLoop();
            } catch (IOException e) {
                if (listener != null) listener.onError("Join error: " + e.getMessage());
            }
        }).start();
    }

    private void setupStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }

    private void startListenLoop() {
        listenThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleLine(line);
                }
            } catch (IOException e) {
                if (listener != null) listener.onError("Connection lost: " + e.getMessage());
            }
        });
        listenThread.start();
    }

    private void handleLine(String line) {
        if (line.startsWith("MOVE")) {
            // FORMAT: MOVE fr fc tr tc [PROMO Q]
            String[] parts = line.split(" ");
            try {
                int fr = Integer.parseInt(parts[1]);
                int fc = Integer.parseInt(parts[2]);
                int tr = Integer.parseInt(parts[3]);
                int tc = Integer.parseInt(parts[4]);
                String promo = "";
                if (parts.length >= 7 && "PROMO".equals(parts[5])) {
                    promo = parts[6];
                }
                if (listener != null) listener.onMoveReceived(fr, fc, tr, tc, promo);
            } catch (Exception e) {
                if (listener != null) listener.onError("Bad MOVE message: " + line);
            }
        } else if (line.startsWith("SPELL_CAST")) {
            // FORMAT: SPELL_CAST side spellId sr sc tr tc dr dc pieceType
            String[] parts = line.split(" ");
            try {
                if (parts.length < 10) throw new IllegalArgumentException("Too few fields");
                boolean casterWhite = "1".equals(parts[1]);
                String spellId = parts[2];
                SpellTarget target = new SpellTarget();
                int sr = Integer.parseInt(parts[3]);
                int sc = Integer.parseInt(parts[4]);
                int tr = Integer.parseInt(parts[5]);
                int tc = Integer.parseInt(parts[6]);
                int dr = Integer.parseInt(parts[7]);
                int dc = Integer.parseInt(parts[8]);
                target.sourceRow = sr >= 0 ? sr : null;
                target.sourceCol = sc >= 0 ? sc : null;
                target.targetRow = tr >= 0 ? tr : null;
                target.targetCol = tc >= 0 ? tc : null;
                target.destRow = dr >= 0 ? dr : null;
                target.destCol = dc >= 0 ? dc : null;
                target.resurrectPieceType = "_".equals(parts[9]) ? null : parts[9];
                if (listener != null) listener.onSpellCastReceived(spellId, casterWhite, target);
            } catch (Exception e) {
                if (listener != null) listener.onError("Bad SPELL_CAST message: " + line);
            }
        } else if (line.startsWith("SPELL_PHASE")) {
            // FORMAT: SPELL_PHASE phaseId side row col
            String[] parts = line.split(" ");
            try {
                if (parts.length < 5) throw new IllegalArgumentException("Too few fields");
                String phaseId = parts[1];
                boolean casterWhite = "1".equals(parts[2]);
                int row = Integer.parseInt(parts[3]);
                int col = Integer.parseInt(parts[4]);
                if (listener != null) listener.onSpellPhaseReceived(phaseId, casterWhite, row, col);
            } catch (Exception e) {
                if (listener != null) listener.onError("Bad SPELL_PHASE message: " + line);
            }
        } else if (line.startsWith("OFFER_DRAW")) {
            if (listener != null) listener.onDrawOffered();
        } else if (line.startsWith("DRAW_ACCEPT")) {
            if (listener != null) listener.onDrawAccepted();
        } else if (line.startsWith("DRAW_DECLINE")) {
            if (listener != null) listener.onDrawDeclined();
        } else if (line.startsWith("RESIGN")) {
            if (listener != null) listener.onResign();
        }
    }

    public void sendMove(int fr, int fc, int tr, int tc, String promo) {
        if (out == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("MOVE ").append(fr).append(' ').append(fc).append(' ').append(tr).append(' ').append(tc);
        if (promo != null && !promo.isEmpty()) {
            sb.append(' ').append("PROMO").append(' ').append(promo);
        }
        out.println(sb.toString());
    }

    public void sendSpellCast(String spellId, boolean casterWhite, SpellTarget target) {
        if (out == null || spellId == null) return;
        int sr = (target != null && target.sourceRow != null) ? target.sourceRow : -1;
        int sc = (target != null && target.sourceCol != null) ? target.sourceCol : -1;
        int tr = (target != null && target.targetRow != null) ? target.targetRow : -1;
        int tc = (target != null && target.targetCol != null) ? target.targetCol : -1;
        int dr = (target != null && target.destRow != null) ? target.destRow : -1;
        int dc = (target != null && target.destCol != null) ? target.destCol : -1;
        String pieceType = (target != null && target.resurrectPieceType != null && !target.resurrectPieceType.trim().isEmpty())
                ? target.resurrectPieceType.trim()
                : "_";
        out.println("SPELL_CAST " + (casterWhite ? 1 : 0) + " " + spellId + " "
                + sr + " " + sc + " " + tr + " " + tc + " " + dr + " " + dc + " " + pieceType);
    }

    public void sendSpellPhase(String phaseId, boolean casterWhite, int row, int col) {
        if (out == null || phaseId == null) return;
        out.println("SPELL_PHASE " + phaseId + " " + (casterWhite ? 1 : 0) + " " + row + " " + col);
    }

    public void sendOfferDraw() {
        if (out == null) return;
        out.println("OFFER_DRAW");
    }

    public void sendDrawResponse(boolean accept) {
        if (out == null) return;
        out.println(accept ? "DRAW_ACCEPT" : "DRAW_DECLINE");
    }

    public void sendResign() {
        if (out == null) return;
        out.println("RESIGN");
    }

    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    public void setListener(Listener l) {
        this.listener = l;
    }
}
