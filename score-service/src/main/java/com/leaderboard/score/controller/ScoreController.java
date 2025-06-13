package com.leaderboard.score.controller;

import com.leaderboard.score.dto.*;
import com.leaderboard.score.entity.Score;
import com.leaderboard.score.exception.InvalidScoreException;
import com.leaderboard.score.exception.PlayerNotFoundException;
import com.leaderboard.score.exception.RateLimitExceededException;
import com.leaderboard.score.service.AntiCheatService;
import com.leaderboard.score.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Score REST Controller
 **/
@RestController
@RequestMapping("/scores")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private AntiCheatService antiCheatService;

    /**
     * Health check endpoint - put this first to avoid conflicts
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "score-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Submit a new score
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitScore(@Valid @RequestBody ScoreSubmissionRequest request) {
        try {
            ScoreResponse response = scoreService.submitScore(request);

            // Return different status codes based on score status
            if (response.getStatus() == Score.ScoreStatus.VERIFIED) {
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } else if (response.getStatus() == Score.ScoreStatus.UNDER_REVIEW) {
                return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
            } else {
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (RateLimitExceededException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Rate limit exceeded");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
        } catch (PlayerNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Player not found");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            error.put("cause", e.getClass().getSimpleName());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get score by ID - use regex to ensure only numbers
     */
    @GetMapping("/id/{scoreId:[0-9]+}")
    public ResponseEntity<ScoreResponse> getScore(@PathVariable Long scoreId) {
        try {
            ScoreResponse response = scoreService.getScoreById(scoreId);
            return ResponseEntity.ok(response);
        } catch (InvalidScoreException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get player's scores
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<ScoreResponse>> getPlayerScores(@PathVariable Long playerId) {
        List<ScoreResponse> scores = scoreService.getPlayerScores(playerId);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get player's scores for a specific leaderboard
     */
    @GetMapping("/player/{playerId}/leaderboard/{leaderboardId}")
    public ResponseEntity<List<ScoreResponse>> getPlayerScoresForLeaderboard(
            @PathVariable Long playerId,
            @PathVariable String leaderboardId) {
        List<ScoreResponse> scores = scoreService.getPlayerScoresForLeaderboard(playerId, leaderboardId);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get player's best score for a leaderboard
     */
    @GetMapping("/player/{playerId}/leaderboard/{leaderboardId}/best")
    public ResponseEntity<ScoreResponse> getPlayerBestScore(
            @PathVariable Long playerId,
            @PathVariable String leaderboardId) {
        Optional<ScoreResponse> bestScore = scoreService.getPlayerBestScore(playerId, leaderboardId);
        return bestScore.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get top scores for a leaderboard
     */
    @GetMapping("/leaderboard/{leaderboardId}/top")
    public ResponseEntity<List<ScoreResponse>> getTopScores(
            @PathVariable String leaderboardId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ScoreResponse> scores = scoreService.getTopScores(leaderboardId, limit);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get scores for a leaderboard with pagination
     */
    @GetMapping("/leaderboard/{leaderboardId}")
    public ResponseEntity<Page<ScoreResponse>> getLeaderboardScores(
            @PathVariable String leaderboardId,
            @RequestParam Score.ScoreType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ScoreResponse> scores = scoreService.getLeaderboardScores(leaderboardId, type, page, size);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get player's rank in leaderboard
     */
    @GetMapping("/player/{playerId}/leaderboard/{leaderboardId}/rank")
    public ResponseEntity<Map<String, Long>> getPlayerRank(
            @PathVariable Long playerId,
            @PathVariable String leaderboardId) {
        Optional<Long> rank = scoreService.getPlayerRank(playerId, leaderboardId);

        if (rank.isPresent()) {
            Map<String, Long> response = new HashMap<>();
            response.put("rank", rank.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get player score summary for a leaderboard
     */
    @GetMapping("/player/{playerId}/leaderboard/{leaderboardId}/summary")
    public ResponseEntity<PlayerScoreSummary> getPlayerScoreSummary(
            @PathVariable Long playerId,
            @PathVariable String leaderboardId) {
        PlayerScoreSummary summary = scoreService.getPlayerScoreSummary(playerId, leaderboardId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get score statistics for a leaderboard
     */
    @GetMapping("/leaderboard/{leaderboardId}/statistics")
    public ResponseEntity<ScoreStatistics> getScoreStatistics(@PathVariable String leaderboardId) {
        ScoreStatistics statistics = scoreService.getScoreStatistics(leaderboardId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get recent scores for a leaderboard
     */
    @GetMapping("/leaderboard/{leaderboardId}/recent")
    public ResponseEntity<List<ScoreResponse>> getRecentScores(
            @PathVariable String leaderboardId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "20") int limit) {
        List<ScoreResponse> scores = scoreService.getRecentScores(leaderboardId, hours, limit);
        return ResponseEntity.ok(scores);
    }

    /**
     * Admin: Verify a score
     */
    @PostMapping("/id/{scoreId:[0-9]+}/verify")
    public ResponseEntity<ScoreResponse> verifyScore(
            @PathVariable Long scoreId,
            @RequestBody(required = false) String notes) {
        try {
            ScoreResponse response = scoreService.verifyScore(scoreId, notes != null ? notes : "Manually verified");
            return ResponseEntity.ok(response);
        } catch (InvalidScoreException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Admin: Reject a score
     */
    @PostMapping("/id/{scoreId:[0-9]+}/reject")
    public ResponseEntity<ScoreResponse> rejectScore(
            @PathVariable Long scoreId,
            @RequestBody String reason) {
        try {
            ScoreResponse response = scoreService.rejectScore(scoreId, reason);
            return ResponseEntity.ok(response);
        } catch (InvalidScoreException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Admin: Get scores requiring review
     */
    @GetMapping("/admin/review")
    public ResponseEntity<List<ScoreResponse>> getScoresRequiringReview() {
        List<ScoreResponse> scores = scoreService.getScoresRequiringReview();
        return ResponseEntity.ok(scores);
    }

    /**
     * Admin: Get suspicious scores for a leaderboard
     */
    @GetMapping("/admin/suspicious/{leaderboardId}")
    public ResponseEntity<List<ScoreResponse>> getSuspiciousScores(
            @PathVariable String leaderboardId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Score> suspiciousScores = antiCheatService.findSuspiciousScores(leaderboardId, limit);
        List<ScoreResponse> responses = suspiciousScores.stream()
                .map(ScoreResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get anti-cheat explanation
     */
    @GetMapping("/anticheat/explain")
    public ResponseEntity<Map<String, String>> getAntiCheatExplanation(
            @RequestParam double suspicionScore) {
        String explanation = antiCheatService.getSuspicionExplanation(suspicionScore);
        Map<String, String> response = new HashMap<>();
        response.put("explanation", explanation);
        response.put("suspicionScore", String.valueOf(suspicionScore));
        return ResponseEntity.ok(response);
    }

    /**
     * Global exception handlers
     */
    @ExceptionHandler(InvalidScoreException.class)
    public ResponseEntity<Map<String, String>> handleInvalidScore(InvalidScoreException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid score");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePlayerNotFound(PlayerNotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Player not found");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitExceededException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Rate limit exceeded");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}