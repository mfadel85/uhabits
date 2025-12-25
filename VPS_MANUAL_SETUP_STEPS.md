# 🔧 Manual VPS Setup Steps (Execute via SSH)

Execute these commands **on your VPS via SSH**.

---

## 📋 Prerequisites Check

```bash
# SSH into your VPS first
ssh your_username@your-vps-ip

# Check what's already installed
php -v                    # Should show PHP 8.2
node -v                   # Node.js version
nginx -v                  # Nginx version
mysql --version           # Check if MySQL installed

# Check your domain
curl -I http://kpitracker.quest
```

---

## 🗄️ Step 1: Database Setup (MySQL)

### Install MySQL (if not installed):
```bash
sudo apt update
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure installation
sudo mysql_secure_installation
# Answer: 
# - Set root password? Y (choose strong password)
# - Remove anonymous users? Y
# - Disallow root login remotely? Y
# - Remove test database? Y
# - Reload privilege tables? Y
```

### Create Database and User:
```bash
sudo mysql -u root -p
```

Then in MySQL prompt, run:
```sql
CREATE DATABASE uhabits_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'uhabits_user'@'localhost' IDENTIFIED BY 'ye7leCek1_2fut';

GRANT ALL PRIVILEGES ON uhabits_analytics.* TO 'uhabits_user'@'localhost';

FLUSH PRIVILEGES;

USE uhabits_analytics;

-- Create tables
CREATE TABLE habit_syncs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    sync_timestamp BIGINT NOT NULL,
    habit_name VARCHAR(255) NOT NULL,
    habit_group VARCHAR(100),
    priority INT,
    category VARCHAR(100),
    frequency INT,
    score DECIMAL(10,2),
    checkmarks JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_habit_name (habit_name),
    INDEX idx_sync_timestamp (sync_timestamp),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analytics_summary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    sync_timestamp BIGINT NOT NULL,
    total_habits INT,
    active_habits INT,
    total_checkmarks INT,
    average_score DECIMAL(10,2),
    priority_distribution JSON,
    category_distribution JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_sync (user_id, sync_timestamp),
    INDEX idx_user_id (user_id),
    INDEX idx_sync_timestamp (sync_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE habit_performance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    habit_name VARCHAR(255) NOT NULL,
    date_recorded DATE NOT NULL,
    completion_rate DECIMAL(5,2),
    streak_days INT,
    score DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_habit_date (user_id, habit_name, date_recorded),
    INDEX idx_user_habit (user_id, habit_name),
    INDEX idx_date (date_recorded)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verify tables created
SHOW TABLES;

-- Exit MySQL
EXIT;
```

### Test Database Connection:
```bash
mysql -u uhabits_user -p
# Enter password when prompted
# Type: EXIT
```

**✅ Save your password!** Write down:
- Database: `uhabits_analytics`
- Username: `uhabits_user`
- Password: `ye7leCek1_2fut`

---

## 🔐 Step 2: Generate Secure API Key

```bash
# Generate a secure 32-character API key
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

**Output example:** `a7K9mP3xR5tY8vW2nQ6sL4jH1fG0cB9Z`

**✅ Save this API key!** You'll need it in multiple places.
Z2yITovrkGIOgWlOW4704gvtzeSueNT8
---

## 📁 Step 3: Create Directory Structure

```bash
# Create directories for kpitracker.quest
sudo mkdir -p /var/www/kpitracker.quest/{api,public,logs,backups}

# Set permissions
sudo chown -R $USER:www-data /var/www/kpitracker.quest
sudo chmod -R 755 /var/www/kpitracker.quest

# Verify
ls -la /var/www/kpitracker.quest
```

---

## 🚀 Step 4: Create PHP API Backend

### Create the main sync API file:
```bash
sudo nano /var/www/kpitracker.quest/api/sync.php
```

**Paste this complete code** (replace placeholders):

```php
<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, x-api-key, X-Api-Key');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Configuration - CHANGE THESE!
define('DB_HOST', 'localhost');
define('DB_NAME', 'uhabits_analytics');
define('DB_USER', 'uhabits_user');
define('DB_PASS', 'ye7leCek1_2fut');  // ← Change this!
define('API_KEY', 'Z2yITovrkGIOgWlOW4704gvtzeSueNT8');             // ← Change this!

