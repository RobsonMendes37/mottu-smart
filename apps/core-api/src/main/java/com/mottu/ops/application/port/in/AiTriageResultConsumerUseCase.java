package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.TriageResultPayloadDto;

public interface AiTriageResultConsumerUseCase {
    void processAiResult(TriageResultPayloadDto payload);
}
