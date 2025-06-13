package com.leaderboard.score.repository;

import com.leaderboard.score.entity.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Complete Score Repository
 * Data access layer for Score entities with comprehensive query methods.
 */
@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    // ===== BASIC SCORE QUERIES =====

    /**
     * Find scores by player ID
     */
    List<Score> findByPlayerIdOrderBySubmittedAtDesc(Long playerId);

    /**
     * Find scores by leaderboard ID
     */
    List<Score> findByLeaderboardIdOrderByValueDesc(String leaderboardId);

    /**
     * Find scores by leaderboard ID with pagination
     */
    Page<Score> findByLeaderboardIdOrderByValueDesc(String leaderboardId, Pageable pageable);

    /**
     * Find player scores in specific leaderboard
     */
    List<Score> findByPlayerIdAndLeaderboardIdOrderBySubmittedAtDesc(Long playerId, String leaderboardId);

    /**
     * Find scores by status
     */
    List<Score> findByStatusOrderBySubmittedAtDesc(Score.ScoreStatus status);

    // ===== LEADERBOARD QUERIES =====

    /**
     * Get top scores for leaderboard (all-time)
     */
    @Query("SELECT s FROM Score s WHERE s.leaderboardId = :leaderboardId " +
            "AND s.status = 'VERIFIED' " +
            "ORDER BY CASE WHEN s.type = 'TIME' THEN s.value END ASC, " +
            "CASE WHEN s.type != 'TIME' THEN s.value END DESC")
    List<Score> findTopScoresByLeaderboard(@Param("leaderboardId") String leaderboardId, Pageable pageable);

    /**
     * Get top scores for leaderboard after specific time
     */
    @Query("SELECT s FROM Score s WHERE s.leaderboardId = :leaderboardId " +
            "AND s.submittedAt >= :cutoffTime " +
            "AND s.status = 'VERIFIED' " +
            "ORDER BY CASE WHEN s.type = 'TIME' THEN s.value END ASC, " +
            "CASE WHEN s.type != 'TIME' THEN s.value END DESC")
    List<Score> findTopScoresAfterTime(@Param("leaderboardId") String leaderboardId,
                                       @Param("cutoffTime") LocalDateTime cutoffTime,
                                       Pageable pageable);

    /**
     * Get scores by leaderboard and time range
     */
    List<Score> findByLeaderboardIdAndSubmittedAtAfter(String leaderboardId, LocalDateTime cutoffTime);

    // ===== PLAYER RANKING QUERIES =====

    /**
     * Find player's best score for a leaderboard
     */
    @Query("SELECT s FROM Score s WHERE s.playerId = :playerId AND s.leaderboardId = :leaderboardId " +
            "AND s.status = 'VERIFIED' ORDER BY " +
            "CASE WHEN s.type = 'TIME' THEN s.value END ASC, " +
            "CASE WHEN s.type != 'TIME' THEN s.value END DESC " +
            "LIMIT 1")
    Optional<Score> findBestScoreForPlayerInLeaderboard(@Param("playerId") Long playerId,
                                                        @Param("leaderboardId") String leaderboardId);

    /**
     * Find player's rank in leaderboard
     */
    @Query("SELECT COUNT(DISTINCT s2.playerId) + 1 FROM Score s1, Score s2 " +
            "WHERE s1.playerId = :playerId AND s1.leaderboardId = :leaderboardId AND s1.status = 'VERIFIED' " +
            "AND s2.leaderboardId = :leaderboardId AND s2.status = 'VERIFIED' " +
            "AND ((s1.type = 'TIME' AND s2.value < s1.value) OR " +
            "(s1.type != 'TIME' AND s2.value > s1.value))")
    Optional<Long> findPlayerRankInLeaderboard(@Param("playerId") Long playerId,
                                               @Param("leaderboardId") String leaderboardId);

    /**
     * Get player scores with context (surrounding players)
     */
    @Query("SELECT s FROM Score s WHERE s.playerId = :playerId " +
            "AND s.leaderboardId = :leaderboardId " +
            "AND s.status = 'VERIFIED' " +
            "ORDER BY CASE WHEN s.type = 'TIME' THEN s.value END ASC, " +
            "CASE WHEN s.type != 'TIME' THEN s.value END DESC")
    List<Score> findPlayerBestScores(@Param("playerId") Long playerId,
                                     @Param("leaderboardId") String leaderboardId,
                                     Pageable pageable);

    // ===== STATISTICS QUERIES =====

    /**
     * Count unique players in leaderboard
     */
    @Query("SELECT COUNT(DISTINCT s.playerId) FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Long countUniquePlayersByLeaderboard(@Param("leaderboardId") String leaderboardId);

    /**
     * Find max score in leaderboard
     */
    @Query("SELECT MAX(s.value) FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Long findMaxScoreByLeaderboard(@Param("leaderboardId") String leaderboardId);

    /**
     * Find min score in leaderboard
     */
    @Query("SELECT MIN(s.value) FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Long findMinScoreByLeaderboard(@Param("leaderboardId") String leaderboardId);

    /**
     * Find average score in leaderboard
     */
    @Query("SELECT AVG(s.value) FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Double findAvgScoreByLeaderboard(@Param("leaderboardId") String leaderboardId);

    /**
     * Count total scores in leaderboard
     */
    @Query("SELECT COUNT(s) FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Long countVerifiedScoresByLeaderboard(@Param("leaderboardId") String leaderboardId);

    /**
     * Find average score for player in leaderboard
     */
    @Query("SELECT AVG(s.value) FROM Score s WHERE s.playerId = :playerId AND s.leaderboardId = :leaderboardId AND s.status = 'VERIFIED'")
    Optional<Double> findAverageScoreForPlayerInLeaderboard(@Param("playerId") Long playerId,
                                                            @Param("leaderboardId") String leaderboardId);

    // ===== TIME-BASED QUERIES =====

    /**
     * Find recent scores for player
     */
    @Query("SELECT s FROM Score s WHERE s.playerId = :playerId AND s.submittedAt > :since ORDER BY s.submittedAt DESC")
    List<Score> findRecentScoresForPlayer(@Param("playerId") Long playerId, @Param("since") LocalDateTime since);

    /**
     * Find recent scores for leaderboard
     */
    @Query("SELECT s FROM Score s WHERE s.leaderboardId = :leaderboardId AND s.submittedAt > :since " +
            "AND s.status = 'VERIFIED' ORDER BY s.submittedAt DESC")
    List<Score> findRecentScoresForLeaderboard(@Param("leaderboardId") String leaderboardId,
                                               @Param("since") LocalDateTime since);

    /**
     * Find scores in time range
     */
    List<Score> findBySubmittedAtBetweenOrderBySubmittedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find recent scores (general)
     */
    @Query("SELECT s FROM Score s WHERE s.submittedAt >= :cutoffTime ORDER BY s.submittedAt DESC")
    List<Score> findRecentScores(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);

    // ===== ANTI-CHEAT & MODERATION QUERIES =====

    /**
     * Find suspicious scores
     */
    List<Score> findBySuspiciousTrueOrderBySubmittedAtDesc();

    /**
     * Find scores requiring review
     */
    @Query("SELECT s FROM Score s WHERE s.status IN ('PENDING', 'UNDER_REVIEW') ORDER BY s.submittedAt ASC")
    List<Score> findScoresRequiringReview();

    /**
     * Find potential outliers for anti-cheat
     */
    @Query("SELECT s FROM Score s WHERE s.leaderboardId = :leaderboardId " +
            "AND s.value > (SELECT AVG(s2.value) * :multiplier FROM Score s2 WHERE s2.leaderboardId = :leaderboardId AND s2.status = 'VERIFIED') " +
            "AND s.status = 'PENDING' ORDER BY s.value DESC")
    List<Score> findPotentialOutliers(@Param("leaderboardId") String leaderboardId, @Param("multiplier") Double multiplier);

    /**
     * Count scores submitted by player in time range (for rate limiting)
     */
    @Query("SELECT COUNT(s) FROM Score s WHERE s.playerId = :playerId AND s.submittedAt BETWEEN :startTime AND :endTime")
    Long countScoresSubmittedByPlayerInTimeRange(@Param("playerId") Long playerId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    // ===== SESSION & TRACKING QUERIES =====

    /**
     * Find scores by session ID
     */
    List<Score> findBySessionIdOrderBySubmittedAtDesc(String sessionId);

    /**
     * Find scores by player and status
     */
    List<Score> findByPlayerIdAndStatusOrderBySubmittedAtDesc(Long playerId, Score.ScoreStatus status);

    /**
     * Count scores by leaderboard and status
     */
    Long countByLeaderboardIdAndStatus(String leaderboardId, Score.ScoreStatus status);

    /**
     * Count scores by player and status
     */
    Long countByPlayerIdAndStatus(Long playerId, Score.ScoreStatus status);

    // ===== TRENDING & IMPROVEMENT QUERIES =====

    /**
     * Find player scores for improvement analysis
     */
    List<Score> findByPlayerIdAndLeaderboardIdAndSubmittedAtAfter(Long playerId, String leaderboardId, LocalDateTime cutoffTime);

    /**
     * Get leaderboard statistics summary
     */
    @Query("SELECT s.leaderboardId, COUNT(s), AVG(s.value), MIN(s.value), MAX(s.value) " +
            "FROM Score s WHERE s.status = 'VERIFIED' GROUP BY s.leaderboardId")
    List<Object[]> findLeaderboardStatistics();

    // ===== MAINTENANCE QUERIES =====

    /**
     * Delete old scores (for cleanup)
     */
    void deleteBySubmittedAtBefore(LocalDateTime cutoffTime);

    /**
     * Find all distinct leaderboard IDs
     */
    @Query("SELECT DISTINCT s.leaderboardId FROM Score s")
    List<String> findAllLeaderboardIds();

    /**
     * Find all distinct player IDs in leaderboard
     */
    @Query("SELECT DISTINCT s.playerId FROM Score s WHERE s.leaderboardId = :leaderboardId")
    List<Long> findAllPlayerIdsInLeaderboard(@Param("leaderboardId") String leaderboardId);

    List<Score> findByLeaderboardId(String leaderboardId);

    @Query("SELECT s FROM Score s WHERE s.leaderboardId = :leaderboardId " +
            "AND s.status = 'VERIFIED' " +
            "ORDER BY CASE WHEN s.type = 'TIME' THEN s.value END ASC, " +
            "CASE WHEN s.type != 'TIME' THEN s.value END DESC")
    Page<Score> findTopScoresByItsLeaderboard(@Param("leaderboardId") String leaderboardId, Pageable pageable);

    Page<Score> findByLeaderboardIdAndTypeOrderByValueAsc(String leaderboardId, Score.ScoreType type, Pageable pageable);

    Page<Score> findByLeaderboardIdAndTypeOrderByValueDesc(String leaderboardId, Score.ScoreType type, Pageable pageable);
}