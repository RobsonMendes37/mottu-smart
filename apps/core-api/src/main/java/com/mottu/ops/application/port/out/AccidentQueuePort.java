package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.AccidentJobPayloadDto;

public interface AccidentQueuePort {
    void publishJob(AccidentJobPayloadDto job);
}

