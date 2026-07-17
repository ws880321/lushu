# 自驾游路书 MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付自驾游路书 MVP：Spring Boot 后端 + 微信小程序（路书生成/详情/仪表盘）+ Vue 3 管理后台（POI 标注/模板管理），4-6 周单人开发周期。

**Architecture:** Spring Boot 3.3 单体后端，按 com.roadbook.{auth,route,poi,vehicle,alert,template,amap,common} 分包；微信原生小程序 + Vant Weapp；Vue 3 + Element Plus 管理后台。阿里云 ECS Docker Compose 部署。

**Tech Stack:** Spring Boot 3.3, JDK 21 (虚拟线程), MySQL 8.0, Redis 7, 高德 Web API, JWT, 微信小程序原生 + Vant Weapp 1.x, Vue 3.4 + Element Plus 2.x, Docker Compose

## Global Constraints

- JDK 21+ required (虚拟线程用于高德 API 并发调用)
- MySQL 8.0+ required (SPATIAL INDEX 和 ST_Distance_Sphere)
- 所有 API 返回统一 `ApiResponse<T>` 格式 `{code, message, data}`
- 禁止字符串拼接 SQL，统一使用 Spring Data JPA 参数化查询
- 微信小程序使用高德 map 组件的 `polyline` + `markers` 展示路线
- 管理后台使用高德 JS API 2.0 做地图选点
- MVP 不建 caravans/caravan_members/subscription_orders 表
- 每个 Task 结束 commit 一次

---

## File Structure

```
路书/
├── roadbook-server/                         # Spring Boot 后端
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── src/
│       ├── main/java/com/roadbook/
│       │   ├── RoadbookApplication.java
│       │   ├── common/
│       │   │   ├── ApiResponse.java
│       │   │   ├── ErrorCode.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── config/
│       │   │       ├── WebMvcConfig.java
│       │   │       └── RedisConfig.java
│       │   ├── auth/
│       │   │   ├── controller/AuthController.java
│       │   │   ├── service/WechatAuthService.java
│       │   │   ├── dto/LoginRequest.java
│       │   │   ├── dto/LoginResponse.java
│       │   │   ├── interceptor/JwtInterceptor.java
│       │   │   ├── entity/User.java
│       │   │   └── repository/UserRepository.java
│       │   ├── amap/
│       │   │   ├── AmapClient.java
│       │   │   ├── config/AmapConfig.java
│       │   │   └── dto/
│       │   │       ├── GeoCodeResponse.java
│       │   │       ├── PoiSearchResponse.java
│       │   │       ├── DrivingRouteResponse.java
│       │   │       └── WeatherResponse.java
│       │   ├── template/
│       │   │   ├── controller/TemplateController.java
│       │   │   ├── controller/TemplateAdminController.java
│       │   │   ├── service/TemplateService.java
│       │   │   ├── repository/TemplateRepository.java
│       │   │   ├── repository/TemplateWaypointRepository.java
│       │   │   ├── entity/RouteTemplate.java
│       │   │   └── entity/TemplateWaypoint.java
│       │   ├── route/
│       │   │   ├── controller/RouteController.java
│       │   │   ├── service/RouteGenerateService.java
│       │   │   ├── service/RouteService.java
│       │   │   ├── repository/RouteRepository.java
│       │   │   ├── repository/RouteWaypointRepository.java
│       │   │   ├── repository/UserFavoriteRepository.java
│       │   │   ├── dto/RouteGenerateRequest.java
│       │   │   ├── dto/RouteDetailResponse.java
│       │   │   ├── entity/Route.java
│       │   │   ├── entity/RouteWaypoint.java
│       │   │   └── entity/UserFavorite.java
│       │   ├── poi/
│       │   │   ├── controller/PoiController.java
│       │   │   ├── service/PoiService.java
│       │   │   ├── repository/PoiRepository.java
│       │   │   ├── repository/PoiRatingRepository.java
│       │   │   ├── entity/Poi.java
│       │   │   └── entity/PoiRating.java
│       │   ├── vehicle/
│       │   │   ├── controller/VehicleController.java
│       │   │   ├── service/VehicleService.java
│       │   │   ├── repository/VehicleRepository.java
│       │   │   └── entity/Vehicle.java
│       │   └── alert/
│       │       ├── controller/AlertController.java
│       │       ├── service/AlertService.java
│       │       └── dto/AlertResponse.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── db/
│       │       ├── migration/V1__init.sql
│       │       └── seed/V1__seed_templates.sql
│       └── test/java/com/roadbook/
│           ├── route/RouteGenerateServiceTest.java
│           ├── alert/AlertServiceTest.java
│           └── auth/AuthControllerTest.java
│
├── roadbook-miniapp/                         # 微信小程序
│   ├── app.js, app.json, app.wxss
│   ├── project.config.json
│   ├── utils/
│   │   ├── api.js, auth.js, cache.js, map.js
│   ├── pages/
│   │   ├── login/    (login.js, login.json, login.wxml, login.wxss)
│   │   ├── generate/ (generate.js, generate.json, generate.wxml, generate.wxss)
│   │   ├── route-detail/ (route-detail.js, ...)
│   │   └── dashboard/ (dashboard.js, ...)
│   └── components/
│       ├── waypoint-card/
│       └── alert-item/
│
└── roadbook-admin/                           # Vue 3 管理后台
    ├── package.json, vite.config.js, index.html
    └── src/
        ├── main.js, App.vue
        ├── router/index.js
        ├── utils/api.js
        ├── views/
        │   ├── Layout.vue, Dashboard.vue
        │   ├── TemplateList.vue, TemplateEdit.vue
        │   ├── PoiList.vue, PoiEdit.vue
        └── components/
            ├── MapPicker.vue, ScoreSlider.vue
```

