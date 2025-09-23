import json
import boto3
from datetime import datetime, timedelta
from decimal import Decimal
import logging

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize DynamoDB
dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
table = dynamodb.Table('uHabits-Analytics-prod')

def decimal_default(obj):
    """JSON serializer for Decimal objects"""
    if isinstance(obj, Decimal):
        return float(obj)
    raise TypeError

def lambda_handler(event, context):
    """
    Enhanced API for live dashboard access
    Supports multiple endpoints for different data views
    """
    
    try:
        # Extract path and method
        path = event.get('path', '/')
        method = event.get('httpMethod', 'GET')
        
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
        
        # Route to appropriate handler
        if path == '/api/dashboard' or path == '/dashboard':
            return get_dashboard_data(headers)
        elif path == '/api/habits' or path == '/habits':
            return get_habits_summary(headers)
        elif path == '/api/performance' or path == '/performance':
            return get_performance_data(headers, event.get('queryStringParameters', {}))
        elif path == '/api/trends' or path == '/trends':
            return get_trends_data(headers, event.get('queryStringParameters', {}))
        else:
            return get_dashboard_data(headers)  # Default to dashboard
            
    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': headers,
            'body': json.dumps({
                'error': 'Internal server error',
                'message': str(e)
            })
        }

def get_dashboard_data(headers):
    """Get main dashboard KPIs and summary data"""
    
    # Scan for all habits
    habits_response = table.scan(
        FilterExpression='begins_with(SK, :sk_prefix)',
        ExpressionAttributeValues={':sk_prefix': 'HABIT#'}
    )
    
    habits = habits_response['Items']
    
    # Calculate summary statistics with safe type conversion
    total_habits = len(habits)
    success_rates = []
    
    for habit in habits:
        try:
            rate = habit.get('success_rate', 0)
            if rate is not None:
                success_rates.append(float(rate))
            else:
                success_rates.append(0.0)
        except (ValueError, TypeError):
            success_rates.append(0.0)
    
    avg_success_rate = sum(success_rates) / len(success_rates) if success_rates else 0
    
    # Priority distribution
    priority_counts = {'CRITICAL': 0, 'HIGH': 0, 'NORMAL': 0, 'LOW': 0}
    priority_performance = {'CRITICAL': [], 'HIGH': [], 'NORMAL': [], 'LOW': []}
    
    for habit in habits:
        priority = habit.get('priority', 'NORMAL')
        if priority not in priority_counts:
            priority = 'NORMAL'  # Default for unknown priorities
        
        priority_counts[priority] = priority_counts.get(priority, 0) + 1
        
        try:
            rate = habit.get('success_rate', 0)
            success_rate = float(rate) if rate is not None else 0.0
        except (ValueError, TypeError):
            success_rate = 0.0
            
        priority_performance[priority].append(success_rate)
    
    # Calculate priority averages
    priority_averages = {}
    for priority, rates in priority_performance.items():
        priority_averages[priority] = sum(rates) / len(rates) if rates else 0
    
    # Performance categories
    excellent = len([r for r in success_rates if r >= 0.8])
    good = len([r for r in success_rates if 0.6 <= r < 0.8])
    needs_work = len([r for r in success_rates if 0.4 <= r < 0.6])
    critical = len([r for r in success_rates if r < 0.4])
    
    dashboard_data = {
        'summary': {
            'total_habits': total_habits,
            'avg_success_rate': round(avg_success_rate, 3),
            'critical_habits': priority_counts.get('CRITICAL', 0),
            'excellent_performers': excellent
        },
        'priority_distribution': priority_counts,
        'priority_performance': {k: round(v, 3) for k, v in priority_averages.items()},
        'performance_categories': {
            'excellent': excellent,
            'good': good,
            'needs_work': needs_work,
            'critical': critical
        },
        'last_updated': datetime.now().isoformat()
    }
    
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps(dashboard_data, default=decimal_default)
    }

