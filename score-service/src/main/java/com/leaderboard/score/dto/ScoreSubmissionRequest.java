package com.leaderboard.score.dto;

import com.leaderboard.score.entity.Score;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Score Submission Request DTO
 */
public class ScoreSubmissionRequest {
    @NotNull(message = "Player ID is required")
    private Long playerId;

    @NotBlank(message = "Leaderboard ID is required")
    private String leaderboardId;

    @NotNull(message = "Score value is required")
    @Min(value = 0, message = "Score value must be non-negative")
    private Long value;

    @NotNull(message = "Score type is required")
    private Score.ScoreType type;

    private String metadata;
    private String sessionId;
    private Long gameplayDurationMs;
    private Integer movesOrActions;
    private String clientVersion;
    private String platform;

    // Constructors
    public ScoreSubmissionRequest() {}

    public ScoreSubmissionRequest(Long playerId, String leaderboardId, Long value, Score.ScoreType type) {
        this.playerId = playerId;
        this.leaderboardId = leaderboardId;
        this.value = value;
        this.type = type;
    }

    // Getters and Setters
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(String leaderboardId) { this.leaderboardId = leaderboardId; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }

    public Score.ScoreType getType() { return type; }
    public void setType(Score.ScoreType type) { this.type = type; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getGameplayDurationMs() { return gameplayDurationMs; }
    public void setGameplayDurationMs(Long gameplayDurationMs) { this.gameplayDurationMs = gameplayDurationMs; }

    public Integer getMovesOrActions() { return movesOrActions; }
    public void setMovesOrActions(Integer movesOrActions) { this.movesOrActions = movesOrActions; }

    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}