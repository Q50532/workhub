package com.example.workhub.exception;

/** An update was based on a stale version of the ticket. */
public class TicketConflictException extends RuntimeException {
    public TicketConflictException(String message) { super(message); }
}
