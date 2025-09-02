package ru.anikeeva.finance.exceptions;

public class BudgetLimitExceedingException extends RuntimeException {
    public BudgetLimitExceedingException(String message) {
        super(message);
    }
}