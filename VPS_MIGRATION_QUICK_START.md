# 🚀 Quick Start: AWS to VPS Migration

## ⚡ Fast Track (Automated)

### Run the Setup Wizard:

```bash
cd /home/muosman/uHabits/uhabits
sudo ./vps-setup-wizard.sh
```

The wizard will:
- ✅ Set up database (MySQL)
- ✅ Create API endpoints (PHP)
- ✅ Configure Nginx
- ✅ Generate secure API key
- ✅ Create test scripts
- ✅ Generate Android config file

**Time:** ~5 minutes

---

## 📝 Manual Steps After Wizard

### 1. Get SSL Certificate (REQUIRED)

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

### 2. Test API

```bash
cd /var/www/your-domain.com
./test-api.sh
```

Expected output:
```json
{"status":"healthy","message":"uHabits sync endpoint is operational",...}
```

### 3. Update Android App

**File:** `uhabits-android/src/main/assets/cloud_config.json`

Replace with the generated config from:
```bash
cat /var/www/your-domain.com/public/cloud_config.json
```

### 4. Update Dashboard HTML Files

Replace all AWS URLs with your domain in:
- `enhanced_dashboard.html` (lines 543, 643, 667)
- `group_analytics_dashboard.html` (lines 1337, 1519)

**Find:**
```javascript
'https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups'
```

**Replace with:**
```javascript
'https://your-domain.com/api/groups'
```

Or use this command:
```bash
cd /home/muosman/uHabits/uhabits

# Replace in enhanced_dashboard.html
sed -i 's|https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod|https://your-domain.com|g' enhanced_dashboard.html

# Replace in group_analytics_dashboard.html
sed -i 's|https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod|https://your-domain.com|g' group_analytics_dashboard.html
```

### 5. Update CloudSyncManager.kt

**File:** `uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt`

**Lines 93-96:** Change default config baseUrl to your domain

---

## 🧪 Testing Checklist

```bash
# 1. Health check
curl -H "x-api-key: YOUR_API_KEY" https://your-domain.com/api/sync.php

# 2. Sync test
curl -X POST https://your-domain.com/api/sync.php \
  -H "Content-Type: application/json" \
  -H "x-api-key: YOUR_API_KEY" \
  -d @test-sync-payload.json

# 3. Database check
mysql -u uhabits_user -p
> USE uhabits_analytics;
> SELECT COUNT(*) FROM habit_syncs;
> SELECT * FROM habit_syncs LIMIT 1;

# 4. Android app test
# Install updated APK and trigger a sync from the app
```

---

## 🔄 URLs to Update

| Component | Old AWS URL | New VPS URL |
|-----------|-------------|-------------|
| **Android Sync** | `https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync` | `https://your-domain.com/api/sync.php` |
| **Dashboard API** | `https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups` | `https://your-domain.com/api/groups` |
| **Health Check** | AWS Lambda | `https://your-domain.com/api/sync.php` (GET) |

---

## 📁 Files to Update

1. ✅ **Android App:**
   - `uhabits-android/src/main/assets/cloud_config.json`
   - `uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt`

2. ✅ **Dashboards:**
   - `enhanced_dashboard.html`
   - `group_analytics_dashboard.html`

3. ✅ **Build new APK:**
   ```bash
   cd /home/muosman/uHabits/uhabits
   ./gradlew :uhabits-android:assembleDebug
   ```

---

## 🔍 Monitoring

### View API Logs:
```bash
tail -f /var/www/your-domain.com/logs/access.log
tail -f /var/www/your-domain.com/logs/error.log
```

### View Database Activity:
```bash
mysql -u uhabits_user -p -e "
  SELECT 
    DATE(created_at) as date,
    COUNT(*) as syncs,
    COUNT(DISTINCT habit_name) as unique_habits
  FROM uhabits_analytics.habit_syncs
  GROUP BY DATE(created_at)
  ORDER BY date DESC
  LIMIT 7;
"
```

### Check API Response Time:
```bash
curl -o /dev/null -s -w "Total: %{time_total}s\n" \
  -H "x-api-key: YOUR_API_KEY" \
  https://your-domain.com/api/sync.php
```

---

## ⚠️ Troubleshooting

