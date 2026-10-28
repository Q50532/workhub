package com.example.workhub.notification;

import java.security.Principal;
import java.util.List;
import com.example.workhub.service.TicketAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Only Managers may inspect or requeue failed notifications. */
@RestController
public class NotificationAdminController {
    private final TicketAccessService access;
    private final NotificationJobMapper mapper;
    private final NotificationService dispatcher;

    public NotificationAdminController(TicketAccessService access, NotificationJobMapper mapper,
                                       NotificationService dispatcher) {
        this.access = access;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    @GetMapping("/api/notifications/failed")
    public List<NotificationJob> failed(@RequestParam(defaultValue="20") int limit,
                                        Principal principal) {
        access.requireManager(principal);
        if (limit < 1 || limit > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100");
        return mapper.failedJobs(limit);
    }

    @PostMapping("/api/notifications/{id}/retry")
    public void retry(@PathVariable long id, Principal principal) {
        access.requireManager(principal);
        if (id <= 0 || mapper.requeueFailed(id) != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed job not found");
        // Dispatch from the durable PENDING queue. Duplicate worker claims are atomic.
        dispatcher.recoverPending();
    }
}
