package com.leaderboard.score.dto;

import java.time.LocalDateTime;

public class PlayerScoreSummary {
    private Long playerId;
    private String leaderboardId;
    private Long bestScore;
    private Double averageScore;
    private Long totalSubmissions;
    private Long rank;
    private LocalDateTime lastSubmission;

    // Constructors
    public PlayerScoreSummary() {}

    // Getters and Setters
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(String leaderboardId) { this.leaderboardId = leaderboardId; }

    public Long getBestScore() { return bestScore; }
    public void setBestScore(Long bestScore) { this.bestScore = bestScore; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    public Long getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(Long totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public Long getRank() { return rank; }
    public void setRank(Long rank) { this.rank = rank; }

    public LocalDateTime getLastSubmission() { return lastSubmission; }
    public void setLastSubmission(LocalDateTime lastSubmission) { this.lastSubmission = lastSubmission; }
}