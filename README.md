# Kotlin + Spring Boot + Redis + Docker + Fly.io

Jednoduchá ukázková Kotlin/Spring Boot aplikace, která vrací JSON přes HTTP, balí se do Docker image, pushuje se na Docker Hub, nasazuje se na Fly.io a používá Redis databázi na Upstash.

---

## 1. Použité cloudové služby

### Fly.io
Fly.io je cloudová platforma, na kterou nasazuji Docker image s Kotlin/Spring Boot aplikací. Aplikace běží v kontejnerech na infrastruktuře Fly.io v různých regionech, takže může být blízko uživatelům (nižší latence). Nasazení probíhá přes `fly launch` (vytvoření konfigurace) a `fly deploy` (nasazení image).

### Upstash
Upstash je serverless databázová služba, kde používám hlavně **Upstash Redis**. Redis běží jako plně spravovaná služba – žádné vlastní servery, platí se podle počtu požadavků.  
Hlavní proměnné:
- `UPSTASH_REDIS_URL` – `rediss://...` (Redis protokol přes TLS), používá se ve Spring Bootu jako `spring.data.redis.url`.
- `UPSTASH_REDIS_REST_URL` a `UPSTASH_REDIS_REST_TOKEN` – pro HTTP/REST API (curl, edge funkce apod.).

---

## 2. Základní Kotlin app

```kotlin
package com.example.cloudcomputing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {

    @GetMapping("/")
    fun hello(): Map<String, String> =
        mapOf("message" to "Hello from Kotlin in the cloud 👋")
}
```

Lokálně běží na: `http://localhost:8080`.

---

## 3. Dockerfile a lokální běh

```dockerfile
# 1) build stage – vytvoří JAR
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2) run stage – jen pustí JAR
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build a run:

```bash
docker build -t cloud-ukazka-kot .
docker run -p 8080:8080 cloud-ukazka-kot
```

Aplikace je pak dostupná na `http://localhost:8080`.

---

## 4. Docker Hub + Fly.io

Push na Docker Hub:

```bash
docker login
docker tag cloud-ukazka-kot kaspi44/cloud-ukazka-kot:v1
docker push kaspi44/cloud-ukazka-kot:v1
```

Nasazení na Fly.io:

```bash
fly auth login
fly launch --name cloud-ukazka-lk --no-deploy
fly deploy --image kaspi44/cloud-ukazka-kot:v1
```

---

## 5. Redis (Upstash) – Spring Data Redis

Důležité proměnné z Upstash:

- `UPSTASH_REDIS_URL`  
  např. `rediss://default:VERY_LONG_PASSWORD@ace-colt-32831.upstash.io:6379` (pro Spring Data Redis, TLS).
- `UPSTASH_REDIS_REST_URL` – např. `https://ace-colt-32831.upstash.io` (REST API).
- `UPSTASH_REDIS_REST_TOKEN` – token pro REST API.

### 5.1 Gradle závislosti (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
}
```

### 5.2 `application.properties`

Varianta přes env proměnnou:

```properties
spring.data.redis.url=${UPSTASH_REDIS_URL}
spring.data.redis.ssl.enabled=true
```

Např. v run konfiguraci:

```text
UPSTASH_REDIS_URL=rediss://default:TVOJE_HESLO@ace-colt-32831.upstash.io:6379
```

Pro debug natvrdo:

```properties
spring.data.redis.url=rediss://default:TVOJE_HESLO@ace-colt-32831.upstash.io:6379
spring.data.redis.ssl.enabled=true
```

---

## 6. Redis test controller

```kotlin
package com.example.cloudcomputing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.redis.core.StringRedisTemplate

@RestController
class RedisTestController(
    private val redisTemplate: StringRedisTemplate
) {

    @GetMapping("/set")
    fun set(
        @RequestParam key: String,
        @RequestParam value: String
    ): String {
        redisTemplate.opsForValue().set(key, value)
        return "OK, uloženo $key=$value"
    }

    @GetMapping("/get")
    fun get(@RequestParam key: String): String? {
        return redisTemplate.opsForValue().get(key) ?: "null (nenalezeno)"
    }
}
```

Test:

- `http://localhost:8080/set?key=test&value=ahoj`
- `http://localhost:8080/get?key=test` → odpověď: `ahoj`

Typické chyby:

- `WRONGPASS` – špatně zkopírovaný `UPSTASH_REDIS_URL` / heslo  
- `connection refused` – chybí `spring.data.redis.ssl.enabled=true` nebo špatné URL/port

---

## 7. Upstash REST API (volitelné)

REST rozhraní používá:

- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`

Příklad v Kotlinu:

```kotlin
val restUrl = System.getenv("UPSTASH_REDIS_REST_URL")
val restToken = System.getenv("UPSTASH_REDIS_REST_TOKEN")
```

Pro běžný provoz aplikace stačí klasické připojení přes `spring-data-redis` a `UPSTASH_REDIS_URL`.
