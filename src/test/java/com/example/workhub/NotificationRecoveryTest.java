package com.example.workhub;

import com.example.workhub.notification.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRecoveryTest {
    private final NotificationJobMapper mapper = mock(NotificationJobMapper.class);
    private final NotificationWorker worker = mock(NotificationWorker.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final NotificationService service = new NotificationService(mapper, worker, executor);

    @Test void expiredAttemptLimitIsHandledBeforePendingScan() {
        when(mapper.expireExhausted()).thenReturn(1);
        when(mapper.pendingIds(50)).thenReturn(List.of(101L));

        service.recoverPending();

        InOrder order = inOrder(mapper);
        order.verify(mapper).expireExhausted();
        order.verify(mapper).pendingIds(50);
        verify(executor).execute(any(Runnable.class));
    }

    @Test void emptyPendingQueueStillExpiresExhaustedJobs() {
        when(mapper.expireExhausted()).thenReturn(2);
        when(mapper.pendingIds(50)).thenReturn(List.of());

        service.recoverPending();

        verify(mapper).expireExhausted();
        verifyNoInteractions(executor);
    }
}
