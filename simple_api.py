import json
import boto3
from datetime import datetime
import logging

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    Ultra-simple analytics API - just get the data without complex processing
    """
    
    # CORS headers
    headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type,x-api-key',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
    }
    
    try:
        logger.info(f"Event: {json.dumps(event)}")
        
        # Handle preflight requests
        if event.get('httpMethod') == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': headers,
                'body': ''
            }
        
        # Initialize DynamoDB
        dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
        table = dynamodb.Table('uHabits-Analytics-prod')
        
        # Simple scan to get just a few records
        response = table.scan(Limit=20)
        items = response.get('Items', [])
        
        logger.info(f"Found {len(items)} items")
        
        # Group by categories - simple approach
        categories = {}
        for item in items:
            category = item.get('category', 'Unknown')
            if category not in categories:
                categories[category] = []
            categories[category].append({
                'habit_name': item.get('habit_name', 'Unknown'),
                'completed': item.get('completed', False),
                'date': str(item.get('date', 'Unknown'))
            })
        
        # Create simple response
        response_data = {
            'categories': categories,
            'total_records': len(items),
            'timestamp': datetime.now().isoformat(),
            'message': 'Simple analytics data'
        }
        
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(response_data, default=str)
        }
        
    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': headers,
            'body': json.dumps({
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            })
        }