// API Key authentication
$headers = getallheaders();
$providedKey = $headers['x-api-key'] ?? $headers['X-Api-Key'] ?? '';

if ($providedKey !== API_KEY) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized - Invalid API key']);
    exit();
}

// Database connection
try {
    $pdo = new PDO(
        "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
        DB_USER,
        DB_PASS,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    error_log("DB Error: " . $e->getMessage());
    exit();
}

// Handle GET request (health check)
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode([
        'status' => 'healthy',
        'message' => 'uHabits sync endpoint is operational',
        'timestamp' => date('c'),
        'service' => 'uhabits-sync-vps',
        'version' => '1.0',
        'database' => 'connected'
    ]);
    exit();
}

// Handle POST request (sync data)
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (!$data) {
        http_response_code(400);
        echo json_encode(['error' => 'Invalid JSON payload']);
        exit();
    }

    // Validate required fields
    $required = ['user_id', 'sync_timestamp', 'habits_data'];
    foreach ($required as $field) {
        if (!isset($data[$field])) {
            http_response_code(400);
            echo json_encode(['error' => "Missing required field: $field"]);
            exit();
        }
    }

    try {
        $pdo->beginTransaction();

        $userId = $data['user_id'];
        $syncTimestamp = $data['sync_timestamp'];
        $habitsData = $data['habits_data'];
        
        // Store summary metrics
        if (isset($data['summary_metrics'])) {
            $stmt = $pdo->prepare("
                INSERT INTO analytics_summary 
                (user_id, sync_timestamp, total_habits, active_habits, 
                 total_checkmarks, average_score, priority_distribution, category_distribution)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                total_habits = VALUES(total_habits),
                active_habits = VALUES(active_habits),
                total_checkmarks = VALUES(total_checkmarks),
                average_score = VALUES(average_score),
                priority_distribution = VALUES(priority_distribution),
                category_distribution = VALUES(category_distribution)
            ");

            $summary = $data['summary_metrics'];
            $stmt->execute([
                $userId,
                $syncTimestamp,
                $summary['total_habits'] ?? 0,
                $summary['active_habits'] ?? 0,
                $summary['total_checkmarks'] ?? 0,
                $summary['average_score'] ?? 0,
                json_encode($data['priority_distribution'] ?? []),
                json_encode($summary['category_distribution'] ?? [])
            ]);
        }

        // Store individual habit data
        $stmt = $pdo->prepare("
            INSERT INTO habit_syncs 
            (user_id, sync_timestamp, habit_name, habit_group, priority, 
             category, frequency, score, checkmarks)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");

        $processedHabits = 0;
        foreach ($habitsData as $habit) {
            $stmt->execute([
                $userId,
                $syncTimestamp,
                $habit['name'] ?? '',
                $habit['group'] ?? 'Default',
                $habit['priority'] ?? 3,
                $habit['category'] ?? 'Uncategorized',
                $habit['frequency'] ?? 0,
                $habit['score'] ?? 0,
                json_encode($habit['checkmarks'] ?? [])
            ]);
            $processedHabits++;
        }

        $pdo->commit();

        echo json_encode([
            'status' => 'success',
            'message' => 'Habit data synced successfully',
            'timestamp' => date('c'),
            'processed' => [
                'habits_count' => $processedHabits,
                'user_id' => $userId
            ]
        ]);

    } catch (Exception $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Sync failed: ' . $e->getMessage()]);
        error_log("Sync error: " . $e->getMessage());
    }

    exit();
}

// Method not allowed
http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
?>
```

**Save:** Press `Ctrl+X`, then `Y`, then `Enter`

### Create API endpoint for analytics/groups:
```bash
sudo nano /var/www/kpitracker.quest/api/groups.php
```

**Paste this code:**

```php
<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, x-api-key');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Configuration - CHANGE THESE!
define('DB_HOST', 'localhost');
define('DB_NAME', 'uhabits_analytics');
define('DB_USER', 'uhabits_user');
define('DB_PASS', 'ye7leCek1_2fut');  // ← Change this!
define('API_KEY', 'Z2yITovrkGIOgWlOW4704gvtzeSueNT8');  

// API Key authentication (optional for GET, but recommended)
$headers = getallheaders();
$providedKey = $headers['x-api-key'] ?? $headers['X-Api-Key'] ?? '';

if ($providedKey !== API_KEY) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized']);
    exit();
}

