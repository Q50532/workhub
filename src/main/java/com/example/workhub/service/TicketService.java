package com.example.workhub.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.example.workhub.exception.*;
import com.example.workhub.mapper.*;
import com.example.workhub.model.*;
import com.example.workhub.notification.NotificationService;
import com.example.workhub.sla.SlaMapper;
import com.example.workhub.sla.SlaPolicy;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TicketService {
    private static final String NOT_FOUND = "__WORKHUB_TICKET_NOT_FOUND__";
    // Benchmark switch: true means ticket read bypasses Redis, while authorization remains unchanged.
    private final boolean cacheDisabled = Boolean.parseBoolean(System.getenv("WORKHUB_CACHE_OFF"));
    /** Bounded stripes: avoid unbounded per-ticket lock allocation. Single-instance protection only. */
    private final Object[] cacheLocks = new Object[64];
    private final TicketMapper tickets;
    private final TicketLogMapper logs;
    private final UserMapper users;
    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final TicketAuditMapper audit;
    private final NotificationService notifications;
    private SlaMapper sla;
    @Autowired public void attachSla(SlaMapper sla) { this.sla = sla; }

    @Autowired
    public TicketService(TicketMapper tickets, TicketLogMapper logs, UserMapper users,
                         StringRedisTemplate redis, JsonMapper json, TicketAuditMapper audit,
                         NotificationService notifications) {
        this.tickets = tickets;
        this.logs = logs;
        this.users = users;
        this.redis = redis;
        this.json = json;
        this.audit = audit;
        this.notifications = notifications;
        for (int i = 0; i < cacheLocks.length; i++) cacheLocks[i] = new Object();
    }

    /** Retained for isolated unit tests; application uses the fully wired constructor above. */
    public TicketService(TicketMapper tickets, TicketLogMapper logs, UserMapper users,
                         StringRedisTemplate redis, JsonMapper json) {
        this(tickets, logs, users, redis, json, null, null);
    }

    /** Compatibility constructor used by existing unit tests. */
    public TicketService(TicketMapper tickets, TicketLogMapper logs, UserMapper users,
                         StringRedisTemplate redis, JsonMapper json, TicketAuditMapper audit) {
        this(tickets, logs, users, redis, json, audit, null);
    }

    private Object cacheLock(int id) { return cacheLocks[Math.floorMod(id, cacheLocks.length)]; }
    private String cached(String key) {
        try { return redis.opsForValue().get(key); }
        catch (RuntimeException e) { return null; } // DB remains source of truth
    }
    private Ticket decode(String data) {
        if (data == null || NOT_FOUND.equals(data)) return null;
        try { return json.readValue(data, Ticket.class); }
        catch (JacksonException e) { return null; }
    }
    private void writeCache(String key, Ticket ticket) {
        try {
            if (ticket == null) {
                redis.opsForValue().set(key, NOT_FOUND, Duration.ofSeconds(15));
            } else {
                redis.opsForValue().set(key, json.writeValueAsString(ticket),
                    Duration.ofSeconds(ThreadLocalRandom.current().nextLong(50, 71)));
            }
        } catch (Exception e) {
            // Cache is best-effort; SQL remains authoritative.
        }
    }
    private void evictAfterCommit(int id) {
        Runnable evict = () -> {
            synchronized (cacheLock(id)) {
                try { redis.delete("ticket:" + id); }
                catch (RuntimeException e) { System.err.println("Cache eviction failed for ticket " + id); }
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evict.run(); }
            });
        } else evict.run();
    }

    public Ticket findById(int id) {
        if (cacheDisabled) return tickets.findById(id); // Cache OFF: MySQL-only ticket read
        String key = "ticket:" + id;
        String value = cached(key);
        if (NOT_FOUND.equals(value)) return null;
        Ticket hit = decode(value);
        if (hit != null) return hit;
        // On a hot cache miss only one thread per stripe loads from MySQL.
        synchronized (cacheLock(id)) {
            value = cached(key);
            if (NOT_FOUND.equals(value)) return null;
            hit = decode(value);
            if (hit != null) return hit;
            Ticket found = tickets.findById(id);
            writeCache(key, found);
            return found;
        }
    }

    public int add(Ticket ticket) {
        if (ticket.getAssignedTo() != null && users.findById(ticket.getAssignedTo()) == null)
            throw new UserNotFoundException("Assignee does not exist");
        return tickets.add(ticket);
    }
    public List<Ticket> findAll() { return tickets.findAll(); }
    public List<Ticket> findByStatus(String value) { return tickets.findByStatus(value); }
    public List<Ticket> findByPriority(String value) { return tickets.findByPriority(value); }
    public List<Ticket> findByAssignedTo(Integer id) { return tickets.findByAssignedTo(id); }
    public List<Ticket> findByFilters(String status, String priority, Integer assignedTo,
                                      int page, int size, String sortBy, String order, Integer viewerId) {
        return tickets.findByFilters(status, priority, assignedTo,
                                    size, (page - 1) * size, sortBy, order, viewerId);
    }
    public int countByFilters(String status, String priority, Integer assignedTo, Integer viewerId) {
        return tickets.countByFilters(status, priority, assignedTo, viewerId);
    }
    public PageResponse findTickets(String status, String priority, Integer assignedTo,
                                     int page, int size, String sortBy, String order, Integer viewerId) {
        return new PageResponse(findByFilters(status, priority, assignedTo, page, size, sortBy, order, viewerId),
                                countByFilters(status, priority, assignedTo, viewerId), page, size);
    }

    private Ticket existing(int id, int version) {
        Ticket ticket = tickets.findById(id);
        if (ticket == null) throw new TicketNotFoundException("Ticket not found");
        if (ticket.getVersion() != version) throw conflict();
        return ticket;
    }
    private TicketConflictException conflict() {
        return new TicketConflictException("Ticket changed; reload and retry");
    }
    private void successful(int updated, int id) {
        if (updated != 1) throw conflict();
        evictAfterCommit(id);
    }
    private void record(int id, int actorId, String action, String before, String after) {
        if (audit == null || actorId <= 0) return;
        TicketAuditLog log = new TicketAuditLog();
        log.setTicketId(id);
        log.setActorId(actorId);
        log.setAction(action);
        log.setOldValue(before);
        log.setNewValue(after);
        audit.add(log); // same transaction: a failed audit rolls back the Ticket update
    }
    private String describeTicketFields(Ticket ticket) {
        // JSON provides a stable, parseable before/after representation for editable fields.
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("title", ticket.getTitle());
        fields.put("description", ticket.getDescription());
        fields.put("category", ticket.getCategory());
        fields.put("resolution", ticket.getResolution());
        try { return json.writeValueAsString(fields); }
        catch (JacksonException e) { throw new IllegalStateException("Cannot serialize audit entry", e); }
    }

    private boolean validTransition(String old, String target) {
        return ("OPEN".equals(old) && "IN_PROGRESS".equals(target))
            || ("IN_PROGRESS".equals(old) && "RESOLVED".equals(target))
            || ("RESOLVED".equals(old) && ("CLOSED".equals(target) || "IN_PROGRESS".equals(target)));
    }

    // Legacy internal signatures preserved for prior unit tests and Java callers.
    // HTTP Controller always supplies the client's version and actor ID.
    @Transactional public int update(int id, Ticket ticket) {
        Ticket found = tickets.findById(id);
        if (found == null) throw new TicketNotFoundException("Ticket not found");
        return update(id, ticket, found.getVersion(), 0);
    }
    @Transactional public int updateStatus(int id, String status) {
        Ticket found = tickets.findById(id);
        if (found == null) throw new TicketNotFoundException("Ticket not found");
        return updateStatus(id, status, found.getVersion(), 0);
    }
    @Transactional public int updateAssignee(int id, Integer assignedTo) {
        Ticket found = tickets.findById(id);
        if (found == null) throw new TicketNotFoundException("Ticket not found");
        return updateAssignee(id, assignedTo, found.getVersion(), 0);
    }
    @Transactional public int updatePriority(int id, String priority) {
        Ticket found = tickets.findById(id);
        if (found == null) throw new TicketNotFoundException("Ticket not found");
        return updatePriority(id, priority, found.getVersion(), 0);
    }
    @Transactional public int delete(int id) {
        Ticket found = tickets.findById(id);
        if (found == null) throw new TicketNotFoundException("Ticket not found");
        return delete(id, found.getVersion(), 0);
    }

    @Transactional public int update(int id, Ticket changes, int expectedVersion, int actorId) {
        Ticket before = existing(id, expectedVersion);
        int affected = tickets.update(id, changes, expectedVersion);
        successful(affected, id);
        record(id, actorId, "EDIT", describeTicketFields(before), describeTicketFields(changes));
        return affected;
    }
    @Transactional public int updateStatus(int id, String status, int expectedVersion, int actorId) {
        Ticket before = existing(id, expectedVersion);
        if (!validTransition(before.getStatus(), status))
            throw new InvalidStatusTransitionException("Invalid status transition");
        int affected = tickets.updateStatus(id, status, before.getStatus(), expectedVersion);
        successful(affected, id);
        TicketLog history = new TicketLog();
        history.setTicketId(id);
        history.setOldStatus(before.getStatus());
        history.setNewStatus(status);
        logs.add(history);
        record(id, actorId, "STATUS", before.getStatus(), status);
        if (notifications != null && actorId > 0) notifications.queueStatusChange(
            id, before.getCreatedBy(), before.getAssignedTo(), actorId, before.getStatus(), status);
        return affected;
    }
    @Transactional public int updateAssignee(int id, Integer assignedTo, int expectedVersion, int actorId) {
        Ticket before = existing(id, expectedVersion);
        if (assignedTo != null && users.findById(assignedTo) == null)
            throw new UserNotFoundException("User not found");
        int affected = tickets.updateAssignee(id, assignedTo, expectedVersion);
        successful(affected, id);
        record(id, actorId, "ASSIGNEE", String.valueOf(before.getAssignedTo()), String.valueOf(assignedTo));
        if (notifications != null && actorId > 0
                && !java.util.Objects.equals(before.getAssignedTo(), assignedTo))
            notifications.queueAssignment(id, assignedTo, actorId);
        return affected;
    }
    @Transactional public int updatePriority(int id, String priority, int expectedVersion, int actorId) {
        Ticket before = existing(id, expectedVersion);
        if (!("LOW".equals(priority) || "MEDIUM".equals(priority)
              || "HIGH".equals(priority) || "URGENT".equals(priority)))
            throw new InvalidPriorityException("Invalid priority");
        int affected = tickets.updatePriority(id, priority, expectedVersion);
        successful(affected, id);
        record(id, actorId, "PRIORITY", before.getPriority(), priority);
        if (sla != null) sla.reset(id, SlaPolicy.minutesFor(priority));
        return affected;
    }
    @Transactional public int delete(int id, int expectedVersion, int actorId) {
        existing(id, expectedVersion);
        int affected = tickets.delete(id, expectedVersion);
        successful(affected, id);
        record(id, actorId, "DELETE", "active", "deleted");
        return affected;
    }
}
