package com.leaderboard.player.exception;

/**
 * Exception thrown when a player is not found
 */
public class PlayerAlreadyExistsException extends RuntimeException {
    public PlayerAlreadyExistsException(String message) {
        super(message);
    }

    public PlayerAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
/**
 * Exception thrown when a player operation is invalid
 */
class InvalidPlayerOperationException extends RuntimeException {
    public InvalidPlayerOperationException(String message) {
        super(message);
    }

    public InvalidPlayerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}