---

## Phase 1: 后端基础设施 (Week 1-2)

### Task 1: 项目初始化 + Docker 环境

**Files:**
- Create: `roadbook-server/pom.xml`
- Create: `roadbook-server/Dockerfile`
- Create: `roadbook-server/docker-compose.yml`
- Create: `roadbook-server/src/main/java/com/roadbook/RoadbookApplication.java`
- Create: `roadbook-server/src/main/resources/application.yml`
- Create: `roadbook-server/src/main/resources/application-dev.yml`

**Interfaces:**
- Produces: `RoadbookApplication` Spring Boot 入口; Docker Compose (MySQL 3306, Redis 6379, App 8080)

- [ ] **Step 1: 创建项目目录结构**

```bash
mkdir -p roadbook-server/src/main/java/com/roadbook
mkdir -p roadbook-server/src/main/resources/db/migration
mkdir -p roadbook-server/src/main/resources/db/seed
mkdir -p roadbook-server/src/test/java/com/roadbook
```

- [ ] **Step 2: 编写 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>
    <groupId>com.roadbook</groupId>
    <artifactId>roadbook-server</artifactId>
    <version>0.1.0</version>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 编写 application.yml**

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
  threads:
    virtual:
      enabled: true
server:
  port: 8080
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 604800000
  amap:
    key: ${AMAP_KEY}
```

- [ ] **Step 4: 编写 application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/roadbook?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    username: root
    password: roadbook123
    hikari:
      maximum-pool-size: 10
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    show-sql: true
app:
  jwt:
    secret: dev-secret-key-do-not-use-in-production-123456
  amap:
    key: your-amap-key-here
```

- [ ] **Step 5: 编写 RoadbookApplication.java**

```java
package com.roadbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoadbookApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoadbookApplication.class, args);
    }
}
```

- [ ] **Step 6: 编写 Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 7: 编写 docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: roadbook123
      MYSQL_DATABASE: roadbook
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./src/main/resources/db/migration:/docker-entrypoint-initdb.d
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JWT_SECRET: dev-jwt-secret-change-in-prod
      AMAP_KEY: ${AMAP_KEY}
    depends_on:
      - mysql
      - redis
volumes:
  mysql_data:
```

- [ ] **Step 8: 验证启动**

```bash
cd roadbook-server
docker-compose up -d mysql redis
mvn spring-boot:run
# 预期: Started RoadbookApplication in X seconds
```

- [ ] **Step 9: Commit**

```bash
git add roadbook-server/
git commit -m "feat: initialize Spring Boot 3.3 project with Docker Compose"
```

---

### Task 2: 统一响应体 + 全局异常处理

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/common/ApiResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/common/ErrorCode.java`
- Create: `roadbook-server/src/main/java/com/roadbook/common/GlobalExceptionHandler.java`
- Create: `roadbook-server/src/main/java/com/roadbook/common/config/WebMvcConfig.java`

**Interfaces:**
- Produces: `ApiResponse<T>` — `{code: int, message: str, data: T}`; `ErrorCode` 枚举; `GlobalExceptionHandler` 拦截所有未处理异常

- [ ] **Step 1: 编写 ErrorCode.java**

```java
package com.roadbook.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    TEMPLATE_NOT_FOUND(40401, "未找到匹配的路线模板"),
    INTERNAL_ERROR(50000, "服务器内部错误"),
    AMAP_API_ERROR(50001, "地图服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 编写 ApiResponse.java**

```java
package com.roadbook.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 3: 编写 GlobalExceptionHandler.java**

```java
package com.roadbook.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ApiResponse.error(ErrorCode.BAD_REQUEST, "参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }
}
```

- [ ] **Step 4: 编写 WebMvcConfig.java**

