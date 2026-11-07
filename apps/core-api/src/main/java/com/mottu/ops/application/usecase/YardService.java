package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.YardResponse;
import com.mottu.ops.application.port.in.GetYardsUseCase;
import com.mottu.ops.application.port.out.YardRepositoryPort;

import java.util.List;

public class YardService implements GetYardsUseCase {

    private final YardRepositoryPort yardRepositoryPort;

    public YardService(YardRepositoryPort yardRepositoryPort) {
        this.yardRepositoryPort = yardRepositoryPort;
    }

    @Override
    public List<YardResponse> getAllYards() {
        return yardRepositoryPort.findAllActive().stream()
                .map(yard -> new YardResponse(
                        yard.getId(),
                        yard.getName(),
                        yard.getCity(),
                        yard.getAddress(),
                        yard.isActive()
                ))
                .toList();
    }
}
