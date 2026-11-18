package com.agrotis.backend.repository;

import com.agrotis.backend.domain.SyncBatch;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import com.agrotis.backend.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncBatchRepository extends JpaRepository<SyncBatch, Long> {

    List<SyncBatch> findAllByOrderByCreatedAtDesc();

    Optional<SyncBatch> findFirstBySourceAndExternalReferenceOrderByCreatedAtAsc(
        String source, String externalReference);

    List<SyncBatch> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        Collection<SyncStatus> statuses, LocalDateTime now);
}
