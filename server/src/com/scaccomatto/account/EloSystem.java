package com.scaccomatto.account;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EloSystem {
    record Rule(String name, int minElo, int maxElo, int win, int draw, int lose) {
        int deltaFor(String result) {
            return switch (result) {
                case "win" -> win;
                case "draw" -> draw;
                case "loss" -> lose;
                default -> throw new IllegalArgumentException("Unsupported result: " + result);
            };
        }
    }

    private static final Pattern RULE_PATTERN = Pattern.compile(
            "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"minElo\"\\s*:\\s*(-?\\d+)\\s*,\\s*"
                    + "\"maxElo\"\\s*:\\s*(-?\\d+)\\s*,\\s*"
                    + "\"win\"\\s*:\\s*(-?\\d+)\\s*,\\s*"
                    + "\"draw\"\\s*:\\s*(-?\\d+)\\s*,\\s*"
                    + "\"lose\"\\s*:\\s*(-?\\d+)\\s*\\}",
            Pattern.DOTALL);

    private final List<Rule> rules;
    private final int minRating;
    private final int maxRating;

    private EloSystem(List<Rule> rules) {
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("ELO rules must contain at least one tier.");
        }
        this.rules = List.copyOf(rules);
        this.minRating = this.rules.stream().mapToInt(Rule::minElo).min().orElse(0);
        this.maxRating = this.rules.stream().mapToInt(Rule::maxElo).max().orElse(3500);
    }

    static EloSystem load(Path path) throws IOException {
        String source = Files.readString(path);
        Matcher matcher = RULE_PATTERN.matcher(source);
        List<Rule> rules = new ArrayList<>();
        while (matcher.find()) {
            rules.add(new Rule(
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)),
                    Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6))));
        }
        rules.sort(Comparator.comparingInt(Rule::minElo));
        return new EloSystem(rules);
    }

    RatingChange apply(int currentRating, String result) {
        Rule before = ruleFor(currentRating);
        int unclamped = currentRating + before.deltaFor(result);
        int nextRating = Math.max(minRating, Math.min(maxRating, unclamped));
        Rule after = ruleFor(nextRating);
        return new RatingChange(
                currentRating,
                nextRating,
                nextRating - currentRating,
                before.name(),
                after.name());
    }

    private Rule ruleFor(int rating) {
        for (Rule rule : rules) {
            if (rating >= rule.minElo() && rating <= rule.maxElo()) return rule;
        }
        return rating < minRating ? rules.get(0) : rules.get(rules.size() - 1);
    }

    record RatingChange(
            int previousRating,
            int newRating,
            int ratingDelta,
            String previousRank,
            String newRank) {
    }
}
