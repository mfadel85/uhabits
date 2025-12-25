import json
import boto3
from datetime import datetime
import logging
import traceback

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

import json
import boto3
from datetime import datetime

def lambda_handler(event, context):
    """
    Diagnostic version - log everything to help debug
    """
    
    # CORS headers
    headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type,x-api-key',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
    }
    
    try:
        # Log the incoming event
        logger.info("=== LAMBDA START ===")
        logger.info(f"Event received: {json.dumps(event)}")
        logger.info(f"Context: {context}")
        
        # Handle preflight requests
        if event.get('httpMethod') == 'OPTIONS':
            logger.info("Handling OPTIONS request")
            return {
                'statusCode': 200,
                'headers': headers,
                'body': json.dumps({'message': 'CORS preflight successful'})
            }
        
        # Test basic functionality
        logger.info("Testing basic functionality...")
        
        # Test 1: Can we create a simple response?
        test_response = {
            'test': 'Lambda is working',
            'timestamp': datetime.now().isoformat(),
            'event_keys': list(event.keys()) if event else [],
        }
        
        logger.info("Basic test successful")
        
        # Test 2: Can we connect to DynamoDB?
        try:
            logger.info("Attempting DynamoDB connection...")
            dynamodb = boto3.resource('dynamodb', region_name='eu-central-1')
            table = dynamodb.Table('uHabits-Analytics-prod')
            logger.info("DynamoDB connection successful")
            
            # Test 3: Can we do a simple scan?
            try:
                logger.info("Attempting simple DynamoDB scan...")
                response = table.scan(Limit=1)
                count = response.get('Count', 0)
                logger.info(f"DynamoDB scan successful, found {count} items")
                test_response['dynamodb_test'] = 'success'
                test_response['item_count'] = count
            except Exception as scan_error:
                logger.error(f"DynamoDB scan failed: {str(scan_error)}")
                test_response['dynamodb_test'] = f'scan_failed: {str(scan_error)}'
                
        except Exception as db_error:
            logger.error(f"DynamoDB connection failed: {str(db_error)}")
            test_response['dynamodb_test'] = f'connection_failed: {str(db_error)}'
        
        logger.info("=== LAMBDA SUCCESS ===")
        
        return {
            'statusCode': 200,
            'headers': headers,
            'body': json.dumps(test_response)
        }
        
    except Exception as e:
        logger.error("=== LAMBDA ERROR ===")
        logger.error(f"Error: {str(e)}")
        logger.error(f"Traceback: {traceback.format_exc()}")
        
        return {
            'statusCode': 500,
            'headers': headers,
            'body': json.dumps({
                'error': str(e),
                'traceback': traceback.format_exc(),
                'timestamp': datetime.now().isoformat()
            })
        }
