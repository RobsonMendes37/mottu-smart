package com.mottu.ops.infrastructure.adapter.out.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.AccidentJobPayloadDto;
import com.mottu.ops.application.port.out.AccidentQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RedisAccidentQueueAdapter implements AccidentQueuePort {

    private static final Logger log = LoggerFactory.getLogger(RedisAccidentQueueAdapter.class);
    public static final String ACCIDENT_JOBS_QUEUE = "queue:ai_accident_jobs";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final List<AccidentJobPayloadDto> inMemoryJobs = new CopyOnWriteArrayList<>();

    public RedisAccidentQueueAdapter(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishJob(AccidentJobPayloadDto job) {
        inMemoryJobs.add(job);
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                String json = objectMapper.writeValueAsString(job);
                redisTemplate.opsForList().leftPush(ACCIDENT_JOBS_QUEUE, json);
                log.info("Published accident job {} to Redis queue {}", job.accidentReportId(), ACCIDENT_JOBS_QUEUE);
            } else {
                log.info("RedisTemplate not configured; queued accident job {} in-memory", job.accidentReportId());
            }
        } catch (Exception e) {
            log.warn("Failed to publish accident job {} to Redis (offline mode active): {}", job.accidentReportId(), e.getMessage());
        }
    }

    public List<AccidentJobPayloadDto> getInMemoryJobs() {
        return List.copyOf(inMemoryJobs);
    }

    public void clearInMemoryJobs() {
        inMemoryJobs.clear();
    }
}