```java
package com.roadbook.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

- [ ] **Step 5: Commit**

---

### Task 3: 数据库初始化 + JPA Entity

**Files:**
- Create: `roadbook-server/src/main/resources/db/migration/V1__init.sql`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/entity/User.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/entity/Route.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/entity/RouteWaypoint.java`
- Create: `roadbook-server/src/main/java/com/roadbook/poi/entity/Poi.java`
- Create: `roadbook-server/src/main/java/com/roadbook/poi/entity/PoiRating.java`
- Create: `roadbook-server/src/main/java/com/roadbook/vehicle/entity/Vehicle.java`
- Create: `roadbook-server/src/main/java/com/roadbook/template/entity/RouteTemplate.java`
- Create: `roadbook-server/src/main/java/com/roadbook/template/entity/TemplateWaypoint.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/entity/UserFavorite.java`

**Interfaces:**
- Produces: 9 个 JPA Entity 对应所有 MVP 表（不含 V2 的 caravans/caravan_members/subscription_orders）

- [ ] **Step 1: 编写 V1__init.sql**

从设计文档附录 A 复制前 9 张表的完整 DDL（users, routes, route_waypoints, pois, poi_ratings, vehicles, route_templates, template_waypoints, user_favorites）。

- [ ] **Step 2: 编写 User.java**

```java
package com.roadbook.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String openid;

    @Column(length = 64)
    private String unionid;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Integer membership = 0;

    private LocalDateTime memberExpire;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: 编写 Route.java**

```java
package com.roadbook.route.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "routes")
public class Route {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(nullable = false, length = 128)
    private String startPoint;

    @Column(nullable = false, length = 128)
    private String endPoint;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLat;

    private Integer totalDistance;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(length = 512)
    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer status = 1;

    private Long templateId;

    @Column(nullable = false)
    private Integer isPublic = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4-9: 编写剩余 7 个 Entity 类**

RouteWaypoint, Poi, PoiRating, Vehicle, RouteTemplate, TemplateWaypoint, UserFavorite — 每个按相同模式：Lombok @Data + @Entity + @Table + 字段映射（snake_case DDL → camelCase Java）+ @PrePersist/@PreUpdate。

- [ ] **Step 10: 验证建表**

```bash
cd roadbook-server
docker-compose down -v && docker-compose up -d mysql
docker-compose exec mysql mysql -uroot -proadbook123 roadbook -e "SHOW TABLES;"
# 预期: 9 张表
```

- [ ] **Step 11: Commit**

---

### Task 4: 高德 API 封装层

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/amap/config/AmapConfig.java`
- Create: `roadbook-server/src/main/java/com/roadbook/amap/AmapClient.java`
- Create: `roadbook-server/src/main/java/com/roadbook/amap/dto/GeoCodeResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/amap/dto/PoiSearchResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/amap/dto/DrivingRouteResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/amap/dto/WeatherResponse.java`

**Interfaces:**
- Produces:
  - `AmapConfig` — 从 `app.amap.key` 读取 key
  - `AmapClient.geocode(String address)` → `GeoCodeResponse`
  - `AmapClient.regeocode(double lng, double lat)` → `GeoCodeResponse`
  - `AmapClient.searchPoi(String keywords, String types, String city, int page)` → `PoiSearchResponse`
  - `AmapClient.drivingRoute(String origin, String destination, String waypoints)` → `DrivingRouteResponse`
  - `AmapClient.weather(String city)` → `WeatherResponse`

- [ ] **Step 1: 编写 AmapConfig.java**

```java
package com.roadbook.amap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.amap")
public class AmapConfig {
    private String key;
}
```

- [ ] **Step 2: 编写 AmapClient.java**

```java
package com.roadbook.amap;

import com.roadbook.amap.config.AmapConfig;
import com.roadbook.amap.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmapClient {
    private final AmapConfig config;
    private final WebClient webClient = WebClient.create("https://restapi.amap.com");

    public GeoCodeResponse geocode(String address) {
        String url = UriComponentsBuilder.fromPath("/v3/geocode/geo")
                .queryParam("key", config.getKey())
                .queryParam("address", address)
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(GeoCodeResponse.class).block();
    }

    public GeoCodeResponse regeocode(double lng, double lat) {
        String location = lng + "," + lat;
        String url = UriComponentsBuilder.fromPath("/v3/geocode/regeo")
                .queryParam("key", config.getKey())
                .queryParam("location", location)
                .queryParam("extensions", "base")
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(GeoCodeResponse.class).block();
    }

    public PoiSearchResponse searchPoi(String keywords, String types, String city, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v3/place/text")
                .queryParam("key", config.getKey())
                .queryParam("keywords", keywords)
                .queryParam("city", city != null ? city : "全国")
                .queryParam("offset", 20)
                .queryParam("page", page)
                .queryParam("extensions", "all");
        if (types != null) builder.queryParam("types", types);
        return webClient.get().uri(builder.build().toUriString()).retrieve()
                .bodyToMono(PoiSearchResponse.class).block();
    }

    public DrivingRouteResponse drivingRoute(String origin, String destination, String waypoints) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v3/direction/driving")
                .queryParam("key", config.getKey())
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("strategy", "10")
                .queryParam("extensions", "all");
        if (waypoints != null && !waypoints.isEmpty())
            builder.queryParam("waypoints", waypoints);
        return webClient.get().uri(builder.build().toUriString()).retrieve()
                .bodyToMono(DrivingRouteResponse.class).block();
    }

