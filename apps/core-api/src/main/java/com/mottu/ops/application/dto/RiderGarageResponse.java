package com.mottu.ops.application.dto;

import java.util.List;

public record RiderGarageResponse(
        ActiveContractDto activeContract,
        int openInspectionsCount,
        List<RecentInspectionDto> recentInspections
) {
}
