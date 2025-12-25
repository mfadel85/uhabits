<?php
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

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);

        // Get latest sync data with is_numerical flag
        $stmt = $pdo->query("
            SELECT 
                habit_name,
                habit_group,
                category,
                priority,
                frequency,
                score,
                streak_length,
                is_numerical,
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
                    'count' => 0,
                    'weighted_sum' => 0,
                    'weight_total' => 0
                ];
            }
            
            $score = floatval($habit['score']);
            $weight = 6 - $priority; // Higher priority = higher weight (5,4,3,2,1)
            
            // Parse checkmarks if it's a JSON string
            $checkmarks = $habit['checkmarks'];
            if (is_string($checkmarks)) {
                $checkmarks = json_decode($checkmarks, true) ?? [];
            }
            
            $categoryData[$category]['habits'][] = [
                'name' => $habit['habit_name'],
                'score' => $score,
                'frequency' => (int)$habit['frequency'],
                'priority' => $priority,
                'streak' => (int)$habit['streak_length'],
                'is_numerical' => (bool)$habit['is_numerical'],
                'checkmarks' => $checkmarks
            ];
            
            $categoryData[$category]['total_score'] += $score;
            $categoryData[$category]['count']++;
            $categoryData[$category]['weighted_sum'] += $score * $weight;
            $categoryData[$category]['weight_total'] += $weight;
            
            $priorityCounts[$priority]++;
        }
        
        // Calculate category performance and grades
        // Use object (associative array) keyed by category name for dashboard compatibility
        $groupPerformance = [];
        $totalWeightedSum = 0;
        $totalWeight = 0;
        $categoryDistribution = [];
        
        foreach ($categoryData as $category => $data) {
            $weightedAvg = $data['weight_total'] > 0 
                ? $data['weighted_sum'] / $data['weight_total']
                : 0;
            
            // Calculate grade based on weighted average (0-100 scale)
            $grade = 'F';
            if ($weightedAvg >= 90) $grade = 'A';
            elseif ($weightedAvg >= 80) $grade = 'B';
            elseif ($weightedAvg >= 70) $grade = 'C';
            elseif ($weightedAvg >= 60) $grade = 'D';
            
            // Key by category name (object format for JavaScript dashboard)
            $groupPerformance[$category] = [
                'weighted_average' => $weightedAvg / 100, // Return as decimal 0-1 for compatibility
                'grade' => $grade,
                'habit_count' => $data['count'],
                'habits' => $data['habits']
            ];
            
            $categoryDistribution[] = [
                'category' => $category,
                'count' => $data['count']
            ];
            
            $totalWeightedSum += $data['weighted_sum'];
            $totalWeight += $data['weight_total'];
        }
        
        $overallAverage = $totalWeight > 0 ? $totalWeightedSum / $totalWeight : 0;
        
        // Build response - group_performance is now an object keyed by category name
        echo json_encode([
            'summary' => [
                'total_habits' => count($habits),
                'active_habits' => count($habits),
                'overall_average' => $overallAverage / 100, // As decimal 0-1
                'data_source' => 'VPS MySQL Database',
                'last_sync' => date('Y-m-d H:i:s', $latestSync / 1000)
            ],
            'group_performance' => (object) $groupPerformance, // Force JSON object output
            'category_distribution' => $categoryDistribution,
            'priority_distribution' => $priorityCounts
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Failed to retrieve data: ' . $e->getMessage()]);
        error_log("Groups API error: " . $e->getMessage());
    }
    
    exit();
}

http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
