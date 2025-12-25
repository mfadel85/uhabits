import json
from datetime import datetime

def lambda_handler(event, context):
    """
    Safe version - handles DynamoDB access gracefully
    """
    
    headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type,x-api-key',
        'Access-Control-Allow-Methods': 'GET,OPTIONS'
    }
    
    if event.get('httpMethod') == 'OPTIONS':
        return {'statusCode': 200, 'headers': headers, 'body': ''}
    
    print(f"🔍 Received request: {json.dumps(event)}")
    
    try:
        # Try to import boto3 and access DynamoDB
        print("📊 Attempting to connect to DynamoDB...")
        
        try:
            import boto3
            print("✅ boto3 imported successfully")
            
            # Try to connect to DynamoDB
            dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
            table = dynamodb.Table('uHabits-Analytics-prod')
            print("✅ DynamoDB connection established")
            
            # Try a small scan first
            print("🔍 Scanning DynamoDB (limit 10)...")
            response = table.scan(Limit=10)
            items = response.get('Items', [])
            
            print(f"✅ Found {len(items)} items in DynamoDB")
            
            if items:
                # We have real data! Process a few items
                processed_habits = []
                for item in items[:5]:  # Process max 5 items to avoid timeout
                    habit_name = (item.get('habit_name') or 
                                item.get('habitName') or 
                                item.get('name') or 
                                f'Habit_{len(processed_habits)+1}')
                    
                    success_rate = 0.7  # Default
                    if 'success_rate' in item:
                        try:
                            success_rate = float(item['success_rate'])
                        except:
                            success_rate = 0.7
                    
                    processed_habits.append({
                        'name': habit_name,
                        'success_rate': success_rate,
                        'priority': item.get('priority', 'NORMAL'),
                        'streak': int(item.get('streak', 0)) if item.get('streak') else 0,
                        'category': 'Personal Improvement'  # Default for now
                    })
                
                real_data = {
                    'group_performance': {
                        'Personal Improvement': {
                            'habit_count': len(processed_habits),
                            'simple_average': sum(h['success_rate'] for h in processed_habits) / len(processed_habits),
                            'weighted_average': sum(h['success_rate'] for h in processed_habits) / len(processed_habits),
                            'grade': 'B',
                            'status': '🌟',
                            'icon': '🌟',
                            'habits': processed_habits
                        }
                    },
                    'summary': {
                        'total_groups': 1,
                        'total_habits_analyzed': len(processed_habits),
                        'data_source': 'Real DynamoDB Data (Limited)',
                        'habits_found': len(processed_habits),
                        'last_sync': datetime.now().isoformat()
                    },
                    'last_updated': datetime.now().isoformat(),
                    'status': 'success',
                    'message': f'Analytics API loaded {len(processed_habits)} real habits from DynamoDB'
                }
                
                print(f"✅ Returning {len(processed_habits)} real habits")
                return {
                    'statusCode': 200,
                    'headers': headers,
                    'body': json.dumps(real_data, default=str)
                }
                
            else:
                print("⚠️ No items found in DynamoDB")
                
        except Exception as dynamo_error:
            print(f"❌ DynamoDB error: {str(dynamo_error)}")
            # Fall through to mock data
        
        # Fallback to mock data
        print("🔄 Using fallback mock data")
        mock_data = {
            'group_performance': {
                'Religious': {
                    'habit_count': 3,
                    'simple_average': 0.75,
                    'weighted_average': 0.75,
                    'grade': 'B',
                    'status': '🕌',
                    'icon': '🕌',
                    'habits': [
                        {'name': 'Morning Prayer', 'success_rate': 0.85, 'priority': 'HIGH', 'streak': 12, 'category': 'Religious'},
                        {'name': 'Evening Prayer', 'success_rate': 0.70, 'priority': 'HIGH', 'streak': 8, 'category': 'Religious'},
                        {'name': 'Quran Reading', 'success_rate': 0.70, 'priority': 'NORMAL', 'streak': 5, 'category': 'Religious'}
                    ]
                },
                'Personal Improvement': {
                    'habit_count': 3,
                    'simple_average': 0.78,
                    'weighted_average': 0.78,
                    'grade': 'B',
                    'status': '🌟',
                    'icon': '🌟',
                    'habits': [
                        {'name': 'Exercise', 'success_rate': 0.80, 'priority': 'HIGH', 'streak': 10, 'category': 'Personal Improvement'},
                        {'name': 'Reading', 'success_rate': 0.85, 'priority': 'NORMAL', 'streak': 15, 'category': 'Personal Improvement'},
                        {'name': 'Meditation', 'success_rate': 0.70, 'priority': 'NORMAL', 'streak': 6, 'category': 'Personal Improvement'}
                    ]
                }
            },
            'summary': {
                'total_groups': 2,
                'total_habits_analyzed': 6,
                'data_source': 'Fallback Mock Data',
                'habits_found': 6,
                'last_sync': datetime.now().isoformat()
            },
            'last_updated': datetime.now().isoformat(),
            'status': 'success',
            'message': 'Analytics API using safe fallback data'
        }
        
        print("✅ Returning mock data")
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(mock_data, default=str)
        }
        
    except Exception as e:
        print(f"❌ Critical error: {str(e)}")
        
        # Emergency response
        emergency_data = {
            'error': 'Lambda function error',
            'message': str(e),
            'status': 'failed',
            'timestamp': datetime.now().isoformat()
        }
        
        return {
            'statusCode': 200,  # Return 200 so dashboard can show error
            'headers': headers,
            'body': json.dumps(emergency_data)
        }
