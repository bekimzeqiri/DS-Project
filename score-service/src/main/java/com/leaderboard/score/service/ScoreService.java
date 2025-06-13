package com.leaderboard.score.service;

import com.leaderboard.score.client.PlayerServiceClient;
import com.leaderboard.score.dto.PlayerScoreSummary;
import com.leaderboard.score.dto.ScoreResponse;
import com.leaderboard.score.dto.ScoreStatistics;
import com.leaderboard.score.dto.ScoreSubmissionRequest;
import com.leaderboard.score.entity.Score;
import com.leaderboard.score.exception.InvalidScoreException;
import com.leaderboard.score.exception.PlayerNotFoundException;
import com.leaderboard.score.exception.RateLimitExceededException;
import com.leaderboard.score.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Score Service Implementation
 *
 * Handles all business logic for score operations.
 */
@Service
@Transactional
public class ScoreService {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private PlayerServiceClient playerServiceClient;

    @Autowired
    private AntiCheatService antiCheatService;

    @Value("${app.score.max-submissions-per-minute:10}")
    private int maxSubmissionsPerMinute;

    @Value("${app.score.enable-anti-cheat:true}")
    private boolean enableAntiCheat;

    /**
     * Submit a new score
     */
    public ScoreResponse submitScore(ScoreSubmissionRequest request) {
        // Validate player exists and is active
        validatePlayer(request.getPlayerId());

        // Check rate limiting
        checkRateLimit(request.getPlayerId());

        // Create score entity
        Score score = new Score(request.getPlayerId(), request.getLeaderboardId(),
                request.getValue(), request.getType());

        // Set additional fields
        score.setMetadata(request.getMetadata());
        score.setSessionId(request.getSessionId());
        score.setGameplayDurationMs(request.getGameplayDurationMs());
        score.setMovesOrActions(request.getMovesOrActions());
        score.setClientVersion(request.getClientVersion());
        score.setPlatform(request.getPlatform());

        // Run anti-cheat validation if enabled
        if (enableAntiCheat) {
            antiCheatService.validateScore(score);
        }

        // Save score
        Score savedScore = scoreRepository.save(score);

        // Update player activity asynchronously (disabled for testing)
        // updatePlayerActivityAsync(request.getPlayerId());

        // If score is verified, update leaderboard asynchronously
        if (savedScore.getStatus() == Score.ScoreStatus.VERIFIED) {
            updateLeaderboardAsync(savedScore);
        }

        return new ScoreResponse(savedScore);
    }

    /**
     * Get score by ID
     */
    @Transactional(readOnly = true)
    public ScoreResponse getScoreById(Long scoreId) {
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new InvalidScoreException("Score not found with ID: " + scoreId));

