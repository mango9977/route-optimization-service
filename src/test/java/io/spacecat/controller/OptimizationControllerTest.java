package io.spacecat.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.spacecat.model.AlgorithmType;
import io.spacecat.model.Point;
import io.spacecat.model.dto.OptimizationRequest;
import io.spacecat.model.dto.OptimizationResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class OptimizationControllerTest {

    @Inject
    @Client("/")
    private HttpClient client;

    @Test
    void testGreedyOptimization() {
        var request = new OptimizationRequest(
                List.of(new Point("A", 0, 0), new Point("B", 1, 1), new Point("C", 2, 2)),
                AlgorithmType.GREEDY
        );
        var response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/v1/optimize", request),
                OptimizationResponse.class
        );
        assertNotNull(response.bestResult());
        assertEquals(3, response.bestResult().points().size());
    }
}