package io.spacecat.service.algorithm;

import io.spacecat.model.Point;
import io.spacecat.model.Route;
import io.spacecat.service.RouteOptimizer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;


@Singleton
@Named("ant")
public class AntOptimizer implements RouteOptimizer {

    @Override
    public Route optimize(List<Point> points) {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }
}
