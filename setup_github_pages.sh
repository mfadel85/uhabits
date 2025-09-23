#!/bin/bash

# GitHub Pages Dashboard Setup
# Host your dashboard for free on GitHub Pages

set -e

echo "🌐 GitHub Pages Dashboard Setup"
echo "==============================="

# Create docs directory for GitHub Pages
mkdir -p docs
mkdir -p docs/assets

echo "📄 Setting up dashboard files..."

# Copy main dashboard
cp advanced_dashboard.html docs/index.html
cp test_group_selection.html docs/groups.html

# Create a simple index page
cat > docs/README.md << 'EOF'
# 🎯 uHabits Group Analytics Dashboard

Live dashboard for habit group performance tracking.

## 📊 Available Dashboards

- **[Main Dashboard](./index.html)** - Advanced group analytics with charts
- **[Group Selection Guide](./groups.html)** - Manual group assignment instructions

## 🔗 API Configuration

Update the API URL in the dashboard:
```javascript
const API_URL = 'https://your-api-gateway-url.amazonaws.com/prod';
```

## 📱 How to Use

1. Install the enhanced uHabits app
2. Create habits and assign them to groups manually
3. Export analytics data from the app
4. View real-time performance on this dashboard

## 🏷️ Available Groups

- 🕌 **Religious** - Spiritual and Islamic practices
- 💼 **Career & Work** - Professional development  
- 👨‍👩‍👧‍👦 **Social & Family** - Relationships and social activities
- 🌟 **Personal Improvement** - Health, fitness, learning

Built with ❤️ by [mfadel85](https://github.com/mfadel85)
EOF

# Create GitHub Pages configuration
cat > docs/_config.yml << 'EOF'
title: "uHabits Group Analytics"
description: "Real-time habit group performance dashboard"
theme: jekyll-theme-minimal
EOF

# Create a simple navigation layout
cat > docs/_layouts/default.html << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ page.title | default: site.title }}</title>
    <style>
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
        }
        .nav {
            background: white;
            padding: 15px;
            border-radius: 10px;
            margin-bottom: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .nav a {
            margin-right: 20px;
            text-decoration: none;
            color: #007bff;
            font-weight: 500;
        }
        .nav a:hover {
            text-decoration: underline;
        }
        .content {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
    <div class="nav">
        <a href="./index.html">📊 Main Dashboard</a>
        <a href="./groups.html">🏷️ Group Guide</a>
        <a href="https://github.com/mfadel85/uhabits">📱 Get App</a>
    </div>
    <div class="content">
        {{ content }}
    </div>
</body>
</html>
EOF

echo "✅ GitHub Pages setup complete!"
echo ""
echo "🚀 Next steps:"
echo "1. Commit and push these files to your repository"
echo "2. Go to GitHub repository settings"
echo "3. Enable GitHub Pages from the 'docs' folder"
echo "4. Your dashboard will be available at:"
echo "   https://mfadel85.github.io/uhabits/"
echo ""
echo "💰 Cost savings:"
echo "✅ Free static hosting (was using S3)"
echo "✅ No S3 requests for dashboard files"
echo "✅ Still keep Lambda + DynamoDB (free tier)"
echo ""
echo "📁 Files created in ./docs/"
ls -la docs/
