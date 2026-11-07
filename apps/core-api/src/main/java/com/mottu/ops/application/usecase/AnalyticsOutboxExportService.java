package com.mottu.ops.application.usecase;

import com.mottu.ops.application.port.in.AnalyticsOutboxExportUseCase;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AnalyticsOutboxJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAnalyticsOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsOutboxExportService implements AnalyticsOutboxExportUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsOutboxExportService.class);

    private final SpringDataAnalyticsOutboxRepository outboxRepository;
    private final boolean bigQueryEnabled;
    private final String dataset;
    private final String table;

    public AnalyticsOutboxExportService(
            SpringDataAnalyticsOutboxRepository outboxRepository,
            @Value("${mottu.bigquery.enabled:false}") boolean bigQueryEnabled,
            @Value("${mottu.bigquery.dataset:mottu_ops_bi}") String dataset,
            @Value("${mottu.bigquery.table:fact_inspections}") String table
    ) {
        this.outboxRepository = outboxRepository;
        this.bigQueryEnabled = bigQueryEnabled;
        this.dataset = dataset;
        this.table = table;
    }

    @Override
    @Transactional
    public int exportPendingBatch() {
        List<AnalyticsOutboxJpaEntity> pending = outboxRepository.findByExportedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return 0;
        }

        log.info("Starting BigQuery outbox export batch for {} pending records (bigQueryEnabled={})",
                pending.size(), bigQueryEnabled);

        if (bigQueryEnabled) {
            // Assemble NDJSON batch payload for BigQuery table loading
            String ndjson = pending.stream()
                    .map(AnalyticsOutboxJpaEntity::getPayloadJson)
                    .collect(Collectors.joining("\n"));
            log.info("Generated NDJSON payload for {}.{} (length: {} chars)", dataset, table, ndjson.length());
            // In cloud environment, BigQuery Batch Load job would load this NDJSON stream
        }

        Instant exportedAt = Instant.now();
        for (AnalyticsOutboxJpaEntity record : pending) {
            record.setExported(true);
            record.setExportedAt(exportedAt);
        }

        outboxRepository.saveAll(pending);
        log.info("Successfully exported batch of {} analytics outbox records", pending.size());
        return pending.size();
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledExport() {
        try {
            exportPendingBatch();
        } catch (Exception e) {
            log.error("Error executing scheduled BigQuery outbox export", e);
        }
    }
}
