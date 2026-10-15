package com.example.workhub.model;

import java.util.List;

public class PageResponse {

    private List<Ticket> data;
    private int total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse(
            List<Ticket> data,
            int total,
            int page,
            int size) {

        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;

        this.totalPages = (total + size - 1) / size;
    }

    public List<Ticket> getData() {
        return data;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }
}