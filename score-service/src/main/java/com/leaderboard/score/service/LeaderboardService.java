package com.leaderboard.score.service;

import com.leaderboard.score.entity.Score;
import com.leaderboard.score.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private ScoreRepository scoreRepository;

    /**
     * Get leaderboard with time-based filtering and caching
     */
    @Cacheable(value = "leaderboards", key = "#leaderboardId + '_' + #limit")
    public List<Map<String, Object>> getLeaderboard(String leaderboardId, int limit) {
        LocalDateTime cutoffTime = getCutoffTime(leaderboardId);

        List<Score> scores;
        if (cutoffTime != null) {
            // Time-based leaderboard
            scores = scoreRepository.findTopScoresAfterTime(leaderboardId, cutoffTime, PageRequest.of(0, limit));
        } else {
            // All-time leaderboard
            scores = scoreRepository.findTopScoresByLeaderboard(leaderboardId, PageRequest.of(0, limit));
        }

        return scores.stream()
                .map(this::enrichScoreWithPlayerInfo)
                .collect(Collectors.toList());
    }

    /**
     * Get player's rank in specific leaderboard
     */
    public Map<String, Object> getPlayerRank(Long playerId, String leaderboardId) {
        LocalDateTime cutoffTime = getCutoffTime(leaderboardId);

        List<Score> scores;
        if (cutoffTime != null) {
            scores = scoreRepository.findTopScoresAfterTime(leaderboardId, cutoffTime, Pageable.unpaged());
        } else {
            scores = scoreRepository.findTopScoresByLeaderboard(leaderboardId, Pageable.unpaged());
        }

        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).getPlayerId().equals(playerId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("rank", i + 1);
                result.put("totalPlayers", scores.size());
                result.put("score", scores.get(i));
                result.put("percentile", Math.round((double)(scores.size() - i) / scores.size() * 100));
                return result;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rank", -1);
        result.put("message", "Player not found in leaderboard");
        return result;
    }

    /**
     * Get leaderboard with surrounding players (context)
     */
    public Map<String, Object> getLeaderboardContext(Long playerId, String leaderboardId, int contextSize) {
        Map<String, Object> playerRank = getPlayerRank(playerId, leaderboardId);

        if ((Integer) playerRank.get("rank") == -1) {
            return playerRank;
        }

        int rank = (Integer) playerRank.get("rank");
        int start = Math.max(1, rank - contextSize);
        int end = rank + contextSize;

        List<Map<String, Object>> leaderboard = getLeaderboard(leaderboardId, end);
        List<Map<String, Object>> context = leaderboard.subList(Math.max(0, start - 1), Math.min(leaderboard.size(), end));

        Map<String, Object> result = new HashMap<>();
        result.put("playerRank", playerRank);
        result.put("context", context);
        result.put("contextStart", start);
        result.put("contextEnd", Math.min(end, leaderboard.size()));

        return result;
    }

    /**
     * Get multiple leaderboards at once
     */
    public Map<String, List<Map<String, Object>>> getMultipleLeaderboards(List<String> leaderboardIds, int limit) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (String leaderboardId : leaderboardIds) {
            result.put(leaderboardId, getLeaderboard(leaderboardId, limit));
        }

        return result;
    }

    /**
     * Get leaderboard statistics
     */
    public Map<String, Object> getLeaderboardStats(String leaderboardId) {
        LocalDateTime cutoffTime = getCutoffTime(leaderboardId);

        List<Score> allScores;
        if (cutoffTime != null) {
            allScores = scoreRepository.findByLeaderboardIdAndSubmittedAtAfter(leaderboardId, cutoffTime);
        } else {
            allScores = scoreRepository.findByLeaderboardId(leaderboardId);
        }

        if (allScores.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "No scores found");
            return result;
        }

        LongSummaryStatistics stats = allScores.stream()
                .mapToLong(Score::getValue)
                .summaryStatistics();

        Map<String, Object> result = new HashMap<>();
        result.put("totalScores", allScores.size());
        result.put("uniquePlayers", allScores.stream().map(Score::getPlayerId).distinct().count());
        result.put("highestScore", stats.getMax());
        result.put("lowestScore", stats.getMin());
        result.put("averageScore", Math.round(stats.getAverage()));
        result.put("totalPoints", stats.getSum());
        result.put("leaderboardType", leaderboardId);

        if (cutoffTime != null) {
            result.put("timeRange", "Since " + cutoffTime.toString());
        } else {
            result.put("timeRange", "All time");
        }

        return result;
    }

    /**
     * Get trending players (most improved scores recently)
     */
    public List<Map<String, Object>> getTrendingPlayers(String leaderboardId, int limit) {
        LocalDateTime recentTime = LocalDateTime.now().minus(7, ChronoUnit.DAYS);

        List<Score> recentScores = scoreRepository.findByLeaderboardIdAndSubmittedAtAfter(leaderboardId, recentTime);

        Map<Long, List<Score>> playerScores = recentScores.stream()
                .collect(Collectors.groupingBy(Score::getPlayerId));

        return playerScores.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> {
                    List<Score> scores = entry.getValue();
                    scores.sort(Comparator.comparing(Score::getSubmittedAt));

                    long improvement = scores.get(scores.size() - 1).getValue() - scores.get(0).getValue();

                    Map<String, Object> trending = new HashMap<>();
                    trending.put("playerId", entry.getKey());
                    trending.put("improvement", improvement);
                    trending.put("recentScores", scores.size());
                    trending.put("latestScore", scores.get(scores.size() - 1).getValue());

                    return trending;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("improvement"), (Long) a.get("improvement")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private LocalDateTime getCutoffTime(String leaderboardId) {
        LocalDateTime now = LocalDateTime.now();

        return switch (leaderboardId.toLowerCase()) {
            case "daily" -> now.minus(1, ChronoUnit.DAYS);
            case "weekly" -> now.minus(7, ChronoUnit.DAYS);
            case "monthly" -> now.minus(30, ChronoUnit.DAYS);
            case "seasonal" -> now.minus(90, ChronoUnit.DAYS);
            default -> null; // All-time for "global" and others
        };
    }

    private Map<String, Object> enrichScoreWithPlayerInfo(Score score) {
        Map<String, Object> enriched = new HashMap<>();
        enriched.put("playerId", score.getPlayerId());
        enriched.put("value", score.getValue());
        enriched.put("type", score.getType());
        enriched.put("submittedAt", score.getSubmittedAt());
        enriched.put("status", score.getStatus());
        enriched.put("platform", score.getPlatform());

        // You could fetch player details here via Feign client
        // For now, just include basic info
        enriched.put("playerName", "Player " + score.getPlayerId());

        return enriched;
    }
}