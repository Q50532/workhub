package com.example.workhub.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.mapper.TicketAuditMapper;
import com.example.workhub.model.TicketAuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.workhub.model.Ticket;
import com.example.workhub.exception.UserNotFoundException;
import com.example.workhub.sla.SlaMapper;
import com.example.workhub.sla.SlaPolicy;

@Service
public class TicketCreationService {
    private final TicketIdempotencyMapper keys;
    private final TicketMapper tickets;
    private final UserMapper users;
    private final SlaMapper sla;
    private TicketAuditMapper audit;
    @Autowired public void attachAudit(TicketAuditMapper audit) { this.audit = audit; }
    private void auditCreation(Ticket ticket) {
        if (audit == null) return;
        TicketAuditLog entry = new TicketAuditLog();
        entry.setTicketId(ticket.getId());
        entry.setActorId(ticket.getCreatedBy());
        entry.setAction("CREATE");
        entry.setOldValue(null);
        entry.setNewValue("Ticket created");
        audit.add(entry);
    }

    public TicketCreationService(TicketIdempotencyMapper keys, TicketMapper tickets, UserMapper users,
                                 SlaMapper sla) {
        this.keys = keys; this.tickets = tickets; this.users = users; this.sla = sla;
    }

    static String requestFingerprint(Ticket ticket) {
        // Length-prefixing removes ambiguity between fields that contain delimiters.
        String payload = part(ticket.getTitle()) + part(ticket.getDescription())
            + part(ticket.getCategory()) + part(String.valueOf(ticket.getCreatedBy()))
            + part(String.valueOf(ticket.getAssignedTo()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    private static String part(String value) { return value.length() + ":" + value; }

    /** Optional header for backwards compatibility; with key, retries return same Ticket ID. */
    @Transactional
    public int create(Ticket ticket, String key) {
        if (ticket.getAssignedTo() != null && users.findById(ticket.getAssignedTo()) == null)
            throw new UserNotFoundException("Assignee does not exist");
        if (key == null || key.isBlank()) {
            tickets.add(ticket); // Legacy contract: return affected row count.
            sla.create(ticket.getId(), SlaPolicy.minutesFor("MEDIUM"));
            auditCreation(ticket);
            return 1;
        }
        if (!key.matches("[A-Za-z0-9:_-]{8,128}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        String hash = requestFingerprint(ticket);
        keys.reserve(ticket.getCreatedBy(), key, hash);
        TicketIdempotencyRecord previous = keys.findForUpdate(ticket.getCreatedBy(), key);
        if (previous == null) throw new IllegalStateException("Cannot reserve idempotency key");
        if (!Objects.equals(previous.getRequestHash(), hash))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key reused with different request");
        if (previous.getTicketId() != null) return previous.getTicketId();
        tickets.add(ticket);
        sla.create(ticket.getId(), SlaPolicy.minutesFor("MEDIUM"));
        if (ticket.getId() <= 0) throw new IllegalStateException("Ticket ID not generated");
        auditCreation(ticket);
        if (keys.complete(ticket.getCreatedBy(), key, ticket.getId()) != 1)
            throw new IllegalStateException("Cannot complete idempotency reservation");
        return ticket.getId();
    }
}