try {
    $pdo = new PDO(
        "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
        DB_USER,
        DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    // Get grouped habit data
    $stmt = $pdo->query("
        SELECT 
            habit_group,
            COUNT(DISTINCT habit_name) as habit_count,
            AVG(score) as avg_score,
            GROUP_CONCAT(
                CONCAT('{\"name\":\"', habit_name, 
                       '\",\"priority\":', COALESCE(priority, 3),
                       ',\"score\":', COALESCE(score, 0),
                       ',\"category\":\"', COALESCE(category, 'Uncategorized'), '\"}')
                SEPARATOR ','
            ) as habits_json
        FROM habit_syncs
        WHERE user_id = 'user_primary'
        GROUP BY habit_group
        ORDER BY avg_score DESC
    ");

    $groups = [];
    $totalHabits = 0;
    
    while ($row = $stmt->fetch()) {
        $groups[] = [
            'group_name' => $row['habit_group'],
            'habit_count' => (int)$row['habit_count'],
            'avg_score' => round((float)$row['avg_score'], 2),
            'habits' => json_decode('[' . $row['habits_json'] . ']')
        ];
        $totalHabits += (int)$row['habit_count'];
    }

    echo json_encode([
        'status' => 'success',
        'data' => [
            'groups' => $groups,
            'summary' => [
                'total_habits_analyzed' => $totalHabits,
                'total_groups' => count($groups)
            ]
        ]
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Failed to fetch analytics: ' . $e->getMessage()]);
}
?>
```

**Save:** Press `Ctrl+X`, then `Y`, then `Enter`

---

## 🌐 Step 5: Configure Nginx

### Create Nginx site configuration:
```bash
sudo nano /etc/nginx/sites-available/kpitracker.quest
```

**Paste this configuration:**

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name kpitracker.quest;

    root /var/www/kpitracker.quest/public;
    index index.html index.php;

    access_log /var/www/kpitracker.quest/logs/access.log;
    error_log /var/www/kpitracker.quest/logs/error.log;

    # CORS headers
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Content-Type, x-api-key, X-Api-Key" always;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Handle OPTIONS preflight
    if ($request_method = OPTIONS) {
        return 204;
    }

    # API endpoints
    location /api/ {
        root /var/www/kpitracker.quest;
        
        location ~ \.php$ {
            include snippets/fastcgi-php.conf;
            fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
            fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
            include fastcgi_params;
        }
        
        try_files $uri $uri/ =404;
    }

    # Main site
    location / {
        try_files $uri $uri/ =404;
    }
}
```

**Save:** Press `Ctrl+X`, then `Y`, then `Enter`

### Enable the site:
```bash
# Create symbolic link
sudo ln -s /etc/nginx/sites-available/kpitracker.quest /etc/nginx/sites-enabled/

# Remove default site if it exists
sudo rm /etc/nginx/sites-enabled/default

# Test Nginx configuration
sudo nginx -t

# If test passes, reload Nginx
sudo systemctl reload nginx
```

---

## 🔒 Step 6: Get SSL Certificate (Let's Encrypt)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get certificate for kpitracker.quest
sudo certbot --nginx -d kpitracker.quest

# Follow prompts:
# - Enter email
# - Agree to terms (A)
# - Share email? (Y/N - your choice)
# - Redirect HTTP to HTTPS? (2 - recommended)
```

**Certbot will:**
- Get SSL certificate
- Configure Nginx for HTTPS
- Set up auto-renewal

### Verify SSL:
```bash
# Test auto-renewal
sudo certbot renew --dry-run

# Check certificate
sudo certbot certificates
```

---

## ✅ Step 7: Test Your API

### Test 1: Health Check
```bash
# HTTP (before SSL)
curl -X GET "http://kpitracker.quest/api/sync.php" \
  -H "x-api-key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8"

# HTTPS (after SSL)
curl -X GET "https://kpitracker.quest/api/sync.php" \
  -H "x-api-key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8"
```

**Expected output:**
```json
{
    "status": "healthy",
    "message": "uHabits sync endpoint is operational",
    "timestamp": "2024-11-20T...",
    "service": "uhabits-sync-vps",
    "version": "1.0",
    "database": "connected"
}
```

### Test 2: Sync Endpoint
```bash
curl -X POST "https://kpitracker.quest/api/sync.php" \
  -H "Content-Type: application/json" \
  -H "x-api-key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8" \
  -d '{
    "user_id": "test_user",
    "sync_timestamp": 1700000000000,
    "habits_data": [{
        "name": "Test Habit",
        "group": "Health",
        "priority": 1,
        "category": "Fitness",
        "score": 85.5,
        "frequency": 7,
        "checkmarks": []
    }],
    "summary_metrics": {
        "total_habits": 1,
        "active_habits": 1,
        "total_checkmarks": 0,
        "average_score": 85.5
    }
  }'
```

**Expected output:**
```json
{
    "status": "success",
    "message": "Habit data synced successfully",
    "timestamp": "2024-11-20T...",
    "processed": {
        "habits_count": 1,
        "user_id": "test_user"
    }
}
```

### Test 3: Verify Data in Database
```bash
mysql -u uhabits_user -p
```

```sql
USE uhabits_analytics;
SELECT * FROM habit_syncs ORDER BY created_at DESC LIMIT 5;
SELECT COUNT(*) as total_records FROM habit_syncs;
EXIT;
```

---

## 📝 Step 8: Create Config Files

### Create Android config file:
```bash
cat > /var/www/kpitracker.quest/public/cloud_config.json << 'EOF'
{
    "aws_config": {
        "region": "custom-vps",
        "environment": "production",
        "api_gateway": {
            "base_url": "https://kpitracker.quest",
            "sync_endpoint": "/api/sync.php",
            "api_key": "Z2yITovrkGIOgWlOW4704gvtzeSueNT8",
            "timeout_seconds": 30
        }
    }
}
EOF

# Make it web accessible
sudo chmod 644 /var/www/kpitracker.quest/public/cloud_config.json
```

### Download config file to your laptop:
```bash
# From your laptop, run:
scp your_username@your-vps-ip:/var/www/kpitracker.quest/public/cloud_config.json ~/Downloads/
```

---

## 🔍 Step 9: Create Monitoring Script

```bash
nano /var/www/kpitracker.quest/check-health.sh
```

**Paste:**
```bash
#!/bin/bash

echo "=== uHabits API Health Check ==="
echo ""

# Check database
mysql -u uhabits_user -p'ye7leCek1_2fut' -e "SELECT COUNT(*) as records FROM uhabits_analytics.habit_syncs;" 2>/dev/null && echo "✅ Database: Connected" || echo "❌ Database: Failed"

# Check API
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "x-api-key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8" https://kpitracker.quest/api/sync.php)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ API: Healthy (HTTP $HTTP_CODE)"
else
    echo "❌ API: Failed (HTTP $HTTP_CODE)"
fi

# Check disk space
DISK=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK" -lt 80 ]; then
    echo "✅ Disk Space: $DISK% used"
else
    echo "⚠️ Disk Space: $DISK% used (Warning)"
fi

# Check Nginx
systemctl is-active --quiet nginx && echo "✅ Nginx: Running" || echo "❌ Nginx: Down"

# Check PHP-FPM
systemctl is-active --quiet php8.2-fpm && echo "✅ PHP-FPM: Running" || echo "❌ PHP-FPM: Down"

echo ""
echo "Recent syncs:"
mysql -u uhabits_user -p'ye7leCek1_2fut' uhabits_analytics -e "
SELECT 
    DATE(created_at) as date,
    COUNT(*) as syncs,
    COUNT(DISTINCT habit_name) as unique_habits
FROM habit_syncs
GROUP BY DATE(created_at)
ORDER BY date DESC
LIMIT 7;
" 2>/dev/null
```

**Make executable:**
```bash
chmod +x /var/www/kpitracker.quest/check-health.sh

# Test it
/var/www/kpitracker.quest/check-health.sh
```

---

## 📊 Step 10: Summary & Next Steps

### What you've completed:
- ✅ MySQL database with tables
- ✅ PHP API endpoints (sync.php, groups.php)
- ✅ Nginx web server configured
- ✅ SSL certificate installed
- ✅ API tested and working
- ✅ Monitoring script created

### Your VPS URLs:
- **API Sync:** `https://kpitracker.quest/api/sync.php`
- **Analytics:** `https://kpitracker.quest/api/groups.php`
- **Config:** `https://kpitracker.quest/cloud_config.json`

### Your Credentials:
```
Database:
  - Name: uhabits_analytics
  - User: uhabits_user
  - Pass: ye7leCek1_2fut

API:
  - Key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8
  - Domain: https://kpitracker.quest
```

---

## 🔄 What to Update on Your Laptop

Now on your laptop, you need to update these files:

### 1. Android App Config:
Replace `uhabits-android/src/main/assets/cloud_config.json` with your downloaded config

### 2. CloudSyncManager.kt:
Update lines 93-96 to use your domain instead of AWS URL

### 3. Dashboard HTML files:
```bash
# On your laptop, in the uHabits directory:
cd /home/muosman/uHabits/uhabits

# Replace AWS URLs with kpitracker.quest
sed -i 's|https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod|https://kpitracker.quest|g' enhanced_dashboard.html

sed -i 's|https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod|https://kpitracker.quest|g' group_analytics_dashboard.html

# Update the sync endpoint in CloudSyncManager.kt
sed -i 's|https://jodcprzip3.execute-api.eu-central-1.amazonaws.com/prod/sync|https://kpitracker.quest/api/sync.php|g' uhabits-android/src/main/java/org/isoron/uhabits/sync/CloudSyncManager.kt
```

### 4. Rebuild APK:
```bash
cd /home/muosman/uHabits/uhabits
./gradlew clean
./gradlew :uhabits-android:assembleDebug
```

---

## 🎯 Quick Reference Commands

**On VPS:**
```bash
# View API logs
tail -f /var/www/kpitracker.quest/logs/access.log

# View error logs
tail -f /var/www/kpitracker.quest/logs/error.log

# Check database
mysql -u uhabits_user -p uhabits_analytics

# Health check
curl -H "x-api-key: Z2yITovrkGIOgWlOW4704gvtzeSueNT8" https://kpitracker.quest/api/sync.php

# Restart services
sudo systemctl restart nginx
sudo systemctl restart php8.2-fpm
```

---

## 🆘 Troubleshooting

### If API returns 500:
```bash
sudo tail -f /var/log/php8.2-fpm.log
sudo tail -f /var/log/nginx/error.log
```

### If database connection fails:
```bash
mysql -u uhabits_user -p
# Test if you can connect

sudo systemctl status mysql
```

### If SSL doesn't work:
```bash
sudo certbot certificates
sudo systemctl status nginx
```

---

**Done!** Your VPS is now set up and ready to replace AWS services! 🎉

Next: Update your Android app and dashboards on your laptop, then test the complete flow.
