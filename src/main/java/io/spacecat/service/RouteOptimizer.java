package io.spacecat.service;

import io.spacecat.model.Point;
import io.spacecat.model.Route;

import java.util.List;

public interface RouteOptimizer {
    Route optimize(List<Point> points);
    String getName();
}
