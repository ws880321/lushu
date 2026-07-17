# 自驾游路书应用 MVP 技术设计文档

> 日期: 2026-07-17
> 状态: 待实现
> 作者: wangshuai + Claude

---

## 1. 产品定位与范围

### 一句话定位

为国内自驾游用户提供"从碎片信息到一键生成可执行行程"的智能路书工具，以及途中的补给提醒与周边信息推送。

### 核心差异化

| 竞品 | 痛点 | 我们的解法 |
|------|------|-----------|
| 小红书/马蜂窝 | 游记是碎片的、非结构化的 | 结构化路书：按天、按途经点、带时间轴 |
| 高德/百度地图 | 只管导航，不关心自驾体验 | 自驾友好度评分（停车场、路宽、房车适配） |
| 《示路》 | 蜘蛛网式自由规划，但无途中服务 | MVP 聚焦路书生成 + 展示，V2 加拖拽编辑 + 车队协同 |

### MVP 范围（4-6 周）

**包含：**
- 微信小程序：路书生成页、路书详情页（地图+时间轴）、途中仪表盘页
- 管理后台 Web：POI 标注、路线模板管理
- 后端 API：微信登录、路书生成、周边提醒、POI 搜索

**不包含（V2+）：**
- 车队协同
- 离线地图
- 自由拖拽编辑路线
- 实时位置共享
- 支付/订阅

---

## 2. 技术选型

| 层 | 选型 | 理由 |
|----|------|------|
| 后端框架 | Spring Boot 3.3 + JDK 21 | 虚拟线程提升高德 API 并发调用；AI 对该技术栈代码生成质量最高 |
| 数据库 | MySQL 8.0 + Redis 7 | 空间索引 `ST_Distance_Sphere` 支撑周边搜索 |
| 地图服务 | 高德地图 Web API + 小程序 SDK | 国内精度最高、POI 覆盖最全 |
| 小程序框架 | 微信原生 + Vant Weapp UI | 单人开发效率最优，不引入跨平台框架 |
| 管理后台 | Vue 3 + Element Plus | 快速搭建 CRUD 页面 |
| 部署 | 阿里云 ECS + Docker Compose | MVP (<1000 DAU) 无需 K8s |
| 支付 | 微信支付 JSAPI | 国内自驾用户微信支付覆盖率 95%+ |

### 架构：Spring Boot 单体，按功能分包

```
com.roadbook
  ├── auth/        # 微信登录、JWT
  ├── route/       # 路书生成引擎
  ├── poi/         # POI 管理、自驾评分
  ├── vehicle/     # 车辆信息、续航计算
  ├── alert/       # 周边提醒、地理围栏
  ├── template/    # 路线模板管理
  ├── amap/        # 高德 API 封装
  └── common/      # 通用工具、异常处理
```

每个包内分层：`controller` → `service` → `repository`。

---

## 3. 数据库设计

### ER 关系

```
users ──1:N──▶ routes ──1:N──▶ route_waypoints ──N:1──▶ pois
  │              │
  │              ▼
  │         route_templates ──1:N──▶ template_waypoints
  │
  ├──1:N──▶ vehicles
  │
  └──N:N──▶ caravans (V2)
                └──1:N──▶ caravan_members (V2)

pois ──1:N──▶ poi_ratings
```

### 核心表设计要点

**users** — 用户表。`openid` 唯一索引，`membership` + `member_expire` 预留订阅。

**routes** — 路书主表。关联用户和模板，含起点空间索引、标签 JSON、公开分享标记。

**route_waypoints** — 途经点核心表。承载时间轴（到达/离开时间、停留时长）、补给标记（`is_break_point`）、与前一点距离、按天分组排序。复合索引 `(route_id, day_number, sort_order)` 是最高频查询。

**pois** — 兴趣点表，含自驾友好度字段（`drive_score`、`parking_score`、`road_score`、`rv_friendly`、`signal_quality`、`pet_friendly`）。`source` 区分数据来源（amap/manual/ugc），`amap_poi_id` 做关联桥。空间索引 `SPATIAL sp_coord` 支撑周边搜索。

