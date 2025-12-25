# uHabits Category Fix Tools

This repository contains tools to help diagnose and fix category issues in uHabits data.

## Problem Overview

The dashboard may show all habits in one category (usually Personal Improvement) despite having correctly set categories in the Android app. This happens because of inconsistency in how categories are stored and transmitted between:

1. Android app
2. AWS storage (DynamoDB or S3)
3. API endpoint
4. Dashboard display

## Tools Included

### 1. Enhanced Dashboard (`group_analytics_dashboard.html`)

The dashboard now includes advanced debugging tools to help identify category issues:

- **AWS Data Analysis:** Shows how categories are stored in AWS and provides CLI commands
- **Category Field Detection:** Automatically detects possible category fields
- **Mobile App & AWS Fix Guide:** Step-by-step instructions for fixing at the source

### 2. AWS Category Analyzer (`aws_category_analyzer.py`)

A Python script that:

- Connects to DynamoDB to analyze habit data
- Identifies potential category fields
- Detects non-standard category values
- Provides interactive tools to map and fix categories

Usage:
```
# First, make it executable
chmod +x aws_category_analyzer.py

# Analysis only
./aws_category_analyzer.py --table YOUR_TABLE_NAME --region eu-central-1

# Analysis and fix
./aws_category_analyzer.py --table YOUR_TABLE_NAME --region eu-central-1 --fix
```

### 3. Android Fix Guide (`ANDROID_CATEGORY_FIX.md`)

A detailed guide for standardizing categories in the Android app:

- How to locate relevant code
- Adding standard category constants
- Updating UI components
- Adding migration code
- Fixing cloud sync

## Fix Approach

The recommended approach is to fix the issue at all levels:

1. **Android App:** 
   - Standardize category values
   - Add migration code for existing habits
   - Use constants to ensure consistency

2. **AWS Database:**
   - Analyze current data with `aws_category_analyzer.py`
   - Apply fixes to standardize stored values

3. **Dashboard:**
   - Already enhanced with better category detection
   - Will work correctly once source data is fixed

## Standard Categories

The standard categories to use throughout the system are:

- `Religious`
- `Career & Work`
- `Social & Family`
- `Personal Improvement`

## Support

For questions or issues, please open an issue in the repository or contact the maintainer.
