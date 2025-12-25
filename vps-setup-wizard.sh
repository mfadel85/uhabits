#!/bin/bash

# uHabits VPS Migration Setup Wizard
# This script helps you migrate from AWS to your VPS

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  uHabits VPS Migration Setup Wizard   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

# Function to generate random API key
generate_api_key() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
}

# Function to prompt for input
prompt() {
    local prompt_text="$1"
    local default_value="$2"
    local var_name="$3"
    
    if [ -n "$default_value" ]; then
        read -p "$(echo -e ${YELLOW}${prompt_text}${NC} [${default_value}]: )" input
        eval $var_name="${input:-$default_value}"
    else
        read -p "$(echo -e ${YELLOW}${prompt_text}${NC}: )" input
        eval $var_name="$input"
    fi
}

# Welcome message
echo -e "${GREEN}This wizard will help you set up uHabits on your VPS${NC}"
echo ""
echo "Current setup:"
echo "  ✓ Ubuntu 24.04"
echo "  ✓ PHP 8.2"
echo "  ✓ Node.js"
echo "  ✓ Nginx"
echo ""

# Step 1: Choose database
echo -e "${BLUE}═══ Step 1: Choose Database ═══${NC}"
echo "1) MySQL/MariaDB (Recommended - structured data)"
echo "2) MongoDB (NoSQL - similar to DynamoDB)"
prompt "Select database (1 or 2)" "1" DB_CHOICE

# Step 2: Choose backend
echo ""
echo -e "${BLUE}═══ Step 2: Choose Backend ═══${NC}"
echo "1) PHP (Simpler, already installed)"
echo "2) Node.js (More scalable)"
prompt "Select backend (1 or 2)" "1" BACKEND_CHOICE

# Step 3: Domain configuration
echo ""
echo -e "${BLUE}═══ Step 3: Domain Configuration ═══${NC}"
prompt "Enter your domain name (e.g., api.yourdomain.com)" "" DOMAIN

# Step 4: Generate API key
echo ""
echo -e "${BLUE}═══ Step 4: API Key ═══${NC}"
GENERATED_KEY=$(generate_api_key)
echo "Generated API key: $GENERATED_KEY"
prompt "Use this API key or enter your own" "$GENERATED_KEY" API_KEY

# Step 5: Database credentials
echo ""
echo -e "${BLUE}═══ Step 5: Database Configuration ═══${NC}"
prompt "Database name" "uhabits_analytics" DB_NAME
prompt "Database username" "uhabits_user" DB_USER
prompt "Database password (will be generated if empty)" "" DB_PASS

if [ -z "$DB_PASS" ]; then
    DB_PASS=$(openssl rand -base64 16)
    echo -e "${GREEN}Generated password: $DB_PASS${NC}"
fi

# Step 6: Installation path
echo ""
echo -e "${BLUE}═══ Step 6: Installation Path ═══${NC}"
prompt "Web root directory" "/var/www/${DOMAIN}" WEB_ROOT

# Summary
echo ""
echo -e "${BLUE}═══════════════════════════════${NC}"
echo -e "${BLUE}    Configuration Summary       ${NC}"
echo -e "${BLUE}═══════════════════════════════${NC}"
echo ""
echo -e "${GREEN}Database:${NC} $([ "$DB_CHOICE" = "1" ] && echo "MySQL/MariaDB" || echo "MongoDB")"
echo -e "${GREEN}Backend:${NC} $([ "$BACKEND_CHOICE" = "1" ] && echo "PHP 8.2" || echo "Node.js")"
echo -e "${GREEN}Domain:${NC} $DOMAIN"
echo -e "${GREEN}API Key:${NC} $API_KEY"
echo -e "${GREEN}DB Name:${NC} $DB_NAME"
echo -e "${GREEN}DB User:${NC} $DB_USER"
echo -e "${GREEN}DB Pass:${NC} $DB_PASS"
echo -e "${GREEN}Web Root:${NC} $WEB_ROOT"
echo ""

read -p "$(echo -e ${YELLOW}Proceed with installation? (y/n)${NC}: )" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Installation cancelled."
    exit 1
fi

echo ""
echo -e "${GREEN}Starting installation...${NC}"
echo ""

# Save configuration
CONFIG_FILE="${WEB_ROOT}/config.env"

# Create directories
echo -e "${YELLOW}► Creating directories...${NC}"
sudo mkdir -p "$WEB_ROOT"/{api,public,logs,backups}
sudo chown -R $USER:www-data "$WEB_ROOT"

