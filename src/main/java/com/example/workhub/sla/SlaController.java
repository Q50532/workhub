package com.example.workhub.sla;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.workhub.service.TicketAccessService;

@RestController
public class SlaController {
    private final TicketAccessService access;
    private final SlaMapper sla;
    public SlaController(TicketAccessService access, SlaMapper sla) { this.access = access; this.sla = sla; }
    @GetMapping("/tickets/{id}/sla")
    public SlaStatus byTicket(@PathVariable int id, Principal principal) {
        access.visibleTicket(id, principal); // data-level authorization comes first
        SlaStatus result = sla.findStatus(id);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "SLA not initialized; apply the V2 Plus migration");
        return result;
    }
}
