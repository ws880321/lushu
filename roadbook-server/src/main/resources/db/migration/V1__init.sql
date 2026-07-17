-- ============================================================
-- V1__init.sql  -- MVP database schema (tables 1-9)
-- ============================================================

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid          VARCHAR(64)  NOT NULL,
    unionid         VARCHAR(64)  NULL,
    nickname        VARCHAR(64)  NOT NULL,
    avatar_url      VARCHAR(512) NULL,
    phone           VARCHAR(20)  NULL,
    membership      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=free 1=monthly 2=yearly',
    member_expire   DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_openid (openid),
    INDEX idx_membership (membership, member_expire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. 路书主表
-- ============================================================
CREATE TABLE routes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(128) NOT NULL,
    description     VARCHAR(1024) NULL,
    total_days      INT          NOT NULL,
    start_point     VARCHAR(128) NOT NULL,
    end_point       VARCHAR(128) NOT NULL,
    start_lng       DECIMAL(10,7) NOT NULL,
    start_lat       DECIMAL(10,7) NOT NULL,
    end_lng         DECIMAL(10,7) NOT NULL,
    end_lat         DECIMAL(10,7) NOT NULL,
    total_distance  INT          NULL,
    tags            JSON         NULL,
    thumbnail_url   VARCHAR(512) NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=draft 2=published 3=archived',
    template_id     BIGINT       NULL,
    is_public       TINYINT      NOT NULL DEFAULT 0,
    view_count      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, status),
    INDEX idx_public (is_public, view_count),
    SPATIAL INDEX sp_start (POINT(start_lng, start_lat)),
    CONSTRAINT fk_route_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. 路书途经点表（核心表）
-- ============================================================
CREATE TABLE route_waypoints (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id        BIGINT       NOT NULL,
    poi_id          BIGINT       NULL,
    sort_order      INT          NOT NULL,
    day_number      INT          NOT NULL,
    point_type      VARCHAR(32)  NOT NULL DEFAULT 'scenic' COMMENT 'scenic|food|hotel|gas|parking|photo|custom',
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512) NULL,
    lng             DECIMAL(10,7) NOT NULL,
    lat             DECIMAL(10,7) NOT NULL,
    arrival_time    TIME         NULL,
    departure_time  TIME         NULL,
    stay_duration   INT          NULL COMMENT 'minutes',
    distance_from_prev INT       NULL COMMENT 'km',
    is_break_point  TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_route (route_id, day_number, sort_order),
    INDEX idx_poi (poi_id),
    SPATIAL INDEX sp_coord (POINT(lng, lat)),
    CONSTRAINT fk_wp_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. POI 兴趣点表（自驾友好度核心）
-- ============================================================
CREATE TABLE pois (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL COMMENT 'scenic|hotel|camping|gas|charging|parking|restaurant|toilet',
    lng             DECIMAL(10,7) NOT NULL,
    lat             DECIMAL(10,7) NOT NULL,
    province        VARCHAR(32)  NULL,
    city            VARCHAR(32)  NULL,
    district        VARCHAR(32)  NULL,
    address         VARCHAR(256) NULL,
    phone           VARCHAR(32)  NULL,
    cover_image     VARCHAR(512) NULL,
    images          JSON         NULL,
    drive_score     DECIMAL(2,1) NULL COMMENT '1.0-5.0',
    parking_score   DECIMAL(2,1) NULL,
    road_score      DECIMAL(2,1) NULL,
    rv_friendly     TINYINT      NULL COMMENT '0=no 1=parking 2=campsite',
    camping_allowed TINYINT      NOT NULL DEFAULT 0,
    signal_quality  TINYINT      NULL COMMENT '1=weak 2=medium 3=strong',
    pet_friendly    TINYINT      NOT NULL DEFAULT 0,
    source          VARCHAR(16)  NOT NULL DEFAULT 'amap' COMMENT 'amap|manual|ugc|crawl',
    amap_poi_id     VARCHAR(64)  NULL,
    confirmed_count INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category, province),
    INDEX idx_drive_score (drive_score),
    SPATIAL INDEX sp_coord (POINT(lng, lat)),
    UNIQUE INDEX uk_amap_poi (amap_poi_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. POI 评分明细表（UGC）
-- ============================================================
CREATE TABLE poi_ratings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    poi_id          BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    drive_score     DECIMAL(2,1) NOT NULL,
    parking_score   DECIMAL(2,1) NULL,
    road_score      DECIMAL(2,1) NULL,
    comment         VARCHAR(512) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_poi_user (poi_id, user_id),
    INDEX idx_poi (poi_id),
    CONSTRAINT fk_rating_poi FOREIGN KEY (poi_id) REFERENCES pois(id),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. 车辆信息表
-- ============================================================
CREATE TABLE vehicles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    name            VARCHAR(64)  NULL,
    brand           VARCHAR(64)  NULL,
    fuel_type       VARCHAR(16)  NOT NULL COMMENT 'gasoline|diesel|electric|hybrid',
    tank_capacity   DECIMAL(8,2) NULL,
    avg_consumption DECIMAL(8,2) NULL,
    range_full      INT          NULL COMMENT 'km',
    plate_number    VARCHAR(16)  NULL,
    is_default      TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    CONSTRAINT fk_vehicle_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. 路线模板表
-- ============================================================
CREATE TABLE route_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    region          VARCHAR(64)  NOT NULL,
    total_days      INT          NOT NULL,
    total_distance  INT          NULL,
    best_season     VARCHAR(64)  NULL,
    difficulty      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=easy 2=medium 3=hard',
    tags            JSON         NULL,
    cover_image     VARCHAR(512) NULL,
    usage_count     INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_region_days (region, total_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. 模板途经点表
-- ============================================================
CREATE TABLE template_waypoints (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id     BIGINT       NOT NULL,
    sort_order      INT          NOT NULL,
    day_number      INT          NOT NULL,
    point_type      VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    lng             DECIMAL(10,7) NOT NULL,
    lat             DECIMAL(10,7) NOT NULL,
    poi_id          BIGINT       NULL,
    stay_duration   INT          NULL,
    tips            VARCHAR(512) NULL,
    INDEX idx_template (template_id, day_number, sort_order),
    CONSTRAINT fk_tw_template FOREIGN KEY (template_id) REFERENCES route_templates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. 用户收藏表
-- ============================================================
CREATE TABLE user_favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    fav_type        VARCHAR(16)  NOT NULL COMMENT 'route|poi|template',
    target_id       BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_type_target (user_id, fav_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