**vehicles** — 车辆信息，含油/电类型、容量、平均消耗，用于续航预警计算。

**route_templates** — 路线模板，区域 + 天数索引，`usage_count` 记录热度。

**poi_ratings** — UGC 评分明细，`(poi_id, user_id)` 唯一约束每人仅评一次。

**caravans / caravan_members** — V2 车队，MVP 不建。

完整 DDL 见附录 A。

---

## 4. 核心 API 设计

### 4.1 POST /api/v1/routes/generate — 路书生成

**处理流程：**

```
请求 → 参数校验 → 模板匹配(region+days+tags加权) → 高德驾车路径规划
                                                       ↓
                                                 POI增强(查自有表)
                                                       ↓
                                                 途经点排序+时间轴计算
                                                       ↓
                                                 保存 routes+waypoints → 返回JSON
```

**输入：** `{ total_days, start_point, end_point, preferences: { difficulty, tags, daily_drive_hours }, vehicle_info }`

**输出：** `{ route_id, total_distance_km, estimated_cost, weather_alert, itinerary: [{ day, waypoints: [...] }], fuel_stops: [...] }`

**降级策略：** 无精确匹配模板时返回 `40401` + 推荐的天数或区域接近的模板。

### 4.2 GET /api/v1/nearby/alerts — 周边提醒

**处理流程：**

```
当前坐标 → MySQL ST_Distance_Sphere 半径查询 → POI 匹配 → 规则引擎分级 → 返回JSON
```

**参数：** `lng, lat, radius, include_types, vehicle_range_left`

**提醒分级：**
- `critical`：最后补给机会、天气封路
- `warning`：停车难、信号弱、路况差
- `info`：惊喜推荐、路过景点

---

## 5. 前端页面原型

### 5.1 路书生成页（小程序）

- 起终点：地图选点 + 文字搜索
- 偏好设置：难度切换、标签 chip 选择、每日驾驶时长滑块、避开高速开关
- 车辆信息：自动带出或手动输入
- 热门模板：底部横向滚动推荐卡片，与天数联动过滤

### 5.2 路书详情页（小程序）

- 上半屏地图（高德 map 组件 + polyline + marker），Tab 切换 Day1/Day2/Day3 时飞线动画
- 下半屏时间轴列表，每节点含类型图标、停留时间、自驾评分、贴士
- 补给节点橙色高亮，普通景点蓝色
- 节点点击地图联动移动到该点
- "导航到这里"唤起高德地图 App

### 5.3 途中仪表盘页（小程序，MVP 简化）

- 小地图 + 距下一站距离 + 预计到达时间
- 车辆状态卡片：剩余油量/电量可视化、到终点还需油量预估
- 周边补给列表（加油站/餐厅/停车场，含距离和方位）
- 前方提醒卡片（天气/路况/拍照点）
- 车队占位区（V2 开放）

### 5.4 管理后台（Web）

- 左侧导航：仪表盘、路书模板、POI 管理、用户管理、数据统计
- POI 编辑页：表单 + 地图选点 + 自驾友好度评分 slider + tips 文本框

---

## 6. 路书生成引擎：规则模板匹配

MVP 采用规则模板匹配而非 LLM 生成。

**匹配算法：**
1. 按 `region`（从起终点坐标反查省份）+ `total_days` 精确匹配
2. 按 `tags` 交集数量加权排序
3. 无精确匹配时降级推荐天数 ±1 或相邻区域的模板

**首期模板（10 条）：**
川西（3）、云南（2）、西北（2）、华东（1）、贵州（1）、甘南（1）。

**模板数据结构：** 每条模板含 `name, region, total_days, difficulty, tags` + `template_waypoints[]`（含建议停留时长和自驾贴士）。

---

## 7. POI 数据冷启动

**三阶段：**

| 阶段 | 时间 | 方式 | 产出 |
|------|------|------|------|
| 1 | Week 1-2 | 高德 API 实时查询 | 零数据也能跑通 |
| 2 | Week 3-4 | 人工标注种子 POI | 50 个高频路线沿途 POI |
| 3 | Week 5-6 | 模板途经点自动入库 | 10 条模板 × ~8 个途经点 = 80 个 |

