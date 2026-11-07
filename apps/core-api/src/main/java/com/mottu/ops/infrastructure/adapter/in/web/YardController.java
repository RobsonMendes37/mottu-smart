package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.YardResponse;
import com.mottu.ops.application.port.in.GetYardsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/yards")
public class YardController {

    private final GetYardsUseCase getYardsUseCase;

    public YardController(GetYardsUseCase getYardsUseCase) {
        this.getYardsUseCase = getYardsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<YardResponse>> listYards() {
        return ResponseEntity.ok(getYardsUseCase.getAllYards());
    }
}
