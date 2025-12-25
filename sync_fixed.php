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

// Configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'uhabits_analytics');
define('DB_USER', 'uhabits_user');
define('DB_PASS', 'ye7leCek1_2fut');
define('API_KEY', 'Z2yITovrkGIOgWlOW4704gvtzeSueNT8');

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

// Helper function to convert priority string to integer
function priorityToInt($priority) {
    $priorityMap = [
        'CRITICAL' => 1,
        'HIGH' => 2,
        'MEDIUM' => 3,
        'LOW' => 4,
        'NONE' => 5
    ];
    
    // If it's already a number, return it
    if (is_numeric($priority)) {
        return (int)$priority;
    }
    
    // Convert string priority to int
    $upperPriority = strtoupper(trim($priority));
    return $priorityMap[$upperPriority] ?? 3; // Default to MEDIUM (3)
}

// Helper function to convert frequency string to integer
function frequencyToInt($frequency) {
    // If it's already a number, return it
    if (is_numeric($frequency)) {
        return (int)$frequency;
    }
    
    // If it's a string like "Frequency(numerator=6, denominator=7)"
    if (is_string($frequency) && preg_match('/numerator=(\d+).*denominator=(\d+)/', $frequency, $matches)) {
        $numerator = (int)$matches[1];
        $denominator = (int)$matches[2];
        // Store as times per week (numerator * 7 / denominator)
        return (int)round(($numerator * 7) / $denominator);
    }
    
    return 7; // Default to daily (7 times per week)
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
             category, frequency, score, streak_length, checkmarks)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");

        $processedHabits = 0;
        foreach ($habitsData as $habit) {
            // Convert priority to integer
            $priorityInt = priorityToInt($habit['priority'] ?? 3);
            // Convert frequency to integer
            $frequencyInt = frequencyToInt($habit['frequency'] ?? 7);
            
            // Use success_rate as score (convert to percentage 0-100)
            // Note: success_rate is 0-1, weighted_success_rate can exceed 1.0
            $score = 0;
            if (isset($habit['success_rate'])) {
                $score = floatval($habit['success_rate']) * 100;
            } elseif (isset($habit['score'])) {
                $score = floatval($habit['score']);
            }
            
            // Cap score at 100% to handle any edge cases
            $score = min($score, 100);
            
            // Get streak length
            $streakLength = isset($habit['streak_length']) ? intval($habit['streak_length']) : 0;
            
            $stmt->execute([
                $userId,
                $syncTimestamp,
                $habit['name'] ?? '',
                $habit['group'] ?? 'Default',
                $priorityInt,  // Now using integer
                $habit['category'] ?? 'Uncategorized',
                $frequencyInt,  // Now using integer
                $score,  // Now using actual success rate as percentage
                $streakLength,  // Store streak length
                json_encode($habit['checkmarks'] ?? [])
            ]);
            $processedHabits++;
        }

        $pdo->commit();

        echo json_encode([
            'status' => 'success',
            'message' => 'Habit data synced successfully with preserved categories.',
            'timestamp' => date('c'),
            'processed' => [
                'summary' => "Stored 1 sync record, $processedHabits habit records with preserved original categories",
                'habits_count' => $processedHabits,
                'user_id' => $userId,
                'sync_timestamp' => $syncTimestamp
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
