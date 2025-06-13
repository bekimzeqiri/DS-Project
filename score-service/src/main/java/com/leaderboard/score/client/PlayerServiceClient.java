package com.leaderboard.score.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Player Service Client
 *
 * Feign client for communicating with the Player Service.
 */
@FeignClient(name = "player-service", url = "${feign.client.config.player-service.url:}")
public interface PlayerServiceClient {

    @GetMapping("/players/{playerId}")
    PlayerInfo getPlayer(@PathVariable("playerId") Long playerId);

    @PostMapping("/players/{playerId}/activity")
    void updatePlayerActivity(@PathVariable("playerId") Long playerId);

    @PostMapping("/players/{playerId}/games/increment")
    void incrementGamesPlayed(@PathVariable("playerId") Long playerId);

    /**
     * Player Info DTO for Feign communication
     */
    class PlayerInfo {
        private Long id;
        private String username;
        private String displayName;
        private String status;
        private Boolean active;

        // Constructors
        public PlayerInfo() {}

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}

/**
 * Leaderboard Service Client
 *
 * Feign client for communicating with the Leaderboard Service.
 */
@FeignClient(name = "leaderboard-service", url = "${feign.client.config.leaderboard-service.url:}")
interface LeaderboardServiceClient {

    @PostMapping("/leaderboards/{leaderboardId}/scores")
    void updateLeaderboard(@PathVariable("leaderboardId") String leaderboardId, ScoreUpdateRequest request);

    /**
     * Score Update Request for Leaderboard Service
     */
    class ScoreUpdateRequest {
        private Long playerId;
        private Long scoreValue;
        private String scoreType;

        // Constructors
        public ScoreUpdateRequest() {}

        public ScoreUpdateRequest(Long playerId, Long scoreValue, String scoreType) {
            this.playerId = playerId;
            this.scoreValue = scoreValue;
            this.scoreType = scoreType;
        }

        // Getters and Setters
        public Long getPlayerId() { return playerId; }
        public void setPlayerId(Long playerId) { this.playerId = playerId; }

        public Long getScoreValue() { return scoreValue; }
        public void setScoreValue(Long scoreValue) { this.scoreValue = scoreValue; }

        public String getScoreType() { return scoreType; }
        public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    }
}