package com.example.workhub.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException (String message){
        super(message);
    }
    
}
