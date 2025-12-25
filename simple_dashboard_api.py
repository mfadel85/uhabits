import json
import boto3
from datetime import datetime
from decimal import Decimal
import logging

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def decimal_default(obj):
    """JSON serializer for Decimal objects"""
    if isinstance(obj, Decimal):
        return float(obj)
    raise TypeError

def lambda_handler(event, context):
    """
    Simple API for dashboard access with better error handling
    """
    
    try:
        logger.info(f"Event: {json.dumps(event)}")
        
        # Extract path and method
        path = event.get('path', '/')
        method = event.get('httpMethod', 'GET')
        
        logger.info(f"Path: {path}, Method: {method}")
        
        # CORS headers
        headers = {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Headers': 'Content-Type',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
        }
        
        # Handle preflight requests
        if method == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': headers,
                'body': ''
            }
        
        # Initialize DynamoDB with error handling
        try:
            dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
            table = dynamodb.Table('uHabits-Analytics-prod')
            logger.info("DynamoDB connection established")
        except Exception as db_error:
            logger.error(f"DynamoDB connection error: {str(db_error)}")
            return {
                'statusCode': 500,
                'headers': headers,
                'body': json.dumps({
                    'error': 'Database connection failed',
                    'message': str(db_error)
                })
            }
        
        # Route to appropriate handler
        if '/api/groups' in path or '/groups' in path:
            return get_group_analytics(headers, table)
        else:
            return get_simple_response(headers)
            
    except Exception as e:
        logger.error(f"Lambda handler error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type',
                'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
            },
            'body': json.dumps({
                'error': 'Internal server error',
                'message': str(e),
                'timestamp': datetime.now().isoformat()
            })
        }

def get_simple_response(headers):
    """Return a simple test response"""
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps({
            'status': 'API is working',
            'timestamp': datetime.now().isoformat(),
            'message': 'Dashboard API is responding correctly'
        })
    }

