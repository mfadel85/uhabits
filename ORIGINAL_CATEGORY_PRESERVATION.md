# Original Category Preservation in uHabits

## Overview

The Lambda function has been modified to preserve original category names exactly as they're entered in the mobile app. This change ensures that the categories you create in the mobile application are maintained as-is during the sync process, without any automatic standardization.

## Changes Made

### Before:
- Categories were automatically standardized into four groups:
  - "Religious"
  - "Career & Work"
  - "Social & Family" 
  - "Personal Improvement"
- Standardization was based on keywords in the category name
- Default category was "Personal Improvement" for missing categories

### After:
- Categories are preserved exactly as entered in the mobile app
- No automatic standardization is performed
- Only habits with completely missing categories receive a default "Uncategorized" value

## How It Works

When habit data is synced from your phone to AWS:

1. The Lambda function receives the habit data
2. Original categories are preserved exactly as-is
3. Only habits with null/empty categories receive the default "Uncategorized" value
4. All data is stored in DynamoDB with the original categories
5. The response includes information about any default categories applied

## Implementation Details

The Lambda function's `standardize_category()` function has been modified to:

```python
def standardize_category(category):
    # Return the exact category from the app, or use a default if none is provided
    if not category:
        return "Uncategorized"  # Default category only if completely missing
        
    # Return the original category as-is without any modification
    return category
```

This function now acts as a pass-through for categories, only providing a default for completely missing categories.

## Verification

You can verify this change by:

1. Running the `deploy_preserved_categories.sh` script to deploy the updated Lambda function
2. Syncing your habits from your phone
3. Checking your analytics dashboard to confirm categories appear exactly as entered in the app

## Next Steps

After deploying this update:

1. Sync your app to upload habits with original categories
2. Check your dashboard to confirm categories appear as entered
3. Edit categories directly in the app if you need to make changes

## Troubleshooting

If categories are not displaying correctly in your dashboard after syncing:

1. Verify that the Lambda function has been updated by checking the timestamp
2. Ensure your app is syncing correctly
3. Check that your dashboard is configured to display the categories as-is
