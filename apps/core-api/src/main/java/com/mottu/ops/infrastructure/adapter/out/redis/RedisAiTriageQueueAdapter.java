package com.mottu.ops.infrastructure.adapter.out.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.TriageJobPayloadDto;
import com.mottu.ops.application.port.out.AiTriageQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RedisAiTriageQueueAdapter implements AiTriageQueuePort {

    private static final Logger log = LoggerFactory.getLogger(RedisAiTriageQueueAdapter.class);
    public static final String JOBS_QUEUE = "queue:ai_triage_jobs";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final List<TriageJobPayloadDto> inMemoryJobs = new CopyOnWriteArrayList<>();

    public RedisAiTriageQueueAdapter(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishJob(TriageJobPayloadDto job) {
        inMemoryJobs.add(job);
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                String json = objectMapper.writeValueAsString(job);
                redisTemplate.opsForList().leftPush(JOBS_QUEUE, json);
                log.info("Published AI triage job {} to Redis queue {}", job.inspectionId(), JOBS_QUEUE);
            } else {
                log.info("RedisTemplate not configured; queued AI triage job {} in-memory", job.inspectionId());
            }
        } catch (Exception e) {
            log.warn("Failed to publish job {} to Redis (offline mode active): {}", job.inspectionId(), e.getMessage());
        }
    }

    public List<TriageJobPayloadDto> getInMemoryJobs() {
        return List.copyOf(inMemoryJobs);
    }

    public void clearInMemoryJobs() {
        inMemoryJobs.clear();
    }
}
