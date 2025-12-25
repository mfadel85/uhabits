<?php
/**
 * Google Looker Studio API Endpoint
 * 
 * This endpoint provides habit data in a flat, denormalized format
 * optimized for Google Looker Studio (formerly Data Studio) consumption.
 * 
 * Endpoints:
 * - /api/looker.php?type=habits - Daily habit records (main dataset)
 * - /api/looker.php?type=summary - Aggregated summary metrics
 * - /api/looker.php?type=categories - Category performance over time
 * - /api/looker.php?type=streaks - Streak history data
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
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

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    error_log("Looker API DB Error: " . $e->getMessage());
    exit();
}

// Helper: Convert priority int to label
function getPriorityLabel($priority) {
    $labels = [1 => 'Critical', 2 => 'High', 3 => 'Medium', 4 => 'Low', 5 => 'None'];
    return $labels[$priority] ?? 'Medium';
}

// Helper: Calculate grade from score
function getGrade($score) {
    if ($score >= 90) return 'A';
    if ($score >= 80) return 'B';
    if ($score >= 70) return 'C';
    if ($score >= 60) return 'D';
    return 'F';
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $type = $_GET['type'] ?? 'habits';
    $startDate = $_GET['start_date'] ?? null;
    $endDate = $_GET['end_date'] ?? null;
    $userId = $_GET['user_id'] ?? null;
    
    switch ($type) {
        case 'habits':
            // Main dataset: Flatten checkmarks into daily records
            $whereClause = "WHERE 1=1";
            $params = [];
            
            if ($userId) {
                $whereClause .= " AND user_id = ?";
                $params[] = $userId;
            }
            
            $stmt = $pdo->prepare("
                SELECT 
                    id,
                    user_id,
                    sync_timestamp,
                    habit_name,
                    habit_group,
                    category,
                    priority,
                    frequency,
                    score,
                    streak_length,
                    is_numerical,
                    checkmarks
                FROM habit_syncs
                $whereClause
                ORDER BY sync_timestamp DESC, category, habit_name
            ");
            
            $stmt->execute($params);
            $habits = $stmt->fetchAll();
            
            $flattenedData = [];
            
            foreach ($habits as $habit) {
                $checkmarks = $habit['checkmarks'];
                if (is_string($checkmarks)) {
                    $checkmarks = json_decode($checkmarks, true) ?? [];
                }
                
                // If no checkmarks, still output one row with habit metadata
                if (empty($checkmarks)) {
                    $flattenedData[] = [
                        'sync_id' => (int)$habit['id'],
                        'user_id' => $habit['user_id'],
                        'sync_timestamp' => $habit['sync_timestamp'],
                        'sync_date' => date('Y-m-d', $habit['sync_timestamp'] / 1000),
                        'sync_datetime' => date('Y-m-d H:i:s', $habit['sync_timestamp'] / 1000),
                        'habit_name' => $habit['habit_name'],
                        'habit_group' => $habit['habit_group'] ?? 'Default',
                        'category' => $habit['category'] ?? 'Uncategorized',
                        'priority' => (int)$habit['priority'],
                        'priority_label' => getPriorityLabel($habit['priority']),
                        'frequency' => (int)$habit['frequency'],
                        'score' => round((float)$habit['score'], 2),
                        'score_percentage' => round((float)$habit['score'], 2) . '%',
                        'grade' => getGrade($habit['score']),
                        'streak_length' => (int)$habit['streak_length'],
                        'is_numerical' => (bool)$habit['is_numerical'],
                        'habit_type' => $habit['is_numerical'] ? 'Numerical' : 'Yes/No',
                        // Daily data fields
                        'record_date' => null,
                        'value' => null,
                        'completed' => null,
                        'day_of_week' => null
                    ];
                    continue;
                }
                
                // Flatten each checkmark into a separate row
                foreach ($checkmarks as $check) {
                    $recordDate = $check['date'] ?? null;
                    $value = $check['value'] ?? null;
                    $completed = isset($check['completed']) ? (bool)$check['completed'] : null;
                    
                    // Apply date filters if provided
                    if ($startDate && $recordDate < $startDate) continue;
                    if ($endDate && $recordDate > $endDate) continue;
                    
                    $dayOfWeek = $recordDate ? date('l', strtotime($recordDate)) : null;
                    
                    $flattenedData[] = [
                        'sync_id' => (int)$habit['id'],
                        'user_id' => $habit['user_id'],
                        'sync_timestamp' => $habit['sync_timestamp'],
                        'sync_date' => date('Y-m-d', $habit['sync_timestamp'] / 1000),
                        'sync_datetime' => date('Y-m-d H:i:s', $habit['sync_timestamp'] / 1000),
                        'habit_name' => $habit['habit_name'],
                        'habit_group' => $habit['habit_group'] ?? 'Default',
                        'category' => $habit['category'] ?? 'Uncategorized',
                        'priority' => (int)$habit['priority'],
                        'priority_label' => getPriorityLabel($habit['priority']),
                        'frequency' => (int)$habit['frequency'],
                        'score' => round((float)$habit['score'], 2),
                        'score_percentage' => round((float)$habit['score'], 2) . '%',
                        'grade' => getGrade($habit['score']),
                        'streak_length' => (int)$habit['streak_length'],
                        'is_numerical' => (bool)$habit['is_numerical'],
                        'habit_type' => $habit['is_numerical'] ? 'Numerical' : 'Yes/No',
                        // Daily data
                        'record_date' => $recordDate,
                        'value' => $value !== null ? (float)$value : null,
                        'completed' => $completed,
                        'completed_text' => $completed ? 'Yes' : 'No',
                        'day_of_week' => $dayOfWeek
                    ];
                }
            }
            
            echo json_encode([
                'data' => $flattenedData,
                'metadata' => [
                    'total_records' => count($flattenedData),
                    'endpoint' => 'habits',
                    'description' => 'Daily habit performance records',
                    'generated_at' => date('Y-m-d H:i:s')
                ]
            ]);
            break;
            
        case 'summary':
            // Aggregated summary by sync
            $stmt = $pdo->query("
                SELECT 
                    sync_timestamp,
                    user_id,
                    COUNT(*) as total_habits,
                    COUNT(*) as active_habits,
                    ROUND(AVG(score), 2) as average_score,
                    ROUND(MIN(score), 2) as min_score,
                    ROUND(MAX(score), 2) as max_score,
                    SUM(CASE WHEN is_numerical = 1 THEN 1 ELSE 0 END) as numerical_habits,
                    SUM(CASE WHEN is_numerical = 0 THEN 1 ELSE 0 END) as boolean_habits,
                    SUM(CASE WHEN priority = 1 THEN 1 ELSE 0 END) as critical_count,
                    SUM(CASE WHEN priority = 2 THEN 1 ELSE 0 END) as high_count,
                    SUM(CASE WHEN priority = 3 THEN 1 ELSE 0 END) as medium_count,
                    SUM(CASE WHEN priority = 4 THEN 1 ELSE 0 END) as low_count,
                    SUM(CASE WHEN score >= 90 THEN 1 ELSE 0 END) as grade_a_count,
                    SUM(CASE WHEN score >= 80 AND score < 90 THEN 1 ELSE 0 END) as grade_b_count,
                    SUM(CASE WHEN score >= 70 AND score < 80 THEN 1 ELSE 0 END) as grade_c_count,
                    SUM(CASE WHEN score >= 60 AND score < 70 THEN 1 ELSE 0 END) as grade_d_count,
                    SUM(CASE WHEN score < 60 THEN 1 ELSE 0 END) as grade_f_count
                FROM habit_syncs
                GROUP BY sync_timestamp, user_id
                ORDER BY sync_timestamp DESC
            ");
            
            $summaries = $stmt->fetchAll();
            
            $formattedSummaries = array_map(function($s) {
                return [
                    'sync_date' => date('Y-m-d', $s['sync_timestamp'] / 1000),
                    'sync_datetime' => date('Y-m-d H:i:s', $s['sync_timestamp'] / 1000),
                    'sync_timestamp' => $s['sync_timestamp'],
                    'user_id' => $s['user_id'],
                    'total_habits' => (int)$s['total_habits'],
                    'active_habits' => (int)$s['active_habits'],
                    'average_score' => (float)$s['average_score'],
                    'min_score' => (float)$s['min_score'],
                    'max_score' => (float)$s['max_score'],
                    'numerical_habits' => (int)$s['numerical_habits'],
                    'boolean_habits' => (int)$s['boolean_habits'],
                    'critical_count' => (int)$s['critical_count'],
                    'high_count' => (int)$s['high_count'],
                    'medium_count' => (int)$s['medium_count'],
                    'low_count' => (int)$s['low_count'],
                    'grade_a_count' => (int)$s['grade_a_count'],
                    'grade_b_count' => (int)$s['grade_b_count'],
                    'grade_c_count' => (int)$s['grade_c_count'],
                    'grade_d_count' => (int)$s['grade_d_count'],
                    'grade_f_count' => (int)$s['grade_f_count']
                ];
            }, $summaries);
            
            echo json_encode([
                'data' => $formattedSummaries,
                'metadata' => [
                    'total_records' => count($formattedSummaries),
                    'endpoint' => 'summary',
                    'description' => 'Aggregated sync summary metrics',
                    'generated_at' => date('Y-m-d H:i:s')
                ]
            ]);
            break;
            
        case 'categories':
            // Category performance over time
            $stmt = $pdo->query("
                SELECT 
                    sync_timestamp,
                    category,
                    COUNT(*) as habit_count,
                    ROUND(AVG(score), 2) as avg_score,
                    ROUND(MIN(score), 2) as min_score,
                    ROUND(MAX(score), 2) as max_score,
                    SUM(streak_length) as total_streak_days,
                    ROUND(AVG(streak_length), 1) as avg_streak
                FROM habit_syncs
                WHERE category IS NOT NULL AND category != ''
                GROUP BY sync_timestamp, category
                ORDER BY sync_timestamp DESC, avg_score DESC
            ");
            
            $categories = $stmt->fetchAll();
            
            $formattedCategories = array_map(function($c) {
                return [
                    'sync_date' => date('Y-m-d', $c['sync_timestamp'] / 1000),
                    'sync_datetime' => date('Y-m-d H:i:s', $c['sync_timestamp'] / 1000),
                    'category' => $c['category'],
                    'habit_count' => (int)$c['habit_count'],
                    'average_score' => (float)$c['avg_score'],
                    'min_score' => (float)$c['min_score'],
                    'max_score' => (float)$c['max_score'],
                    'grade' => getGrade($c['avg_score']),
                    'total_streak_days' => (int)$c['total_streak_days'],
                    'average_streak' => (float)$c['avg_streak']
                ];
            }, $categories);
            
            echo json_encode([
                'data' => $formattedCategories,
                'metadata' => [
                    'total_records' => count($formattedCategories),
                    'endpoint' => 'categories',
                    'description' => 'Category performance metrics over time',
                    'generated_at' => date('Y-m-d H:i:s')
                ]
            ]);
            break;
            
        case 'streaks':
            // Streak analysis by habit
            $stmt = $pdo->query("
                SELECT 
                    sync_timestamp,
                    habit_name,
                    category,
                    priority,
                    streak_length,
                    score
                FROM habit_syncs
                WHERE streak_length > 0
                ORDER BY sync_timestamp DESC, streak_length DESC
            ");
            
            $streaks = $stmt->fetchAll();
            
            $formattedStreaks = array_map(function($s) {
                return [
                    'sync_date' => date('Y-m-d', $s['sync_timestamp'] / 1000),
                    'sync_datetime' => date('Y-m-d H:i:s', $s['sync_timestamp'] / 1000),
                    'habit_name' => $s['habit_name'],
                    'category' => $s['category'] ?? 'Uncategorized',
                    'priority' => (int)$s['priority'],
                    'priority_label' => getPriorityLabel($s['priority']),
                    'streak_length' => (int)$s['streak_length'],
                    'score' => (float)$s['score']
                ];
            }, $streaks);
            
            echo json_encode([
                'data' => $formattedStreaks,
                'metadata' => [
                    'total_records' => count($formattedStreaks),
                    'endpoint' => 'streaks',
                    'description' => 'Habit streak tracking over time',
                    'generated_at' => date('Y-m-d H:i:s')
                ]
            ]);
            break;
            
        default:
            http_response_code(400);
            echo json_encode([
                'error' => 'Invalid type parameter',
                'valid_types' => ['habits', 'summary', 'categories', 'streaks'],
                'usage' => [
                    'habits' => '/api/looker.php?type=habits&start_date=2025-01-01&end_date=2025-12-31',
                    'summary' => '/api/looker.php?type=summary',
                    'categories' => '/api/looker.php?type=categories',
                    'streaks' => '/api/looker.php?type=streaks'
                ]
            ]);
    }
    
    exit();
}

http_response_code(405);
echo json_encode(['error' => 'Method not allowed. Use GET.']);
?>
