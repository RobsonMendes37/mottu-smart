package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.TriageJobPayloadDto;

public interface AiTriageQueuePort {
    void publishJob(TriageJobPayloadDto job);
}