    public WeatherResponse weather(String city) {
        String url = UriComponentsBuilder.fromPath("/v3/weather/weatherInfo")
                .queryParam("key", config.getKey())
                .queryParam("city", city)
                .queryParam("extensions", "all")
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(WeatherResponse.class).block();
    }
}
```

- [ ] **Step 3: 编写 4 个 DTO 类**

GeoCodeResponse（含 GeoCode 和 RegeoCode 内部类）、PoiSearchResponse、DrivingRouteResponse、WeatherResponse。每个使用 `@Data` + `@JsonIgnoreProperties(ignoreUnknown = true)`，仅映射业务需要的字段。

- [ ] **Step 4: 验证高德 API 调用**

在 application-dev.yml 设置有效的 AMAP_KEY，启动后用 curl 测试 geocode 和 searchPoi。

- [ ] **Step 5: Commit**

---

### Task 5: 微信登录 + JWT 认证

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/auth/controller/AuthController.java`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/service/WechatAuthService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/dto/LoginRequest.java`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/dto/LoginResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/interceptor/JwtInterceptor.java`
- Create: `roadbook-server/src/main/java/com/roadbook/auth/repository/UserRepository.java`

**Interfaces:**
- Consumes: `User` (Task 3), `ApiResponse`, `ErrorCode` (Task 2)
- Produces:
  - `POST /api/v1/auth/wechat-login` — `{code, nickname?, avatarUrl?}` → `{token, userId, nickname, avatarUrl, membership}`
  - `JwtInterceptor` — 从 `Authorization: Bearer <token>` 解析 userId → `request.setAttribute("userId", userId)`

- [ ] **Step 1: 编写 LoginRequest.java 和 LoginResponse.java**
- [ ] **Step 2: 编写 UserRepository.java**
- [ ] **Step 3: 编写 WechatAuthService.java**

核心逻辑：`login(code, nickname, avatarUrl)` → 调用微信 `jscode2session`（MVP 阶段可用 mock openid） → 查/建 User → `generateToken(user)` 用 jjwt 签发 JWT。

- [ ] **Step 4: 编写 AuthController.java**
- [ ] **Step 5: 编写 JwtInterceptor.java 并注册到 WebMvcConfig**
- [ ] **Step 6: 验证登录 API**

```bash
curl -X POST http://localhost:8080/api/v1/auth/wechat-login \
  -H "Content-Type: application/json" \
  -d '{"code":"test123","nickname":"测试用户"}'
# 预期: {"code":0,"message":"success","data":{"token":"eyJ...","userId":1}}
```

- [ ] **Step 7: Commit**

---

### Task 6: 路线模板数据 + 匹配 Service

**Files:**
- Create: `roadbook-server/src/main/resources/db/seed/V1__seed_templates.sql`
- Create: `roadbook-server/src/main/java/com/roadbook/template/repository/TemplateRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/template/repository/TemplateWaypointRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/template/service/TemplateService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/template/controller/TemplateController.java`

**Interfaces:**
- Consumes: `RouteTemplate`, `TemplateWaypoint` (Task 3)
- Produces:
  - `TemplateService.match(region, totalDays, tags)` → `Optional<RouteTemplate>` 加权匹配
  - `TemplateService.listPopular(int limit)` → 热门模板（按 usage_count DESC）
  - `GET /api/v1/templates?region=&days=` — 模板搜索
  - `GET /api/v1/templates/popular?limit=6` — 热门模板

- [ ] **Step 1: 编写 V1__seed_templates.sql**

插入 10 条种子模板（川西 3、云南 2、西北 2、华东 1、贵州 1、甘南 1），每条含完整的 template_waypoints（8-15 个途经点，含 tips）。

- [ ] **Step 2: 编写 Repository**

TemplateRepository: `findByRegionAndTotalDaysAndStatus(region, days, 1)` + `findPopular(limit)` native query。TemplateWaypointRepository: `findByTemplateIdOrderByDayNumberAscSortOrderAsc(templateId)`。

- [ ] **Step 3: 编写 TemplateService.java（含匹配算法）**

```java
public Optional<RouteTemplate> match(String region, int totalDays, List<String> preferTags) {
    // 1. 精确匹配 region + days
    // 2. 宽松匹配 region + days±1
    // 3. tags 交集加权排序
    // 4. 全部不匹配返回 Optional.empty()
}

public void incrementUsage(Long templateId) {
    // 生成路书时 +1 usage_count
}
```

