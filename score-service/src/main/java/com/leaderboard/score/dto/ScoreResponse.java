package com.leaderboard.score.dto;

import com.leaderboard.score.entity.Score;

import java.time.LocalDateTime;

public class ScoreResponse {
    private Long id;
    private Long playerId;
    private String leaderboardId;
    private Long value;
    private Score.ScoreType type;
    private LocalDateTime submittedAt;
    private String metadata;
    private String sessionId;
    private Boolean verified;
    private Score.ScoreStatus status;
    private Boolean suspicious;
    private Double suspicionScore;
    private Long gameplayDurationMs;
    private Integer movesOrActions;
    private String clientVersion;
    private String platform;

    // Constructors
    public ScoreResponse() {}

    public ScoreResponse(Score score) {
        this.id = score.getId();
        this.playerId = score.getPlayerId();
        this.leaderboardId = score.getLeaderboardId();
        this.value = score.getValue();
        this.type = score.getType();
        this.submittedAt = score.getSubmittedAt();
        this.metadata = score.getMetadata();
        this.sessionId = score.getSessionId();
        this.verified = score.getVerified();
        this.status = score.getStatus();
        this.suspicious = score.getSuspicious();
        this.suspicionScore = score.getSuspicionScore();
        this.gameplayDurationMs = score.getGameplayDurationMs();
        this.movesOrActions = score.getMovesOrActions();
        this.clientVersion = score.getClientVersion();
        this.platform = score.getPlatform();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getLeaderboardId() { return leaderboardId; }
    public void setLeaderboardId(String leaderboardId) { this.leaderboardId = leaderboardId; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }

    public Score.ScoreType getType() { return type; }
    public void setType(Score.ScoreType type) { this.type = type; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public Score.ScoreStatus getStatus() { return status; }
    public void setStatus(Score.ScoreStatus status) { this.status = status; }

    public Boolean getSuspicious() { return suspicious; }
    public void setSuspicious(Boolean suspicious) { this.suspicious = suspicious; }

    public Double getSuspicionScore() { return suspicionScore; }
    public void setSuspicionScore(Double suspicionScore) { this.suspicionScore = suspicionScore; }

    public Long getGameplayDurationMs() { return gameplayDurationMs; }
    public void setGameplayDurationMs(Long gameplayDurationMs) { this.gameplayDurationMs = gameplayDurationMs; }

    public Integer getMovesOrActions() { return movesOrActions; }
    public void setMovesOrActions(Integer movesOrActions) { this.movesOrActions = movesOrActions; }

    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
