"""
AWS Lambda Function for uHabits Cloud Analytics Sync
Serverless backend optimized for single-user, low-volume usage

This function processes habit data from the uHabits Android app
and stores it in DynamoDB for PowerBI Pro integration.
"""

import json
import boto3
import logging
import os
from datetime import datetime
from decimal import Decimal

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize DynamoDB client
dynamodb = boto3.resource('dynamodb')
table_name = os.environ.get('TABLE_NAME', 'uHabits-Analytics-prod')
table = dynamodb.Table(table_name)

def lambda_handler(event, context):
    """
    Main Lambda handler for habit data sync and analytics
    
    POST /sync - Expected payload from Android app:
    {
        "user_id": "user_primary",
        "sync_timestamp": 1672531200000,
        "summary_metrics": {...},
        "priority_distribution": {...},
        "habits_data": [...]
    }
    
    GET /sync - Returns analytics data for dashboard
    """
    
    try:
        logger.info(f"Event received: {json.dumps(event, default=str)}")
        logger.info(f"Table name: {table_name}")
        
        # Check HTTP method
        http_method = event.get('httpMethod', 'POST')
        
        if http_method == 'GET':
            # Return analytics data for dashboard
            return handle_analytics_request(event, context)
        
        # Handle POST request for sync
        # Parse the request body
        if 'body' in event:
            # API Gateway integration
            body = json.loads(event['body'])
        else:
            # Direct Lambda invocation
            body = event
        
        logger.info(f"Processing sync request for user: {body.get('user_id', 'unknown')}")
        
        # Validate required fields
        required_fields = ['user_id', 'sync_timestamp', 'habits_data']
        for field in required_fields:
            if field not in body:
                return create_response(400, f"Missing required field: {field}")
        
        # Process the sync data
        result = process_habit_sync(body)
        
        logger.info(f"Sync completed successfully: {result['summary']}")
        
        # Add category standardization info to response if any were standardized
        category_info = ""
        if 'standardized_categories' in result and result['standardized_categories'] > 0:
            category_info = f" Standardized {result['standardized_categories']} categories."
        
        return create_response(200, {
            "status": "success",
            "message": f"Habit data synced successfully.{category_info}",
            "timestamp": datetime.utcnow().isoformat(),
            "processed": result
        })
        
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON payload: {str(e)}")
        return create_response(400, "Invalid JSON payload")
        
    except Exception as e:
        import traceback
        logger.error(f"Sync error: {str(e)}")
        logger.error(f"Traceback: {traceback.format_exc()}")
        return create_response(500, f"Internal server error: {str(e)}")

