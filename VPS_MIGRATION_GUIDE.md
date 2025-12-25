# 🚀 AWS to VPS Migration Guide for uHabits Analytics

## 📋 Overview

This guide will help you migrate from AWS services (Lambda, DynamoDB, API Gateway) to your Ubuntu 24.04 VPS with PHP 8.2, Node.js, and Nginx.

---

## 🎯 Current AWS Services to Replace

### What you currently have:
1. **AWS Lambda** - Python backend function (`habit_sync_function.py`)
2. **DynamoDB** - NoSQL database for habit data
3. **API Gateway** - REST API endpoints
4. **CloudWatch** - Logs and monitoring

### What you'll set up on VPS:
1. **PHP/Node.js Backend** - API endpoints
2. **MySQL/MariaDB or MongoDB** - Database
3. **Nginx** - Web server + reverse proxy
4. **PM2** - Process manager (for Node.js if used)

---

## 📊 Step-by-Step Migration Plan

### **Phase 1: Database Setup (Choose One)**

#### Option A: MySQL/MariaDB (Recommended for structured data)

```bash
# Install MySQL
sudo apt update
sudo apt install mysql-server -y

# Secure installation
sudo mysql_secure_installation

# Create database and user
sudo mysql -u root -p
```

```sql
CREATE DATABASE uhabits_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'uhabits_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON uhabits_analytics.* TO 'uhabits_user'@'localhost';
FLUSH PRIVILEGES;

USE uhabits_analytics;

-- Main habits data table
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

-- Analytics summary table
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

-- Performance tracking table
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
```

#### Option B: MongoDB (If you prefer NoSQL like DynamoDB)

```bash
# Install MongoDB
sudo apt install -y gnupg curl
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | \
   sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

sudo apt update
sudo apt install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# Create database and user
mongosh
```

```javascript
use uhabits_analytics

db.createUser({
  user: "uhabits_user",
  pwd: "your_secure_password",
  roles: [{ role: "readWrite", db: "uhabits_analytics" }]
})

// Create collections with indexes
db.habit_syncs.createIndex({ "user_id": 1, "sync_timestamp": -1 })
db.habit_syncs.createIndex({ "habit_name": 1 })
db.habit_syncs.createIndex({ "priority": 1 })
db.habit_syncs.createIndex({ "category": 1 })
```

---

### **Phase 2: Backend API Setup**

#### Option A: PHP Backend (Since you have PHP 8.2 installed)

Create: `/var/www/your-domain.com/api/sync.php`

```php
<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, x-api-key');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'uhabits_analytics');
define('DB_USER', 'uhabits_user');
define('DB_PASS', 'your_secure_password');

// API Key authentication
define('API_KEY', 'your_secure_api_key_here'); // Generate a strong key

// Validate API key
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
    error_log("Database error: " . $e->getMessage());
    exit();
}

// Handle GET request (health check)
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode([
        'status' => 'healthy',
        'message' => 'uHabits sync endpoint is operational',
        'timestamp' => date('c'),
        'service' => 'uhabits-sync-vps',
        'version' => '1.0'
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

#### Option B: Node.js Backend

Create: `/var/www/your-domain.com/api/server.js`

```javascript
const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Configuration
const API_KEY = process.env.API_KEY || 'your_secure_api_key_here';
const PORT = process.env.PORT || 3000;

// Database pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'uhabits_user',
    password: 'your_secure_password',
    database: 'uhabits_analytics',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// API Key middleware
const authenticate = (req, res, next) => {
    const apiKey = req.headers['x-api-key'];
    if (!apiKey || apiKey !== API_KEY) {
        return res.status(401).json({ error: 'Unauthorized - Invalid API key' });
    }
    next();
};

// Health check endpoint
app.get('/sync', authenticate, (req, res) => {
    res.json({
        status: 'healthy',
        message: 'uHabits sync endpoint is operational',
        timestamp: new Date().toISOString(),
        service: 'uhabits-sync-vps',
        version: '1.0'
    });
});