**合规说明：**
- 高德 API 搜索结果可缓存但不可全量下载
- 小红书/马蜂窝爬取违反 ToS，**不建议**
- OSM 数据可合法使用（需标注版权）
- UGC 内容需审核机制

---

## 8. 离线能力（MVP）

**策略：** 小程序本地缓存 JSON（文字级），不做地图离线。

出发前自动缓存路书详情、沿途 POI、路线 polyline 到 `wx.setStorage`。使用时先尝试网络，失败后读取本地缓存并显示 "当前离线，数据为出发前版本" banner。

地图离线显示需原生 SDK，V2 Flutter 端再做。

---

## 9. 商业化预留

- `users.membership`（0=免费, 1=月度, 2=年度）+ `member_expire` 已建字段
- `subscription_orders` 表 DDL 已设计，MVP 不建
- 代码层预留 `@RequireMembership` 注解（先注释）
- 免费额度：3 条路书/月；付费不限
- 车队功能仅付费用户可创建

---

## 10. 开发排期

| 周次 | 主题 | 核心产出 | 验收标准 |
|------|------|---------|---------|
| 1 | 基础设施 | 项目骨架、数据库、高德 API 封装、微信登录、Docker Compose | 登录 API 可调通 |
| 2 | 路书引擎 | 模板匹配算法、路书生成 Service、10 条模板数据 | 输入参数返回完整路书 JSON |
| 3 | POI + 管理后台 | POI CRUD、周边搜索、管理后台 POI 编辑页、小程序初始化 | 周边提醒 API 可用 |
| 4 | 小程序路书页 | 登录页、生成页、详情页（地图+时间轴） | 小程序端走通完整闭环 |
| 5 | 途中仪表盘 + 联调 | 仪表盘页、全链路联调、异常处理 | 全流程无阻断 |
| 6 | 测试 + 上线 | 后端集成测试、真机测试、性能优化、部署上线 | 公网可访问 |

---

## 11. 技术风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| **路线模板质量不足** | 🔴 高 | 优先 Top 10 高频路线；无匹配时降级推荐；管理后台模板编辑器优先做；预留 LLM 兜底接口 |
| **微信小程序地图限制** | 🟡 中 | Tab 分天 + marker 聚合；参数化输入替代拖拽编辑；V2 管理后台 Web 端或 Flutter 原生端实现完整交互 |
| **单人 + AI 开发质量一致性** | 🔴 高 | 所有 AI 生成代码走 code-reviewer agent 检查；禁止字符串拼接 SQL；微信支付严格按官方 SDK 接入；上线前 OWASP ZAP 扫描 |

---

## 附录 A：完整 DDL

```sql
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

-- ============================================================
-- 10. 车队表（V2）
-- ============================================================
CREATE TABLE caravans (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL,
    owner_id        BIGINT       NOT NULL,
    invite_code     VARCHAR(8)   NOT NULL,
    route_id        BIGINT       NULL,
    max_members     INT          NOT NULL DEFAULT 10,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_invite_code (invite_code),
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE caravan_members (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    caravan_id          BIGINT       NOT NULL,
    user_id             BIGINT       NOT NULL,
    role                VARCHAR(16)  NOT NULL DEFAULT 'member',
    last_lng            DECIMAL(10,7) NULL,
    last_lat            DECIMAL(10,7) NULL,
    location_updated_at DATETIME     NULL,
    joined_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_caravan_user (caravan_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 11. 订阅支付流水表（V2）
-- ============================================================
CREATE TABLE subscription_orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    order_no          VARCHAR(32)  NOT NULL,
    wx_transaction_id VARCHAR(64)  NULL,
    plan              VARCHAR(16)  NOT NULL COMMENT 'monthly|yearly',
    amount_yuan       INT          NOT NULL COMMENT 'amount in fen',
    status            VARCHAR(16)  NOT NULL COMMENT 'pending|paid|refunded|expired',
    paid_at           DATETIME     NULL,
    expire_at         DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
