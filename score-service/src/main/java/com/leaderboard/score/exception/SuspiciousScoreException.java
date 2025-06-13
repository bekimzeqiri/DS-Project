package com.leaderboard.score.exception;

public class SuspiciousScoreException extends RuntimeException {
    private final double suspicionScore;
    private final String reasons;

    public SuspiciousScoreException(String message, double suspicionScore, String reasons) {
        super(message);
        this.suspicionScore = suspicionScore;
        this.reasons = reasons;
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    public String getReasons() {
        return reasons;
    }
}