def process_habit_sync(sync_data):
    """
    Process and store habit synchronization data with enhanced performance history
    """
    user_id = sync_data['user_id']
    sync_timestamp = sync_data['sync_timestamp']
    
    # Convert float values to Decimal for DynamoDB
    def convert_floats(obj):
        if isinstance(obj, dict):
            return {k: convert_floats(v) for k, v in obj.items()}
        elif isinstance(obj, list):
            return [convert_floats(item) for item in obj]
        elif isinstance(obj, float):
            return Decimal(str(obj))
        else:
            return obj
    
    # Standardize category values to ensure consistency
    def standardize_category(category):
        if not category:
            return "Personal Improvement"  # Default category
            
        category_lower = category.lower()
        
        # Standard category mapping
        if "religious" in category_lower or "pray" in category_lower or "faith" in category_lower:
            return "Religious"
        elif "career" in category_lower or "work" in category_lower or "job" in category_lower:
            return "Career & Work"
        elif "social" in category_lower or "family" in category_lower or "friend" in category_lower:
            return "Social & Family"
        elif "personal" in category_lower or "health" in category_lower or "self" in category_lower:
            return "Personal Improvement"
            
        # If no match, return default
        return "Personal Improvement"
    
    # Prepare DynamoDB items
    converted_data = convert_floats(sync_data)
    
    # Store sync summary
    summary_item = {
        'PK': f"USER#{user_id}",
        'SK': f"SYNC#{sync_timestamp}",
        'user_id': user_id,
        'sync_timestamp': sync_timestamp,
        'sync_date': converted_data.get('sync_date'),
        'summary_metrics': converted_data.get('summary_metrics', {}),
        'priority_distribution': converted_data.get('priority_distribution', {}),
        'device_info': converted_data.get('device_info', {}),
        'metadata': converted_data.get('metadata', {}),
        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
    }
    
    # Store individual habit records with performance history
    habits_stored = 0
    performance_records = 0
    standardized_categories = 0
    
    with table.batch_writer() as batch:
        # Store summary
        batch.put_item(Item=summary_item)
        
        # Store individual habits with enhanced data
        for habit in converted_data.get('habits_data', []):
            habit_id = habit.get('id', 'unknown')
            
            # Main habit record
            # Standardize category to ensure consistency
            raw_category = habit.get('category')
            standardized_category = standardize_category(raw_category)
            
            # Log if category was changed to help with debugging
            if raw_category and raw_category != standardized_category:
                logger.info(f"Standardized category for habit '{habit.get('name')}': '{raw_category}' → '{standardized_category}'")
                standardized_categories += 1
            
            habit_item = {
                'PK': f"USER#{user_id}",
                'SK': f"HABIT#{sync_timestamp}#{habit_id}",
                'user_id': user_id,
                'sync_timestamp': sync_timestamp,
                'habit_id': habit_id,
                'habit_name': habit.get('name'),
                'priority': habit.get('priority'),
                'weight': habit.get('weight'),
                'success_rate': habit.get('success_rate'),
                'weighted_success_rate': habit.get('weighted_success_rate'),
                'streak_length': habit.get('streak_length'),
                'is_numerical': habit.get('is_numerical'),
                'target_value': habit.get('target_value'),
                'frequency': habit.get('frequency'),
                'color': habit.get('color'),
                'type': habit.get('type'),
                'category': standardized_category,
                'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
            }
            batch.put_item(Item=habit_item)
            habits_stored += 1
            
            # Store performance history for charting
            performance_history = habit.get('performance_history', {})
            if performance_history:
                
                # Store daily performance data
                daily_data = performance_history.get('daily_data', [])
                for daily_record in daily_data:
                    daily_item = {
                        'PK': f"USER#{user_id}",
                        'SK': f"DAILY#{habit_id}#{daily_record.get('date', '')}",
                        'user_id': user_id,
                        'habit_id': habit_id,
                        'habit_name': habit.get('name'),
                        'priority': habit.get('priority'),
                        'weight': habit.get('weight'),
                        'category': standardized_category,  # Include category in daily records
                        'date': daily_record.get('date'),
                        'completed': daily_record.get('completed'),
                        'value': daily_record.get('value'),
                        'notes': daily_record.get('notes'),
                        'day_of_week': daily_record.get('day_of_week'),
                        'record_type': 'daily_performance',
                        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
                    }
                    batch.put_item(Item=daily_item)
                    performance_records += 1
                
                # Store weekly aggregations
                weekly_data = performance_history.get('weekly_data', [])
                for weekly_record in weekly_data:
                    weekly_item = {
                        'PK': f"USER#{user_id}",
                        'SK': f"WEEKLY#{habit_id}#{weekly_record.get('week_start', '')}",
                        'user_id': user_id,
                        'habit_id': habit_id,
                        'habit_name': habit.get('name'),
                        'priority': habit.get('priority'),
                        'weight': habit.get('weight'),
                        'category': standardized_category,  # Include category in weekly records
                        'week_start': weekly_record.get('week_start'),
                        'week_end': weekly_record.get('week_end'),
                        'completed_days': weekly_record.get('completed_days'),
                        'expected_days': weekly_record.get('expected_days'),
                        'completion_rate': weekly_record.get('completion_rate'),
                        'total_value': weekly_record.get('total_value'),
                        'record_type': 'weekly_performance',
                        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
                    }
                    batch.put_item(Item=weekly_item)
                    performance_records += 1
                
                # Store monthly aggregations
                monthly_data = performance_history.get('monthly_data', [])
                for monthly_record in monthly_data:
                    monthly_item = {
                        'PK': f"USER#{user_id}",
                        'SK': f"MONTHLY#{habit_id}#{monthly_record.get('month', '')}",
                        'user_id': user_id,
                        'habit_id': habit_id,
                        'habit_name': habit.get('name'),
                        'priority': habit.get('priority'),
                        'weight': habit.get('weight'),
                        'category': standardized_category,  # Include category in monthly records
                        'month': monthly_record.get('month'),
                        'completed_days': monthly_record.get('completed_days'),
                        'expected_days': monthly_record.get('expected_days'),
                        'completion_rate': monthly_record.get('completion_rate'),
                        'total_value': monthly_record.get('total_value'),
                        'record_type': 'monthly_performance',
                        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
                    }
                    batch.put_item(Item=monthly_item)
                    performance_records += 1
                
                # Store streak history
                streak_history = performance_history.get('streak_history', [])
                for i, streak_record in enumerate(streak_history):
                    streak_item = {
                        'PK': f"USER#{user_id}",
                        'SK': f"STREAK#{habit_id}#{streak_record.get('start_date', '')}#{i}",
                        'user_id': user_id,
                        'habit_id': habit_id,
                        'habit_name': habit.get('name'),
                        'priority': habit.get('priority'),
                        'weight': habit.get('weight'),
                        'category': standardized_category,  # Include category in streak records
                        'start_date': streak_record.get('start_date'),
                        'end_date': streak_record.get('end_date'),
                        'length': streak_record.get('length'),
                        'record_type': 'streak_history',
                        'ttl': int(sync_timestamp / 1000) + (365 * 24 * 60 * 60)  # 1 year TTL
                    }
                    batch.put_item(Item=streak_item)
                    performance_records += 1
    
    summary_text = f"Stored 1 sync record, {habits_stored} habit records, and {performance_records} performance records"
    if standardized_categories > 0:
        summary_text += f", standardized {standardized_categories} categories"
    
    return {
        'summary': summary_text,
        'habits_count': habits_stored,
        'performance_records': performance_records,
        'standardized_categories': standardized_categories,
        'sync_timestamp': sync_timestamp
    }