- [ ] **Step 4: 编写 TemplateController.java**

```java
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {
    // GET / — search by region, days
    // GET /popular — hot templates for generate page
}
```

- [ ] **Step 5: 导入种子数据并验证**

```bash
docker-compose exec -T mysql mysql -uroot -proadbook123 roadbook < src/main/resources/db/seed/V1__seed_templates.sql
docker-compose exec mysql mysql -uroot -proadbook123 roadbook -e "SELECT id,name,region,total_days FROM route_templates;"
# 预期: 10 条
```

- [ ] **Step 6: Commit**

---

### Task 7: 路书生成核心引擎

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/route/dto/RouteGenerateRequest.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/dto/RouteDetailResponse.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/repository/RouteRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/repository/RouteWaypointRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/service/RouteGenerateService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/service/RouteService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/controller/RouteController.java`

**Interfaces:**
- Consumes: `TemplateService` (Task 6), `AmapClient` (Task 4), `Route`, `RouteWaypoint` (Task 3)
- Produces:
  - `RouteGenerateService.generate(userId, request)` → `RouteDetailResponse`
  - `POST /api/v1/routes/generate` — 输入参数 → 返回完整路书
  - `GET /api/v1/routes/{id}` — 路书详情
  - `GET /api/v1/routes?page=&size=` — 我的路书列表

- [ ] **Step 1: 编写 RouteGenerateRequest.java（含校验注解）**

```java
@Data
public class RouteGenerateRequest {
    @NotNull @Min(1) @Max(15) private Integer totalDays;
    @NotNull private PointInfo startPoint;
    @NotNull private PointInfo endPoint;
    private Preferences preferences;
    private VehicleInfo vehicleInfo;

    @Data public static class PointInfo {
        @NotNull private String name;
        @NotNull private BigDecimal lng;
        @NotNull private BigDecimal lat;
    }
    @Data public static class Preferences {
        private String difficulty = "easy";
        private List<String> tags;
        private Boolean preferScenicRoad = true;
        private Double dailyDriveHours = 4.0;
    }
    @Data public static class VehicleInfo {
        private String fuelType;
        private Integer rangeKm;
    }
}
```

- [ ] **Step 2: 编写 RouteDetailResponse.java** — 完整响应 DTO，含 EstimatedCost、DayItinerary、WaypointDetail、FuelStop 内嵌类（均用 @Builder）。

- [ ] **Step 3: 编写 RouteGenerateService.java（核心引擎 ~150 行）**

处理流程：
1. 起点坐标 → `amapClient.regeocode()` → 获取省份
2. 省份 + 天数 + 标签 → `templateService.match()` → 获取模板
3. `templateService.incrementUsage(templateId)`
4. 创建 Route → `routeRepo.save()`
5. 复制 TemplateWaypoint → RouteWaypoint，计算时间轴（按天分组，每天 8:00 出发，按停留时长累加 arrival/departure）
6. 构建 RouteDetailResponse（按天分组 itinerary、提取 fuelStops、估算费用）

- [ ] **Step 4: 编写 RouteService.java + RouteController.java**

RouteService: 列表查询（按 userId 分页）、详情查询（含 waypoints）。
RouteController: `POST /generate`, `GET /{id}`, `GET /`。

- [ ] **Step 5: 验证路书生成完整流程**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/wechat-login \
  -H "Content-Type: application/json" -d '{"code":"test"}' | jq -r '.data.token')

curl -X POST http://localhost:8080/api/v1/routes/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"totalDays":3,"startPoint":{"name":"成都","lng":104.0657,"lat":30.6574},"endPoint":{"name":"成都","lng":104.0657,"lat":30.6574},"preferences":{"tags":["摄影"]}}'
# 预期: {"code":0,"data":{"routeId":1,"itinerary":[{"day":1,"waypoints":[...]}]}}
```

- [ ] **Step 6: Commit**

---

### Task 8: 周边提醒 API

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/poi/repository/PoiRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/alert/service/AlertService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/alert/controller/AlertController.java`
- Create: `roadbook-server/src/main/java/com/roadbook/alert/dto/AlertResponse.java`

**Interfaces:**
- Consumes: `Poi` (Task 3)
- Produces: `GET /api/v1/nearby/alerts?lng=&lat=&radius=&include_types=&vehicle_range_left=` → `AlertResponse`

- [ ] **Step 1: 编写 PoiRepository.java 空间查询**

```java
@Query(value = """
    SELECT *, ST_Distance_Sphere(POINT(:lng, :lat), POINT(lng, lat)) AS distance_m
    FROM pois
    WHERE ST_Distance_Sphere(POINT(:lng, :lat), POINT(lng, lat)) <= :radius
      AND (:categories IS NULL OR category IN (:categoryList))
    ORDER BY distance_m LIMIT 20
    """, nativeQuery = true)
