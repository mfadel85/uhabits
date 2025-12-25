<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
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

// Handle GET request (dashboard data)
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        // Get latest sync data
        $stmt = $pdo->query("
            SELECT 
                habit_name,
                habit_group,
                category,
                priority,
                frequency,
                score,
                streak_length,
                checkmarks,
                sync_timestamp
            FROM habit_syncs
            WHERE sync_timestamp = (
                SELECT MAX(sync_timestamp) FROM habit_syncs
            )
            ORDER BY category, priority, habit_name
        ");
        
        $habits = $stmt->fetchAll();
        
        if (empty($habits)) {
            // Return empty structure if no data
            echo json_encode([
                'summary' => [
                    'total_habits' => 0,
                    'active_habits' => 0,
                    'overall_average' => 0,
                    'data_source' => 'VPS MySQL Database',
                    'last_sync' => null
                ],
                'group_performance' => [],
                'category_distribution' => [],
                'priority_distribution' => [],
                'habits' => []
            ]);
            exit();
        }
        
        // Get latest sync timestamp
        $latestSync = $habits[0]['sync_timestamp'] ?? null;
        
        // Organize by category
        $categoryData = [];
        $priorityCounts = [1 => 0, 2 => 0, 3 => 0, 4 => 0, 5 => 0];
        
        foreach ($habits as $habit) {
            $category = $habit['category'] ?? 'Uncategorized';
            $priority = (int)$habit['priority'];
            
            if (!isset($categoryData[$category])) {
                $categoryData[$category] = [
                    'habits' => [],
                    'total_score' => 0,
                    'count' => 0
                ];
            }
            
            $categoryData[$category]['habits'][] = $habit;
            $categoryData[$category]['total_score'] += (float)$habit['score'];
            $categoryData[$category]['count']++;
            
            if (isset($priorityCounts[$priority])) {
                $priorityCounts[$priority]++;
            }
        }
        
        // Calculate group performance
        $groupPerformance = [];
        foreach ($categoryData as $category => $data) {
            $avgScore = $data['count'] > 0 ? $data['total_score'] / $data['count'] : 0;
            
            // Calculate grade based on average score
            $grade = 'F';
            if ($avgScore >= 90) $grade = 'A';
            elseif ($avgScore >= 80) $grade = 'B';
            elseif ($avgScore >= 70) $grade = 'C';
            elseif ($avgScore >= 60) $grade = 'D';
            
            // Return as decimal (0-1) since dashboard multiplies by 100
            $groupPerformance[$category] = [
                'weighted_average' => round($avgScore / 100, 4),  // Convert percentage to decimal
                'habit_count' => $data['count'],
                'grade' => $grade,  // Add grade
                'habits' => array_map(function($h) {
                    return [
                        'name' => $h['habit_name'],
                        'score' => (float)$h['score'],  // Keep as percentage for display
                        'frequency' => (int)$h['frequency'],
                        'priority' => (int)$h['priority'],
                        'streak' => (int)($h['streak_length'] ?? 0),  // Add streak
                        'checkmarks' => json_decode($h['checkmarks'] ?? '[]', true)
                    ];
                }, $data['habits'])
            ];
        }
        
        // Calculate overall metrics
        $totalHabits = count($habits);
        $totalScore = array_sum(array_column($habits, 'score'));
        $overallAverage = $totalHabits > 0 ? $totalScore / $totalHabits : 0;  // Already in percentage (0-100)
        
        // Priority distribution
        $priorityLabels = [
            1 => 'Critical',
            2 => 'High',
            3 => 'Medium',
            4 => 'Low',
            5 => 'None'
        ];
        
        $priorityDistribution = [];
        foreach ($priorityCounts as $level => $count) {
            if ($count > 0) {
                $priorityDistribution[] = [
                    'priority' => $priorityLabels[$level],
                    'count' => $count,
                    'percentage' => round(($count / $totalHabits) * 100, 1)
                ];
            }
        }
        
        // Category distribution
        $categoryDistribution = [];
        foreach ($categoryData as $category => $data) {
            $categoryDistribution[] = [
                'category' => $category,
                'count' => $data['count'],
                'percentage' => round(($data['count'] / $totalHabits) * 100, 1)
            ];
        }
        
        // Build response
        $response = [
            'summary' => [
                'total_habits' => $totalHabits,
                'active_habits' => $totalHabits, // All synced habits are considered active
                'overall_average' => round($overallAverage, 2),
                'data_source' => 'VPS MySQL Database (kpitracker.quest)',
                'last_sync' => $latestSync
            ],
            'group_performance' => $groupPerformance,
            'category_distribution' => $categoryDistribution,
            'priority_distribution' => $priorityDistribution,
            'habits' => array_map(function($h) {
                return [
                    'name' => $h['habit_name'],
                    'category' => $h['category'],
                    'group' => $h['habit_group'],
                    'priority' => (int)$h['priority'],
                    'frequency' => (int)$h['frequency'],
                    'score' => (float)$h['score'],
                    'checkmarks' => json_decode($h['checkmarks'] ?? '[]', true)
                ];
            }, $habits)
        ];
        
        echo json_encode($response, JSON_PRETTY_PRINT);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Failed to generate dashboard data: ' . $e->getMessage()]);
        error_log("Dashboard API error: " . $e->getMessage());
    }
    
    exit();
}

// Method not allowed
http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
?>
