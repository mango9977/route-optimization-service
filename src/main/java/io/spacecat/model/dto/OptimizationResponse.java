package io.spacecat.model.dto;

import io.spacecat.model.Route;

import java.util.List;

public record OptimizationResponse(
        String requestId,
        List<AlgorithmResult> results,      // все запущенные алгоритмы
        AlgorithmResult bestResult,          // алгоритм с минимальным расстоянием (если есть)
        int totalPoints,
        long totalTimeMs
) {
    public record AlgorithmResult(
            String algorithmName,
            Route route,
            long durationMs,
            String status  // "SUCCESS" или "FAILED"
    ) {}
}
