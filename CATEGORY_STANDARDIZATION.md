# Category Standardization in uHabits Cloud Analytics

## Overview

The category standardization feature has been implemented in the AWS Lambda function to ensure consistent category naming across all habit data in DynamoDB. This update ensures that all categories are properly displayed in your analytics dashboard.

## Implementation Details

The Lambda function now includes a `standardize_category()` function that maps various category names to four standard categories:

1. **Religious** - For spiritual and religious habits
   - Matched terms: "religious", "pray", "faith"

2. **Career & Work** - For professional habits
   - Matched terms: "career", "work", "job"

3. **Social & Family** - For relationship habits
   - Matched terms: "social", "family", "friend"

4. **Personal Improvement** - For health and self-development habits
   - Matched terms: "personal", "health", "self"
   - Also used as the default category for unmatched or empty categories

## How It Works

When habit data is synced from your phone to AWS:

1. The Lambda function receives the habit data
2. For each habit record, it standardizes the category name
3. The standardized category is applied to all related records (habits, daily, weekly, monthly, streak)
4. The function logs how many categories were standardized
5. The response includes information about standardized categories

## Verification

The Lambda function update has been successfully deployed. You can verify this by:

1. Running the `verify_lambda_update.sh` script
2. Syncing your habits from your phone
3. Checking your analytics dashboard to confirm categories are displayed correctly

## Next Steps

After deploying this update:

1. Sync your app to upload any updated categories
2. Check your dashboard to confirm categories are showing correctly
3. If categories are still not displaying correctly, you may need to refresh your browser cache

## Troubleshooting

If categories are still not displaying correctly in your dashboard after syncing:

1. Verify that the Lambda function has been updated by running `verify_lambda_update.sh`
2. Check that your app is using the standard category names
3. Verify that habit data is being synced to AWS correctly
4. Clear your browser cache and reload the dashboard
