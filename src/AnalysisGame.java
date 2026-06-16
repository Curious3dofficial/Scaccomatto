import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisGame extends JFrame {
    private final MainMenu appHost;

    private Frame dialogOwner() {
        return appHost != null ? appHost : this;
    }

    private Board              board;
    private MoveHistoryPanel   moveHistoryPanel;
    private OpeningDetector    openingDetector;
    private OpeningNamePanel   openingNamePanel;
    private StockfishEngine    stockfishEngine;
    private PositionEvaluator  positionEvaluator;
    private EvaluationBar      evaluationBar;
    private JLabel             evaluationLabel;
    private JTextArea          bestMoveArea;
    private final List<JLabel> lineScoreLabels = new ArrayList<>();
    private final List<JLabel> lineMoveLabels  = new ArrayList<>();
    
    // Settings toggles
    private JCheckBox          analysisCheckBox;
    private JCheckBox          legalMovesCheckBox;
    private JTextField         pgnTextField;

    // â”€â”€ single-worker analysis â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Every time a new position arrives we bump this counter.  The worker
    // checks it after analyzePosition() returns; if it changed while it was
    // searching, the result is stale and is silently dropped.
    private volatile int       analysisGeneration = 0;
    private volatile boolean   workerAlive        = false;
    private volatile boolean   shutdownRequested  = false;
    // wakes the worker when a new position is ready
    private final Object       analysisLock       = new Object();
    
    private boolean            analysisEnabled    = true;
    private boolean            showLegalMoves     = true;

    public AnalysisGame() {
        this(null);
    }

    public AnalysisGame(MainMenu appHost) {
        this.appHost = appHost;
        setTitle("Java Chess - Analysis Board");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        if (appHost == null) {
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setResizable(false);
        }

        // â”€â”€ start Stockfish â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        try {
            stockfishEngine = new StockfishEngine();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dialogOwner(),
                "Failed to start Stockfish\n" + e.getMessage(),
                "Engine Error",
                JOptionPane.ERROR_MESSAGE);
        }
        try {
            positionEvaluator = new PositionEvaluator();
        } catch (Exception e) {
            positionEvaluator = null;
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
            }
        });

        initializeGame();
        if (appHost == null) {
            setVisible(true);
        }

        // start the persistent worker, then request initial analysis
        startWorker();
        requestAnalysis();
    }

    // â”€â”€ UI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void initializeGame() {
        openingDetector = new OpeningDetector();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 24, 42));

        // â”€â”€ LEFT: eval bar + board â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(590, 800));
        leftPanel.setBackground(new Color(30, 24, 42));

        evaluationBar = new EvaluationBar();
        evaluationBar.setPreferredSize(new Dimension(30, 560));
        leftPanel.add(evaluationBar, BorderLayout.WEST);

        board = new Board(null);
        board.setInputEnabled(true);
        board.setAnalysisMode(true);
        board.setAnalysisGame(this);
        board.setShowLegalMoves(true);
        leftPanel.add(board, BorderLayout.CENTER);

        JPanel topPanel = new PlayerInfoBarPanel();
        topPanel.setPreferredSize(new Dimension(560, 56));
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JLabel topPlayer = new JLabel("Opponent (Black)");
        topPlayer.setFont(new Font("Segoe UI", Font.BOLD, 22));
        topPlayer.setForeground(new Color(242, 242, 246));
        JLabel topInfo = new JLabel("Analysis");
        topInfo.setFont(new Font("Consolas", Font.BOLD, 20));
        topInfo.setForeground(new Color(245, 245, 248));
        topPanel.add(topPlayer, BorderLayout.WEST);
        topPanel.add(topInfo, BorderLayout.EAST);

        JPanel bottomPanel = new PlayerInfoBarPanel();
        bottomPanel.setPreferredSize(new Dimension(560, 56));
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JLabel bottomPlayer = new JLabel("You (White)");
        bottomPlayer.setFont(new Font("Segoe UI", Font.BOLD, 22));
        bottomPlayer.setForeground(new Color(242, 242, 246));
        JLabel bottomInfo = new JLabel("Analysis");
        bottomInfo.setFont(new Font("Consolas", Font.BOLD, 20));
        bottomInfo.setForeground(new Color(245, 245, 248));
        bottomPanel.add(bottomPlayer, BorderLayout.WEST);
        bottomPanel.add(bottomInfo, BorderLayout.EAST);

        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(bottomPanel, BorderLayout.SOUTH);

        // â”€â”€ CENTER: analysis info â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 24, 42));
        centerPanel.setPreferredSize(new Dimension(430, 800));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(40, 30, 58));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(96, 78, 128), 1, true),
            BorderFactory.createEmptyBorder(24, 22, 24, 22)
        ));

        // title
        JLabel analysisTitle = new JLabel("Engine Analysis");
        analysisTitle.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
        analysisTitle.setForeground(new Color(244, 240, 255));
        analysisTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(analysisTitle);
        controlPanel.add(Box.createVerticalStrut(20));

        // eval number
        evaluationLabel = new JLabel("Eval: 0.0");
        evaluationLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 32));
        evaluationLabel.setForeground(new Color(245, 236, 255));
        evaluationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(evaluationLabel);
        controlPanel.add(Box.createVerticalStrut(15));

        // best-move label
        JLabel bestMoveTitle = new JLabel("Best Move:");
        bestMoveTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        bestMoveTitle.setForeground(new Color(205, 194, 226));
        bestMoveTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(bestMoveTitle);
        controlPanel.add(Box.createVerticalStrut(5));

        bestMoveArea = new JTextArea(3, 20);
        bestMoveArea.setEditable(false);
        bestMoveArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        bestMoveArea.setBackground(new Color(33, 24, 48));
        bestMoveArea.setForeground(new Color(245, 240, 255));
        bestMoveArea.setLineWrap(true);
        bestMoveArea.setWrapStyleWord(true);
        bestMoveArea.setText("Waiting for engine...");
        JScrollPane scrollPane = new JScrollPane(bestMoveArea);
        scrollPane.setPreferredSize(new Dimension(360, 120));
        scrollPane.setMaximumSize(new Dimension(360, 120));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getViewport().setBackground(new Color(33, 24, 48));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(96, 78, 128), 1, true));
        controlPanel.add(scrollPane);
        controlPanel.add(Box.createVerticalStrut(20));

        // â”€â”€ Settings Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JLabel settingsTitle = new JLabel("Settings");
        settingsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        settingsTitle.setForeground(new Color(235, 227, 250));
        settingsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(settingsTitle);
        controlPanel.add(Box.createVerticalStrut(10));

        // Analysis Toggle
        analysisCheckBox = new JCheckBox("Analysis");
        analysisCheckBox.setSelected(true);
        analysisCheckBox.setBackground(new Color(40, 30, 58));
        analysisCheckBox.setForeground(new Color(245, 238, 255));
        analysisCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        analysisCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        analysisCheckBox.setOpaque(false);
        analysisCheckBox.addActionListener(e -> {
            analysisEnabled = analysisCheckBox.isSelected();
            if (analysisEnabled) {
                requestAnalysis();
            } else {
                bestMoveArea.setText("Analysis disabled");
                evaluationLabel.setText("Eval: -");
            }
        });
        controlPanel.add(analysisCheckBox);
        controlPanel.add(Box.createVerticalStrut(8));

        // Flip Board Toggle
        JCheckBox flipBoardCheckBox = new JCheckBox("Flip Board");
        flipBoardCheckBox.setSelected(false);
        flipBoardCheckBox.setBackground(new Color(40, 30, 58));
        flipBoardCheckBox.setForeground(new Color(245, 238, 255));
        flipBoardCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        flipBoardCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        flipBoardCheckBox.setOpaque(false);
        flipBoardCheckBox.addActionListener(e -> {
            board.flipBoard();
        });
        controlPanel.add(flipBoardCheckBox);
        controlPanel.add(Box.createVerticalStrut(8));

        // Legal Moves Toggle
        legalMovesCheckBox = new JCheckBox("Legal Moves");
        legalMovesCheckBox.setSelected(true);
        legalMovesCheckBox.setBackground(new Color(40, 30, 58));
        legalMovesCheckBox.setForeground(new Color(245, 238, 255));
        legalMovesCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        legalMovesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        legalMovesCheckBox.setOpaque(false);
        legalMovesCheckBox.setToolTipText("Show legal moves when a piece is selected");
        legalMovesCheckBox.addActionListener(e -> {
            showLegalMoves = legalMovesCheckBox.isSelected();
            board.setShowLegalMoves(showLegalMoves);
        });
        controlPanel.add(legalMovesCheckBox);
        controlPanel.add(Box.createVerticalStrut(15));

        // PGN Load Section
        JLabel pgnLabel = new JLabel("Load position from PGN:");
        pgnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pgnLabel.setForeground(new Color(186, 174, 210));
        pgnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(pgnLabel);
        controlPanel.add(Box.createVerticalStrut(5));

        pgnTextField = new JTextField();
        pgnTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pgnTextField.setBackground(new Color(33, 24, 48));
        pgnTextField.setForeground(new Color(245, 238, 255));
        pgnTextField.setCaretColor(new Color(245, 238, 255));
        pgnTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(96, 78, 128), 1, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        pgnTextField.setMaximumSize(new Dimension(360, 38));
        pgnTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        pgnTextField.setToolTipText("Enter PGN moves (e.g., e4 e5 Nf3)");
        controlPanel.add(pgnTextField);
        controlPanel.add(Box.createVerticalStrut(5));

        JButton loadPgnBtn = new JButton("Load PGN");
        loadPgnBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadPgnBtn.setMaximumSize(new Dimension(360, 42));
        styleButton(loadPgnBtn);
        loadPgnBtn.setBackground(new Color(126, 90, 198));
        loadPgnBtn.addActionListener(e -> loadPGN());
        controlPanel.add(loadPgnBtn);
        controlPanel.add(Box.createVerticalStrut(20));

        // Reset button
        JButton resetBtn = new JButton("Reset Board");
        resetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetBtn.setMaximumSize(new Dimension(260, 44));
        styleButton(resetBtn);
        resetBtn.addActionListener(e -> {
            board.resetToStartPosition();
            pgnTextField.setText("");
            requestAnalysis();
        });
        controlPanel.add(resetBtn);
        controlPanel.add(Box.createVerticalStrut(10));

        // Back button
        JButton backBtn = new JButton("Back to Menu");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(260, 44));
        styleButton(backBtn);
        backBtn.setBackground(new Color(74, 55, 108));
        backBtn.addActionListener(e -> {
            shutdown();
            if (appHost != null) {
                appHost.returnToMenuFromEmbeddedScreen();
            } else {
                dispose();
                SwingUtilities.invokeLater(() -> new MainMenu());
            }
        });
        controlPanel.add(backBtn);

        centerPanel.add(controlPanel, BorderLayout.NORTH);

        // â”€â”€ RIGHT: opening + history â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel historyContainer = new JPanel(new BorderLayout());
        historyContainer.setBackground(new Color(30, 24, 42));
        historyContainer.setPreferredSize(new Dimension(360, 800));

        JPanel engineSection = new JPanel();
        engineSection.setLayout(new BoxLayout(engineSection, BoxLayout.Y_AXIS));
        engineSection.setBackground(new Color(28, 28, 28));
        engineSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JPanel topTabs = new JPanel(new GridLayout(1, 3, 0, 0));
        topTabs.setBackground(new Color(28, 28, 28));
        topTabs.add(createTabLabel("Analysis", true));
        topTabs.add(createTabLabel("Games", false));
        topTabs.add(createTabLabel("Explore", false));
        engineSection.add(topTabs);

        JPanel engineBar = new JPanel(new BorderLayout());
        engineBar.setBackground(new Color(35, 35, 35));
        engineBar.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        analysisCheckBox = new JCheckBox("Analysis");
        analysisCheckBox.setSelected(true);
        analysisCheckBox.setOpaque(false);
        analysisCheckBox.setFont(new Font("Segoe UI", Font.BOLD, 24));
        analysisCheckBox.setForeground(new Color(225, 225, 225));
        analysisCheckBox.addActionListener(e -> {
            analysisEnabled = analysisCheckBox.isSelected();
            if (analysisEnabled) {
                requestAnalysis();
            } else {
                updateEngineRows("-", "Analysis disabled");
                evaluationLabel.setText("Eval: -");
            }
        });
        JLabel depthLabel = new JLabel("depth=14");
        depthLabel.setForeground(new Color(200, 200, 200));
        depthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        engineBar.add(analysisCheckBox, BorderLayout.WEST);
        engineBar.add(depthLabel, BorderLayout.EAST);
        engineSection.add(engineBar);

        JPanel linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.Y_AXIS));
        linePanel.setBackground(new Color(30, 30, 30));
        linePanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        for (int i = 0; i < 3; i++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(new Color(28, 28, 28));
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(52, 52, 52)),
                BorderFactory.createEmptyBorder(6, 2, 6, 2)
            ));
            JLabel score = new JLabel("+0.00", SwingConstants.CENTER);
            score.setPreferredSize(new Dimension(72, 32));
            score.setOpaque(true);
            score.setBackground(new Color(238, 238, 238));
            score.setForeground(new Color(22, 22, 22));
            score.setFont(new Font("Segoe UI", Font.BOLD, 22));
            JLabel line = new JLabel("Waiting for engine...");
            line.setForeground(new Color(230, 230, 230));
            line.setFont(new Font("Consolas", Font.PLAIN, 24));
            lineScoreLabels.add(score);
            lineMoveLabels.add(line);
            row.add(score, BorderLayout.WEST);
            row.add(line, BorderLayout.CENTER);
            linePanel.add(row);
        }
        engineSection.add(linePanel);

        openingNamePanel = new OpeningNamePanel();
        moveHistoryPanel = new MoveHistoryPanel();
        moveHistoryPanel.setBoardReference(board);
        moveHistoryPanel.setReviewAction(this::runReview);
        moveHistoryPanel.setBackground(new Color(28, 24, 36));

        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setBackground(new Color(30, 24, 42));
        topRight.add(engineSection, BorderLayout.NORTH);
        historyContainer.add(topRight,  BorderLayout.NORTH);
        historyContainer.add(moveHistoryPanel,  BorderLayout.CENTER);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(historyContainer, BorderLayout.CENTER);
        if (appHost != null) {
            appHost.showEmbeddedScreen(mainPanel, this::shutdown);
        } else {
            add(mainPanel);
        }
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(new Color(250, 246, 255));
        btn.setBackground(new Color(124, 90, 196));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createLineBorder(new Color(162, 134, 216), 1, true));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JLabel createTabLabel(String text, boolean selected) {
        JLabel tab = new JLabel(text, SwingConstants.CENTER);
        tab.setOpaque(true);
        tab.setBackground(selected ? new Color(42, 42, 42) : new Color(30, 30, 30));
        tab.setForeground(selected ? new Color(245, 245, 245) : new Color(180, 180, 180));
        tab.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        return tab;
    }

    private void updateEngineRows(String scoreText, String lineText) {
        for (int i = 0; i < lineScoreLabels.size(); i++) {
            lineScoreLabels.get(i).setText(i == 0 ? scoreText : "-");
            lineMoveLabels.get(i).setText(i == 0 ? lineText : "...");
        }
    }

    // â”€â”€ PGN Loading â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void loadPGN() {
        String pgnInput = pgnTextField.getText().trim();
        if (pgnInput.isEmpty()) {
            JOptionPane.showMessageDialog(dialogOwner(),
                "Please enter PGN moves",
                "Empty Input",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Reset board first
            board.resetToStartPosition();
            
            // Parse and apply moves
            String[] moves = pgnInput.split("\\s+");
            boolean success = true;
            
            for (String move : moves) {
                // Skip move numbers like "1.", "2.", etc.
                if (move.matches("\\d+\\.+")) {
                    continue;
                }
                
                // Clean up the move (remove check/checkmate symbols)
                String cleanMove = move.replaceAll("[+#!?]", "");
                
                if (!cleanMove.isEmpty()) {
                    boolean moveApplied = board.applyMoveFromNotation(cleanMove);
                    if (!moveApplied) {
                        success = false;
                        JOptionPane.showMessageDialog(dialogOwner(),
                            "Invalid move: " + move + "\nPosition loaded up to this point.",
                            "PGN Error",
                            JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                }
            }
            
            if (success) {
                JOptionPane.showMessageDialog(dialogOwner(),
                    "PGN loaded successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            requestAnalysis();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dialogOwner(),
                "Error loading PGN: " + e.getMessage(),
                "PGN Error",
                JOptionPane.ERROR_MESSAGE);
            board.resetToStartPosition();
        }
    }

    // â”€â”€ called by Board after every move in analysis mode â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void onPositionChanged() {
        // update opening name
        String fen = board.generateFEN();
        if (openingDetector != null && openingNamePanel != null) {
            String name = openingDetector.getOpeningName(fen);
            if (name != null && !name.isEmpty()) {
                openingNamePanel.setOpeningName(name);
                if (moveHistoryPanel != null) {
                    moveHistoryPanel.setOpeningTitle(name);
                }
            }
        }
        requestAnalysis();
    }

    // â”€â”€ analysis worker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * Signal that a fresh analysis is needed.  Safe to call from any thread.
     */
    private void requestAnalysis() {
        if (!analysisEnabled) return;
        
        analysisGeneration++;                       // invalidate any in-flight result
        synchronized (analysisLock) {
            analysisLock.notifyAll();               // wake the worker
        }
    }

    /**
     * Single background thread.  Loops forever until shutdown.
     * Uses analysisGeneration to detect whether the position changed while
     * Stockfish was still searching â€“ if so, the result is thrown away and
     * the loop immediately starts a new search on the current position.
     */
    private void startWorker() {
        if (stockfishEngine == null && positionEvaluator == null) return;

        Thread worker = new Thread(() -> {
            workerAlive = true;
            while (!shutdownRequested) {

                // â”€â”€ wait for work â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                int genBeforeWait;
                synchronized (analysisLock) {
                    genBeforeWait = analysisGeneration;
                    // if nobody has requested anything yet, wait
                    try {
                        analysisLock.wait(200);     // 200 ms timeout as safety
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                if (shutdownRequested) break;

                // snapshot the generation we are about to serve
                int myGen = analysisGeneration;
                
                // Skip if analysis is disabled
                if (!analysisEnabled) continue;

                try {
                    String fen = board.generateFEN();

                    // show "analyzingâ€¦" while we wait
                    EngineAnalysis result = null;
                    if (stockfishEngine != null) {
                        result = stockfishEngine.analyzePosition(fen, 14);
                    }
                    if (positionEvaluator != null) {
                        EngineAnalysis peResult = analyzeWithPositionEvaluator(fen);
                        if (result == null) {
                            result = peResult;
                        } else if (peResult != null) {
                            result.evaluation = peResult.evaluation;
                            result.isMate = peResult.isMate;
                            result.mateIn = peResult.mateIn;
                        }
                    }
                    if (result == null) continue;

                    // â”€â”€ stale-check: if generation changed while we searched,
                    //    drop this result silently â€“ the loop will re-enter and
                    //    search again with the new position.
                    if (analysisGeneration != myGen) continue;
                    if (shutdownRequested)           break;

                    // push onto EDT
                    final EngineAnalysis finalResult = result;
                    SwingUtilities.invokeLater(() -> applyResult(finalResult));

                } catch (IOException e) {
                    if (!shutdownRequested) e.printStackTrace();
                }
            }
            workerAlive = false;
        });
        worker.setDaemon(true);
        worker.start();
    }

    /** Push a finished EngineAnalysis into every UI widget. */
    private void applyResult(EngineAnalysis a) {
        if (a == null || !analysisEnabled) return;

        // â”€â”€ eval bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (a.isMate) {
            evaluationBar.setMate(a.mateIn, a.evaluation > 0);
        } else {
            evaluationBar.setEvaluation(a.evaluation);
        }

        // â”€â”€ eval label â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (a.isMate) {
            String side = a.evaluation > 0 ? "White" : "Black";
            evaluationLabel.setText(side + " mates in " + a.mateIn);
            evaluationLabel.setForeground(new Color(255, 200, 80));   // gold for mate
        } else {
            if (a.evaluation == 0) {
                evaluationLabel.setText("Eval:  0.0");
            } else {
                evaluationLabel.setText(String.format("Eval: %+.1f", a.evaluation / 100.0));
            }
            evaluationLabel.setForeground(new Color(245, 236, 255));
        }

        // â”€â”€ best move â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (a.bestMove != null && !a.bestMove.equals("(none)")) {
            bestMoveArea.setText(formatBestMove(a.bestMove));
        } else {
            bestMoveArea.setText("-");
        }

        String scoreText = a.isMate ? ("M" + a.mateIn) : String.format("%+.2f", a.evaluation / 100.0);
        String lineText = (a.bestMove != null && !a.bestMove.equals("(none)"))
            ? formatBestMove(a.bestMove)
            : "-";
        updateEngineRows(scoreText, lineText);
    }

    // â”€â”€ shutdown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void shutdown() {
        shutdownRequested = true;
        synchronized (analysisLock) {
            analysisLock.notifyAll();
        }
        // give worker a moment to finish its current analyzePosition call
        // (it will return once Stockfish emits bestmove at depth 20)
        long deadline = System.currentTimeMillis() + 3000;
        while (workerAlive && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        if (stockfishEngine != null) {
            try { stockfishEngine.close(); } catch (IOException ignored) {}
        }
        if (positionEvaluator != null) {
            positionEvaluator.close();
        }
    }

    // â”€â”€ formatters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private String formatBestMove(String uciMove) {
        if (uciMove == null || uciMove.length() < 4) return uciMove;
        String move = "" + uciMove.charAt(0) + uciMove.charAt(1)
                    + " -> " + uciMove.charAt(2) + uciMove.charAt(3);
        if (uciMove.length() == 5) {
            move += " =" + Character.toUpperCase(uciMove.charAt(4));
        }
        return move;
    }

    private EngineAnalysis analyzeWithPositionEvaluator(String fen) throws IOException {
        if (positionEvaluator == null) return null;
        EngineAnalysis out = new EngineAnalysis();
        double centipawns = positionEvaluator.evaluateCentipawns(fen, 14);
        out.evaluation = (int) Math.round(centipawns);
        out.isMate = false;
        out.mateIn = 0;
        return out;
    }

    private void runReview() {
        List<String> fens = board.getFensBeforeEachMove();
        List<String> moves = board.getUciMoveHistoryFromStates();
        int count = Math.min(fens.size(), moves.size());
        if (count <= 0) {
            JOptionPane.showMessageDialog(dialogOwner(), "Play some moves first, then run Review.", "Review", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<String> reviewFens = new ArrayList<>(fens.subList(0, count));
        List<String> reviewMoves = new ArrayList<>(moves.subList(0, count));

        final JDialog progress = new JDialog(dialogOwner(), "Review", false);
        progress.setLayout(new BorderLayout());
        progress.add(new JLabel("Running game review...", SwingConstants.CENTER), BorderLayout.CENTER);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        progress.add(bar, BorderLayout.SOUTH);
        progress.setSize(320, 90);
        progress.setLocationRelativeTo(this);

        SwingWorker<Review.GameSummary, Void> worker = new SwingWorker<Review.GameSummary, Void>() {
            @Override
            protected Review.GameSummary doInBackground() throws Exception {
                Review review = new Review();
                try {
                    return review.reviewGame(reviewFens, reviewMoves);
                } finally {
                    review.close();
                }
            }

            @Override
            protected void done() {
                progress.dispose();
                try {
                    Review.GameSummary summary = get();
                    JTextArea area = new JTextArea(buildReviewReport(summary));
                    area.setEditable(false);
                    area.setFont(new Font("Consolas", Font.PLAIN, 13));
                    area.setCaretPosition(0);
                    JScrollPane pane = new JScrollPane(area);
                    pane.setPreferredSize(new Dimension(820, 560));
                    JOptionPane.showMessageDialog(AnalysisGame.this, pane, "Game Review", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        AnalysisGame.this,
                        "Review failed: " + ex.getMessage(),
                        "Review Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
        progress.setVisible(true);
    }

    private String buildReviewReport(Review.GameSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("White Accuracy: %.1f%%%n", summary.whiteAccuracy));
        sb.append(String.format("Black Accuracy: %.1f%%%n%n", summary.blackAccuracy));
        for (int i = 0; i < summary.moves.size(); i++) {
            Review.MoveResult r = summary.moves.get(i);
            String side = (i % 2 == 0) ? "White" : "Black";
            sb.append(String.format(
                "#%02d %-5s  %-11s  acc=%5.1f%%  score=%6s  best=%s%n",
                i + 1,
                side,
                r.classification,
                r.accuracy,
                r.scoreAfter,
                r.bestMove
            ));
        }
        return sb.toString();
    }

    // â”€â”€ getters used by Board â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    
    private static class PlayerInfoBarPanel extends JPanel {
        PlayerInfoBarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            GradientPaint bg = new GradientPaint(0, 0, new Color(43, 45, 49), 0, h, new Color(30, 32, 36));
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 36), 0, h / 2, new Color(255, 255, 255, 0)));
            g2.fillRect(0, 0, w, Math.max(1, h / 2));
            g2.dispose();
            super.paintComponent(g);
        }
    }
public MoveHistoryPanel getMoveHistoryPanel() { return moveHistoryPanel; }
    public OpeningDetector  getOpeningDetector()  { return openingDetector;  }
    public OpeningNamePanel getOpeningNamePanel() { return openingNamePanel; }
}


