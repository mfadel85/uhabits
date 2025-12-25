import json
import boto3
from datetime import datetime
from decimal import Decimal
import signal

def get_category_icon(category):
    """Return icon for category"""
    icons = {
        'Health & Fitness': '💪',
        'Work & Productivity': '💼', 
        'Social & Family': '👨‍👩‍👧‍👦',
        'Personal Improvement': '🌟',
        'Finance & Money': '💰',
        'Learning & Education': '📚',
        'Hobbies & Recreation': '🎨',
        'Travel & Adventure': '✈️',
        'Spiritual & Mindfulness': '🧘',
        'Home & Environment': '🏠'
    }
    return icons.get(category, '📊')

def calculate_grade(average):
    """Calculate letter grade from average"""
    if average >= 0.90: return 'A+'
    elif average >= 0.85: return 'A'
    elif average >= 0.80: return 'A-'
    elif average >= 0.75: return 'B+'
    elif average >= 0.70: return 'B'
    elif average >= 0.65: return 'B-'
    elif average >= 0.60: return 'C+'
    elif average >= 0.55: return 'C'
    elif average >= 0.50: return 'C-'
    elif average >= 0.45: return 'D+'
    elif average >= 0.40: return 'D'
    else: return 'F'

def process_real_data(items):
    """Process DynamoDB items into dashboard format"""
    try:
        print(f"📊 Processing {len(items)} DynamoDB records...")
        
        # Group habits by category
        categories = {}
        total_habits = 0
        
        for item in items:
            try:
                habit_name = item.get('habitName', 'Unknown Habit')
                category = item.get('category', 'Uncategorized')
                
                # Convert Decimal to float for success_rate
                success_rate = item.get('successRate', 0)
                if isinstance(success_rate, Decimal):
                    success_rate = float(success_rate)
                
                # Get other fields with defaults
                streak = int(item.get('currentStreak', 0))
                priority = item.get('priority', 'NORMAL')
                
                # Initialize category if not exists
                if category not in categories:
                    categories[category] = []
                
                # Add habit to category
                categories[category].append({
                    'name': habit_name,
                    'success_rate': round(success_rate, 2),
                    'priority': priority,
                    'streak': streak,
                    'category': category
                })
                
                total_habits += 1
                
            except Exception as e:
                print(f"⚠️ Error processing item {item}: {str(e)}")
                continue
        
        # Build group performance
        group_performance = {}
        
        for category, habits in categories.items():
            if not habits:
                continue
                
            # Calculate averages
            success_rates = [h['success_rate'] for h in habits]
            simple_avg = sum(success_rates) / len(success_rates)
            
            # Priority weights
            priority_weights = {'HIGH': 1.5, 'NORMAL': 1.0, 'LOW': 0.5}
            weighted_sum = sum(h['success_rate'] * priority_weights.get(h['priority'], 1.0) for h in habits)
            weight_total = sum(priority_weights.get(h['priority'], 1.0) for h in habits)
            weighted_avg = weighted_sum / weight_total if weight_total > 0 else simple_avg
            
            group_performance[category] = {
                'habit_count': len(habits),
                'simple_average': round(simple_avg, 2),
                'weighted_average': round(weighted_avg, 2),
                'grade': calculate_grade(weighted_avg),
                'status': get_category_icon(category),
                'icon': get_category_icon(category),
                'habits': habits
            }
        
        print(f"✅ Processed {total_habits} habits into {len(group_performance)} categories")
        
        return {
            'group_performance': group_performance,
            'summary': {
                'total_groups': len(group_performance),
                'total_habits_analyzed': total_habits,
                'data_source': 'Real DynamoDB Data',
                'habits_found': total_habits,
                'last_sync': datetime.now().isoformat()
            },
            'last_updated': datetime.now().isoformat(),
            'status': 'success',
            'message': f'Analytics API loaded {total_habits} real habits from DynamoDB'
        }
        
    except Exception as e:
        print(f"❌ Error in process_real_data: {str(e)}")
        return {
            'group_performance': {},
            'summary': {
                'total_groups': 0,
                'total_habits_analyzed': 0,
                'data_source': 'Processing Failed',
                'habits_found': 0,
                'last_sync': datetime.now().isoformat()
            },
            'last_updated': datetime.now().isoformat(),
            'status': 'error',
            'message': f'Failed to process DynamoDB data: {str(e)}'
        }