def get_group_analytics(headers, table):
    """Get group-based analytics and performance data with error handling"""
    
    try:
        logger.info("Starting group analytics query")
        
        # Test DynamoDB connection first
        try:
            # Simple scan to test connectivity
            test_response = table.scan(Limit=1)
            logger.info(f"DynamoDB test successful, scanned items: {test_response.get('Count', 0)}")
        except Exception as scan_error:
            logger.error(f"DynamoDB scan test failed: {str(scan_error)}")
            return {
                'statusCode': 500,
                'headers': headers,
                'body': json.dumps({
                    'error': 'Database query failed',
                    'message': str(scan_error)
                })
            }
        
        # Scan for all habit data - look at DAILY records since that's where the real data is
        try:
            # First try to get HABIT# records
            habits_response = table.scan(
                FilterExpression='begins_with(SK, :sk_prefix)',
                ExpressionAttributeValues={':sk_prefix': 'HABIT#'},
                Limit=50
            )
            habit_records = habits_response.get('Items', [])
            logger.info(f"Found {len(habit_records)} HABIT# records")
            
            # Get DAILY# records efficiently using pagination
            daily_records = []
            last_evaluated_key = None
            scan_count = 0
            max_scans = 3  # Limit to 3 scans to avoid timeout
            
            while scan_count < max_scans:
                scan_kwargs = {
                    'FilterExpression': 'begins_with(SK, :sk_prefix)',
                    'ExpressionAttributeValues': {':sk_prefix': 'DAILY#'},
                    'Limit': 300
                }
                
                if last_evaluated_key:
                    scan_kwargs['ExclusiveStartKey'] = last_evaluated_key
                
                scan_response = table.scan(**scan_kwargs)
                daily_records.extend(scan_response.get('Items', []))
                
                last_evaluated_key = scan_response.get('LastEvaluatedKey')
                scan_count += 1
                
                if not last_evaluated_key:
                    break
            
            logger.info(f"Found {len(daily_records)} DAILY# records in {scan_count} scans")
            
            # Extract unique habits from daily records efficiently
            unique_habits = {}
            processed_count = 0
            
            for record in daily_records:
                try:
                    habit_id = record.get('habit_id')
                    habit_name = record.get('habit_name')
                    
                    if not habit_id or not habit_name:
                        continue
                        
                    if habit_id not in unique_habits:
                        # Standardize category
                        raw_category = record.get('category', 'Personal Improvement')
                        if 'religious' in str(raw_category).lower():
                            category = 'Religious'
                        elif 'career' in str(raw_category).lower() or 'work' in str(raw_category).lower():
                            category = 'Career & Work'
                        elif 'social' in str(raw_category).lower() or 'family' in str(raw_category).lower():
                            category = 'Social & Family'
                        else:
                            category = 'Personal Improvement'
                            
                        unique_habits[habit_id] = {
                            'habit_id': habit_id,
                            'habit_name': habit_name,
                            'category': category,
                            'priority': record.get('priority', 'NORMAL'),
                            'success_rate': 0.0,
                            'completed_count': 0,
                            'total_count': 0
                        }
                    
                    # Count completions for success rate calculation
                    unique_habits[habit_id]['total_count'] += 1
                    if record.get('completed'):
                        unique_habits[habit_id]['completed_count'] += 1
                    
                    processed_count += 1
                    
                except Exception as record_error:
                    logger.warning(f"Error processing record {processed_count}: {str(record_error)}")
                    continue
            
            logger.info(f"Processed {processed_count} records, found {len(unique_habits)} unique habits")
            
            # Calculate success rates
            for habit_id, habit_data in unique_habits.items():
                if habit_data['total_count'] > 0:
                    habit_data['success_rate'] = habit_data['completed_count'] / habit_data['total_count']
            
            # Convert to list and combine with any HABIT# records
            habits = list(unique_habits.values())
            
            # Add any HABIT# records that aren't already included
            for habit_record in habit_records:
                habit_id = habit_record.get('habit_id')
                if habit_id not in unique_habits:
                    habits.append(habit_record)
            
            logger.info(f"Total unique habits found: {len(habits)}")
            
        except Exception as habit_error:
            logger.error(f"Habit query failed: {str(habit_error)}")
            # Return basic structure even if query fails
            habits = []
        
        # Create group performance data
        group_performance = {
            'Religious': {
                'habit_count': 0,
                'simple_average': 0.0,
                'weighted_average': 0.0,
                'grade': 'N/A',
                'status': '🕌',
                'icon': '🕌',
                'habits': []
            },
            'Career & Work': {
                'habit_count': 0,
                'simple_average': 0.0,
                'weighted_average': 0.0,
                'grade': 'N/A',
                'status': '💼',
                'icon': '💼',
                'habits': []
            },
            'Social & Family': {
                'habit_count': 0,
                'simple_average': 0.0,
                'weighted_average': 0.0,
                'grade': 'N/A',
                'status': '👨‍👩‍👧‍👦',
                'icon': '👨‍👩‍👧‍👦',
                'habits': []
            },
            'Personal Improvement': {
                'habit_count': 0,
                'simple_average': 0.0,
                'weighted_average': 0.0,
                'grade': 'N/A',
                'status': '🌟',
                'icon': '🌟',
                'habits': []
            }
        }
        
        # Process habits if we have data
        if habits:
            for habit in habits:
                try:
                    # Get category (look for both 'category' and 'group' fields)
                    category = habit.get('category') or habit.get('group') or 'Personal Improvement'
                    
                    # Standardize category names
                    if 'religious' in category.lower():
                        category = 'Religious'
                    elif 'career' in category.lower() or 'work' in category.lower():
                        category = 'Career & Work'
                    elif 'social' in category.lower() or 'family' in category.lower():
                        category = 'Social & Family'
                    else:
                        category = 'Personal Improvement'
                    
                    # Ensure category exists in our structure
                    if category not in group_performance:
                        category = 'Personal Improvement'
                    
                    # Get success rate safely
                    success_rate = 0.0
                    if 'success_rate' in habit:
                        try:
                            success_rate = float(habit['success_rate'])
                        except (ValueError, TypeError):
                            success_rate = 0.0
                    
                    # Add to group
                    group_performance[category]['habit_count'] += 1
                    group_performance[category]['habits'].append({
                        'name': habit.get('habit_name', 'Unknown'),
                        'success_rate': success_rate,
                        'priority': habit.get('priority', 'NORMAL')
                    })
                    
                except Exception as process_error:
                    logger.warning(f"Error processing habit: {str(process_error)}")
                    continue
            
            # Calculate averages for groups with habits
            for group, data in group_performance.items():
                if data['habit_count'] > 0:
                    rates = [h['success_rate'] for h in data['habits']]
                    data['simple_average'] = round(sum(rates) / len(rates), 3)
                    data['weighted_average'] = data['simple_average']  # Simplified for now
                    
                    # Assign grade
                    avg = data['weighted_average']
                    if avg >= 0.85:
                        data['grade'] = 'A'
                        data['status'] = '🌟'
                    elif avg >= 0.7:
                        data['grade'] = 'B'
                        data['status'] = '✅'
                    elif avg >= 0.55:
                        data['grade'] = 'C'
                        data['status'] = '⚠️'
                    else:
                        data['grade'] = 'D'
                        data['status'] = '❌'
        
        response_data = {
            'group_performance': group_performance,
            'summary': {
                'total_groups': len([g for g in group_performance.values() if g['habit_count'] > 0]),
                'total_habits_analyzed': sum(g['habit_count'] for g in group_performance.values()),
                'data_source': 'DynamoDB',
                'habits_found': len(habits)
            },
            'last_updated': datetime.now().isoformat()
        }
        
        logger.info(f"Returning group analytics with {len(habits)} habits processed")
        
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(response_data, default=decimal_default)
        }
        
    except Exception as e:
        logger.error(f"Group analytics error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': headers,
            'body': json.dumps({
                'error': 'Failed to generate group analytics',
                'message': str(e),
                'timestamp': datetime.now().isoformat()
            })
        }
