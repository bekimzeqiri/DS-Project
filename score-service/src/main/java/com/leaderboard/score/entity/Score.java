package com.leaderboard.score.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Score Entity
 *
 * Represents a score submission in the leaderboard system.
 */
@Entity
@Table(name = "scores", indexes = {
        @Index(name = "idx_score_player", columnList = "playerId"),
        @Index(name = "idx_score_leaderboard", columnList = "leaderboardId"),
        @Index(name = "idx_score_value", columnList = "value"),
        @Index(name = "idx_score_timestamp", columnList = "submittedAt"),
        @Index(name = "idx_score_player_leaderboard", columnList = "playerId, leaderboardId"),
        @Index(name = "idx_score_leaderboard_value", columnList = "leaderboardId, value")
})
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "Player ID is required")
    private Long playerId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Leaderboard ID is required")
    private String leaderboardId;

    @Column(nullable = false)
    @NotNull(message = "Score value is required")
    @Min(value = 0, message = "Score value must be non-negative")
    private Long value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreType type = ScoreType.POINTS;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(length = 1000)
    private String metadata; // JSON string for additional game-specific data

    @Column(length = 100)
    private String sessionId; // Game session identifier

    @Column(nullable = false)
    private Boolean verified = false; // Whether the score has been verified

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreStatus status = ScoreStatus.PENDING;

    @Column(length = 500)
    private String verificationNotes; // Notes from verification process

    // Anti-cheat fields
    @Column(nullable = false)
    private Boolean suspicious = false;

    @Column
    private Double suspicionScore = 0.0; // 0.0 to 1.0, higher = more suspicious

    @Column(length = 1000)
    private String suspicionReasons; // JSON array of reasons why score is suspicious

    // Performance metrics
    @Column
    private Long gameplayDurationMs; // How long the game session lasted

    @Column
    private Integer movesOrActions; // Number of moves/actions taken

    @Column(length = 100)
    private String clientVersion; // Version of the game client

    @Column(length = 50)
    private String platform; // Platform (web, mobile, desktop, etc.)

    // Constructors
    public Score() {
        this.submittedAt = LocalDateTime.now();
    }

    public Score(Long playerId, String leaderboardId, Long value, ScoreType type) {
        this();
        this.playerId = playerId;
        this.leaderboardId = leaderboardId;
        this.value = value;
        this.type = type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getLeaderboardId() {
        return leaderboardId;
    }

    public void setLeaderboardId(String leaderboardId) {
        this.leaderboardId = leaderboardId;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public ScoreType getType() {
        return type;
    }

    public void setType(ScoreType type) {
        this.type = type;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public ScoreStatus getStatus() {
        return status;
    }

    public void setStatus(ScoreStatus status) {
        this.status = status;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public Boolean getSuspicious() {
        return suspicious;
    }

    public void setSuspicious(Boolean suspicious) {
        this.suspicious = suspicious;
    }

    public Double getSuspicionScore() {
        return suspicionScore;
    }

    public void setSuspicionScore(Double suspicionScore) {
        this.suspicionScore = suspicionScore;
    }

    public String getSuspicionReasons() {
        return suspicionReasons;
    }

    public void setSuspicionReasons(String suspicionReasons) {
        this.suspicionReasons = suspicionReasons;
    }

    public Long getGameplayDurationMs() {
        return gameplayDurationMs;
    }

    public void setGameplayDurationMs(Long gameplayDurationMs) {
        this.gameplayDurationMs = gameplayDurationMs;
    }

    public Integer getMovesOrActions() {
        return movesOrActions;
    }

    public void setMovesOrActions(Integer movesOrActions) {
        this.movesOrActions = movesOrActions;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    // Helper methods
    public boolean isBetterThan(Score other) {
        if (!this.leaderboardId.equals(other.leaderboardId) || this.type != other.type) {
            throw new IllegalArgumentException("Cannot compare scores from different leaderboards or types");
        }

        return switch (this.type) {
            case TIME_MS -> this.value < other.value; // Lower time is better
            case POINTS, DISTANCE -> this.value > other.value; // Higher is better
        };
    }

    public void markAsVerified(String notes) {
        this.verified = true;
        this.status = ScoreStatus.VERIFIED;
        this.verificationNotes = notes;
    }

    public void markAsRejected(String reason) {
        this.verified = false;
        this.status = ScoreStatus.REJECTED;
        this.verificationNotes = reason;
    }

    public void markAsSuspicious(double suspicionScore, String reasons) {
        this.suspicious = true;
        this.suspicionScore = suspicionScore;
        this.suspicionReasons = reasons;
        this.status = ScoreStatus.UNDER_REVIEW;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return Objects.equals(id, score.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", leaderboardId='" + leaderboardId + '\'' +
                ", value=" + value +
                ", type=" + type +
                ", submittedAt=" + submittedAt +
                ", status=" + status +
                '}';
    }

    /**
     * Score Type Enum
     */
    public enum ScoreType {
        POINTS,    // Higher is better
        TIME_MS,   // Lower is better (time in milliseconds)
        DISTANCE   // Higher is better
    }

    /**
     * Score Status Enum
     */
    public enum ScoreStatus {
        PENDING,      // Newly submitted, awaiting processing
        VERIFIED,     // Verified and accepted
        REJECTED,     // Rejected due to validation failure
        UNDER_REVIEW, // Flagged for manual review
        EXPIRED       // Old score that has expired
    }
}