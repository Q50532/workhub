package com.example.workhub;

import com.example.workhub.notification.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.ArgumentCaptor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationOutboxTest {
    private final NotificationJobMapper mapper = mock(NotificationJobMapper.class);
    private final NotificationWorker worker = mock(NotificationWorker.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final NotificationService service = new NotificationService(mapper, worker, executor);

    @BeforeEach void startTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        when(mapper.add(any(NotificationJob.class))).thenAnswer(invocation -> {
            NotificationJob job = invocation.getArgument(0);
            job.setId(42L);
            return 1;
        });
    }

    @AfterEach void cleanup() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void publishOnlyAfterCommit() {
        service.queueStatusChange(16, 1, 2, 2, "OPEN", "IN_PROGRESS");
        ArgumentCaptor<NotificationJob> row = ArgumentCaptor.forClass(NotificationJob.class);
        verify(mapper).add(row.capture());
        assertEquals(1, row.getValue().getRecipientId());
        assertEquals("STATUS_CHANGED", row.getValue().getEventType());
        verifyNoInteractions(executor); // pending record exists inside transaction
        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, callbacks.size());
        callbacks.forEach(TransactionSynchronization::afterCommit);
        verify(executor).execute(any(Runnable.class));
    }

    @Test void rollbackMustNotDispatch() {
        service.queueAssignment(16, 2, 1);
        // rollback means Spring does not call afterCommit
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations())
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(executor);
    }

    @Test void duplicateRecipientCreatesOnlyOneNotification() {
        service.queueStatusChange(16, 2, 2, 1, "OPEN", "IN_PROGRESS");
        verify(mapper, times(1)).add(any());
    }
}
