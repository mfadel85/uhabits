#!/usr/bin/env python3
# AWS Category Field Analyzer for uHabits
# This script helps identify and fix category fields in AWS DynamoDB tables

import boto3
import json
import argparse
from collections import defaultdict
import sys

def analyze_table(table_name, region='eu-central-1'):
    """
    Analyzes a DynamoDB table to find potential category fields and values
    """
    print(f"Connecting to DynamoDB in {region}...")
    dynamodb = boto3.resource('dynamodb', region_name=region)
    table = dynamodb.Table(table_name)
    
    try:
        # Get table information
        print(f"Analyzing table '{table_name}'...")
        response = table.scan()
        items = response['Items']
        
        if not items:
            print("No items found in table.")
            return
            
        print(f"Found {len(items)} items in table.")
        
        # Analyze fields
        field_values = defaultdict(set)
        field_types = {}
        
        # First pass - identify fields and their types
        for item in items:
            for field, value in item.items():
                value_type = type(value).__name__
                # Limit set size to prevent memory issues
                if len(field_values[field]) < 100:
                    field_values[field].add(str(value))
                field_types[field] = value_type
        
        # Look for fields that might be categories
        potential_category_fields = []
        for field, values in field_values.items():
            # If field has a small number of distinct values, it might be categorical
            if 1 < len(values) < 10:
                potential_category_fields.append(field)
            # Or if the field name suggests it's a category
            elif any(kw in field.lower() for kw in ['category', 'group', 'type', 'class', 'kind']):
                potential_category_fields.append(field)
        
        print("\nPotential category fields:")
        for field in potential_category_fields:
            print(f"- {field} ({field_types.get(field, 'unknown')}): {', '.join(list(field_values[field])[:5])}")
        
        # Ask user to select the category field
        if potential_category_fields:
            if len(potential_category_fields) == 1:
                category_field = potential_category_fields[0]
                print(f"\nAutoselected category field: {category_field}")
            else:
                print("\nSelect the category field by number:")
                for i, field in enumerate(potential_category_fields):
                    print(f"{i+1}. {field}")
                
                choice = input("Enter your selection (1-{0}): ".format(len(potential_category_fields)))
                try:
                    index = int(choice) - 1
                    if 0 <= index < len(potential_category_fields):
                        category_field = potential_category_fields[index]
                    else:
                        print("Invalid choice. Aborting.")
                        return
                except ValueError:
                    print("Invalid input. Aborting.")
                    return
                    
            analyze_category_distribution(items, category_field)
            return category_field
        else:
            print("No potential category fields found.")
            return None
            
    except Exception as e:
        print(f"Error analyzing table: {e}")
        return None

def analyze_category_distribution(items, category_field):
    """
    Analyzes the distribution of values in the selected category field
    """
    category_counts = defaultdict(int)
    categories_by_habit = {}
    
    for item in items:
        if category_field in item:
            category = str(item[category_field])
            category_counts[category] += 1
            
            # Store category by habit name for reference
            habit_name = item.get('name', item.get('id', 'unknown'))
            categories_by_habit[habit_name] = category
    
    print(f"\nCategory distribution for field '{category_field}':")
    for category, count in sorted(category_counts.items(), key=lambda x: -x[1]):
        print(f"- {category}: {count} items")
    
    # Check for standardization issues
    standard_categories = {
        'Religious', 'Career & Work', 'Social & Family', 'Personal Improvement'
    }
    
    non_standard = [cat for cat in category_counts.keys() if cat not in standard_categories]
    if non_standard:
        print("\nNon-standard category values detected:")
        for cat in non_standard:
            print(f"- {cat}")
        
        print("\nSample habits with non-standard categories:")
        count = 0
        for habit, category in categories_by_habit.items():
            if category in non_standard:
                print(f"- {habit}: {category}")
                count += 1
                if count >= 5:
                    break
    
    return category_counts

def fix_categories(table_name, category_field, region='eu-central-1'):
    """
    Creates a standardized mapping and offers to update the table
    """
    if not category_field:
        print("No category field specified.")
        return
        
    print(f"Connecting to DynamoDB in {region}...")
    dynamodb = boto3.resource('dynamodb', region_name=region)
    table = dynamodb.Table(table_name)
    
    try:
        # Scan table for current categories
        response = table.scan()
        items = response['Items']
        
        if not items:
            print("No items found in table.")
            return
        
        # Create category mapping
        category_map = {}
        unique_categories = set()
        
        for item in items:
            if category_field in item:
                unique_categories.add(str(item[category_field]))
        
        # Generate mapping interactively
        standard_categories = [
            'Religious', 
            'Career & Work', 
            'Social & Family', 
            'Personal Improvement'
        ]
        
        print("\nCreating category mapping:")
        for category in unique_categories:
            print(f"\nCurrent category: '{category}'")
            print("Map to which standard category?")
            for i, std_cat in enumerate(standard_categories):
                print(f"{i+1}. {std_cat}")
            
            choice = input("Enter your selection (1-4), or press Enter to skip: ")
            if choice.strip():
                try:
                    index = int(choice) - 1
                    if 0 <= index < len(standard_categories):
                        category_map[category] = standard_categories[index]
                        print(f"Mapped '{category}' to '{standard_categories[index]}'")
                except ValueError:
                    print(f"Invalid input. Skipping '{category}'.")
        
        # Show final mapping
        print("\nFinal category mapping:")
        for old, new in category_map.items():
            print(f"- '{old}' → '{new}'")
        
        # Confirm update
        confirm = input("\nUpdate categories in the database? (y/n): ")
        if confirm.lower() != 'y':
            print("Update canceled.")
            return
        
        # Update items
        update_count = 0
        for item in items:
            if category_field in item:
                old_category = str(item[category_field])
                if old_category in category_map:
                    new_category = category_map[old_category]
                    
                    # Get the primary key for the item
                    key = {}
                    for k in item:
                        if k.lower() in ['id', 'uuid', 'habitid']:
                            key[k] = item[k]
                    
                    if not key:
                        print(f"Could not determine key for item: {item}")
                        continue
                    
                    # Update the item
                    try:
                        table.update_item(
                            Key=key,
                            UpdateExpression=f'SET {category_field} = :cat',
                            ExpressionAttributeValues={':cat': new_category}
                        )
                        print(f"Updated {item.get('name', key)} from '{old_category}' to '{new_category}'")
                        update_count += 1
                    except Exception as e:
                        print(f"Error updating item {key}: {e}")
        
        print(f"\nUpdated {update_count} items with standardized categories.")
        
    except Exception as e:
        print(f"Error fixing categories: {e}")

def main():
    parser = argparse.ArgumentParser(description='Analyze and fix category fields in DynamoDB tables')
    parser.add_argument('--table', required=True, help='DynamoDB table name')
    parser.add_argument('--region', default='eu-central-1', help='AWS region')
    parser.add_argument('--fix', action='store_true', help='Fix categories after analysis')
    args = parser.parse_args()
    
    category_field = analyze_table(args.table, args.region)
    
    if args.fix and category_field:
        fix_categories(args.table, category_field, args.region)

if __name__ == '__main__':
    main()
