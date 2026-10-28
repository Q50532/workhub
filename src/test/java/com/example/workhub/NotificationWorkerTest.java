package com.example.workhub;

import com.example.workhub.notification.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationWorkerTest {
    private final NotificationJobMapper mapper = mock(NotificationJobMapper.class);
    private final NotificationWorker worker = new NotificationWorker(mapper);

    @Test void claimThenMarkReady() {
        NotificationJob job = new NotificationJob();
        job.setTicketId(16);
        job.setRecipientId(2);
        job.setEventType("STATUS_CHANGED");
        job.setOldValue("OPEN");
        job.setNewValue("IN_PROGRESS");
        when(mapper.claim(45L)).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(job);
        when(mapper.markReady(anyLong(), anyString())).thenReturn(1);
        worker.process(45L);
        verify(mapper).markReady(45L, "Ticket #16 status: OPEN -> IN_PROGRESS");
    }

    @Test void duplicateWorkerCannotClaimSameJob() {
        when(mapper.claim(45L)).thenReturn(0);
        worker.process(45L);
        verify(mapper, never()).markReady(anyLong(), anyString());
    }

    @Test void failedPreparationReleasesForRetry() {
        NotificationJob job = new NotificationJob();
        job.setEventType("UNKNOWN");
        when(mapper.claim(45L)).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(job);
        worker.process(45L);
        verify(mapper).releaseForRetry(45L);
    }
}