def handle_analytics_request(event, context):
    """
    Handle GET request for analytics dashboard
    """
    try:
        # Get recent data for analytics
        user_id = "user_primary"  # Default user for single-user app
        
        # Scan for recent daily performance data
        from boto3.dynamodb.conditions import Key, Attr
        
        # Get recent daily performance records
        response = table.scan(
            FilterExpression=Attr('record_type').eq('daily_performance') & 
                           Attr('user_id').eq(user_id),
            Limit=100
        )
        
        daily_records = response.get('Items', [])
        
        # Get habit summary data
        habit_response = table.scan(
            FilterExpression=Attr('user_id').eq(user_id) & 
                           Attr('SK').begins_with('HABIT#'),
            Limit=50
        )
        
        habits = habit_response.get('Items', [])
        
        # Process data for analytics
        analytics_data = {
            'habits': [],
            'daily_performance': [],
            'categories': {},
            'summary': {
                'total_habits': len(habits),
                'total_records': len(daily_records),
                'last_updated': datetime.utcnow().isoformat()
            }
        }
        
        # Process habits
        for habit in habits:
            analytics_data['habits'].append({
                'id': habit.get('habit_id'),
                'name': habit.get('habit_name'),
                'category': habit.get('category', 'Personal Improvement'),
                'priority': habit.get('priority'),
                'success_rate': float(habit.get('success_rate', 0)) if habit.get('success_rate') else 0,
                'streak_length': int(habit.get('streak_length', 0)) if habit.get('streak_length') else 0
            })
        
        # Process daily performance
        for record in daily_records:
            analytics_data['daily_performance'].append({
                'habit_id': record.get('habit_id'),
                'habit_name': record.get('habit_name'),
                'category': record.get('category', 'Personal Improvement'),
                'date': record.get('date'),
                'completed': bool(record.get('completed', False)),
                'value': float(record.get('value', 0)) if record.get('value') else 0
            })
        
        # Count categories
        category_counts = {}
        for habit in habits:
            category = habit.get('category', 'Personal Improvement')
            category_counts[category] = category_counts.get(category, 0) + 1
        
        analytics_data['categories'] = category_counts
        
        return create_response(200, analytics_data)
        
    except Exception as e:
        logger.error(f"Analytics error: {str(e)}")
        return create_response(500, {"error": f"Failed to fetch analytics: {str(e)}"})

def create_response(status_code, body):
    """
    Create standardized HTTP response
    """
    return {
        'statusCode': status_code,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',  # Configure as needed
            'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key',
            'Access-Control-Allow-Methods': 'OPTIONS,POST,GET'
        },
        'body': json.dumps(body, default=str)
    }

def get_user_analytics(user_id, days=30):
    """
    Retrieve analytics data for PowerBI integration
    (This would be a separate Lambda function or additional endpoint)
    """
    from boto3.dynamodb.conditions import Key
    
    # Query recent sync records
    response = table.query(
        KeyConditionExpression=Key('PK').eq(f"USER#{user_id}") & 
                              Key('SK').begins_with('SYNC#'),
        ScanIndexForward=False,  # Most recent first
        Limit=days
    )
    
    return response['Items']

# DynamoDB Table Schema (for reference):
"""
Table Name: uHabits-Analytics
Partition Key: PK (String) - USER#{user_id}
Sort Key: SK (String) - SYNC#{timestamp} or HABIT#{timestamp}#{habit_id}

Indexes:
- GSI1: SK (String) - For querying by sync timestamp across users
- LSI1: sync_date (String) - For date-based queries

Sample Items:
1. Sync Summary:
   PK: "USER#user_primary"
   SK: "SYNC#1672531200000"
   
2. Individual Habit:
   PK: "USER#user_primary" 
   SK: "HABIT#1672531200000#habit_123"
"""
