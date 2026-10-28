package com.example.workhub.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.example.workhub.mapper.TicketAuditMapper;
import com.example.workhub.model.TicketAuditLog;
import com.example.workhub.service.TicketAccessService;

@RestController
public class TicketAuditController {
    private final TicketAuditMapper audit;
    private final TicketAccessService access;
    public TicketAuditController(TicketAuditMapper audit, TicketAccessService access) {
        this.audit = audit;
        this.access = access;
    }
    @GetMapping("/tickets/{id}/audit")
    public List<TicketAuditLog> findByTicketId(@PathVariable int id, Principal principal) {
        access.visibleTicket(id, principal);
        return audit.findByTicketId(id);
    }
}
