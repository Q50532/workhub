package com.example.workhub.model;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class Ticket {

    private int id;
    @NotBlank(message="title must not be blank")
    @Size(max=150,message="title must be at most 150 characters")
    private String title;
    @NotBlank(message = "description must not be blank")
    private String description;
    @NotBlank(message = "category must not be blank")
    @Size(max=50, message = "category must be at most 50 characters")
    private String category;

    
    private int createdBy;
    private Integer assignedTo;

    private LocalDateTime createdAt;
    private String status;
    private String resolution;
    private String priority;

    private boolean deleted;
    private int version;

    public Ticket() {
    }

    // public Ticket(
    //         int id,
    //         String title,
    //         String description,
    //         String category,
    //         int createdBy,
    //         Integer assignedTo,
    //         LocalDateTime createdAt,
    //         String status,
    //         String resolution) {

    //     this.id = id;
    //     this.title = title;
    //     this.description = description;
    //     this.category = category;
    //     this.createdBy = createdBy;
    //     this.assignedTo = assignedTo;
    //     this.createdAt = createdAt;
    //     this.status = status;
    //     this.resolution = resolution;
    // }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getPriority(){
        return priority;
    }

    public void setPriority(String priority){
        this.priority=priority;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isDeleted(){
        return deleted;
    }

    public void setDeleted(boolean deleted){
        this.deleted=deleted;
    }
}