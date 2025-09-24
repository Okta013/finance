package ru.anikeeva.finance.exceptions;

public class LoginLockException extends RuntimeException {
    public LoginLockException(String message) {
        super(message);
    }
}