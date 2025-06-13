package com.leaderboard.player.dto;

import com.leaderboard.player.entity.Player;

public class PlayerStatistics {
    private Long playerId;
    private String username;
    private String displayName;
    private Long totalGamesPlayed;
    private Long totalScore;
    private Integer currentLevel;
    private Long experiencePoints;
    private Double averageScore;
    private Integer globalRank;

    // Constructors
    public PlayerStatistics() {}

    public PlayerStatistics(Player player) {
        this.playerId = player.getId();
        this.username = player.getUsername();
        this.displayName = player.getDisplayName();
        this.totalGamesPlayed = player.getTotalGamesPlayed();
        this.totalScore = player.getTotalScore();
        this.currentLevel = player.getCurrentLevel();
        this.experiencePoints = player.getExperiencePoints();
        this.averageScore = player.getTotalGamesPlayed() > 0 ?
                (double) player.getTotalScore() / player.getTotalGamesPlayed() : 0.0;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Long getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(Long totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public Long getTotalScore() { return totalScore; }
    public void setTotalScore(Long totalScore) { this.totalScore = totalScore; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Long getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(Long experiencePoints) { this.experiencePoints = experiencePoints; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    public Integer getGlobalRank() { return globalRank; }
    public void setGlobalRank(Integer globalRank) { this.globalRank = globalRank; }
}