package com.example.workhub;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.workhub.mapper.*;
import com.example.workhub.model.*;
import com.example.workhub.exception.TicketConflictException;
import com.example.workhub.service.TicketService;
import tools.jackson.databind.json.JsonMapper;

class TicketOptimisticLockingTest {
    private final TicketMapper tickets = mock(TicketMapper.class);
    private final TicketLogMapper logs = mock(TicketLogMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final TicketAuditMapper audit = mock(TicketAuditMapper.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final TicketService service = new TicketService(tickets, logs, users,
            redis, mock(JsonMapper.class), audit);
    private Ticket original(int version) {
        Ticket t = new Ticket();
        t.setVersion(version);
        t.setStatus("OPEN");
        t.setPriority("MEDIUM");
        return t;
    }

    @Test void stalePageMustNotOverwriteNewerVersion() {
        when(tickets.findById(16)).thenReturn(original(8));
        assertThrows(TicketConflictException.class,
                () -> service.updatePriority(16, "HIGH", 7, 1));
        verify(tickets, never()).updatePriority(anyInt(), anyString(), anyInt());
        verify(audit, never()).add(any());
    }

    @Test void statusConflictMustNotWriteHistoryOrAudit() {
        when(tickets.findById(16)).thenReturn(original(7));
        when(tickets.updateStatus(16, "IN_PROGRESS", "OPEN", 7)).thenReturn(0);
        assertThrows(TicketConflictException.class,
                () -> service.updateStatus(16, "IN_PROGRESS", 7, 1));
        verify(logs, never()).add(any());
        verify(audit, never()).add(any());
    }

    @Test void successfulStatusUpdateRecordsActor() {
        when(tickets.findById(16)).thenReturn(original(7));
        when(tickets.updateStatus(16, "IN_PROGRESS", "OPEN", 7)).thenReturn(1);
        assertEquals(1, service.updateStatus(16, "IN_PROGRESS", 7, 1));
        verify(logs).add(any(TicketLog.class));
        verify(audit).add(argThat(event -> event.getActorId() == 1
                && "STATUS".equals(event.getAction())
                && "OPEN".equals(event.getOldValue())
                && "IN_PROGRESS".equals(event.getNewValue())));
    }
}
