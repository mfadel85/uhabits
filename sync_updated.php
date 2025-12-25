<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
$host = 'localhost';
$dbname = 'uhabits_analytics';
$username = 'uhabits_user';
$password = 'ye7leCek1_2fut';

function priorityToInt($priority) {
    $priorityMap = [
        'HIGHEST' => 1,
        'HIGH' => 2,
        'MEDIUM' => 3,
        'LOW' => 4,
        'LOWEST' => 5
    ];
    
    if (is_numeric($priority)) {
        return intval($priority);
    }
    
    if (is_string($priority)) {
        $upperPriority = strtoupper(trim($priority));
        return $priorityMap[$upperPriority] ?? 3;
    }
    
    return 3;
}

function frequencyToInt($frequency) {
    if (is_numeric($frequency)) {
        return intval($frequency);
    }
    
    if (is_string($frequency)) {
        if (preg_match('/(\d+)\s*\/\s*(\d+)/', $frequency, $matches)) {
            $numerator = intval($matches[1]);
            $denominator = intval($matches[2]);
            if ($denominator > 0) {
                return intval(($numerator / $denominator) * 7);
            }
        }
        
        $lowerFreq = strtolower(trim($frequency));
        $frequencyMap = [
            'daily' => 7,
            'weekly' => 1,
            'monthly' => 0,
            'every day' => 7,
            'once a week' => 1,
            'twice a week' => 2,
            'three times a week' => 3
        ];
        
        return $frequencyMap[$lowerFreq] ?? 7;
    }
    
    return 7;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        $rawData = file_get_contents('php://input');
        error_log("Received data: " . substr($rawData, 0, 500));
        
        $data = json_decode($rawData, true);
        
        if ($data === null) {
            throw new Exception('Invalid JSON data: ' . json_last_error_msg());
        }

        if (!isset($data['habits_data'])) {
            throw new Exception('No habits_data field found in the request');
        }

        $userId = $data['user_id'] ?? 'user_primary';
        $syncTimestamp = $data['sync_timestamp'] ?? time() * 1000;
        $habitsData = $data['habits_data'];

        $pdo->beginTransaction();

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
             category, frequency, score, streak_length, is_numerical, checkmarks)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");

        $processedHabits = 0;
        foreach ($habitsData as $habit) {
            // Convert priority to integer
            $priorityInt = priorityToInt($habit['priority'] ?? 3);
            // Convert frequency to integer
            $frequencyInt = frequencyToInt($habit['frequency'] ?? 7);
            
            // Use success_rate as score (convert to percentage 0-100)
            $score = 0;
            if (isset($habit['success_rate'])) {
                $score = floatval($habit['success_rate']) * 100;
            } elseif (isset($habit['score'])) {
                $score = floatval($habit['score']);
            }
            
            // Cap score at 100%
            $score = min($score, 100);
            
            // Get streak length
            $streakLength = isset($habit['streak_length']) ? intval($habit['streak_length']) : 0;
            
            // Get is_numerical flag
            $isNumerical = isset($habit['is_numerical']) ? intval($habit['is_numerical']) : 0;
            
            // Extract performance history data (last 30 days of daily_data)
            $checkmarksData = [];
            if (isset($habit['performance_history']['daily_data'])) {
                $dailyData = $habit['performance_history']['daily_data'];
                // Get last 30 days (app sends 90 days, we only need recent 30)
                $recentData = array_slice($dailyData, 0, 30);
                
                foreach ($recentData as $day) {
                    $checkmarksData[] = [
                        'date' => $day['date'] ?? '',
                        'value' => intval($day['value'] ?? 0) / 1000, // Convert from milliseconds storage to actual value
                        'completed' => boolval($day['completed'] ?? false)
                    ];
                }
            }
            
            $stmt->execute([
                $userId,
                $syncTimestamp,
                $habit['name'] ?? '',
                $habit['group'] ?? 'Default',
                $priorityInt,
                $habit['category'] ?? 'Uncategorized',
                $frequencyInt,
                $score,
                $streakLength,
                $isNumerical,
                json_encode($checkmarksData)
            ]);
            $processedHabits++;
        }

        $pdo->commit();

        echo json_encode([
            'status' => 'success',
            'message' => 'Habit data synced successfully with performance history.',
            'timestamp' => date('c'),
            'processed' => [
                'summary' => "Stored $processedHabits habit records with performance history",
                'habits_count' => $processedHabits,
                'user_id' => $userId,
                'sync_timestamp' => $syncTimestamp
            ]
        ]);

    } catch (Exception $e) {
        if (isset($pdo)) {
            $pdo->rollBack();
        }
        http_response_code(500);
        echo json_encode(['error' => 'Sync failed: ' . $e->getMessage()]);
        error_log("Sync error: " . $e->getMessage());
    }

    exit();
}

// Method not allowed
http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
