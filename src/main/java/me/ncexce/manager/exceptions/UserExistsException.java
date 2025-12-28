package me.ncexce.manager.exceptions;

public class UserExistsException extends RuntimeException{
    public UserExistsException(String message) { super(message); }
}
