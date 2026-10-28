package com.example.workhub.sla;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.notification.NotificationService;

/** Separate @Transactional bean avoids self-invocation bypass of Spring AOP proxy. */
@Service
public class SlaEscalationWorker {
    private final SlaMapper sla;
    private final TicketMapper tickets;
    private final UserMapper users;
    private final NotificationService notifications;
    public SlaEscalationWorker(SlaMapper sla, TicketMapper tickets, UserMapper users,
                               NotificationService notifications) {
        this.sla = sla; this.tickets = tickets; this.users = users; this.notifications = notifications;
    }
    @Transactional
    public void escalate(int ticketId) {
        if (sla.markEscalated(ticketId) != 1) return;
        Ticket ticket = tickets.findById(ticketId);
        if (ticket == null) return;
        for (int managerId : users.findManagerIds()) {
            notifications.queueSlaEscalation(ticketId, managerId);
        }
    }
}
