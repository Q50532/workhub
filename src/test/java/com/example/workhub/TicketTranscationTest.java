package com.example.workhub;

import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketTransactionTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void rollbackShouldRestoreTicketStatus() {

        int ticketId = 14;

        // 1. Read original state from MySQL
        Ticket originalTicket = ticketMapper.findById(ticketId);
        assertNotNull(originalTicket);

        String originalStatus = originalTicket.getStatus();

        String newStatus = switch (originalStatus) {
            case "OPEN" -> "IN_PROGRESS";
            case "IN_PROGRESS" -> "RESOLVED";
            case "RESOLVED" -> "IN_PROGRESS";
            default -> throw new IllegalStateException(
                    "Ticket has no valid transition for this test"
            );
        };

        // 2. Create a transaction template
        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        // 3. Update database, then force a rollback
        assertThrows(IllegalStateException.class, () ->
            template.execute(status -> {

                int result = ticketService.updateStatus(
                        ticketId, newStatus
                );

                assertEquals(1, result);

                // The update is visible inside this transaction
                assertEquals(
                        newStatus,
                        ticketMapper.findById(ticketId).getStatus()
                );

                // Force the transaction to fail
                throw new IllegalStateException("Force rollback");
            })
        );

        // 4. Check the database after rollback
        Ticket restoredTicket = ticketMapper.findById(ticketId);

        assertEquals(
                originalStatus,
                restoredTicket.getStatus()
        );
    }
}