def lambda_handler(event, context):
    print("🚀 Lambda handler starting...")
    
    headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
    }
    
    try:
        if event.get('httpMethod') == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': headers,
                'body': json.dumps({'message': 'CORS preflight successful'})
            }
            
        # Set up timeout protection
        def timeout_handler(signum, frame):
            print("❌ DynamoDB operation timed out!")
            raise Exception("DynamoDB_FAILED")
        
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(2)  # 2 second timeout
        
        # Try to read from DynamoDB
        print("🔄 Connecting to DynamoDB...")
        dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
        table = dynamodb.Table('uHabits-Analytics-prod')
        
        print("🔄 Scanning DynamoDB table...")
        response = table.scan()
        signal.alarm(0)  # Cancel alarm on success
        
        items = response.get('Items', [])
        print(f"✅ Found {len(items)} records in DynamoDB")
        
        if items:
            print("🔄 Processing real DynamoDB data...")
            # Process the real data into groups
            real_data = process_real_data(items)
            
            if real_data['group_performance'] and sum(g.get('habit_count', 0) for g in real_data['group_performance'].values()) > 0:
                print(f"✅ Successfully processed {sum(g.get('habit_count', 0) for g in real_data['group_performance'].values())} habits")
                print(f"✅ Returning data with {len(real_data['group_performance'])} groups")
                
                return {
                    'statusCode': 200,
                    'headers': headers,
                    'body': json.dumps(real_data, default=str)
                }
        
        # If we get here, either no items or processing failed
        print("⚠️ No processable data found, using fallback mock data")
        
    except Exception as e:
        signal.alarm(0)  # Cancel alarm on error
        if str(e) == "DynamoDB_FAILED":
            print("⚠️ DynamoDB access failed, using fallback mock data")
        else:
            print(f"❌ Error accessing DynamoDB: {str(e)}")
        
        # Fall through to mock data
    
    # Fallback mock data
    print("🔄 Using fallback mock data")
    real_data = {
        'group_performance': {
            'Health & Fitness': {
                'habit_count': 7,
                'simple_average': 0.65,
                'weighted_average': 0.65,
                'grade': 'C+',
                'status': '💪',
                'icon': '💪',
                'habits': [
                    {'name': 'Morning Walk', 'success_rate': 0.75, 'priority': 'HIGH', 'streak': 12, 'category': 'Health & Fitness'},
                    {'name': 'Gym Workout', 'success_rate': 0.60, 'priority': 'HIGH', 'streak': 7, 'category': 'Health & Fitness'},
                    {'name': 'Healthy Breakfast', 'success_rate': 0.82, 'priority': 'HIGH', 'streak': 15, 'category': 'Health & Fitness'},
                    {'name': 'Water Intake', 'success_rate': 0.88, 'priority': 'NORMAL', 'streak': 18, 'category': 'Health & Fitness'},
                    {'name': 'Yoga Session', 'success_rate': 0.45, 'priority': 'NORMAL', 'streak': 3, 'category': 'Health & Fitness'},
                    {'name': 'Evening Run', 'success_rate': 0.38, 'priority': 'LOW', 'streak': 2, 'category': 'Health & Fitness'},
                    {'name': 'Stretching', 'success_rate': 0.67, 'priority': 'NORMAL', 'streak': 8, 'category': 'Health & Fitness'}
                ]
            },
            'Work & Productivity': {
                'habit_count': 6,
                'simple_average': 0.72,
                'weighted_average': 0.72,
                'grade': 'B-',
                'status': '💼',
                'icon': '💼',
                'habits': [
                    {'name': 'Daily Planning', 'success_rate': 0.85, 'priority': 'HIGH', 'streak': 14, 'category': 'Work & Productivity'},
                    {'name': 'Focus Time', 'success_rate': 0.78, 'priority': 'HIGH', 'streak': 11, 'category': 'Work & Productivity'},
                    {'name': 'Email Cleanup', 'success_rate': 0.65, 'priority': 'NORMAL', 'streak': 6, 'category': 'Work & Productivity'},
                    {'name': 'Skills Learning', 'success_rate': 0.58, 'priority': 'NORMAL', 'streak': 4, 'category': 'Work & Productivity'},
                    {'name': 'Weekly Review', 'success_rate': 0.42, 'priority': 'LOW', 'streak': 1, 'category': 'Work & Productivity'},
                    {'name': 'Network Building', 'success_rate': 0.50, 'priority': 'LOW', 'streak': 3, 'category': 'Work & Productivity'}
                ]
            },
            'Social & Family': {
                'habit_count': 6,
                'simple_average': 0.70,
                'weighted_average': 0.70,
                'grade': 'B',
                'status': '👨‍👩‍👧‍👦',
                'icon': '👨‍👩‍👧‍👦',
                'habits': [
                    {'name': 'Family Time', 'success_rate': 0.78, 'priority': 'HIGH', 'streak': 8, 'category': 'Social & Family'},
                    {'name': 'Call Parents', 'success_rate': 0.85, 'priority': 'HIGH', 'streak': 12, 'category': 'Social & Family'},
                    {'name': 'Friends Meetup', 'success_rate': 0.62, 'priority': 'NORMAL', 'streak': 4, 'category': 'Social & Family'},
                    {'name': 'Community Service', 'success_rate': 0.70, 'priority': 'NORMAL', 'streak': 6, 'category': 'Social & Family'},
                    {'name': 'Social Media Connect', 'success_rate': 0.55, 'priority': 'LOW', 'streak': 2, 'category': 'Social & Family'},
                    {'name': 'Extended Family', 'success_rate': 0.73, 'priority': 'NORMAL', 'streak': 7, 'category': 'Social & Family'}
                ]
            },
            'Personal Improvement': {
                'habit_count': 6,
                'simple_average': 0.74,
                'weighted_average': 0.74,
                'grade': 'B',
                'status': '🌟',
                'icon': '🌟',
                'habits': [
                    {'name': 'Morning Exercise', 'success_rate': 0.72, 'priority': 'HIGH', 'streak': 9, 'category': 'Personal Improvement'},
                    {'name': 'Daily Reading', 'success_rate': 0.88, 'priority': 'HIGH', 'streak': 13, 'category': 'Personal Improvement'},
                    {'name': 'Meditation', 'success_rate': 0.65, 'priority': 'NORMAL', 'streak': 5, 'category': 'Personal Improvement'},
                    {'name': 'Journal Writing', 'success_rate': 0.70, 'priority': 'NORMAL', 'streak': 8, 'category': 'Personal Improvement'},
                    {'name': 'Healthy Eating', 'success_rate': 0.68, 'priority': 'HIGH', 'streak': 4, 'category': 'Personal Improvement'},
                    {'name': 'Sleep Schedule', 'success_rate': 0.82, 'priority': 'HIGH', 'streak': 10, 'category': 'Personal Improvement'}
                ]
            }
        },
        'summary': {
            'total_groups': 4,
            'total_habits_analyzed': 25,
            'data_source': 'Fallback Mock Data (DynamoDB Failed)',
            'habits_found': 25,
            'last_sync': datetime.now().isoformat()
        },
        'last_updated': datetime.now().isoformat(),
        'status': 'success',
        'message': 'Analytics API using fallback mock data - DynamoDB timeout or access failed'
    }
    
    print(f"✅ Returning fallback data with {len(real_data['group_performance'])} groups")
    
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps(real_data, default=str)
    }
