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


curl --location 'http://localhost:8080/api/v1/optimize' \
--header 'Content-Type: application/json' \
--data '{
"points": [
{"id": "A", "lat": 0, "lng": 0},
{"id": "B", "lat": 1, "lng": 1},
{"id": "C", "lat": 2, "lng": 2}
]
}'