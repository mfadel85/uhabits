import json
import boto3
from datetime import datetime, timedelta
from decimal import Decimal

def process_real_data(items):
    """
    Process DynamoDB items into the dashboard format
    """
    print(f"🔄 Processing {len(items)} DynamoDB records...")
    
    # Group habits by category
    groups = {
        'Religious': {'habits': [], 'total_score': 0, 'count': 0},
        'Career & Work': {'habits': [], 'total_score': 0, 'count': 0},
        'Social & Family': {'habits': [], 'total_score': 0, 'count': 0},
        'Personal Improvement': {'habits': [], 'total_score': 0, 'count': 0}
    }
    
    # Track unique habits by name
    habit_data = {}
    
    for item in items:
        try:
            # Extract habit information
            habit_name = item.get('habit_name') or item.get('habitName') or item.get('name', 'Unknown Habit')
            
            # Skip if no habit name
            if habit_name == 'Unknown Habit':
                continue
                
            # Get category (try multiple field names)
            category = (item.get('category') or 
                       item.get('group') or 
                       item.get('habit_category') or 
                       item.get('habitCategory') or 
                       'Personal Improvement')  # default
            
            # Map category to our standard groups
            if 'relig' in category.lower() or 'prayer' in habit_name.lower() or 'quran' in habit_name.lower():
                group_key = 'Religious'
            elif 'career' in category.lower() or 'work' in category.lower() or 'job' in habit_name.lower():
                group_key = 'Career & Work'
            elif 'social' in category.lower() or 'family' in category.lower() or 'friend' in habit_name.lower():
                group_key = 'Social & Family'
            else:
                group_key = 'Personal Improvement'
            
            # Get performance data
            success_rate = float(item.get('success_rate', 0))
            if isinstance(item.get('success_rate'), Decimal):
                success_rate = float(item.get('success_rate'))
            
            # Initialize habit if not seen before
            if habit_name not in habit_data:
                habit_data[habit_name] = {
                    'name': habit_name,
                    'category': group_key,
                    'success_rate': success_rate,
                    'priority': item.get('priority', 'NORMAL'),
                    'streak': int(item.get('streak', 0)) if item.get('streak') else 0,
                    'records': 1
                }
            else:
                # Update with average success rate
                habit_data[habit_name]['records'] += 1
                habit_data[habit_name]['success_rate'] = (
                    habit_data[habit_name]['success_rate'] + success_rate
                ) / 2
                
        except Exception as e:
            print(f"⚠️ Error processing item: {e}")
            continue
    
    # Convert to groups format
    for habit_name, habit_info in habit_data.items():
        group_key = habit_info['category']
        if group_key in groups:
            groups[group_key]['habits'].append(habit_info)
            groups[group_key]['total_score'] += habit_info['success_rate']
            groups[group_key]['count'] += 1
    
    # Build final response format
    group_performance = {}
    total_habits = 0
    
    for group_name, group_data in groups.items():
        habit_count = len(group_data['habits'])
        if habit_count > 0:
            avg_score = group_data['total_score'] / habit_count
            grade = 'A' if avg_score >= 0.9 else 'B' if avg_score >= 0.8 else 'C' if avg_score >= 0.7 else 'D' if avg_score >= 0.6 else 'F'
            
            # Get icon
            icon = {'Religious': '🕌', 'Career & Work': '💼', 'Social & Family': '👨‍👩‍👧‍👦', 'Personal Improvement': '🌟'}[group_name]
            
            group_performance[group_name] = {
                'habit_count': habit_count,
                'simple_average': avg_score,
                'weighted_average': avg_score,
                'grade': grade,
                'status': icon,
                'icon': icon,
                'habits': group_data['habits']
            }
            total_habits += habit_count
    
    print(f"✅ Processed {total_habits} unique habits across {len(group_performance)} groups")
    
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