List<Object[]> findNearby(...);
```

- [ ] **Step 2: 编写 AlertService.java**

核心逻辑：
1. 调 PoiRepository.findNearby() 获取半径内 POI
2. 规则引擎分级：
   - `critical`：gas/charging 类型且 vehicleRangeLeft < distance + 50km
   - `warning`：parking 类型
   - `info`：scenic/restaurant 类型
3. 聚合 summary 计数

- [ ] **Step 3: 编写 AlertController.java + AlertResponse.java**

```java
@RestController
@RequestMapping("/api/v1/nearby")
public class AlertController {
    @GetMapping("/alerts")
    public ApiResponse<AlertResponse> getAlerts(
            @RequestParam BigDecimal lng, @RequestParam BigDecimal lat,
            @RequestParam(defaultValue = "30000") int radius,
            @RequestParam(required = false) String includeTypes,
            @RequestParam(required = false) Integer vehicleRangeLeft) {
        // ...
    }
}
```

- [ ] **Step 4: 验证周边提醒 API**

```bash
curl "http://localhost:8080/api/v1/nearby/alerts?lng=101.57&lat=30.04&radius=50000" \
  -H "Authorization: Bearer $TOKEN"
# 预期: {"code":0,"data":{"alerts":[...],"summary":{...}}}
```

- [ ] **Step 5: Commit**

---

### Task 9: 车辆管理 + 收藏 API

**Files:**
- Create: `roadbook-server/src/main/java/com/roadbook/vehicle/repository/VehicleRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/vehicle/service/VehicleService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/vehicle/controller/VehicleController.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/repository/UserFavoriteRepository.java`
- Create: `roadbook-server/src/main/java/com/roadbook/route/controller/FavoriteController.java`

**Interfaces:**
- `POST /api/v1/vehicles` — 添加车辆
- `GET /api/v1/vehicles` — 我的车辆列表
- `PUT /api/v1/vehicles/{id}/default` — 设为默认
- `POST /api/v1/favorites` — `{favType, targetId}` 收藏
- `DELETE /api/v1/favorites?type=&targetId=` — 取消收藏
- `GET /api/v1/favorites?type=` — 收藏列表

- [ ] **Step 1-3: 车辆 CRUD（标准 Spring Data JPA 模式）**
- [ ] **Step 4-6: 收藏 CRUD**
- [ ] **Step 7: Commit**

---

## Phase 2: 管理后台 (Week 3-4)

### Task 10: Vue 3 管理后台初始化

**Files:**
- Create: `roadbook-admin/package.json`, `vite.config.js`, `index.html`
- Create: `roadbook-admin/src/main.js`, `App.vue`, `router/index.js`, `utils/api.js`
- Create: `roadbook-admin/src/views/Layout.vue`

**Interfaces:**
- Produces: Vue 3 + Element Plus 项目骨架，路由 5 个页面（Dashboard, TemplateList, TemplateEdit, PoiList, PoiEdit），axios API 封装含 token 管理，Vite 代理 /api → localhost:8080。

- [ ] **Step 1: `npm create vite@latest roadbook-admin -- --template vue`，安装 element-plus, vue-router, axios, pinia**
- [ ] **Step 2: 编写 vite.config.js（proxy + port 3000）**
- [ ] **Step 3: 编写 main.js（Element Plus + Router 注册）**
- [ ] **Step 4: 编写 router/index.js（5 个子路由）**
- [ ] **Step 5: 编写 utils/api.js（axios 拦截器：自动带 token，401 跳登录）**
- [ ] **Step 6: 编写 Layout.vue（左侧导航 + <router-view>）**
- [ ] **Step 7: 验证 `npm run dev` → localhost:3000 看到管理后台骨架**
- [ ] **Step 8: Commit**

---

### Task 11: POI 管理页面

**Files:**
- Create: `roadbook-admin/src/views/PoiList.vue`
- Create: `roadbook-admin/src/views/PoiEdit.vue`
- Create: `roadbook-admin/src/components/MapPicker.vue`
- Create: `roadbook-admin/src/components/ScoreSlider.vue`
- Create: `roadbook-server/src/main/java/com/roadbook/poi/service/PoiService.java`
- Create: `roadbook-server/src/main/java/com/roadbook/poi/controller/PoiController.java`（admin 路径）

**Interfaces:**
- `GET /api/v1/admin/pois?page=&size=&category=&province=` — 分页列表
- `POST /api/v1/admin/pois` — 创建 POI
- `PUT /api/v1/admin/pois/{id}` — 更新 POI（含自驾友好度）
- `DELETE /api/v1/admin/pois/{id}` — 删除 POI

- [ ] **Step 1-2: 后端 PoiService + PoiController**

```java
@RestController
@RequestMapping("/api/v1/admin/pois")
@RequiredArgsConstructor
public class PoiController {
    // CRUD 4 个端点
}
```

- [ ] **Step 3: PoiList.vue** — 搜索框 + 类别筛选 + 表格（名称/类别/自驾评分/省份）+ 分页 + 新增/编辑/删除按钮
- [ ] **Step 4: MapPicker.vue** — 加载高德 JS API 2.0，显示地图，点击获取坐标，emit 给父组件
- [ ] **Step 5: ScoreSlider.vue** — el-slider 封装，0.5-5.0，步长 0.5，显示星级
- [ ] **Step 6: PoiEdit.vue** — 完整表单：名称、类别、坐标(MapPicker)、省份城市、drive_score/parking_score/road_score(ScoreSlider)、rv_friendly/signal_quality(Select)、pet_friendly/camping_allowed(Switch)、自驾贴士(Textarea)
- [ ] **Step 7: 验证 POI CRUD 全流程**
- [ ] **Step 8: Commit**

---

### Task 12: 模板管理页面

**Files:**
- Create: `roadbook-admin/src/views/TemplateList.vue`
- Create: `roadbook-admin/src/views/TemplateEdit.vue`
- Create: `roadbook-server/src/main/java/com/roadbook/template/controller/TemplateAdminController.java`

**Interfaces:**
- `GET /api/v1/admin/templates` — 模板列表
- `POST /api/v1/admin/templates` — 创建模板
- `PUT /api/v1/admin/templates/{id}` — 编辑模板
- `POST /api/v1/admin/templates/{id}/waypoints` — 添加途经点
- `DELETE /api/v1/admin/templates/{id}/waypoints/{wpId}` — 删除途经点

- [ ] **Step 1-2: 后端 TemplateAdminController**
- [ ] **Step 3: TemplateList.vue** — 表格 + 新增/编辑
- [ ] **Step 4: TemplateEdit.vue** — 模板基本信息 + 途经点内嵌表格（可拖拽排序、地图选点添加、编辑 tips）
- [ ] **Step 5: 验证**
- [ ] **Step 6: Commit**

---

## Phase 3: 微信小程序 (Week 5-6)

### Task 13: 小程序项目初始化

**Files:**
- Create: `roadbook-miniapp/app.js`, `app.json`, `app.wxss`, `project.config.json`
- Create: `roadbook-miniapp/utils/api.js`, `auth.js`, `cache.js`, `map.js`

- [ ] **Step 1: app.json — 4 个页面注册 + 定位权限声明**
- [ ] **Step 2: app.js — globalData(baseUrl, token) + onLaunch 读缓存 token**
- [ ] **Step 3: utils/api.js — wx.request 封装（自动带 token，401 跳登录，统一错误处理）**
- [ ] **Step 4: utils/cache.js — cacheRoute(id, data) / getCachedRoute(id) / isCacheStale(id)**
- [ ] **Step 5: utils/map.js — buildPolyline(waypoints, day) / buildMarkers(waypoints, day)**
- [ ] **Step 6: 微信开发者工具打开验证编译通过**
- [ ] **Step 7: Commit**

---

### Task 14: 小程序登录页

**Files:**
- Create: `roadbook-miniapp/pages/login/login.js`, `login.json`, `login.wxml`, `login.wxss`

- [ ] **Step 1: login.wxml — Logo + 标语 + 微信登录按钮 + 用户协议**
- [ ] **Step 2: login.js — `wx.login()` → `wx.getUserProfile()` → 调 `/auth/wechat-login` → 存 token → 跳转**
- [ ] **Step 3: 验证登录流程**
- [ ] **Step 4: Commit**

---

### Task 15: 路书生成页

**Files:**
- Create: `roadbook-miniapp/pages/generate/generate.js`, `generate.json`, `generate.wxml`, `generate.wxss`

- [ ] **Step 1: generate.wxml**

三大区域卡片：
1. 起终点：起点文字输入 + `wx.chooseLocation` 地图选点 → 终点同理 → 天数 stepper
2. 偏好设置：难度三选一（轻松/中等/挑战）+ 标签横向滚动 chip + 每日驾驶 slider
3. 热门模板：scroll-view 横向卡片，点击直接生成

底部：生成按钮 + 车辆信息（如有）

- [ ] **Step 2: generate.js**

核心逻辑：
- `onStartFocus()`: `wx.chooseLocation()` → 设 startName/startLng/startLat
- `onGenerate()`: 调 `/routes/generate` → cacheRoute() → `wx.navigateTo` 详情页
- `onLoad()`: 调 `/templates/popular` 加载热门模板

- [ ] **Step 3: 验证生成 → 跳转详情**
- [ ] **Step 4: Commit**

---

### Task 16: 路书详情页（地图 + 时间轴）

**Files:**
- Create: `roadbook-miniapp/pages/route-detail/route-detail.js`, `.json`, `.wxml`, `.wxss`
- Create: `roadbook-miniapp/components/waypoint-card/` (4 个文件)

- [ ] **Step 1: route-detail.wxml — 上半屏 map + 下半屏时间轴**

结构：
- `<map>` 组件（polyline + markers 绑定数据）
- 地图浮层：标题 + 距离/天数/费用概览
- Day Tab：横向滚动 `<view>` 列表
- 时间轴：`<scroll-view>` 内每个途经点 = 时间 + 圆点 + waypoint-card 组件

- [ ] **Step 2: route-detail.js**

核心交互：
- `onLoad(id)`: 优先读缓存 → 网络失败用缓存 + toast "当前离线"
- `selectDay(day)`: 更新 polylines、markers、centerLng/Lat
- `onWaypointTap`: `mapCtx.moveToLocation()` 地图移动
- `onMarkerTap`: `wx.showModal` + `wx.openLocation` 导航

- [ ] **Step 3: waypoint-card 组件**

显示：类型图标 + 名称 + 停留时间 + 自驾评分 + tips + 补给提醒标记（橙色边框）

- [ ] **Step 4: 验证详情页全交互（地图加载、切 Tab、点 marker、导航跳转）**
- [ ] **Step 5: Commit**

---

### Task 17: 途中仪表盘页

**Files:**
- Create: `roadbook-miniapp/pages/dashboard/dashboard.js`, `.json`, `.wxml`, `.wxss`
- Create: `roadbook-miniapp/components/alert-item/` (4 个文件)

- [ ] **Step 1: dashboard.wxml**

布局：
1. 小地图（当前位置 + marker）
2. 车辆状态卡片（油量进度条 + 续航文字）
3. 周边补给区域（alert-item 列表）
4. 车队占位区（V2）

- [ ] **Step 2: dashboard.js**

核心逻辑：
- `onShow()`: `wx.getLocation()` → 获取当前位置
- `fetchAlerts()`: 调 `/nearby/alerts` 传入 lng/lat/vehicleRangeLeft
- 补给提醒按 level 着色（critical=红色, warning=橙色, info=蓝色）

- [ ] **Step 3: alert-item 组件** — 按级别着色，显示距离、标题、可点击导航

- [ ] **Step 4: 验证仪表盘**
- [ ] **Step 5: Commit**

---

### Task 18: 后端集成测试

**Files:**
- Create: `roadbook-server/src/test/java/com/roadbook/auth/AuthControllerTest.java`
- Create: `roadbook-server/src/test/java/com/roadbook/route/RouteGenerateServiceTest.java`
- Create: `roadbook-server/src/test/java/com/roadbook/alert/AlertServiceTest.java`

- [ ] **Step 1: AuthControllerTest** — `@SpringBootTest` + `@AutoConfigureMockMvc`，测试登录返回 token
- [ ] **Step 2: RouteGenerateServiceTest** — H2 内存库 + mock AmapClient，测试模板匹配 → 路书生成 → 途经点创建
- [ ] **Step 3: AlertServiceTest** — mock PoiRepository，验证提醒分级（critical/warning/info）
- [ ] **Step 4: `mvn test` → Tests run: X, Failures: 0, Errors: 0**
- [ ] **Step 5: Commit**

---

### Task 19: 阿里云部署

**Files:**
- Create: `roadbook-server/nginx.conf`
- Create: `roadbook-server/docker-compose.prod.yml`

- [ ] **Step 1: docker-compose.prod.yml — 从环境变量读取 JWT_SECRET/AMAP_KEY/MYSQL_ROOT_PASSWORD，所有服务 restart:always**
- [ ] **Step 2: nginx.conf — HTTPS 反代 /api/ → app:8080，/admin/ → 静态文件**

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    location /api/ {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location /admin/ {
        root /var/www/roadbook-admin;
        try_files $uri $uri/ /admin/index.html;
    }
}
server {
    listen 80; server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

- [ ] **Step 3: 部署执行 — ECS 初始化 → scp 代码 → docker-compose up -d → 导入种子 → 验证公网访问**
- [ ] **Step 4: `cd roadbook-admin && npm run build && scp -r dist/ user@host:/var/www/roadbook-admin/`**
- [ ] **Step 5: Commit**

---

## 验证清单

完成所有 19 个 Task 后，完整验证：

1. ✅ 微信登录 → 获取 token
2. ✅ 生成路书 → 返回完整 JSON（按天分组 + 时间轴 + 补给提醒）
3. ✅ 路书详情页 → 地图渲染 + Tab 切换 + marker 弹窗 + 导航跳转
4. ✅ 途中仪表盘 → 当前位置 + 周边补给 + 分级着色
5. ✅ 管理后台 POI → CRUD + 地图选坐标 + 自驾评分
6. ✅ 管理后台模板 → CRUD + 途经点编辑
7. ✅ 离线缓存 → 断网后仍可查看路书详情
8. ✅ API 异常处理 → 401/400/500 返回统一格式
9. ✅ 部署 → 公网 HTTPS 可访问
