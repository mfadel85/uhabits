#!/usr/bin/env python3
"""
uHabits Analytics - Python Data Consumer
Quick start script for analyzing your habit data
"""

import boto3
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime, timedelta
import json

class UHabitsAnalytics:
    def __init__(self, table_name="uHabits-Analytics-prod", region="eu-central-1"):
        self.dynamodb = boto3.resource('dynamodb', region_name=region)
        self.table = self.dynamodb.Table(table_name)
        
    def get_all_data(self):
        """Get all data from DynamoDB"""
        response = self.table.scan()
        items = response['Items']
        
        # Handle pagination
        while 'LastEvaluatedKey' in response:
            response = self.table.scan(ExclusiveStartKey=response['LastEvaluatedKey'])
            items.extend(response['Items'])
            
        return items
    
    def get_habits_summary(self):
        """Get habits summary data"""
        response = self.table.scan(
            FilterExpression='begins_with(SK, :sk_prefix)',
            ExpressionAttributeValues={':sk_prefix': 'HABIT#'}
        )
        return pd.DataFrame(response['Items'])
    
    def get_daily_performance(self):
        """Get daily performance data"""
        response = self.table.scan(
            FilterExpression='begins_with(SK, :sk_prefix)',
            ExpressionAttributeValues={':sk_prefix': 'DAILY#'}
        )
        return pd.DataFrame(response['Items'])
    
    def analyze_priority_performance(self):
        """Analyze performance by priority"""
        habits_df = self.get_habits_summary()
        
        if habits_df.empty:
            print("No habit data found")
            return
            
        # Convert numeric columns
        numeric_cols = ['success_rate', 'weighted_success_rate', 'streak_length', 'priority_weight']
        for col in numeric_cols:
            if col in habits_df.columns:
                habits_df[col] = pd.to_numeric(habits_df[col], errors='coerce')
        
        # Priority analysis
        priority_stats = habits_df.groupby('priority').agg({
            'success_rate': ['mean', 'std', 'count'],
            'weighted_success_rate': 'mean',
            'streak_length': 'mean'
        }).round(3)
        
        print("📊 Priority Performance Analysis")
        print("=" * 40)
        print(priority_stats)
        
        return priority_stats
    
    def plot_priority_performance(self):
        """Create visualizations"""
        habits_df = self.get_habits_summary()
        
        if habits_df.empty:
            print("No data to plot")
            return
            
        # Convert numeric columns
        habits_df['success_rate'] = pd.to_numeric(habits_df['success_rate'], errors='coerce')
        habits_df['priority_weight'] = pd.to_numeric(habits_df['priority_weight'], errors='coerce')
        
        # Create plots
        fig, axes = plt.subplots(2, 2, figsize=(15, 10))
        fig.suptitle('uHabits Analytics Dashboard', fontsize=16)
        
        # Plot 1: Success rate by priority
        if 'priority' in habits_df.columns:
            sns.boxplot(data=habits_df, x='priority', y='success_rate', ax=axes[0,0])
            axes[0,0].set_title('Success Rate by Priority')
            axes[0,0].set_ylim(0, 1)
        
        # Plot 2: Priority distribution
        if 'priority' in habits_df.columns:
            habits_df['priority'].value_counts().plot(kind='pie', ax=axes[0,1])
            axes[0,1].set_title('Habits by Priority')
        
        # Plot 3: Success rate vs Priority weight
        if all(col in habits_df.columns for col in ['priority_weight', 'success_rate']):
            axes[1,0].scatter(habits_df['priority_weight'], habits_df['success_rate'])
            axes[1,0].set_xlabel('Priority Weight')
            axes[1,0].set_ylabel('Success Rate')
            axes[1,0].set_title('Success Rate vs Priority Weight')
        
        # Plot 4: Habit count by success rate ranges
        if 'success_rate' in habits_df.columns:
            habits_df['success_category'] = pd.cut(habits_df['success_rate'], 
                                                 bins=[0, 0.5, 0.8, 1.0], 
                                                 labels=['Low (<50%)', 'Medium (50-80%)', 'High (80%+)'])
            habits_df['success_category'].value_counts().plot(kind='bar', ax=axes[1,1])
            axes[1,1].set_title('Habits by Success Rate Category')
            axes[1,1].set_ylabel('Number of Habits')
        
        plt.tight_layout()
        plt.savefig('uhabits_analytics_dashboard.png', dpi=300, bbox_inches='tight')
        plt.show()
        
        print("📈 Dashboard saved as 'uhabits_analytics_dashboard.png'")

def main():
    """Main analysis function"""
    print("🚀 uHabits Analytics - Python Consumer")
    print("=" * 50)
    
    try:
        analytics = UHabitsAnalytics()
        
        # Get data overview
        all_data = analytics.get_all_data()
        print(f"📊 Total records in database: {len(all_data)}")
        
        # Analyze by record type
        record_types = {}
        for item in all_data:
            sk_prefix = item.get('SK', '').split('#')[0]
            record_types[sk_prefix] = record_types.get(sk_prefix, 0) + 1
        
        print("\n📋 Record Types:")
        for record_type, count in record_types.items():
            print(f"   {record_type}: {count} records")
        
        # Priority analysis
        print("\n" + "=" * 50)
        analytics.analyze_priority_performance()
        
        # Create visualizations
        print("\n📈 Creating visualizations...")
        analytics.plot_priority_performance()
        
        print("\n🎉 Analysis complete!")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        print("Make sure AWS credentials are configured and DynamoDB table exists")

if __name__ == "__main__":
    main()
