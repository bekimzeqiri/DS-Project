package com.leaderboard.player.service;

import com.leaderboard.player.dto.PlayerRegistrationRequest;
import com.leaderboard.player.dto.PlayerResponse;
import com.leaderboard.player.dto.PlayerStatistics;
import com.leaderboard.player.dto.PlayerUpdateRequest;
import com.leaderboard.player.entity.Player;
import com.leaderboard.player.exception.PlayerAlreadyExistsException;
import com.leaderboard.player.exception.PlayerNotFoundException;
import com.leaderboard.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Player Service Implementation
 *
 * Handles all business logic for player operations.
 */
@Service
@Transactional
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new player
     */
    public PlayerResponse registerPlayer(PlayerRegistrationRequest request) {
        // Check if username already exists
        if (playerRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new PlayerAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (playerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new PlayerAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Create new player
        Player player = new Player(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        // Set display name (use username if not provided)
        if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
            player.setDisplayName(request.getDisplayName().trim());
        }

        // Save player
        Player savedPlayer = playerRepository.save(player);

        return new PlayerResponse(savedPlayer);
    }

    /**
     * Get player by ID
     */
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        return new PlayerResponse(player);
    }

    /**
     * Get player by username
     */
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerByUsername(String username) {
        Player player = playerRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));

        return new PlayerResponse(player);
    }

    /**
     * Update player profile
     */
    public PlayerResponse updatePlayer(Long playerId, PlayerUpdateRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        // Update fields if provided
        if (request.getDisplayName() != null) {
            player.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getBio() != null) {
            player.setBio(request.getBio().trim());
        }
        if (request.getAvatarUrl() != null) {
            player.setAvatarUrl(request.getAvatarUrl().trim());
        }

        Player updatedPlayer = playerRepository.save(player);
        return new PlayerResponse(updatedPlayer);
    }

    /**
     * Deactivate player account
     */
    public void deactivatePlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        player.setActive(false);
        player.setStatus(Player.PlayerStatus.INACTIVE);
        playerRepository.save(player);
    }

    /**
     * Get all active players with pagination
     */
    @Transactional(readOnly = true)
    public Page<PlayerResponse> getActivePlayers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Player> players = playerRepository.findActivePlayers(pageable);

        return players.map(PlayerResponse::new);
    }

    /**
     * Search players by display name
     */
    @Transactional(readOnly = true)
    public Page<PlayerResponse> searchPlayersByDisplayName(String displayName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Player> players = playerRepository.findByDisplayNameContainingIgnoreCase(displayName, pageable);

        return players.map(PlayerResponse::new);
    }

    /**
     * Search players by any field
     */
    @Transactional(readOnly = true)
    public Page<PlayerResponse> searchPlayers(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Player> players = playerRepository.searchPlayers(searchTerm, pageable);

        return players.map(PlayerResponse::new);
    }

    /**
     * Get top players by score
     */
    @Transactional(readOnly = true)
    public List<PlayerResponse> getTopPlayersByScore(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Player> players = playerRepository.findTopPlayersByScore(pageable);

        return players.getContent().stream()
                .map(PlayerResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get top players by level
     */
    @Transactional(readOnly = true)
    public List<PlayerResponse> getTopPlayersByLevel(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Player> players = playerRepository.findTopPlayersByLevel(pageable);

        return players.getContent().stream()
                .map(PlayerResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get player statistics
     */
    @Transactional(readOnly = true)
    public PlayerStatistics getPlayerStatistics(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        return new PlayerStatistics(player);
    }

    /**
     * Update player's last active time
     */
    public void updateLastActiveTime(Long playerId) {
        LocalDateTime now = LocalDateTime.now();
        playerRepository.updateLastActiveTime(playerId, now, now);
    }

    /**
     * Increment games played for a player
     */
    public void incrementGamesPlayed(Long playerId) {
        playerRepository.incrementGamesPlayed(playerId, LocalDateTime.now());
    }

    /**
     * Add score to player
     */
    public void addScoreToPlayer(Long playerId, Long score) {
        playerRepository.addScoreToPlayer(playerId, score, LocalDateTime.now());
    }

    /**
     * Add experience points to player
     */
    public void addExperienceToPlayer(Long playerId, Long experience) {
        playerRepository.addExperienceToPlayer(playerId, experience, LocalDateTime.now());

        // Update level if necessary (handled in entity)
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player != null) {
            player.addExperience(0L); // Trigger level calculation
            playerRepository.save(player);
        }
    }

    /**
     * Get recently active players
     */
    @Transactional(readOnly = true)
    public List<PlayerResponse> getRecentlyActivePlayers(int hours, int limit) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        List<Player> players = playerRepository.findPlayersActiveAfter(cutoffTime);

        return players.stream()
                .limit(limit)
                .map(PlayerResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get player count statistics
     */
    @Transactional(readOnly = true)
    public PlayerCountStatistics getPlayerCountStatistics() {
        long totalActive = playerRepository.countActivePlayers();
        long totalInactive = playerRepository.countByStatus(Player.PlayerStatus.INACTIVE);
        long totalSuspended = playerRepository.countByStatus(Player.PlayerStatus.SUSPENDED);
        long totalBanned = playerRepository.countByStatus(Player.PlayerStatus.BANNED);

        return new PlayerCountStatistics(totalActive, totalInactive, totalSuspended, totalBanned);
    }

    /**
     * Check if username is available
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !playerRepository.existsByUsernameIgnoreCase(username);
    }

    /**
     * Check if email is available
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !playerRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Player Count Statistics inner class
     */
    public static class PlayerCountStatistics {
        private final long totalActive;
        private final long totalInactive;
        private final long totalSuspended;
        private final long totalBanned;

        public PlayerCountStatistics(long totalActive, long totalInactive, long totalSuspended, long totalBanned) {
            this.totalActive = totalActive;
            this.totalInactive = totalInactive;
            this.totalSuspended = totalSuspended;
            this.totalBanned = totalBanned;
        }

        // Getters
        public long getTotalActive() { return totalActive; }
        public long getTotalInactive() { return totalInactive; }
        public long getTotalSuspended() { return totalSuspended; }
        public long getTotalBanned() { return totalBanned; }
        public long getTotal() { return totalActive + totalInactive + totalSuspended + totalBanned; }
    }
}