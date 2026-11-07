package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.YardResponse;

import java.util.List;

public interface GetYardsUseCase {
    List<YardResponse> getAllYards();
}
