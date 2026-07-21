#!/bin/bash
# 路书 MVP 一键部署（阿里云轻量服务器 Ubuntu）
set -e
echo "=== 路书部署 ==="

apt-get update -qq
apt-get install -y -qq openjdk-21-jre-headless mysql-server-8.0 nginx redis-server 2>/dev/null || true

mysql -e "CREATE DATABASE IF NOT EXISTS roadbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
mysql -e "CREATE USER IF NOT EXISTS 'roadbook'@'localhost' IDENTIFIED BY 'R0adB00k!2024'; GRANT ALL ON roadbook.* TO 'roadbook'@'localhost'; FLUSH PRIVILEGES;" 2>/dev/null || true

mkdir -p /opt/roadbook
mkdir -p /opt/roadbook/uploads
chmod 755 /opt/roadbook/uploads
cat > /opt/roadbook/application-prod.yml << 'YML'
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/roadbook?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    username: roadbook
    password: R0adB00k!2024
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    hibernate:
      ddl-auto: update
server:
  port: 8080
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 604800000
  amap:
    key: ${AMAP_KEY}
  wechat:
    appid: wx779bb87ab22d48fb
    secret: ${WECHAT_SECRET}
  deepseek:
    key: ${DEEPSEEK_KEY}
    url: https://api.deepseek.com/v1/chat/completions
YML

cat > /etc/systemd/system/roadbook.service << 'UNIT'
[Unit]
Description=Roadbook API
After=network.target mysql.service redis.service
[Service]
Type=simple
WorkingDirectory=/opt/roadbook
Environment="JWT_SECRET=R0adB00k!JWT!Secret!2024!ChangeMe"
Environment="AMAP_KEY=your-amap-key"
Environment="WECHAT_SECRET=your-wechat-secret"
Environment="DEEPSEEK_KEY=your-deepseek-key"
Environment="SPRING_PROFILES_ACTIVE=prod"
ExecStart=/usr/bin/java -jar /opt/roadbook/roadbook-server.jar
Restart=on-failure
[Install]
WantedBy=multi-user.target
UNIT

cat > /etc/nginx/sites-available/roadbook << 'NGX'
server {
    listen 80;
    server_name _;
    client_max_body_size 10m;
    location /api/ { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /admin/ { alias /opt/roadbook/admin/; index index.html; try_files $uri $uri/ /admin/index.html; }
    location /uploads/ { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; }
    location /share/ { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; }
    location / { return 301 /admin/; }
}
NGX
ln -sf /etc/nginx/sites-available/roadbook /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

systemctl daemon-reload
systemctl enable roadbook redis-server mysql nginx
systemctl restart redis-server mysql
sleep 2
systemctl restart roadbook
sleep 5

echo "=== 部署完成 ==="
echo "管理后台: http://$(curl -s ifconfig.me)/admin/"
echo "API: http://$(curl -s ifconfig.me)/api/v1/"
