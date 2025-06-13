package com.leaderboard.score.dto;

public class ScoreStatistics {
    private String leaderboardId;
    private Long totalScores;
    private Double averageScore;
    private Long minScore;
    private Long maxScore;
    private Long totalPlayers;

    // Constructors
    public ScoreStatistics() {}

    public ScoreStatistics(String leaderboardId, Long totalScores, Double averageScore,
                           Long minScore, Long maxScore, Long totalPlayers) {
        this.leaderboardId = leaderboardId;
        this.totalScores = totalScores;
        this.averageScore = averageScore;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.totalPlayers = totalPlayers;
    }

    // Getters and Setters
    public String getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(String leaderboardId) { this.leaderboardId = leaderboardId; }

    public Long getTotalScores() { return totalScores; }
    public void setTotalScores(Long totalScores) { this.totalScores = totalScores; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    public Long getMinScore() { return minScore; }
    public void setMinScore(Long minScore) { this.minScore = minScore; }

    public Long getMaxScore() { return maxScore; }
    public void setMaxScore(Long maxScore) { this.maxScore = maxScore; }

    public Long getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(Long totalPlayers) { this.totalPlayers = totalPlayers; }
}