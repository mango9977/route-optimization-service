package io.spacecat.model.dto;

import io.spacecat.model.AlgorithmType;
import io.spacecat.model.Point;

import java.util.List;

public record OptimizationRequest(
        List<Point> points,
        AlgorithmType algorithm
) {}