# Database setup
if [ "$DB_CHOICE" = "1" ]; then
    echo ""
    echo -e "${YELLOW}► Setting up MySQL database...${NC}"
    
    # Check if MySQL is installed
    if ! command -v mysql &> /dev/null; then
        echo "MySQL not found. Installing..."
        sudo apt update
        sudo apt install -y mysql-server
        sudo systemctl start mysql
        sudo systemctl enable mysql
    fi
    
    # Create database and user
    sudo mysql -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    sudo mysql -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';"
    sudo mysql -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';"
    sudo mysql -e "FLUSH PRIVILEGES;"
    
    # Create tables
    sudo mysql "$DB_NAME" <<'EOF'
CREATE TABLE IF NOT EXISTS habit_syncs (
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

CREATE TABLE IF NOT EXISTS analytics_summary (
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
EOF
    
    echo -e "${GREEN}✓ MySQL database setup complete${NC}"
fi

# Backend setup
if [ "$BACKEND_CHOICE" = "1" ]; then
    echo ""
    echo -e "${YELLOW}► Setting up PHP backend...${NC}"
    
    # Create PHP API file
    cat > "$WEB_ROOT/api/sync.php" <<PHPEOF
<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, x-api-key');

if (\$_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

define('DB_HOST', 'localhost');
define('DB_NAME', '$DB_NAME');
define('DB_USER', '$DB_USER');
define('DB_PASS', '$DB_PASS');
define('API_KEY', '$API_KEY');

\$headers = getallheaders();
\$providedKey = \$headers['x-api-key'] ?? \$headers['X-Api-Key'] ?? '';

if (\$providedKey !== API_KEY) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized']);
    exit();
}

try {
    \$pdo = new PDO(
        "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
        DB_USER,
        DB_PASS,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]
    );
} catch (PDOException \$e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    exit();
}

if (\$_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode([
        'status' => 'healthy',
        'message' => 'uHabits sync endpoint is operational',
        'timestamp' => date('c'),
        'service' => 'uhabits-sync-vps',
        'version' => '1.0'
    ]);
    exit();
}

if (\$_SERVER['REQUEST_METHOD'] === 'POST') {
    \$data = json_decode(file_get_contents('php://input'), true);
    
    if (!isset(\$data['user_id']) || !isset(\$data['sync_timestamp']) || !isset(\$data['habits_data'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing required fields']);
        exit();
    }
    
    try {
        \$pdo->beginTransaction();
        
        // Store summary
        if (isset(\$data['summary_metrics'])) {
            \$stmt = \$pdo->prepare("
                INSERT INTO analytics_summary 
                (user_id, sync_timestamp, total_habits, active_habits, total_checkmarks, average_score, priority_distribution, category_distribution)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                total_habits = VALUES(total_habits),
                active_habits = VALUES(active_habits)
            ");
            
            \$s = \$data['summary_metrics'];
            \$stmt->execute([
                \$data['user_id'],
                \$data['sync_timestamp'],
                \$s['total_habits'] ?? 0,
                \$s['active_habits'] ?? 0,
                \$s['total_checkmarks'] ?? 0,
                \$s['average_score'] ?? 0,
                json_encode(\$data['priority_distribution'] ?? []),
                json_encode(\$s['category_distribution'] ?? [])
            ]);
        }
        
        // Store habits
        \$stmt = \$pdo->prepare("
            INSERT INTO habit_syncs 
            (user_id, sync_timestamp, habit_name, habit_group, priority, category, frequency, score, checkmarks)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        
        \$count = 0;
        foreach (\$data['habits_data'] as \$habit) {
            \$stmt->execute([
                \$data['user_id'],
                \$data['sync_timestamp'],
                \$habit['name'] ?? '',
                \$habit['group'] ?? 'Default',
                \$habit['priority'] ?? 3,
                \$habit['category'] ?? 'Uncategorized',
                \$habit['frequency'] ?? 0,
                \$habit['score'] ?? 0,
                json_encode(\$habit['checkmarks'] ?? [])
            ]);
            \$count++;
        }
        
        \$pdo->commit();
        
        echo json_encode([
            'status' => 'success',
            'message' => 'Synced successfully',
            'processed' => ['habits_count' => \$count]
        ]);
    } catch (Exception \$e) {
        \$pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Sync failed']);
    }
    exit();
}

http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
?>
PHPEOF

    echo -e "${GREEN}✓ PHP API created${NC}"
fi

# Nginx configuration
echo ""
echo -e "${YELLOW}► Configuring Nginx...${NC}"

sudo tee "/etc/nginx/sites-available/uhabits-$DOMAIN" > /dev/null <<NGINXEOF
server {
    listen 80;
    server_name $DOMAIN;
    
    root $WEB_ROOT/public;
    index index.html;
    
    access_log $WEB_ROOT/logs/access.log;
    error_log $WEB_ROOT/logs/error.log;
    
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, POST, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Content-Type, x-api-key" always;
    
    if (\$request_method = OPTIONS) {
        return 204;
    }
    
    location /api/ {
        root $WEB_ROOT;
        
        location ~ \.php\$ {
            include fastcgi_params;
            fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
            fastcgi_param SCRIPT_FILENAME \$document_root\$fastcgi_script_name;
        }
    }
    
    location / {
        try_files \$uri \$uri/ =404;
    }
}
NGINXEOF

# Enable site
sudo ln -sf "/etc/nginx/sites-available/uhabits-$DOMAIN" "/etc/nginx/sites-enabled/"
sudo nginx -t && sudo systemctl reload nginx

echo -e "${GREEN}✓ Nginx configured${NC}"

# Save configuration
cat > "$WEB_ROOT/config.env" <<CONFIGEOF
# uHabits VPS Configuration
DOMAIN=$DOMAIN
API_KEY=$API_KEY
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASS=$DB_PASS
WEB_ROOT=$WEB_ROOT
DATABASE=$([ "$DB_CHOICE" = "1" ] && echo "MySQL" || echo "MongoDB")
BACKEND=$([ "$BACKEND_CHOICE" = "1" ] && echo "PHP" || echo "Node.js")
INSTALLED_DATE=$(date)
CONFIGEOF

# Create test script
cat > "$WEB_ROOT/test-api.sh" <<'TESTEOF'
#!/bin/bash
source config.env

echo "Testing uHabits API..."
echo ""

# Test health endpoint
echo "1. Testing health check..."
curl -X GET "http://$DOMAIN/api/sync.php" \
  -H "x-api-key: $API_KEY"
echo ""
echo ""

# Test sync endpoint
echo "2. Testing sync endpoint..."
curl -X POST "http://$DOMAIN/api/sync.php" \
  -H "Content-Type: application/json" \
  -H "x-api-key: $API_KEY" \
  -d '{
    "user_id": "test_user",
    "sync_timestamp": 1700000000000,
    "habits_data": [{
        "name": "Test Habit",
        "group": "Test",
        "priority": 1,
        "category": "Test",
        "score": 100,
        "frequency": 7,
        "checkmarks": []
    }],
    "summary_metrics": {
        "total_habits": 1,
        "active_habits": 1,
        "total_checkmarks": 0,
        "average_score": 100
    }
  }'
echo ""
TESTEOF

chmod +x "$WEB_ROOT/test-api.sh"

# Create Android config
cat > "$WEB_ROOT/public/cloud_config.json" <<ANDROIDEOF
{
    "aws_config": {
        "region": "custom-vps",
        "environment": "production",
        "api_gateway": {
            "base_url": "https://$DOMAIN",
            "sync_endpoint": "/api/sync.php",
            "api_key": "$API_KEY",
            "timeout_seconds": 30
        }
    }
}
ANDROIDEOF

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Installation Complete! 🎉            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Configuration saved to:${NC} $WEB_ROOT/config.env"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo ""
echo "1. Get SSL certificate:"
echo -e "   ${GREEN}sudo certbot --nginx -d $DOMAIN${NC}"
echo ""
echo "2. Test your API:"
echo -e "   ${GREEN}cd $WEB_ROOT && ./test-api.sh${NC}"
echo ""
echo "3. Update Android app with new config:"
echo -e "   ${GREEN}cat $WEB_ROOT/public/cloud_config.json${NC}"
echo ""
echo "4. View logs:"
echo -e "   ${GREEN}tail -f $WEB_ROOT/logs/*.log${NC}"
echo ""
echo -e "${BLUE}API Endpoint:${NC} https://$DOMAIN/api/sync.php"
echo -e "${BLUE}API Key:${NC} $API_KEY"
echo -e "${BLUE}Health Check:${NC} curl -H 'x-api-key: $API_KEY' https://$DOMAIN/api/sync.php"
echo ""
echo -e "${YELLOW}Don't forget to update your Android app and dashboard HTML files!${NC}"
echo ""
