package com.example.workhub.sla;

import java.time.LocalDateTime;

public class SlaStatus {
    private int ticketId;
    private LocalDateTime dueAt;
    private LocalDateTime escalatedAt;
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
}
