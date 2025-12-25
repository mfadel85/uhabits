import json

def lambda_handler(event, context):
    """
    Ultra-minimal test function - just to verify Lambda is working
    """
    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        'body': json.dumps({
            'message': 'Lambda is working!',
            'timestamp': '2025-10-16',
            'test': True
        })
    }
