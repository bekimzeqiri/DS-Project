package com.leaderboard.score.service;

import com.leaderboard.score.entity.Score;
import com.leaderboard.score.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Anti-Cheat Service
 *
 * Implements various anti-cheat detection algorithms to identify
 * potentially fraudulent or suspicious scores.
 */
@Service
public class AntiCheatService {

    @Autowired
    private ScoreRepository scoreRepository;

    @Value("${app.score.max-score-increase-factor:5.0}")
    private Double maxScoreIncreaseFactor;

    /**
     * Validate a score for potential cheating
     */
    public void validateScore(Score score) {
        List<String> suspicionReasons = new ArrayList<>();
        double suspicionScore = 0.0;

        // Check 1: Unrealistic score improvement
        suspicionScore += checkScoreImprovement(score, suspicionReasons);

        // Check 2: Unrealistic gameplay duration
        suspicionScore += checkGameplayDuration(score, suspicionReasons);

        // Check 3: Statistical outlier detection
        suspicionScore += checkStatisticalOutlier(score, suspicionReasons);

        // Check 4: Rapid successive submissions
        suspicionScore += checkRapidSubmissions(score, suspicionReasons);

        // Check 5: Session consistency
        suspicionScore += checkSessionConsistency(score, suspicionReasons);

        // Apply suspicion score and determine status
        if (suspicionScore > 0.8) {
            score.markAsSuspicious(suspicionScore, String.join(", ", suspicionReasons));
        } else if (suspicionScore > 0.4) {
            score.setStatus(Score.ScoreStatus.UNDER_REVIEW);
            score.setSuspicious(true);
            score.setSuspicionScore(suspicionScore);
            score.setSuspicionReasons(String.join(", ", suspicionReasons));
        } else {
            score.setStatus(Score.ScoreStatus.VERIFIED);
        }
    }

    /**
     * Check for unrealistic score improvement
     */
    private double checkScoreImprovement(Score score, List<String> reasons) {
        Optional<Score> bestScore = scoreRepository.findBestScoreForPlayerInLeaderboard(
                score.getPlayerId(), score.getLeaderboardId());

        if (bestScore.isPresent()) {
            Score previousBest = bestScore.get();
            double improvementFactor;

            if (score.getType() == Score.ScoreType.TIME_MS) {
                // For time scores, lower is better
                if (score.getValue() < previousBest.getValue()) {
                    improvementFactor = (double) previousBest.getValue() / score.getValue();
                } else {
                    return 0.0; // No improvement
                }
            } else {
                // For points/distance, higher is better
                if (score.getValue() > previousBest.getValue()) {
                    improvementFactor = (double) score.getValue() / previousBest.getValue();
                } else {
                    return 0.0; // No improvement
                }
            }

            if (improvementFactor > maxScoreIncreaseFactor) {
                reasons.add("Unrealistic improvement factor: " + String.format("%.2f", improvementFactor));
                return Math.min(0.5, (improvementFactor - maxScoreIncreaseFactor) / maxScoreIncreaseFactor);
            }
        }

        return 0.0;
    }

    /**
     * Check for unrealistic gameplay duration
     */
    private double checkGameplayDuration(Score score, List<String> reasons) {
        if (score.getGameplayDurationMs() == null) {
            return 0.0;
        }

        long durationSeconds = score.getGameplayDurationMs() / 1000;

        // Check for suspiciously short gameplay
        if (durationSeconds < 10) {
            reasons.add("Suspiciously short gameplay duration: " + durationSeconds + "s");
            return 0.3;
        }

        // Check for unrealistic score-to-time ratio
        if (score.getType() != Score.ScoreType.TIME_MS) {
            double scorePerSecond = (double) score.getValue() / durationSeconds;

            // This is a simplified check - in practice, you'd have game-specific thresholds
            if (scorePerSecond > 1000) { // Example threshold
                reasons.add("Unrealistic score per second: " + String.format("%.2f", scorePerSecond));
                return 0.4;
            }
        }

        return 0.0;
    }

    /**
     * Check if score is a statistical outlier
     */
    private double checkStatisticalOutlier(Score score, List<String> reasons) {
        List<Score> potentialOutliers = scoreRepository.findPotentialOutliers(
                score.getLeaderboardId(), 3.0); // 3x average

        boolean isOutlier = potentialOutliers.stream()
                .anyMatch(s -> s.getValue().equals(score.getValue()));

        if (isOutlier) {
            reasons.add("Statistical outlier detected");
            return 0.3;
        }

        return 0.0;
    }

    /**
     * Check for rapid successive submissions
     */
    private double checkRapidSubmissions(Score score, List<String> reasons) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Score> recentScores = scoreRepository.findRecentScoresForPlayer(
                score.getPlayerId(), fiveMinutesAgo);

        if (recentScores.size() > 5) {
            reasons.add("Too many recent submissions: " + recentScores.size() + " in 5 minutes");
            return 0.2;
        }

        // Check for identical scores in rapid succession
        long identicalScores = recentScores.stream()
                .filter(s -> s.getValue().equals(score.getValue()) &&
                        s.getLeaderboardId().equals(score.getLeaderboardId()))
                .count();

        if (identicalScores > 1) {
            reasons.add("Identical scores submitted rapidly");
            return 0.25;
        }

        return 0.0;
    }

    /**
     * Check session consistency
     */
    private double checkSessionConsistency(Score score, List<String> reasons) {
        if (score.getSessionId() == null) {
            return 0.0;
        }

        List<Score> sessionScores = scoreRepository.findBySessionIdOrderBySubmittedAtDesc(score.getSessionId());

        if (sessionScores.size() > 1) {
            // Check for inconsistent improvements within the same session
            boolean hasInconsistentProgress = false;

            for (int i = 1; i < sessionScores.size(); i++) {
                Score current = sessionScores.get(i - 1);
                Score previous = sessionScores.get(i);

                // Check if there's a sudden dramatic improvement
                if (score.getType() == Score.ScoreType.TIME_MS) {
                    if (previous.getValue() > 0 && current.getValue() < previous.getValue() * 0.5) {
                        hasInconsistentProgress = true;
                        break;
                    }
                } else {
                    if (previous.getValue() > 0 && current.getValue() > previous.getValue() * 2) {
                        hasInconsistentProgress = true;
                        break;
                    }
                }
            }

            if (hasInconsistentProgress) {
                reasons.add("Inconsistent progress within session");
                return 0.3;
            }
        }

        return 0.0;
    }

    /**
     * Get suspicion score explanation
     */
    public String getSuspicionExplanation(double suspicionScore) {
        if (suspicionScore > 0.8) {
            return "High suspicion - likely cheating";
        } else if (suspicionScore > 0.6) {
            return "Medium-high suspicion - requires review";
        } else if (suspicionScore > 0.4) {
            return "Medium suspicion - flagged for review";
        } else if (suspicionScore > 0.2) {
            return "Low suspicion - monitored";
        } else {
            return "No suspicion detected";
        }
    }

    /**
     * Batch analyze scores for patterns
     */
    public List<Score> findSuspiciousScores(String leaderboardId, int limit) {
        return scoreRepository.findPotentialOutliers(leaderboardId, 2.5)
                .stream()
                .limit(limit)
                .toList();
    }
}