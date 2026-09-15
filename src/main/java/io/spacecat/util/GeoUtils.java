package io.spacecat.util;

import io.spacecat.model.Point;

import java.util.List;

public final class GeoUtils {

    private GeoUtils() {}

    /** Евклидово расстояние — для тестов. В проде заменить на гаверсинус. */
    public static double distance(Point a, Point b) {
        return Math.hypot(a.lat() - b.lat(), a.lng() - b.lng());
    }

    /** Суммарная длина маршрута (учитывая возврат в начальную точку). */
    public static double totalDistance(List<Point> route) {
        if (route.size() < 2) return 0;
        double sum = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            sum += distance(route.get(i), route.get(i + 1));
        }
        // Замыкаем маршрут: возврат в стартовую точку
        sum += distance(route.get(route.size() - 1), route.get(0));
        return sum;
    }
}