def lambda_handler(event, context):
    """
    Minimal working API for uHabits dashboard
    Returns mock data in the expected format
    """
    
    # Simple CORS headers
    headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type,x-api-key',
        'Access-Control-Allow-Methods': 'GET,OPTIONS'
    }
    
    # Handle preflight requests
    if event.get('httpMethod') == 'OPTIONS':
        return {
            'statusCode': 200,
            'headers': headers,
            'body': ''
        }
    
    # Log the incoming request for debugging
    print(f"Received request: {json.dumps(event)}")
    
    try:
        # Try to connect to DynamoDB with timeout protection
        print("📊 Attempting to connect to DynamoDB...")
        
        try:
            import signal
            
            def timeout_handler(signum, frame):
                raise Exception("DynamoDB connection timeout")
            
            # Set 2-second timeout for DynamoDB operations
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(2)
            
            dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
            table = dynamodb.Table('uHabits-Analytics-prod')
            
            print("✅ DynamoDB connection established")
            print("� Scanning DynamoDB for habit data...")
            
            # Try a very small scan first
            response = table.scan(Limit=10)
            signal.alarm(0)  # Cancel the alarm
            
        except Exception as dynamo_error:
            signal.alarm(0)  # Cancel the alarm
            print(f"❌ DynamoDB error: {str(dynamo_error)}")
            print("🔄 Falling back to mock data due to DynamoDB issue")
            raise Exception("DynamoDB_FAILED")
        
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
        if str(e) == "DynamoDB_FAILED":
            print("⚠️ DynamoDB access failed, using fallback mock data")
        else:
            print(f"❌ Error in lambda_handler: {str(e)}")
        
        # Fall through to mock data
    
    # Fallback mock data
    print("🔄 Using fallback mock data")
    real_data = {
            'group_performance': {
                'Religious': {
                    'habit_count': 8,
                    'simple_average': 0.75,
                    'weighted_average': 0.75,
                    'grade': 'B',
                    'status': '🕌',
                    'icon': '🕌',
                    'habits': [
                        {'name': 'Morning Prayer', 'success_rate': 0.85, 'priority': 'HIGH', 'streak': 12, 'category': 'Religious'},
                        {'name': 'Evening Prayer', 'success_rate': 0.92, 'priority': 'HIGH', 'streak': 15, 'category': 'Religious'},
                        {'name': 'Quran Reading', 'success_rate': 0.78, 'priority': 'HIGH', 'streak': 8, 'category': 'Religious'},
                        {'name': 'Friday Prayer', 'success_rate': 0.68, 'priority': 'NORMAL', 'streak': 4, 'category': 'Religious'},
                        {'name': 'Dhikr', 'success_rate': 0.72, 'priority': 'NORMAL', 'streak': 6, 'category': 'Religious'},
                        {'name': 'Islamic Study', 'success_rate': 0.65, 'priority': 'NORMAL', 'streak': 3, 'category': 'Religious'},
                        {'name': 'Charity', 'success_rate': 0.80, 'priority': 'HIGH', 'streak': 9, 'category': 'Religious'},
                        {'name': 'Night Prayer', 'success_rate': 0.58, 'priority': 'LOW', 'streak': 2, 'category': 'Religious'}
                    ]
                },
                'Career & Work': {
                    'habit_count': 5,
                    'simple_average': 0.81,
                    'weighted_average': 0.81,
                    'grade': 'A',
                    'status': '💼',
                    'icon': '💼',
                    'habits': [
                        {'name': 'Daily Planning', 'success_rate': 0.88, 'priority': 'HIGH', 'streak': 11, 'category': 'Career & Work'},
                        {'name': 'Skill Development', 'success_rate': 0.82, 'priority': 'HIGH', 'streak': 7, 'category': 'Career & Work'},
                        {'name': 'Project Review', 'success_rate': 0.75, 'priority': 'NORMAL', 'streak': 5, 'category': 'Career & Work'},
                        {'name': 'Network Building', 'success_rate': 0.68, 'priority': 'NORMAL', 'streak': 3, 'category': 'Career & Work'},
                        {'name': 'Industry Reading', 'success_rate': 0.92, 'priority': 'HIGH', 'streak': 14, 'category': 'Career & Work'}
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
                    'data_source': 'Fallback Mock Data (No Real Data Found)',
                    'habits_found': 25,
                    'last_sync': datetime.now().isoformat()
                },
                'last_updated': datetime.now().isoformat(),
                'status': 'success',
                'message': 'Analytics API using fallback mock data - no processable data in DynamoDB'
            }
        
        # Use real_data instead of mock_data
        print(f"✅ Returning data with {len(real_data['group_performance'])} groups")
        
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(real_data, default=str)
        }
        
    except Exception as e:
        print(f"❌ Error in lambda_handler: {str(e)}")
        import traceback
        traceback.print_exc()
        
        # Return error response
        error_response = {
            'error': 'Internal server error',
            'message': str(e),
            'status': 'failed'
        }
        
        return {
            'statusCode': 500,
            'headers': headers,
            'body': json.dumps(error_response)
        }
