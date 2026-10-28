package com.example.workhub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.security.Principal;
import com.example.workhub.service.TicketAccessService;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TicketAccessServiceTest {
    private UserMapper users;
    private TicketMapper tickets;
    private TicketAccessService access;
    private final Principal alice = () -> "alice";
    private User user(int id, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername("alice");
        user.setRole(role);
        return user;
    }
    private Ticket ticket(int creator, Integer assignee) {
        Ticket t = new Ticket();
        t.setCreatedBy(creator);
        t.setAssignedTo(assignee);
        return t;
    }
    @BeforeEach void init() {
        users = mock(UserMapper.class);
        tickets = mock(TicketMapper.class);
        access = new TicketAccessService(users, tickets);
    }
    @Test void ordinaryUserCannotReadOthersTicket() {
        when(users.findByUsername("alice")).thenReturn(user(2, "USER"));
        when(tickets.findById(7)).thenReturn(ticket(1, 3));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> access.visibleTicket(7, alice));
        assertEquals(403, ex.getStatusCode().value());
    }
    @Test void assigneeCanReadTicket() {
        when(users.findByUsername("alice")).thenReturn(user(2, "USER"));
        when(tickets.findById(7)).thenReturn(ticket(1, 2));
        assertNotNull(access.visibleTicket(7, alice));
    }
    @Test void managerCanReadAnyTicket() {
        when(users.findByUsername("alice")).thenReturn(user(2, "MANAGER"));
        when(tickets.findById(7)).thenReturn(ticket(1, 3));
        assertNotNull(access.visibleTicket(7, alice));
    }
    @Test void ordinaryUserCannotAssignTickets() {
        when(users.findByUsername("alice")).thenReturn(user(2, "USER"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> access.requireManager(alice));
        assertEquals(403, ex.getStatusCode().value());
    }
}
