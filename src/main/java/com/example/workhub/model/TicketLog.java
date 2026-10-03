package com.example.workhub.model;

import java.time.LocalDateTime;



public class TicketLog {
    private int id;
    private int ticketId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime createdAt;

    public TicketLog(){

    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id=id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
