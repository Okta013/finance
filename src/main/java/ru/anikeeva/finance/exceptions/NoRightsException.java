package ru.anikeeva.finance.exceptions;

public class NoRightsException extends RuntimeException {
    public NoRightsException(String message) {
        super(message);
    }
}