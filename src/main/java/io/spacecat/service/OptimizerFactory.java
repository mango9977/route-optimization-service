package io.spacecat.service;

import io.micronaut.context.annotation.Any;
import io.spacecat.model.AlgorithmType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

@Singleton
public class OptimizerFactory {
    private final Map<AlgorithmType, RouteOptimizer> optimizers = new EnumMap<>(AlgorithmType.class);

    @Inject
    public OptimizerFactory(@Any @Named("greedy") RouteOptimizer greedy,
                            @Named("ant") RouteOptimizer ant,
                            @Named("exact") RouteOptimizer exact) {
        optimizers.put(AlgorithmType.GREEDY, greedy);
        optimizers.put(AlgorithmType.ANT_COLONY, ant);
        optimizers.put(AlgorithmType.EXACT, exact);
    }

    public RouteOptimizer getOptimizer(AlgorithmType type) {
        return optimizers.get(type);
    }
}
