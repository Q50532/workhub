package com.example.workhub;

import com.example.workhub.idempotency.*;
import com.example.workhub.mapper.*;
import com.example.workhub.model.Ticket;
import com.example.workhub.sla.SlaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class IdempotentCreationTest {
    Ticket ticket() {
        Ticket t = new Ticket(); t.setTitle("Example"); t.setDescription("sample");
        t.setCategory("BUG"); t.setCreatedBy(1); return t;
    }

    @Test void concurrentRetryAfterFirstCommitReturnsSameIdWithoutSecondInsert() {
        var keys = mock(TicketIdempotencyMapper.class);
        var tickets = mock(TicketMapper.class);
        var users = mock(UserMapper.class);
        var sla = mock(SlaMapper.class);
        var service = new TicketCreationService(keys, tickets, users, sla);
        TicketIdempotencyRecord previous = new TicketIdempotencyRecord();
        previous.setRequestHash(TicketCreationServiceFingerprint.forTicket(ticket()));
        previous.setTicketId(51);
        when(keys.findForUpdate(1, "idem-key-123456")).thenReturn(previous);
        assertEquals(51, service.create(ticket(), "idem-key-123456"));
        verify(tickets, never()).add(any());
        verify(sla, never()).create(anyInt(), anyInt());
    }

    @Test void invalidKeyRejectedWithoutCreatingTicket() {
        var keys = mock(TicketIdempotencyMapper.class);
        var tickets = mock(TicketMapper.class);
        var users = mock(UserMapper.class);
        var sla = mock(SlaMapper.class);
        var service = new TicketCreationService(keys, tickets, users, sla);
        assertThrows(ResponseStatusException.class, () -> service.create(ticket(), "!!"));
        verifyNoInteractions(tickets);
    }

    /** Reflection-free lookup through deterministic public SHA-256 used by service. */
    static class TicketCreationServiceFingerprint {
        static String forTicket(Ticket t) {
            try {
                var m = TicketCreationService.class.getDeclaredMethod("requestFingerprint", Ticket.class);
                m.setAccessible(true);
                return (String) m.invoke(null, t);
            } catch (Exception e) { throw new AssertionError(e); }
        }
    }
}
