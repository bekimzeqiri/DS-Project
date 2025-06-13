package com.leaderboard.score.controller;

import com.leaderboard.score.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leaderboards")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * Get enhanced leaderboard with caching
     */
    @GetMapping("/{leaderboardId}")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard(
            @PathVariable String leaderboardId,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> leaderboard = leaderboardService.getLeaderboard(leaderboardId, limit);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get player's rank in leaderboard
     */
    @GetMapping("/{leaderboardId}/rank/{playerId}")
    public ResponseEntity<Map<String, Object>> getPlayerRank(
            @PathVariable String leaderboardId,
            @PathVariable Long playerId) {

        Map<String, Object> rank = leaderboardService.getPlayerRank(playerId, leaderboardId);
        return ResponseEntity.ok(rank);
    }

    /**
     * Get leaderboard with context around player
     */
    @GetMapping("/{leaderboardId}/context/{playerId}")
    public ResponseEntity<Map<String, Object>> getLeaderboardContext(
            @PathVariable String leaderboardId,
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "5") int contextSize) {

        Map<String, Object> context = leaderboardService.getLeaderboardContext(playerId, leaderboardId, contextSize);
        return ResponseEntity.ok(context);
    }

    /**
     * Get multiple leaderboards at once
     */
    @GetMapping("/multiple")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getMultipleLeaderboards(
            @RequestParam(defaultValue = "global,weekly,monthly") String leaderboards,
            @RequestParam(defaultValue = "5") int limit) {

        List<String> leaderboardIds = Arrays.asList(leaderboards.split(","));
        Map<String, List<Map<String, Object>>> result = leaderboardService.getMultipleLeaderboards(leaderboardIds, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * Get leaderboard statistics
     */
    @GetMapping("/{leaderboardId}/stats")
    public ResponseEntity<Map<String, Object>> getLeaderboardStats(@PathVariable String leaderboardId) {
        Map<String, Object> stats = leaderboardService.getLeaderboardStats(leaderboardId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get trending players (most improved)
     */
    @GetMapping("/{leaderboardId}/trending")
    public ResponseEntity<List<Map<String, Object>>> getTrendingPlayers(
            @PathVariable String leaderboardId,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> trending = leaderboardService.getTrendingPlayers(leaderboardId, limit);
        return ResponseEntity.ok(trending);
    }

    /**
     * Get all available leaderboard types
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getLeaderboardTypes() {
        List<Map<String, String>> types = Arrays.asList(
                Map.of("id", "global", "name", "Global", "description", "All-time best scores"),
                Map.of("id", "daily", "name", "Daily", "description", "Best scores from last 24 hours"),
                Map.of("id", "weekly", "name", "Weekly", "description", "Best scores from last 7 days"),
                Map.of("id", "monthly", "name", "Monthly", "description", "Best scores from last 30 days"),
                Map.of("id", "seasonal", "name", "Seasonal", "description", "Best scores from last 90 days")
        );
        return ResponseEntity.ok(types);
    }
}