package com.example.workhub;

import com.example.workhub.notification.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationWorkerTest {
    private final NotificationJobMapper mapper = mock(NotificationJobMapper.class);
    private final NotificationWorker worker = new NotificationWorker(mapper);

    private NotificationJob sampleJob() {
        NotificationJob job = new NotificationJob();
        job.setTicketId(16);
        job.setRecipientId(2);
        job.setEventType("STATUS_CHANGED");
        job.setOldValue("OPEN");
        job.setNewValue("IN_PROGRESS");
        return job;
    }

    @Test void claimThenMarkReadyWithSameToken() {
        when(mapper.claim(eq(45L), anyString())).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(sampleJob());
        when(mapper.markReady(eq(45L), anyString(), anyString())).thenReturn(1);

        worker.process(45L);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).claim(eq(45L), token.capture());
        assertFalse(token.getValue().isBlank());
        verify(mapper).markReady(45L, token.getValue(),
                "Ticket #16 status: OPEN -> IN_PROGRESS");
        verify(mapper, never()).releaseForRetry(anyLong(), anyString());
    }

    @Test void duplicateWorkerCannotClaimSameJob() {
        when(mapper.claim(eq(45L), anyString())).thenReturn(0);

        worker.process(45L);

        verify(mapper, never()).findById(anyLong());
        verify(mapper, never()).markReady(anyLong(), anyString(), anyString());
        verify(mapper, never()).releaseForRetry(anyLong(), anyString());
    }

    @Test void failedPreparationReleasesOnlyOwnedClaim() {
        NotificationJob job = new NotificationJob();
        job.setEventType("UNKNOWN");
        when(mapper.claim(eq(45L), anyString())).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(job);
        when(mapper.releaseForRetry(eq(45L), anyString())).thenReturn(1);

        worker.process(45L);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).claim(eq(45L), token.capture());
        verify(mapper).releaseForRetry(45L, token.getValue());
        verify(mapper, never()).markReady(anyLong(), anyString(), anyString());
    }

    @Test void outdatedWorkerMustNotReleaseNewClaimAfterFailedCompletion() {
        when(mapper.claim(eq(45L), anyString())).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(sampleJob());
        // SQL returns 0 if a new claim token has replaced our token.
        when(mapper.markReady(eq(45L), anyString(), anyString())).thenReturn(0);

        worker.process(45L);

        verify(mapper).markReady(eq(45L), anyString(), anyString());
        verify(mapper, never()).releaseForRetry(anyLong(), anyString());
    }

    @Test void staleClaimOnExceptionCannotReleaseReplacementClaim() {
        NotificationJob job = new NotificationJob();
        job.setEventType("UNKNOWN");
        when(mapper.claim(eq(45L), anyString())).thenReturn(1);
        when(mapper.findById(45L)).thenReturn(job);
        // A different worker reclaims the lease; release should affect zero rows.
        when(mapper.releaseForRetry(eq(45L), anyString())).thenReturn(0);

        assertDoesNotThrow(() -> worker.process(45L));
        verify(mapper).releaseForRetry(eq(45L), anyString());
    }
}
