package com.leadgen.bot.exception;

public class FloodLimitException extends RuntimeException {
    public FloodLimitException(String message) {
        super(message);
    }
}
