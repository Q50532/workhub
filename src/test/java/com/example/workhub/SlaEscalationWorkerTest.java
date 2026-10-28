package com.example.workhub;

import com.example.workhub.sla.*;
import com.example.workhub.mapper.*;
import com.example.workhub.model.Ticket;
import com.example.workhub.notification.NotificationService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;

class SlaEscalationWorkerTest {
    @Test void claimedOverdueTicketNotifiesManagersOnce() {
        var mapper = mock(SlaMapper.class);
        var tickets = mock(TicketMapper.class);
        var users = mock(UserMapper.class);
        var notifications = mock(NotificationService.class);
        when(mapper.markEscalated(16)).thenReturn(1, 0);
        Ticket ticket = new Ticket(); ticket.setId(16);
        when(tickets.findById(16)).thenReturn(ticket);
        when(users.findManagerIds()).thenReturn(List.of(1, 3));
        var worker = new SlaEscalationWorker(mapper, tickets, users, notifications);
        worker.escalate(16); worker.escalate(16);
        verify(notifications, times(1)).queueSlaEscalation(16, 1);
        verify(notifications, times(1)).queueSlaEscalation(16, 3);
    }
}
