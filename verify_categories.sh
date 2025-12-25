#!/bin/bash
# Helper script to verify categories after implementing the mobile app fix

# Colors for better visibility
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse command line arguments
TEST_MODE=false
for arg in "$@"
do
    case $arg in
        --test)
        TEST_MODE=true
        shift # Remove --test from processing
        ;;
    esac
done

echo -e "${YELLOW}===== uHabits Category Verification Tool =====${NC}"
echo "This script helps verify if your categories are correctly synced to AWS"
echo

# Show usage info if --help is specified
if [ "$1" == "--help" ]; then
    echo "Usage: ./verify_categories.sh [options]"
    echo
    echo "Options:"
    echo "  --test     Run in test mode using sample data (no AWS access needed)"
    echo "  --help     Show this help message"
    echo
    echo "Example:"
    echo "  ./verify_categories.sh"
    echo "  ./verify_categories.sh --test"
    exit 0
fi

# Display mode
if [ "$TEST_MODE" = true ]; then
    echo -e "${YELLOW}Running in TEST MODE with sample data${NC}"
else
    echo -e "${YELLOW}Running in NORMAL MODE - will query AWS DynamoDB${NC}"
fi
echo

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null
then
    echo -e "${RED}AWS CLI is not installed. Please install it first:${NC}"
    echo "pip install awscli"
    echo "aws configure"
    exit 1
fi

# Check AWS configuration
echo -e "${YELLOW}Checking AWS configuration...${NC}"
AWS_PROFILE=$(aws configure get aws_access_key_id 2>/dev/null)
if [ -z "$AWS_PROFILE" ]; then
    echo -e "${RED}Warning: AWS CLI does not appear to be configured.${NC}"
    echo "Please run 'aws configure' to set up your credentials first."
    read -p "Continue anyway? (y/n): " CONTINUE
    if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}AWS CLI is configured with credentials.${NC}"
fi

# Get AWS region
read -p "Enter your AWS region (default: eu-central-1): " AWS_REGION
AWS_REGION=${AWS_REGION:-eu-central-1}

# Get table name
read -p "Enter your DynamoDB table name: " TABLE_NAME

if [ -z "$TABLE_NAME" ]; then
    echo -e "${RED}Table name is required.${NC}"
    exit 1
fi

echo -e "\n${YELLOW}Listing available tables in $AWS_REGION...${NC}"
aws dynamodb list-tables --region $AWS_REGION

# Make sure we can write to the output file
touch habits_scan.txt
chmod 644 habits_scan.txt

echo -e "\n${YELLOW}Scanning $TABLE_NAME for habits...${NC}"

# Create habits_scan.txt file with write permissions
rm -f habits_scan.txt
touch habits_scan.txt
chmod 644 habits_scan.txt

if [ "$TEST_MODE" = true ]; then
    echo -e "${YELLOW}Running in TEST MODE - creating sample data instead of querying AWS${NC}"
    cat > habits_scan.txt << EOF
Daily Prayer	Religious
Read Quran	Religious
Work Project	Career & Work
Study Programming	Career & Work
Call Parents	Social & Family
Family Dinner	Social & Family
Exercise	Personal Improvement
Morning Meditation	Personal Improvement
Journal	personal
Reading	PERSONAL
Coding	work
Gym	health
EOF
else
    # Normal AWS query mode - using temporary file approach
    echo -e "${YELLOW}Querying DynamoDB table $TABLE_NAME...${NC}"
    
    # Run AWS command and save output to variable instead of direct redirection
    AWS_RESULT=$(aws dynamodb scan --table-name "$TABLE_NAME" --region "$AWS_REGION" --query "Items[*].[name.S, category.S]" --output text 2>&1)
    
    # Check if the command was successful
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error executing AWS command:${NC}"
        echo "$AWS_RESULT"
        echo -e "${RED}Please check your AWS permissions and table name.${NC}"
        exit 1
    fi
    
    # Save the result to the file
    echo "$AWS_RESULT" > habits_scan.txt
    
    # Verify the file was written
    if [ ! -s habits_scan.txt ]; then
        echo -e "${RED}Error: Failed to write data to habits_scan.txt${NC}"
        echo "AWS command output:"
        echo "$AWS_RESULT"
        exit 1
    fi
fi

echo -e "\n${YELLOW}Analyzing categories...${NC}"
echo "----------------------------------------"

# Check if file exists and has content
if [ ! -s habits_scan.txt ]; then
    echo -e "${RED}Error: No data found in the table or scan failed.${NC}"
    echo "Please check your table name and AWS permissions."
    exit 1
fi

# Debug info - show first few lines of the file
echo -e "\n${YELLOW}Sample data from habits_scan.txt:${NC}"
head -n 5 habits_scan.txt

