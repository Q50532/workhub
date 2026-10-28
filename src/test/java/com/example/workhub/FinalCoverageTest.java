package com.example.workhub;

import com.example.workhub.service.CommentService;
import com.example.workhub.service.TicketAccessService;
import com.example.workhub.mapper.*;
import com.example.workhub.model.*;
import com.example.workhub.idempotency.*;
import com.example.workhub.notification.*;
import com.example.workhub.sla.SlaMapper;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FinalCoverageTest {
    @Test void commentAuditUsesCurrentActor() {
        CommentMapper comments = mock(CommentMapper.class);
        TicketMapper tickets = mock(TicketMapper.class);
        TicketAuditMapper audit = mock(TicketAuditMapper.class);
        CommentService service = new CommentService(comments, tickets);
        service.attachAudit(audit);
        Ticket ticket = new Ticket(); ticket.setId(16);
        when(tickets.findById(16)).thenReturn(ticket);
        when(comments.add(any())).thenReturn(1);
        Comment comment = new Comment(); comment.setTicketId(16); comment.setAuthorId(2);
        comment.setContent("work note");
        service.add(comment);
        var entry = org.mockito.ArgumentCaptor.forClass(TicketAuditLog.class);
        verify(audit).add(entry.capture());
        assertEquals("COMMENT", entry.getValue().getAction());
        assertEquals(2, entry.getValue().getActorId());
    }

    @Test void managerOnlyNotificationRetry() {
        TicketAccessService access = mock(TicketAccessService.class);
        NotificationJobMapper jobs = mock(NotificationJobMapper.class);
        NotificationService worker = mock(NotificationService.class);
        NotificationAdminController api = new NotificationAdminController(access, jobs, worker);
        Principal alice = () -> "Alice";
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(access).requireManager(alice);
        assertThrows(ResponseStatusException.class, () -> api.retry(12L, alice));
        verify(jobs, never()).requeueFailed(anyLong());
        Principal tom = () -> "Tom";
        when(jobs.requeueFailed(12L)).thenReturn(1);
        api.retry(12L, tom);
        verify(jobs).requeueFailed(12L);
        verify(worker).recoverPending();
    }

    @Test void originalRequestKeyReplayCannotCreateAnotherAudit() {
        TicketIdempotencyMapper keys = mock(TicketIdempotencyMapper.class);
        TicketMapper tickets = mock(TicketMapper.class);
        UserMapper users = mock(UserMapper.class);
        SlaMapper sla = mock(SlaMapper.class);
        TicketAuditMapper audit = mock(TicketAuditMapper.class);
        TicketCreationService service = new TicketCreationService(keys,tickets,users,sla);
        service.attachAudit(audit);
        Ticket ticket = new Ticket(); ticket.setTitle("Bug"); ticket.setDescription("Issue");
        ticket.setCategory("BACKEND"); ticket.setCreatedBy(1);
        TicketIdempotencyRecord replay = new TicketIdempotencyRecord();
        replay.setTicketId(99);
        // Hash is intentionally looked up with the existing deterministic implementation.
        try {
            var fingerprint = TicketCreationService.class.getDeclaredMethod("requestFingerprint", Ticket.class);
            fingerprint.setAccessible(true);
            replay.setRequestHash((String) fingerprint.invoke(null, ticket));
        } catch (Exception error) { throw new AssertionError(error); }
        when(keys.findForUpdate(1,"key-repeat-123456")).thenReturn(replay);
        assertEquals(99, service.create(ticket,"key-repeat-123456"));
        verifyNoInteractions(audit);
        verify(tickets, never()).add(any());
    }
}