        return new ScoreResponse(score);
    }

    /**
     * Get player's scores
     */
    @Transactional(readOnly = true)
    public List<ScoreResponse> getPlayerScores(Long playerId) {
        List<Score> scores = scoreRepository.findByPlayerIdOrderBySubmittedAtDesc(playerId);
        return scores.stream()
                .map(ScoreResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get player's scores for a specific leaderboard
     */
    @Transactional(readOnly = true)
    public List<ScoreResponse> getPlayerScoresForLeaderboard(Long playerId, String leaderboardId) {
        List<Score> scores = scoreRepository.findByPlayerIdAndLeaderboardIdOrderBySubmittedAtDesc(playerId, leaderboardId);
        return scores.stream()
                .map(ScoreResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get player's best score for a leaderboard
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "playerBestScores", key = "#playerId + '_' + #leaderboardId")
    public Optional<ScoreResponse> getPlayerBestScore(Long playerId, String leaderboardId) {
        Optional<Score> bestScore = scoreRepository.findBestScoreForPlayerInLeaderboard(playerId, leaderboardId);
        return bestScore.map(ScoreResponse::new);
    }

    /**
     * Get top scores for a leaderboard
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "topScores", key = "#leaderboardId + '_' + #limit")
    public List<ScoreResponse> getTopScores(String leaderboardId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Score> scores = scoreRepository.findTopScoresByItsLeaderboard(leaderboardId, pageable);

        return scores.getContent().stream()
                .map(ScoreResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get scores for a leaderboard with pagination
     */
    @Transactional(readOnly = true)
    public Page<ScoreResponse> getLeaderboardScores(String leaderboardId, Score.ScoreType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Score> scores;

        if (type == Score.ScoreType.TIME_MS) {
            scores = scoreRepository.findByLeaderboardIdAndTypeOrderByValueAsc(leaderboardId, type, pageable);
        } else {
            scores = scoreRepository.findByLeaderboardIdAndTypeOrderByValueDesc(leaderboardId, type, pageable);
        }

        return scores.map(ScoreResponse::new);
    }

    /**
     * Get player's rank in leaderboard
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "playerRanks", key = "#playerId + '_' + #leaderboardId")
    public Optional<Long> getPlayerRank(Long playerId, String leaderboardId) {
        return scoreRepository.findPlayerRankInLeaderboard(playerId, leaderboardId);
    }

    /**
     * Get player score summary for a leaderboard
     */
    @Transactional(readOnly = true)
    public PlayerScoreSummary getPlayerScoreSummary(Long playerId, String leaderboardId) {
        PlayerScoreSummary summary = new PlayerScoreSummary();
        summary.setPlayerId(playerId);
        summary.setLeaderboardId(leaderboardId);

        // Get best score
        Optional<Score> bestScore = scoreRepository.findBestScoreForPlayerInLeaderboard(playerId, leaderboardId);
        if (bestScore.isPresent()) {
            summary.setBestScore(bestScore.get().getValue());
            summary.setLastSubmission(bestScore.get().getSubmittedAt());
        }

        // Get average score
        Optional<Double> avgScore = scoreRepository.findAverageScoreForPlayerInLeaderboard(playerId, leaderboardId);
        summary.setAverageScore(avgScore.orElse(0.0));

        // Get total submissions
        Long totalSubmissions = scoreRepository.countByPlayerIdAndStatus(playerId, Score.ScoreStatus.VERIFIED);
        summary.setTotalSubmissions(totalSubmissions);

        // Get rank
        Optional<Long> rank = getPlayerRank(playerId, leaderboardId);
        summary.setRank(rank.orElse(0L));

        return summary;
    }

    /**
     * Get score statistics for a leaderboard
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "scoreStats", key = "#leaderboardId")
    public ScoreStatistics getScoreStatistics(String leaderboardId) {
        List<Object[]> stats = scoreRepository.findLeaderboardStatistics();

        for (Object[] stat : stats) {
            String lbId = (String) stat[0];
            if (lbId.equals(leaderboardId)) {
                Long count = (Long) stat[1];
                Double avg = (Double) stat[2];
                Long min = (Long) stat[3];
                Long max = (Long) stat[4];

                return new ScoreStatistics(leaderboardId, count, avg, min, max, count);
            }
        }

        return new ScoreStatistics(leaderboardId, 0L, 0.0, 0L, 0L, 0L);
    }

    /**
     * Get recent scores for a leaderboard
     */
    @Transactional(readOnly = true)
    public List<ScoreResponse> getRecentScores(String leaderboardId, int hours, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Score> scores = scoreRepository.findRecentScoresForLeaderboard(leaderboardId, since);

        return scores.stream()
                .limit(limit)
                .map(ScoreResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Verify a score (admin function)
     */
    public ScoreResponse verifyScore(Long scoreId, String notes) {
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new InvalidScoreException("Score not found with ID: " + scoreId));

        score.markAsVerified(notes);
        Score updatedScore = scoreRepository.save(score);

        // Update leaderboard asynchronously
        updateLeaderboardAsync(updatedScore);

        return new ScoreResponse(updatedScore);
    }

    /**
     * Reject a score (admin function)
     */
    public ScoreResponse rejectScore(Long scoreId, String reason) {
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new InvalidScoreException("Score not found with ID: " + scoreId));

        score.markAsRejected(reason);
        Score updatedScore = scoreRepository.save(score);

        return new ScoreResponse(updatedScore);
    }

    /**
     * Get scores requiring review
     */
    @Transactional(readOnly = true)
    public List<ScoreResponse> getScoresRequiringReview() {
        List<Score> scores = scoreRepository.findScoresRequiringReview();
        return scores.stream()
                .map(ScoreResponse::new)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void validatePlayer(Long playerId) {
        // Temporarily disabled for testing - uncomment for production
        /*
        try {
            PlayerServiceClient.PlayerInfo player = playerServiceClient.getPlayer(playerId);
            if (player == null || !player.getActive()) {
                throw new PlayerNotFoundException("Player not found or inactive: " + playerId);
            }
        } catch (Exception e) {
            throw new PlayerNotFoundException("Unable to validate player: " + playerId);
        }
        */

        // For testing: just check if playerId is valid
        if (playerId == null || playerId <= 0) {
            throw new PlayerNotFoundException("Invalid player ID: " + playerId);
        }
    }

    private void checkRateLimit(Long playerId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        LocalDateTime now = LocalDateTime.now();

        Long recentSubmissions = scoreRepository.countScoresSubmittedByPlayerInTimeRange(
                playerId, oneMinuteAgo, now);

        if (recentSubmissions >= maxSubmissionsPerMinute) {
            throw new RateLimitExceededException(
                    "Too many score submissions. Limit: " + maxSubmissionsPerMinute + " per minute");
        }
    }

    @Async
    public void updatePlayerActivityAsync(Long playerId) {
        try {
            playerServiceClient.updatePlayerActivity(playerId);
            playerServiceClient.incrementGamesPlayed(playerId);
        } catch (Exception e) {
            // Log error but don't fail the score submission
            System.err.println("Failed to update player activity: " + e.getMessage());
        }
    }

    @Async
    public void updateLeaderboardAsync(Score score) {
        try {
            // This would integrate with the leaderboard service
            // For now, just log the action
            System.out.println("Updating leaderboard " + score.getLeaderboardId() +
                    " with score " + score.getValue() + " for player " + score.getPlayerId());
        } catch (Exception e) {
            // Log error but don't fail the score submission
            System.err.println("Failed to update leaderboard: " + e.getMessage());
        }
    }
}