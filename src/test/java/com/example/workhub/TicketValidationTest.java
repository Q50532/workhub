package com.example.workhub;

import com.example.workhub.model.Ticket;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TicketValidationTest {

    private static final ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();

    private static final Validator validator =
            factory.getValidator();

    // Create a valid Ticket for testing
    private Ticket createValidTicket() {
        Ticket ticket = new Ticket();

        ticket.setTitle("Fix login bug");
        ticket.setDescription("Login is not working");
        ticket.setCategory("BACKEND");
        ticket.setCreatedBy(1);

        return ticket;
    }

    @Test
    void blankTitleShouldFail() {
        Ticket ticket = createValidTicket();
        ticket.setTitle(" ");

        assertFalse(
            validator.validateProperty(ticket, "title").isEmpty()
        );
    }

    @Test
    void longTitleShouldFail() {
        Ticket ticket = createValidTicket();
        ticket.setTitle("a".repeat(151));

        assertFalse(
            validator.validateProperty(ticket, "title").isEmpty()
        );
    }

    @Test
    void blankDescriptionShouldFail() {
        Ticket ticket = createValidTicket();
        ticket.setDescription("");

        assertFalse(
            validator.validateProperty(ticket, "description").isEmpty()
        );
    }

    @Test
    void validTicketShouldPass() {
        Ticket ticket = createValidTicket();

        assertTrue(
            validator.validate(ticket).isEmpty()
        );
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }
}