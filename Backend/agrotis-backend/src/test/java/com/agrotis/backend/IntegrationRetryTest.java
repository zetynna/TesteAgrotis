package com.agrotis.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.agrotis.backend.domain.SyncBatch;
import com.agrotis.backend.domain.SyncStatus;
import com.agrotis.backend.dto.IntegrationRequest;
import com.agrotis.backend.repository.SyncBatchRepository;
import com.agrotis.backend.service.IntegrationPayloadProcessor;
import com.agrotis.backend.service.IntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IntegrationRetryTest {

    @Test
    void transientFailuresUseExponentialDelayAndEventuallyEnterDlq() {
        SyncBatchRepository repository = org.mockito.Mockito.mock(SyncBatchRepository.class);
        IntegrationPayloadProcessor processor = org.mockito.Mockito.mock(IntegrationPayloadProcessor.class);
        ObjectMapper mapper = new ObjectMapper();
        SyncBatch batch = new SyncBatch("source", "ref", "LABORATORY", "{}", "hash", "key");
        when(repository.findById(1L)).thenReturn(Optional.of(batch));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("temporary database failure"))
            .when(processor).process(any(), any());
        var service = new IntegrationService(repository, processor, mapper, 3, 2, 20, 900);

        service.processOne(1L);
        assertEquals(SyncStatus.FAILED, batch.getStatus());
        long firstDelay = Duration.between(LocalDateTime.now(), batch.getNextAttemptAt()).toSeconds();
        assertTrue(firstDelay >= 1 && firstDelay <= 2);

        service.processOne(1L);
        assertEquals(SyncStatus.FAILED, batch.getStatus());
        long secondDelay = Duration.between(LocalDateTime.now(), batch.getNextAttemptAt()).toSeconds();
        assertTrue(secondDelay >= 3 && secondDelay <= 4);

        service.processOne(1L);
        assertEquals(SyncStatus.DLQ, batch.getStatus());
        assertEquals(3, batch.getAttemptCount());
    }
}
