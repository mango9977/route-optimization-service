package io.spacecat.service.algorithm;

import io.spacecat.model.Point;
import io.spacecat.model.Route;
import io.spacecat.service.RouteOptimizer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@Named("greedy")
public class GreedyOptimizer implements RouteOptimizer {
    @Override
    public Route optimize(List<Point> points) {
        long start = System.nanoTime();
        if (points.isEmpty()) return new Route(List.of(), 0, 0);

        List<Point> result = new ArrayList<>();
        var current = points.getFirst();
        result.add(current);
        var unvisited = new HashSet<>(points);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            Point nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Point p : unvisited) {
                double d = distance(current, p);
                if (d < minDist) {
                    minDist = d;
                    nearest = p;
                }
            }
            result.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        double totalDist = totalDistance(result);
        return new Route(result, totalDist, duration);
    }

    private double distance(Point a, Point b) {
        // формула гаверсинуса или евклидово (для теста)
        return Math.hypot(a.lat() - b.lat(), a.lng() - b.lng());
    }

    private double totalDistance(List<Point> route) {
        double sum = 0;
        for (int i = 0; i < route.size()-1; i++) {
            sum += distance(route.get(i), route.get(i+1));
        }
        return sum;
    }

    @Override
    public String getName() {
        return "Greedy (Nearest Neighbor)";
    }
}
