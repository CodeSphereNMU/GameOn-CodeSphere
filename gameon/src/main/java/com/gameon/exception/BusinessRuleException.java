package com.gameon.exception;

/**
 * Exception thrown when a business rule is violated.
 * Examples: BR1 (max 1 active listing), BR5 (sport not on profile), BR10 (time conflict).
 */
public class BusinessRuleException extends RuntimeException {

    private final String ruleCode;

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public BusinessRuleException(String message, String ruleCode) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
