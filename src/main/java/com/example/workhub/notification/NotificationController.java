package com.example.workhub.notification;

import java.security.Principal;
import java.util.List;
import com.example.workhub.service.TicketAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NotificationController {
    private final TicketAccessService access;
    private final NotificationJobMapper mapper;

    public NotificationController(TicketAccessService access, NotificationJobMapper mapper) {
        this.access = access;
        this.mapper = mapper;
    }

    /** User-scoped inbox. Never accepts a recipientId from the HTTP request. */
    @GetMapping("/api/notifications")
    public List<NotificationJob> mine(@RequestParam(defaultValue = "20") int limit,
                                      Principal principal) {
        if (limit < 1 || limit > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100");
        return mapper.readyForRecipient(access.currentUser(principal).getId(), limit);
    }
}
