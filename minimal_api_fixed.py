import json
from datetime import datetime

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
        # Return mock data that matches the expected dashboard format
        mock_data = {
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
                'data_source': 'Mock Data for Testing',
                'habits_found': 25,
                'last_sync': datetime.now().isoformat()
            },
            'last_updated': datetime.now().isoformat(),
            'status': 'success',
            'message': 'Analytics API is working correctly with mock data'
        }
        
        print(f"✅ Returning mock data with {len(mock_data['group_performance'])} groups")
        
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(mock_data, default=str)
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
