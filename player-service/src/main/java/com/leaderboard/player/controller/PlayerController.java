package com.leaderboard.player.controller;

import com.leaderboard.player.dto.PlayerRegistrationRequest;
import com.leaderboard.player.dto.PlayerResponse;
import com.leaderboard.player.dto.PlayerStatistics;
import com.leaderboard.player.dto.PlayerUpdateRequest;
import com.leaderboard.player.exception.PlayerAlreadyExistsException;
import com.leaderboard.player.exception.PlayerNotFoundException;
import com.leaderboard.player.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player REST Controller
 *
 * Handles all HTTP requests related to player operations.
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    /**
     * Register a new player
     */
    @PostMapping("/register")
    public ResponseEntity<PlayerResponse> registerPlayer(@Valid @RequestBody PlayerRegistrationRequest request) {
        try {
            PlayerResponse response = playerService.registerPlayer(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (PlayerAlreadyExistsException e) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
    }

    /**
     * Get player by ID
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable Long playerId) {
        try {
            PlayerResponse response = playerService.getPlayerById(playerId);
            return ResponseEntity.ok(response);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get player by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<PlayerResponse> getPlayerByUsername(@PathVariable String username) {
        try {
            PlayerResponse response = playerService.getPlayerByUsername(username);
            return ResponseEntity.ok(response);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update player profile
     */
    @PutMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> updatePlayer(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerUpdateRequest request) {
        try {
            PlayerResponse response = playerService.updatePlayer(playerId, request);
            return ResponseEntity.ok(response);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate player account
     */
    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> deactivatePlayer(@PathVariable Long playerId) {
        try {
            playerService.deactivatePlayer(playerId);
            return ResponseEntity.noContent().build();
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active players with pagination
     */
    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> getActivePlayers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PlayerResponse> players = playerService.getActivePlayers(page, size);
        return ResponseEntity.ok(players);
    }

    /**
     * Search players
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PlayerResponse>> searchPlayers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PlayerResponse> players = playerService.searchPlayers(q, page, size);
        return ResponseEntity.ok(players);
    }

    /**
     * Get top players by score
     */
    @GetMapping("/top/score")
    public ResponseEntity<List<PlayerResponse>> getTopPlayersByScore(
            @RequestParam(defaultValue = "10") int limit) {
        List<PlayerResponse> players = playerService.getTopPlayersByScore(limit);
        return ResponseEntity.ok(players);
    }

    /**
     * Get top players by level
     */
    @GetMapping("/top/level")
    public ResponseEntity<List<PlayerResponse>> getTopPlayersByLevel(
            @RequestParam(defaultValue = "10") int limit) {
        List<PlayerResponse> players = playerService.getTopPlayersByLevel(limit);
        return ResponseEntity.ok(players);
    }

    /**
     * Get player statistics
     */
    @GetMapping("/{playerId}/statistics")
    public ResponseEntity<PlayerStatistics> getPlayerStatistics(@PathVariable Long playerId) {
        try {
            PlayerStatistics statistics = playerService.getPlayerStatistics(playerId);
            return ResponseEntity.ok(statistics);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update player's last active time
     */
    @PostMapping("/{playerId}/activity")
    public ResponseEntity<Void> updateLastActiveTime(@PathVariable Long playerId) {
        try {
            playerService.updateLastActiveTime(playerId);
            return ResponseEntity.ok().build();
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get recently active players
     */
    @GetMapping("/recent")
    public ResponseEntity<List<PlayerResponse>> getRecentlyActivePlayers(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "20") int limit) {
        List<PlayerResponse> players = playerService.getRecentlyActivePlayers(hours, limit);
        return ResponseEntity.ok(players);
    }

    /**
     * Get player count statistics
     */
    @GetMapping("/statistics/counts")
    public ResponseEntity<PlayerService.PlayerCountStatistics> getPlayerCountStatistics() {
        PlayerService.PlayerCountStatistics statistics = playerService.getPlayerCountStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Check username availability
     */
    @GetMapping("/check/username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean available = playerService.isUsernameAvailable(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * Check email availability
     */
    @GetMapping("/check/email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@PathVariable String email) {
        boolean available = playerService.isEmailAvailable(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "player-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePlayerNotFound(PlayerNotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Player not found");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PlayerAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handlePlayerAlreadyExists(PlayerAlreadyExistsException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Player already exists");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}