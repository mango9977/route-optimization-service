package io.spacecat.service.algorithm;

import io.spacecat.model.Point;
import io.spacecat.model.Route;
import io.spacecat.service.RouteOptimizer;
import io.spacecat.util.GeoUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Singleton
@Named("ant")
public class AntOptimizer implements RouteOptimizer {

    // --- Параметры ACO ---
    private static final int    ANTS           = 30;      // число муравьёв
    private static final int    ITERATIONS     = 200;     // число итераций
    private static final double ALPHA          = 1.0;     // вес феромона
    private static final double BETA           = 3.0;     // вес «жадности» (1/расстояние)
    private static final double EVAPORATION    = 0.5;     // скорость испарения
    private static final double Q              = 100.0;   // константа отложения феромона
    private static final double INITIAL_PHERO  = 1.0;     // стартовый феромон

    private final Random random = new Random(42);         // фиксированный seed → детерминизм

    @Override
    public Route optimize(List<Point> points) {
        long start = System.nanoTime();

        if (points.isEmpty()) return new Route(List.of(), 0, System.nanoTime() - start);
        if (points.size() == 1) return new Route(List.of(points.get(0)), 0, System.nanoTime() - start);

        int n = points.size();
        double[][] dist = buildDistanceMatrix(points);
        double[][] pheromone = initPheromone(n);

        List<Integer> bestPath = null;
        double bestLength = Double.MAX_VALUE;

        for (int iter = 0; iter < ITERATIONS; iter++) {
            List<List<Integer>> antPaths = new ArrayList<>(ANTS);
            double[] antLengths = new double[ANTS];

            for (int ant = 0; ant < ANTS; ant++) {
                List<Integer> path = buildAntPath(n, dist, pheromone);
                double length = pathLength(path, dist);
                antPaths.add(path);
                antLengths[ant] = length;

                if (length < bestLength) {
                    bestLength = length;
                    bestPath = new ArrayList<>(path);
                }
            }

            evaporate(pheromone);
            depositPheromone(pheromone, antPaths, antLengths);
        }

        List<Point> ordered = new ArrayList<>(n);
        for (int idx : bestPath) ordered.add(points.get(idx));

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        return new Route(ordered, bestLength, durationMs);
    }

    // ---------- Внутренние шаги ACO ----------

    private double[][] initPheromone(int n) {
        double[][] p = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                p[i][j] = INITIAL_PHERO;
        return p;
    }

    /** Муравей стартует из случайного города и жадно-вероятностно идёт по остальным. */
    private List<Integer> buildAntPath(int n, double[][] dist, double[][] pheromone) {
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>(n);

        int current = random.nextInt(n);
        path.add(current);
        visited[current] = true;

        for (int step = 1; step < n; step++) {
            int next = selectNext(current, visited, n, dist, pheromone);
            path.add(next);
            visited[next] = true;
            current = next;
        }
        return path;
    }

    /** Вероятностный выбор следующего города по формуле ACO. */
    private int selectNext(int current, boolean[] visited, int n,
                           double[][] dist, double[][] pheromone) {
        double[] probabilities = new double[n];
        double sum = 0.0;

        for (int j = 0; j < n; j++) {
            if (visited[j]) continue;
            double tau = Math.pow(pheromone[current][j], ALPHA);
            double eta = Math.pow(1.0 / (dist[current][j] + 1e-9), BETA);
            probabilities[j] = tau * eta;
            sum += probabilities[j];
        }

        if (sum == 0.0) {
            // fallback: первый непосещённый
            for (int j = 0; j < n; j++) if (!visited[j]) return j;
        }

        double r = random.nextDouble() * sum;
        double acc = 0.0;
        for (int j = 0; j < n; j++) {
            if (visited[j]) continue;
            acc += probabilities[j];
            if (acc >= r) return j;
        }

        // fallback (численная погрешность)
        for (int j = n - 1; j >= 0; j--) if (!visited[j]) return j;
        throw new IllegalStateException("No unvisited city");
    }

    /** Длина замкнутого маршрута (с возвратом в старт). */
    private double pathLength(List<Integer> path, double[][] dist) {
        double sum = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            sum += dist[path.get(i)][path.get(i + 1)];
        }
        sum += dist[path.get(path.size() - 1)][path.get(0)];
        return sum;
    }

    /** Испарение феромона. */
    private void evaporate(double[][] pheromone) {
        int n = pheromone.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                pheromone[i][j] *= (1.0 - EVAPORATION);
    }

    /** Отложение феромона всеми муравьями (Q / длина маршрута). */
    private void depositPheromone(double[][] pheromone,
                                  List<List<Integer>> antPaths,
                                  double[] antLengths) {
        for (int a = 0; a < antPaths.size(); a++) {
            List<Integer> path = antPaths.get(a);
            double deposit = Q / antLengths[a];
            for (int i = 0; i < path.size() - 1; i++) {
                int u = path.get(i), v = path.get(i + 1);
                pheromone[u][v] += deposit;
                pheromone[v][u] += deposit;
            }
            // замыкающее ребро
            int u = path.get(path.size() - 1), v = path.getFirst();
            pheromone[u][v] += deposit;
            pheromone[v][u] += deposit;
        }
    }

    private double[][] buildDistanceMatrix(List<Point> points) {
        int n = points.size();
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                double v = GeoUtils.distance(points.get(i), points.get(j));
                d[i][j] = v;
                d[j][i] = v;
            }
        return d;
    }

    @Override
    public String getName() {
        return "Ant Colony Optimization";
    }
}
