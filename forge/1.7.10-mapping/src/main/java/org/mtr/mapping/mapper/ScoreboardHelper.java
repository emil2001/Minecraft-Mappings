package org.mtr.mapping.mapper;

import org.mtr.mapping.holder.*;

public final class ScoreboardHelper {
    private ScoreboardHelper() {}
    public static ScoreboardObjective getScoreboardObjective(Scoreboard board, String name) {
        net.minecraft.scoreboard.ScoreObjective objective = board.data.getObjective(name);
        return objective == null ? null : new ScoreboardObjective(objective);
    }
    public static ScoreboardObjective addObjective(Scoreboard board, String name, ScoreboardCriterion criterion, Text title,
            ScoreboardCriterionRenderType renderType) {
        java.util.Objects.requireNonNull(renderType);
        net.minecraft.scoreboard.ScoreObjective objective = board.data.addScoreObjective(name, criterion.data);
        objective.setDisplayName(title.data.getFormattedText().replaceFirst("(?:§r)+$", ""));
        return new ScoreboardObjective(objective);
    }
    public static int getPlayerScore(Scoreboard board, String name, ScoreboardObjective objective) {
        return board.data.func_96529_a(name, objective.data).getScorePoints();
    }
    public static void setPlayerScore(Scoreboard board, String name, ScoreboardObjective objective, int amount) {
        board.data.func_96529_a(name, objective.data).setScorePoints(amount);
    }
    public static void incrementPlayerScore(Scoreboard board, String name, ScoreboardObjective objective, int amount) {
        board.data.func_96529_a(name, objective.data).increseScore(amount);
    }
}
