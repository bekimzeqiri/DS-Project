package com.leaderboard.player.repository;

import com.leaderboard.player.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Player Repository
 *
 * Data access layer for Player entities.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Find player by username (case-insensitive)
     */
    Optional<Player> findByUsernameIgnoreCase(String username);

    /**
     * Find player by email (case-insensitive)
     */
    Optional<Player> findByEmailIgnoreCase(String email);

    /**
     * Check if username exists (case-insensitive)
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find players by status
     */
    List<Player> findByStatus(Player.PlayerStatus status);

    /**
     * Find active players
     */
    @Query("SELECT p FROM Player p WHERE p.active = true AND p.status = 'ACTIVE'")
    Page<Player> findActivePlayers(Pageable pageable);

    /**
     * Find players by display name containing (case-insensitive)
     */
    @Query("SELECT p FROM Player p WHERE LOWER(p.displayName) LIKE LOWER(CONCAT('%', :displayName, '%')) AND p.active = true")
    Page<Player> findByDisplayNameContainingIgnoreCase(@Param("displayName") String displayName, Pageable pageable);

    /**
     * Find top players by total score
     */
    @Query("SELECT p FROM Player p WHERE p.active = true ORDER BY p.totalScore DESC")
    Page<Player> findTopPlayersByScore(Pageable pageable);

    /**
     * Find top players by level
     */
    @Query("SELECT p FROM Player p WHERE p.active = true ORDER BY p.currentLevel DESC, p.experiencePoints DESC")
    Page<Player> findTopPlayersByLevel(Pageable pageable);

    /**
     * Find players created in date range
     */
    @Query("SELECT p FROM Player p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Player> findPlayersCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find players last active after date
     */
    @Query("SELECT p FROM Player p WHERE p.lastActiveAt > :date AND p.active = true ORDER BY p.lastActiveAt DESC")
    List<Player> findPlayersActiveAfter(@Param("date") LocalDateTime date);

    /**
     * Count active players
     */
    @Query("SELECT COUNT(p) FROM Player p WHERE p.active = true AND p.status = 'ACTIVE'")
    long countActivePlayers();

    /**
     * Count players by status
     */
    long countByStatus(Player.PlayerStatus status);

    /**
     * Update player last active time
     */
    @Modifying
    @Query("UPDATE Player p SET p.lastActiveAt = :lastActiveAt, p.updatedAt = :updatedAt WHERE p.id = :playerId")
    void updateLastActiveTime(@Param("playerId") Long playerId,
                              @Param("lastActiveAt") LocalDateTime lastActiveAt,
                              @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Increment total games played
     */
    @Modifying
    @Query("UPDATE Player p SET p.totalGamesPlayed = p.totalGamesPlayed + 1, p.updatedAt = :updatedAt WHERE p.id = :playerId")
    void incrementGamesPlayed(@Param("playerId") Long playerId, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Add score to player
     */
    @Modifying
    @Query("UPDATE Player p SET p.totalScore = p.totalScore + :score, p.updatedAt = :updatedAt WHERE p.id = :playerId")
    void addScoreToPlayer(@Param("playerId") Long playerId, @Param("score") Long score, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Add experience points to player
     */
    @Modifying
    @Query("UPDATE Player p SET p.experiencePoints = p.experiencePoints + :experience, p.updatedAt = :updatedAt WHERE p.id = :playerId")
    void addExperienceToPlayer(@Param("playerId") Long playerId, @Param("experience") Long experience, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Find players with username or email containing search term
     */
    @Query("SELECT p FROM Player p WHERE " +
            "(LOWER(p.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND p.active = true")
    Page<Player> searchPlayers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find players by level range
     */
    @Query("SELECT p FROM Player p WHERE p.currentLevel BETWEEN :minLevel AND :maxLevel AND p.active = true ORDER BY p.currentLevel DESC")
    Page<Player> findPlayersByLevelRange(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel, Pageable pageable);
}