// Sync endpoint
app.post('/sync', authenticate, async (req, res) => {
    const { user_id, sync_timestamp, habits_data, summary_metrics, priority_distribution } = req.body;

    // Validate required fields
    if (!user_id || !sync_timestamp || !habits_data) {
        return res.status(400).json({ 
            error: 'Missing required fields: user_id, sync_timestamp, habits_data' 
        });
    }

    const connection = await pool.getConnection();
    
    try {
        await connection.beginTransaction();

        // Store summary metrics
        if (summary_metrics) {
            await connection.execute(`
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
            `, [
                user_id,
                sync_timestamp,
                summary_metrics.total_habits || 0,
                summary_metrics.active_habits || 0,
                summary_metrics.total_checkmarks || 0,
                summary_metrics.average_score || 0,
                JSON.stringify(priority_distribution || {}),
                JSON.stringify(summary_metrics.category_distribution || {})
            ]);
        }

        // Store individual habit data
        let processedHabits = 0;
        for (const habit of habits_data) {
            await connection.execute(`
                INSERT INTO habit_syncs 
                (user_id, sync_timestamp, habit_name, habit_group, priority, 
                 category, frequency, score, checkmarks)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            `, [
                user_id,
                sync_timestamp,
                habit['name'] || '',
                habit.group || 'Default',
                habit.priority || 3,
                habit.category || 'Uncategorized',
                habit.frequency || 0,
                habit.score || 0,
                JSON.stringify(habit.checkmarks || [])
            ]);
            processedHabits++;
        }

        await connection.commit();

        res.json({
            status: 'success',
            message: 'Habit data synced successfully',
            timestamp: new Date().toISOString(),
            processed: {
                habits_count: processedHabits,
                user_id: user_id
            }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Sync error:', error);
        res.status(500).json({ error: 'Sync failed: ' + error.message });
    } finally {
        connection.release();
    }
});

// Analytics endpoint (for dashboard)
app.get('/api/groups', authenticate, async (req, res) => {
    try {
        const [rows] = await pool.execute(`
            SELECT 
                habit_group as group_name,
                COUNT(*) as habit_count,
                AVG(score) as avg_score,
                JSON_ARRAYAGG(
                    JSON_OBJECT(
                        'name', habit_name,
                        'priority', priority,
                        'score', score,
                        'category', category
                    )
                ) as habits
            FROM habit_syncs
            WHERE user_id = 'user_primary'
            GROUP BY habit_group
            ORDER BY avg_score DESC
        `);

        res.json({
            status: 'success',
            data: {
                groups: rows,
                summary: {
                    total_habits_analyzed: rows.reduce((sum, g) => sum + g.habit_count, 0),
                    total_groups: rows.length
                }
            }
        });
    } catch (error) {
        console.error('Analytics error:', error);
        res.status(500).json({ error: 'Failed to fetch analytics' });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`✅ uHabits API server running on port ${PORT}`);
});

module.exports = app;
```

If using Node.js, install dependencies:
```bash
cd /var/www/your-domain.com/api
npm init -y
npm install express mysql2 cors dotenv
npm install -g pm2

# Create .env file
cat > .env << EOF
API_KEY=your_secure_api_key_here
PORT=3000
DB_HOST=localhost
DB_USER=uhabits_user
DB_PASS=your_secure_password
DB_NAME=uhabits_analytics
EOF

# Start with PM2
pm2 start server.js --name uhabits-api
pm2 save
pm2 startup
```

---

### **Phase 3: Nginx Configuration**

Create: `/etc/nginx/sites-available/uhabits-api`

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name your-domain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name your-domain.com;

    # SSL Configuration (use Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Logs
    access_log /var/log/nginx/uhabits-api-access.log;
    error_log /var/log/nginx/uhabits-api-error.log;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # CORS headers
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Content-Type, x-api-key, X-Api-Key" always;

    # Handle OPTIONS preflight
    if ($request_method = OPTIONS) {
        return 204;
    }

    # PHP API (Option A)
    location /api/ {
        root /var/www/your-domain.com;
        index sync.php;
        
        location ~ \.php$ {
            include fastcgi_params;
            fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
            fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
            fastcgi_index sync.php;
        }
    }

    # Node.js API (Option B) - Uncomment if using Node.js
    # location /api/ {
    #     proxy_pass http://localhost:3000;
    #     proxy_http_version 1.1;
    #     proxy_set_header Upgrade $http_upgrade;
    #     proxy_set_header Connection 'upgrade';
    #     proxy_set_header Host $host;
    #     proxy_set_header X-Real-IP $remote_addr;
    #     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    #     proxy_set_header X-Forwarded-Proto $scheme;
    #     proxy_cache_bypass $http_upgrade;
    # }

    # Dashboard (optional - serve your HTML dashboard)
    location / {
        root /var/www/your-domain.com/public;
        index index.html;
        try_files $uri $uri/ =404;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/uhabits-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

### **Phase 4: SSL Certificate (Let's Encrypt)**

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal is set up automatically
sudo certbot renew --dry-run
```

---

### **Phase 5: Update Android App Configuration**

Create: `/var/www/your-domain.com/cloud_config.json`

```json
{
    "aws_config": {
        "region": "custom-vps",
        "environment": "production",
        "api_gateway": {
            "base_url": "https://your-domain.com",
            "sync_endpoint": "/api/sync.php",
            "api_key": "your_secure_api_key_here",
            "timeout_seconds": 30
        }
    }
}
```

Update the Android app (in `CloudSyncManager.kt`):
- Replace AWS URLs with: `https://your-domain.com/api/sync.php`
- Update API key to your new key

---

### **Phase 6: Update Dashboard HTML Files**

Update these files to point to your VPS:

1. `enhanced_dashboard.html` - Line 543, 643, 667
2. `group_analytics_dashboard.html` - Line 1337, 1519

Replace:
```javascript
'https://bhg1kt9cf2.execute-api.eu-central-1.amazonaws.com/prod/api/groups'
```

With:
```javascript
'https://your-domain.com/api/groups'
```

---

### **Phase 7: Testing**

#### Test API Health:
```bash
curl -X GET https://your-domain.com/api/sync.php \
  -H "x-api-key: your_secure_api_key_here"
```

Expected response:
```json
{
    "status": "healthy",
    "message": "uHabits sync endpoint is operational",
    "timestamp": "2024-11-20T...",
    "service": "uhabits-sync-vps",
    "version": "1.0"
}
```

#### Test Sync Endpoint:
```bash
curl -X POST https://your-domain.com/api/sync.php \
  -H "Content-Type: application/json" \
  -H "x-api-key: your_secure_api_key_here" \
  -d '{
    "user_id": "user_primary",
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

---

### **Phase 8: Monitoring & Maintenance**

#### Create monitoring script: `/opt/uhabits/monitor.sh`

```bash
#!/bin/bash

# Check database
mysql -u uhabits_user -p'your_secure_password' -e "SELECT COUNT(*) FROM uhabits_analytics.habit_syncs;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Database: OK"
else
    echo "❌ Database: FAILED"
    # Send alert (email, webhook, etc.)
fi

# Check API endpoint
response=$(curl -s -o /dev/null -w "%{http_code}" -H "x-api-key: your_secure_api_key_here" https://your-domain.com/api/sync.php)
if [ "$response" = "200" ]; then
    echo "✅ API: OK"
else
    echo "❌ API: FAILED (HTTP $response)"
fi

# Check disk space
disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$disk_usage" -lt 80 ]; then
    echo "✅ Disk: OK ($disk_usage%)"
else
    echo "⚠️ Disk: Warning ($disk_usage%)"
fi
```

Setup cron job:
```bash
sudo chmod +x /opt/uhabits/monitor.sh
crontab -e
# Add: */5 * * * * /opt/uhabits/monitor.sh >> /var/log/uhabits-monitor.log 2>&1
```

---

## 🔐 Security Checklist

- [ ] Strong API key (32+ characters, random)
- [ ] SSL/TLS enabled (HTTPS only)
- [ ] Database user with minimal privileges
- [ ] Firewall configured (UFW)
- [ ] Regular backups automated
- [ ] Log rotation configured
- [ ] Rate limiting enabled (Nginx)
- [ ] Database remote access disabled

---

## 💾 Backup Strategy

```bash
#!/bin/bash
# /opt/uhabits/backup.sh

BACKUP_DIR="/backup/uhabits"
DATE=$(date +%Y%m%d_%H%M%S)

# Database backup
mysqldump -u uhabits_user -p'your_secure_password' uhabits_analytics | gzip > "$BACKUP_DIR/db_$DATE.sql.gz"

# Keep only last 7 days
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete

echo "Backup completed: db_$DATE.sql.gz"
```

Cron: `0 2 * * * /opt/uhabits/backup.sh`

---

## 📊 Cost Comparison

| Service | AWS | VPS |
|---------|-----|-----|
| Compute | Lambda Free Tier | Included |
| Database | DynamoDB Free Tier | Included |
| API | API Gateway Free Tier | Included |
| Storage | S3 ($0.023/GB) | Included |
| **Total** | ~$0-2/month | **$0** (VPS already paid) |

---

## 🚀 Next Steps

1. **Choose database** (MySQL recommended)
2. **Choose backend** (PHP is simpler, Node.js is more scalable)
3. **Set up database and tables**
4. **Deploy backend code**
5. **Configure Nginx**
6. **Get SSL certificate**
7. **Test endpoints**
8. **Update Android app**
9. **Update dashboard files**
10. **Monitor and optimize**

---

## 📞 Need Help?

If you encounter issues:
1. Check Nginx error logs: `sudo tail -f /var/log/nginx/uhabits-api-error.log`
2. Check PHP logs: `sudo tail -f /var/log/php8.2-fpm.log`
3. Check database logs: `sudo tail -f /var/log/mysql/error.log`
4. Test database connection: `mysql -u uhabits_user -p`

---

**Ready to start?** Let me know which options you prefer (MySQL vs MongoDB, PHP vs Node.js) and I'll help you with the specific implementation!
