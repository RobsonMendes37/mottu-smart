package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.AccidentResultPayloadDto;

public interface AccidentResultConsumerUseCase {

    void processResult(AccidentResultPayloadDto payload);
}