# Initialize counters
RELIGIOUS=0
CAREER=0
SOCIAL=0
PERSONAL=0
OTHER=0
TOTAL=0

# Process file line by line to avoid issues with special characters
echo -e "\n${YELLOW}Processing categories...${NC}"
while IFS= read -r line || [ -n "$line" ]; do
    TOTAL=$((TOTAL + 1))
    
    if echo "$line" | grep -qi "Religious"; then
        RELIGIOUS=$((RELIGIOUS + 1))
    elif echo "$line" | grep -qi "Career" || echo "$line" | grep -qi "Work"; then
        CAREER=$((CAREER + 1))
    elif echo "$line" | grep -qi "Social" || echo "$line" | grep -qi "Family"; then
        SOCIAL=$((SOCIAL + 1))
    elif echo "$line" | grep -qi "Personal" || echo "$line" | grep -qi "Improvement"; then
        PERSONAL=$((PERSONAL + 1))
    else
        OTHER=$((OTHER + 1))
    fi
done < habits_scan.txt

echo -e "\n${YELLOW}Found $TOTAL total habits${NC}"

echo -e "${GREEN}Religious:${NC} $RELIGIOUS habits"
echo -e "${GREEN}Career & Work:${NC} $CAREER habits"
echo -e "${GREEN}Social & Family:${NC} $SOCIAL habits" 
echo -e "${GREEN}Personal Improvement:${NC} $PERSONAL habits"

if [ $OTHER -gt 0 ]; then
    echo -e "${RED}Non-standard categories:${NC} $OTHER habits"
    echo -e "\n${YELLOW}Non-standard categories found:${NC}"
    
    echo "Showing habits with non-standard categories:"
    echo "-------------------------------------------"
    
    # Reprocess the file to extract non-standard categories
    while IFS= read -r line || [ -n "$line" ]; do
        if ! echo "$line" | grep -qi "Religious" && \
           ! echo "$line" | grep -qi "Career" && \
           ! echo "$line" | grep -qi "Work" && \
           ! echo "$line" | grep -qi "Social" && \
           ! echo "$line" | grep -qi "Family" && \
           ! echo "$line" | grep -qi "Personal" && \
           ! echo "$line" | grep -qi "Improvement"; then
            echo "$line"
        fi
    done < habits_scan.txt
    
    echo -e "\n${YELLOW}These habits need to be fixed in the mobile app.${NC}"
else
    echo -e "\n${GREEN}Great! All habits are using standard categories.${NC}"
fi

echo -e "\n${YELLOW}Recommendation:${NC}"
if [ $OTHER -gt 0 ]; then
    echo "1. You have non-standard categories that need fixing. Options:"
    echo "   a. Fix them in the mobile app as described in MOBILE_APP_FIX_GUIDE.md"
    echo "   b. Run the AWS category analyzer script to fix them in AWS directly:"
    echo "      python aws_category_analyzer.py --table $TABLE_NAME --region $AWS_REGION --fix"
    echo "2. Force a sync in the mobile app after fixing"
    echo "3. Run this script again to verify the changes"
else
    echo "Your categories look good! The dashboard should display habits correctly."
    echo "If you still don't see correct categories in the dashboard:"
    echo "1. Make sure the dashboard is using the correct API endpoint"
    echo "2. Check that the API is correctly processing category data"
    echo "3. Try clearing your browser cache and refreshing the dashboard"
fi

echo -e "\nResults saved to habits_scan.txt for reference."

# Display AWS diagnostic information if there were issues
if [ $TOTAL -eq 0 ]; then
    echo -e "\n${YELLOW}AWS Diagnostics:${NC}"
    echo "1. Checking DynamoDB table structure..."
    echo -e "   ${YELLOW}Running: aws dynamodb describe-table --table-name $TABLE_NAME --region $AWS_REGION${NC}"
    
    aws dynamodb describe-table --table-name "$TABLE_NAME" --region "$AWS_REGION" --query "Table.AttributeDefinitions" --output json
    
    echo -e "\n2. Testing a basic scan (first item only):"
    echo -e "   ${YELLOW}Running: aws dynamodb scan --table-name $TABLE_NAME --region $AWS_REGION --limit 1${NC}"
    
    aws dynamodb scan --table-name "$TABLE_NAME" --region "$AWS_REGION" --limit 1
    
    echo -e "\n3. Common troubleshooting tips:"
    echo "   - Check that your table has items with name.S and category.S attributes"
    echo "   - Verify you have the correct permissions (dynamodb:Scan)"
    echo "   - Try the aws_category_analyzer.py script which uses a different approach"
fi

echo -e "${YELLOW}===========================================================${NC}"
