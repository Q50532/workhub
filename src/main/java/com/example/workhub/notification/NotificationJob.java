package com.example.workhub.notification;

import java.time.LocalDateTime;

/** Durable database record for a notification being prepared asynchronously. */
public class NotificationJob {
    private Long id;
    private int ticketId;
    private int recipientId;
    private int actorId;
    private String eventType;
    private String oldValue;
    private String newValue;
    private String status;
    private String message;
    private int attempts;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
