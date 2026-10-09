package com.example.workhub.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketUpdateRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "description must not be blank")
    private String description;

    @NotBlank(message = "category must not be blank")
    @Size(max = 50, message = "category must be at most 50 characters")
    private String category;

    private String resolution;

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

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}