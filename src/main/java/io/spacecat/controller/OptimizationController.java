package io.spacecat.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.spacecat.model.AlgorithmType;
import io.spacecat.model.Point;
import io.spacecat.model.Route;
import io.spacecat.model.dto.ErrorResponse;
import io.spacecat.model.dto.OptimizationRequest;
import io.spacecat.model.dto.OptimizationResponse;
import io.spacecat.model.dto.OptimizationResponse.AlgorithmResult;
import io.spacecat.service.OptimizerFactory;
import io.spacecat.service.RouteOptimizer;
import jakarta.inject.Inject;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeUnit;

@Controller("/api/v1/optimize")
@Validated
public class OptimizationController {

    @Inject
    private OptimizerFactory factory;

    @Inject
    private MeterRegistry meterRegistry;

    @Post
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<?> optimize(@Body @Valid OptimizationRequest request) {
        List<Point> points = request.points();
        if (points == null || points.size() < 2) {
            return HttpResponse.badRequest(new ErrorResponse("At least 2 points required"));
        }

        // Всегда запускаем все три алгоритма
        List<AlgorithmType> algorithms = List.of(
                AlgorithmType.GREEDY,
                AlgorithmType.ANT_COLONY,
                AlgorithmType.EXACT
        );

        long startOverall = System.nanoTime();

        try (var scope = StructuredTaskScope.open(
                Joiner.<AlgorithmResult>awaitAllSuccessfulOrThrow())) {

            List<Subtask<AlgorithmResult>> subtasks = new ArrayList<>();

            for (AlgorithmType type : algorithms) {
                RouteOptimizer optimizer = factory.getOptimizer(type);
                Subtask<AlgorithmResult> subtask = scope.fork(
                        () -> runAlgorithm(optimizer, points));
                subtasks.add(subtask);
            }

            scope.join();

            // Собираем результаты
            List<AlgorithmResult> results = new ArrayList<>();
            for (Subtask<AlgorithmResult> subtask : subtasks) {
                if (subtask.state() == Subtask.State.SUCCESS) {
                    results.add(subtask.get());
                }
            }

            // Лучший маршрут среди успешных
            AlgorithmResult best = results.stream()
                    .filter(r -> "SUCCESS".equals(r.status()) && r.route() != null)
                    .min(Comparator.comparingDouble(r -> r.route().totalDistance()))
                    .orElse(null);

            if (best == null) {
                return HttpResponse.serverError("All optimizers failed");
            }

            long totalTime = (System.nanoTime() - startOverall) / 1_000_000;

            OptimizationResponse response = new OptimizationResponse(
                    UUID.randomUUID().toString(),
                    results,
                    best,
                    points.size(),
                    totalTime
            );
            return HttpResponse.ok(response);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HttpResponse.serverError("Optimization interrupted");
        } catch (Exception e) {
            return HttpResponse.serverError("Optimization failed: " + e.getMessage());
        }
    }

    /**
     * Запускает один алгоритм, замеряет время и пишет метрику в Micrometer.
     * Ошибки не пробрасываются наружу — они превращаются в AlgorithmResult со статусом FAILED.
     */
    private AlgorithmResult runAlgorithm(RouteOptimizer optimizer, List<Point> points) {
        long start = System.nanoTime();
        try {
            Route route = optimizer.optimize(points);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            // Метрика: время выполнения по каждому алгоритму
            Timer.builder("optimizer.duration")
                    .description("Duration of optimization algorithm")
                    .tag("algorithm", optimizer.getName())
                    .tag("status", "success")
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);

            // Счётчик успешных запусков
            meterRegistry.counter("optimizer.runs",
                    "algorithm", optimizer.getName(),
                    "status", "success").increment();

            return new AlgorithmResult(optimizer.getName(), route, durationMs, "SUCCESS");

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            // Метрика: время до падения
            Timer.builder("optimizer.duration")
                    .description("Duration of optimization algorithm")
                    .tag("algorithm", optimizer.getName())
                    .tag("status", "failed")
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);

            meterRegistry.counter("optimizer.runs",
                    "algorithm", optimizer.getName(),
                    "status", "failed").increment();

            return new AlgorithmResult(
                    optimizer.getName(),
                    null,
                    durationMs,
                    "FAILED: " + e.getMessage()
            );
        }
    }
}