def get_habits_summary(headers):
    """Get detailed habits list"""
    
    response = table.scan(
        FilterExpression='begins_with(SK, :sk_prefix)',
        ExpressionAttributeValues={':sk_prefix': 'HABIT#'}
    )
    
    habits = []
    for item in response['Items']:
        try:
            habit = {
                'id': item.get('SK', '').replace('HABIT#', ''),
                'name': item.get('habit_name', 'Unknown Habit'),
                'priority': item.get('priority', 'NORMAL'),
                'success_rate': float(item.get('success_rate', 0)) if item.get('success_rate') is not None else 0.0,
                'current_streak': int(item.get('current_streak', 0)) if item.get('current_streak') is not None else 0,
                'best_streak': int(item.get('best_streak', 0)) if item.get('best_streak') is not None else 0,
                'total_completions': int(item.get('total_completions', 0)) if item.get('total_completions') is not None else 0
            }
            habits.append(habit)
        except (ValueError, TypeError) as e:
            logger.warning(f"Skipping invalid habit record: {item.get('SK', 'unknown')}, error: {e}")
            continue
    
    # Sort by success rate descending
    habits.sort(key=lambda x: x['success_rate'], reverse=True)
    
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps({
            'habits': habits,
            'total_count': len(habits),
            'last_updated': datetime.now().isoformat()
        }, default=decimal_default)
    }

def get_performance_data(headers, params):
    """Get performance data with date filtering"""
    
    # Get days parameter (default 30)
    days = int(params.get('days', 30))
    end_date = datetime.now()
    start_date = end_date - timedelta(days=days)
    
    # Scan for daily records in date range
    daily_records = []
    response = table.scan(
        FilterExpression='begins_with(SK, :sk_prefix)',
        ExpressionAttributeValues={':sk_prefix': 'DAILY#'}
    )
    
    for item in response['Items']:
        try:
            # Extract date from SK (format: DAILY#YYYY-MM-DD#HABIT_ID)
            sk_parts = item.get('SK', '').split('#')
            if len(sk_parts) >= 2:
                record_date = datetime.strptime(sk_parts[1], '%Y-%m-%d')
                if start_date <= record_date <= end_date:
                    daily_records.append({
                        'date': sk_parts[1],
                        'habit_id': sk_parts[2] if len(sk_parts) > 2 else '',
                        'habit_name': item.get('habit_name', 'Unknown'),
                        'priority': item.get('priority', 'NORMAL'),
                        'completed': bool(item.get('completed', False)),
                        'success_rate': float(item.get('success_rate', 0)) if item.get('success_rate') is not None else 0.0
                    })
        except (ValueError, IndexError):
            continue
    
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps({
            'daily_records': daily_records,
            'date_range': {
                'start': start_date.strftime('%Y-%m-%d'),
                'end': end_date.strftime('%Y-%m-%d'),
                'days': days
            },
            'total_records': len(daily_records),
            'last_updated': datetime.now().isoformat()
        }, default=decimal_default)
    }

def get_trends_data(headers, params):
    """Get trend analysis data"""
    
    # Get weekly summary data
    response = table.scan(
        FilterExpression='begins_with(SK, :sk_prefix)',
        ExpressionAttributeValues={':sk_prefix': 'WEEKLY#'}
    )
    
    weekly_data = []
    for item in response['Items']:
        try:
            # Extract week from SK (format: WEEKLY#YYYY-WW#HABIT_ID)
            sk_parts = item.get('SK', '').split('#')
            if len(sk_parts) >= 2:
                weekly_data.append({
                    'week': sk_parts[1],
                    'habit_id': sk_parts[2] if len(sk_parts) > 2 else '',
                    'habit_name': item.get('habit_name', 'Unknown'),
                    'priority': item.get('priority', 'NORMAL'),
                    'completion_rate': float(item.get('completion_rate', 0)) if item.get('completion_rate') is not None else 0.0,
                    'total_days': int(item.get('total_days', 0)) if item.get('total_days') is not None else 0,
                    'completed_days': int(item.get('completed_days', 0)) if item.get('completed_days') is not None else 0
                })
        except (ValueError, IndexError):
            continue
    
    # Sort by week
    weekly_data.sort(key=lambda x: x['week'])
    
    return {
        'statusCode': 200,
        'headers': headers,
        'body': json.dumps({
            'weekly_trends': weekly_data,
            'total_weeks': len(set(record['week'] for record in weekly_data)),
            'last_updated': datetime.now().isoformat()
        }, default=decimal_default)
    }
