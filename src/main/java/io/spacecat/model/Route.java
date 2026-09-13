package io.spacecat.model;

import java.util.List;

public record Route(
        List<Point> points,
        double totalDistance,
        long durationMs) {
}