### API Returns 500 Error:
```bash
# Check PHP error log
sudo tail -f /var/log/php8.2-fpm.log

# Check Nginx error log
sudo tail -f /var/log/nginx/error.log

# Test database connection
php -r "new PDO('mysql:host=localhost;dbname=uhabits_analytics', 'uhabits_user', 'your_password');"
```

### Database Connection Failed:
```bash
# Check MySQL is running
sudo systemctl status mysql

# Test connection
mysql -u uhabits_user -p -e "SELECT 1;"

# Check permissions
mysql -u root -p -e "SHOW GRANTS FOR 'uhabits_user'@'localhost';"
```

### Nginx 502 Bad Gateway:
```bash
# Check PHP-FPM is running
sudo systemctl status php8.2-fpm

# Restart services
sudo systemctl restart php8.2-fpm
sudo systemctl restart nginx
```

### Android App Can't Connect:
1. Check SSL certificate: Visit `https://your-domain.com/api/sync.php` in browser
2. Check API key is correct in Android config
3. Check Android app has internet permission
4. Try sync from browser first to isolate issue

---

## 💾 Backup & Recovery

### Create Backup:
```bash
# Database backup
mysqldump -u uhabits_user -p uhabits_analytics | gzip > backup_$(date +%Y%m%d).sql.gz

# Files backup
tar -czf files_backup_$(date +%Y%m%d).tar.gz /var/www/your-domain.com
```

### Restore from Backup:
```bash
# Database restore
gunzip < backup_20241120.sql.gz | mysql -u uhabits_user -p uhabits_analytics

# Files restore
tar -xzf files_backup_20241120.tar.gz -C /
```

---

## 📊 Performance Tuning

### MySQL Optimization:
```bash
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf

# Add:
[mysqld]
innodb_buffer_pool_size = 256M
max_connections = 50
query_cache_size = 32M
query_cache_limit = 2M
```

### PHP-FPM Tuning:
```bash
sudo nano /etc/php/8.2/fpm/pool.d/www.conf

# Adjust:
pm.max_children = 10
pm.start_servers = 2
pm.min_spare_servers = 1
pm.max_spare_servers = 3
```

### Nginx Caching:
```nginx
# Add to nginx config
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

---

## 🔐 Security Hardening

```bash
# 1. Enable UFW firewall
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 2. Disable MySQL remote access
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
# Ensure: bind-address = 127.0.0.1

# 3. Set up fail2ban
sudo apt install fail2ban -y
sudo systemctl enable fail2ban

# 4. Regular updates
sudo apt update && sudo apt upgrade -y
```

---

## ✅ Migration Verification

Run this checklist after migration:

```bash
#!/bin/bash
echo "=== uHabits VPS Migration Verification ==="
echo ""

# 1. Database
mysql -u uhabits_user -p -e "SELECT 'Database OK' as status;" 2>/dev/null && echo "✅ Database" || echo "❌ Database"

# 2. API Health
[ $(curl -s -o /dev/null -w "%{http_code}" -H "x-api-key: YOUR_KEY" https://your-domain.com/api/sync.php) = "200" ] && echo "✅ API Health" || echo "❌ API Health"

# 3. SSL Certificate
[ $(curl -s -o /dev/null -w "%{http_code}" https://your-domain.com) = "200" ] && echo "✅ SSL Certificate" || echo "❌ SSL Certificate"

# 4. Nginx
sudo systemctl is-active --quiet nginx && echo "✅ Nginx Running" || echo "❌ Nginx Down"

# 5. PHP-FPM
sudo systemctl is-active --quiet php8.2-fpm && echo "✅ PHP-FPM Running" || echo "❌ PHP-FPM Down"

echo ""
echo "=== End Verification ==="
```

---

## 🎯 Success Criteria

Your migration is successful when:

- ✅ API health check returns 200
- ✅ Sync test returns success
- ✅ Database stores habit data
- ✅ Android app connects and syncs
- ✅ Dashboard displays data
- ✅ SSL certificate valid
- ✅ All URLs updated

---

## 📞 Need Help?

1. **Check logs first:** API, Nginx, PHP, MySQL
2. **Test each component:** Database → API → Android → Dashboard
3. **Use test scripts:** Run automated tests
4. **Compare with AWS:** What worked differently?

**Estimated Total Time:** 30-60 minutes (including testing)

---

**Ready?** Run `sudo ./vps-setup-wizard.sh` to begin! 🚀
