public class ChessBot {
    
    private String name;
    private int elo;
    private int skillLevel; // Stockfish Skill Level (0-20)
    private int depth; // Search depth
    private int moveTime; // Time to think in milliseconds
    
    // 5 Bot profiles with custom names, ELOs, and Stockfish skill levels
    public static final ChessBot BOBBY_BEGINNER = new ChessBot("Bobby Beginner", 200, 0, 1, 100);
    public static final ChessBot CASUAL_CARL = new ChessBot("Casual Carl", 700, 5, 3, 200);
    public static final ChessBot TACTICAL_TOM = new ChessBot("Tactical Tom", 1400, 10, 6, 500);
    public static final ChessBot STRATEGIC_SARAH = new ChessBot("Strategic Sarah", 2300, 15, 10, 1000);
    public static final ChessBot MAGNUS_AI = new ChessBot("Grandmother Levy", 6700, 20, 18, 2000);
    
    private ChessBot(String name, int elo, int skillLevel, int depth, int moveTime) {
        this.name = name;
        this.elo = elo;
        this.skillLevel = skillLevel;
        this.depth = depth;
        this.moveTime = moveTime;
    }
    
    public String getName() {
        return name;
    }
    
    public int getElo() {
        return elo;
    }
    
    public int getSkillLevel() {
        return skillLevel;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public int getMoveTime() {
        return moveTime;
    }
    
    public String getDescription() {
        if (elo <= 300) return "Perfect for beginners";
        if (elo <= 900) return "Casual player";
        if (elo <= 1700) return "Intermediate strength";
        if (elo <= 2500) return "Advanced tactics";
        return "Grandmother level";
    }
    
    public String getDifficultyStars() {
        if (elo <= 300) return "★☆☆☆☆";
        if (elo <= 900) return "★★☆☆☆";
        if (elo <= 1700) return "★★★☆☆";
        if (elo <= 2500) return "★★★★☆";
        return "★★★★★";
    }
    
    public static ChessBot[] getAllBots() {
        return new ChessBot[]{
            BOBBY_BEGINNER,
            CASUAL_CARL,
            TACTICAL_TOM,
            STRATEGIC_SARAH,
            MAGNUS_AI
        };
    }
    
    @Override
    public String toString() {
        return name + " (ELO " + elo + ")";
    }
}
