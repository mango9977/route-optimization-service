package io.spacecat.service.algorithm;

import io.spacecat.model.Point;
import io.spacecat.model.Route;
import io.spacecat.service.RouteOptimizer;
import io.spacecat.util.GeoUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Named("exact")
public class ExactOptimizer implements RouteOptimizer {

    /** Защита от «зависания» на больших входах. */
    private static final int MAX_POINTS = 13;

    @Override
    public Route optimize(List<Point> points) {
        long start = System.nanoTime();

        if (points.isEmpty()) {
            return new Route(List.of(), 0, System.nanoTime() - start);
        }
        if (points.size() > MAX_POINTS) {
            throw new IllegalArgumentException(
                    "Exact algorithm supports up to " + MAX_POINTS +
                            " points, got " + points.size());
        }

        int n = points.size();
        double[][] dist = buildDistanceMatrix(points);

        // Состояние поиска
        boolean[] visited = new boolean[n];
        List<Integer> currentPath = new ArrayList<>(n);
        List<Integer> bestPath = new ArrayList<>(n);

        // Начинаем с точки 0 (можно перебрать все старты, но для TSP это избыточно)
        visited[0] = true;
        currentPath.add(0);

        double[] bestDist = { Double.MAX_VALUE };

        // Branch & Bound: идём вглубь, отсекая ветви по нижней оценке
        dfs(0, 0.0, 1, n, dist, visited, currentPath, bestPath, bestDist);

        List<Point> ordered = new ArrayList<>(n);
        for (int idx : bestPath) {
            ordered.add(points.get(idx));
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        return new Route(ordered, bestDist[0], durationMs);
    }

    /** Рекурсивный поиск с отсечением. */
    private void dfs(int current,
                     double currentCost,
                     int visitedCount,
                     int n,
                     double[][] dist,
                     boolean[] visited,
                     List<Integer> currentPath,
                     List<Integer> bestPath,
                     double[] bestDist) {

        // Отсечение: если уже хуже лучшего — выходим
        if (currentCost >= bestDist[0]) return;

        // Все точки посещены — замыкаем маршрут
        if (visitedCount == n) {
            double total = currentCost + dist[current][0]; // возврат в старт
            if (total < bestDist[0]) {
                bestDist[0] = total;
                bestPath.clear();
                bestPath.addAll(currentPath);
            }
            return;
        }

        // Перебираем непосещённые точки, начиная с ближайших (ускоряет отсечение)
        int[] candidates = orderByDistance(current, n, dist, visited);
        for (int next : candidates) {
            if (visited[next]) continue;

            visited[next] = true;
            currentPath.add(next);

            dfs(next,
                    currentCost + dist[current][next],
                    visitedCount + 1,
                    n, dist, visited, currentPath, bestPath, bestDist);

            currentPath.remove(currentPath.size() - 1);
            visited[next] = false;
        }
    }

    /** Возвращает индексы всех точек, отсортированные по расстоянию от current. */
    private int[] orderByDistance(int current, int n, double[][] dist, boolean[] visited) {
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(dist[current][a], dist[current][b]));
        int[] result = new int[n];
        for (int i = 0; i < n; i++) result[i] = idx[i];
        return result;
    }

    /** Матрица попарных расстояний. */
    private double[][] buildDistanceMatrix(List<Point> points) {
        int n = points.size();
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double v = GeoUtils.distance(points.get(i), points.get(j));
                d[i][j] = v;
                d[j][i] = v;
            }
        }
        return d;
    }

    @Override
    public String getName() {
        return "Exact (Branch and Bound)";
    }
}