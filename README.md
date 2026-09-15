# Route Optimization Service

Микросервис для оптимизации маршрутов обхода точек на основе трёх алгоритмов: жадного, муравьиного (ACO) и точного (ветвей и границ). Построен на **Micronaut 5**, **Java 25** с использованием **виртуальных потоков** и **StructuredTaskScope**.

---

## 🚀 Требования

- **JDK 25** (preview-функции включены)
- **Gradle 8.x+** (или используйте встроенный `./gradlew`)

---

## ▶️ Запуск

### Запуск в dev-режиме (с автоперезагрузкой)

```bash
./gradlew run

./gradlew build
java --enable-preview -jar build/libs/route-optimization-service-1.0-SNAPSHOT-all.jar

src/main/java/io/spacecat/
├── RouteOptimizationServiceRunner.java   # точка входа
├── controller/
│   └── OptimizationController.java       # REST-контроллер
├── model/
│   ├── Point.java
│   ├── Route.java
│   ├── AlgorithmType.java
│   └── dto/
│       ├── OptimizationRequest.java
│       └── OptimizationResponse.java
└── service/
    ├── RouteOptimizer.java               # интерфейс
    ├── OptimizerFactory.java             # фабрика алгоритмов
    └── algorithm/
        ├── GeoUtils.java                 # общие утилиты
        ├── GreedyOptimizer.java
        ├── AntColonyOptimizer.java
        └── ExactOptimizer.java

curl --location 'http://localhost:8080/api/v1/optimize' \
--header 'Content-Type: application/json' \
--data '{
"points": [
{"id": "A", "lat": 0, "lng": 0},
{"id": "B", "lat": 1, "lng": 1},
{"id": "C", "lat": 2, "lng": 2}
]
}'