package com.example.workhub.service;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.workhub.exception.TicketNotFoundException;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.User;

/** Central authorization checks. A logged-in session is not sufficient for ticket access. */
@Service
public class TicketAccessService {
    private final UserMapper users;
    private final TicketMapper tickets;

    public TicketAccessService(UserMapper users, TicketMapper tickets) {
        this.users = users;
        this.tickets = tickets;
    }

    public User currentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        User user = users.findByUsername(principal.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
        }
        return user;
    }

    public boolean isManager(User user) {
        return "MANAGER".equals(user.getRole());
    }

    public Ticket visibleTicket(int id, Principal principal) {
        Ticket ticket = tickets.findById(id);
        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }
        requireVisible(ticket, currentUser(principal));
        return ticket;
    }

    public void requireVisible(Ticket ticket, User user) {
        if (!isManager(user)
                && ticket.getCreatedBy() != user.getId()
                && !java.util.Objects.equals(ticket.getAssignedTo(), user.getId())) {
            throw forbidden();
        }
    }

    public void requireManager(Principal principal) {
        if (!isManager(currentUser(principal))) {
            throw forbidden();
        }
    }

    public void requireCreatorOrManager(int id, Principal principal) {
        Ticket ticket = visibleTicket(id, principal);
        User user = currentUser(principal);
        if (!isManager(user) && ticket.getCreatedBy() != user.getId()) {
            throw forbidden();
        }
    }

    public void requireAssigneeOrManager(int id, Principal principal) {
        Ticket ticket = visibleTicket(id, principal);
        User user = currentUser(principal);
        if (!isManager(user) && !java.util.Objects.equals(ticket.getAssignedTo(), user.getId())) {
            throw forbidden();
        }
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Ticket access denied");
    }
}
