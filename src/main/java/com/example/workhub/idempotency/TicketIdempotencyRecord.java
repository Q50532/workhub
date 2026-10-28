package com.example.workhub.idempotency;
public class TicketIdempotencyRecord {
    private int actorId;
    private String requestKey;
    private String requestHash;
    private Integer ticketId;
    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }
}
