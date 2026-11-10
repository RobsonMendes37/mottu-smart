package com.mottu.ops.domain.model;

import java.time.Instant;

public record Baseline360Photos(
        String frontPhotoUrl,
        String rearPhotoUrl,
        String rightSidePhotoUrl,
        String leftSidePhotoUrl,
        String odometerPhotoUrl,
        int initialOdometerKm,
        Instant recordedAt
) {
    public Baseline360Photos {
        if (frontPhotoUrl == null || frontPhotoUrl.isBlank() ||
            rearPhotoUrl == null || rearPhotoUrl.isBlank() ||
            rightSidePhotoUrl == null || rightSidePhotoUrl.isBlank() ||
            leftSidePhotoUrl == null || leftSidePhotoUrl.isBlank() ||
            odometerPhotoUrl == null || odometerPhotoUrl.isBlank()) {
            throw new IllegalArgumentException("Todas as 4 fotos externas (frente, traseira, laterais) e a foto do odômetro são obrigatórias");
        }
        if (initialOdometerKm < 0) {
            throw new IllegalArgumentException("Quilometragem inicial não pode ser negativa");
        }
    }

    public boolean isComplete() {
        return frontPhotoUrl != null && rearPhotoUrl != null &&
               rightSidePhotoUrl != null && leftSidePhotoUrl != null &&
               odometerPhotoUrl != null;
    }
}
