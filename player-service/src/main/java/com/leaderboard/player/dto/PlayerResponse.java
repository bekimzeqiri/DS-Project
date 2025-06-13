package com.leaderboard.player.dto;

import com.leaderboard.player.entity.Player;

import java.time.LocalDateTime;

public class PlayerResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActiveAt;
    private Player.PlayerStatus status;
    private Long totalGamesPlayed;
    private Long totalScore;
    private Integer currentLevel;
    private Long experiencePoints;

    // Constructors
    public PlayerResponse() {}

    public PlayerResponse(Player player) {
        this.id = player.getId();
        this.username = player.getUsername();
        this.email = player.getEmail();
        this.displayName = player.getDisplayName();
        this.bio = player.getBio();
        this.avatarUrl = player.getAvatarUrl();
        this.createdAt = player.getCreatedAt();
        this.updatedAt = player.getUpdatedAt();
        this.lastActiveAt = player.getLastActiveAt();
        this.status = player.getStatus();
        this.totalGamesPlayed = player.getTotalGamesPlayed();
        this.totalScore = player.getTotalScore();
        this.currentLevel = player.getCurrentLevel();
        this.experiencePoints = player.getExperiencePoints();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Player.PlayerStatus getStatus() { return status; }
    public void setStatus(Player.PlayerStatus status) { this.status = status; }

    public Long getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(Long totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public Long getTotalScore() { return totalScore; }
    public void setTotalScore(Long totalScore) { this.totalScore = totalScore; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Long getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(Long experiencePoints) { this.experiencePoints = experiencePoints; }
}
