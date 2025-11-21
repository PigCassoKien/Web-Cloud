#!/bin/bash

# SmartQueue LocalStack Setup Script
echo "🚀 Setting up LocalStack for SmartQueue..."

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to start..."
sleep 10

# Set AWS CLI to use LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-southeast-1
export AWS_ENDPOINT_URL=http://localhost:4566

# Create DynamoDB tables
echo "📊 Creating DynamoDB tables..."

# Create tickets table
aws dynamodb create-table \
    --table-name smartqueue-tickets \
    --attribute-definitions \
        AttributeName=ticketId,AttributeType=S \
        AttributeName=queueId,AttributeType=S \
    --key-schema \
        AttributeName=ticketId,KeyType=HASH \
    --global-secondary-indexes \
        IndexName=queueId-index,KeySchema=[{AttributeName=queueId,KeyType=HASH}],Projection={ProjectionType=ALL},BillingMode=PAY_PER_REQUEST \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url http://localhost:4566

# Create queues table
aws dynamodb create-table \
    --table-name smartqueue-queues \
    --attribute-definitions \
        AttributeName=queueId,AttributeType=S \
    --key-schema \
        AttributeName=queueId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url http://localhost:4566

# List tables to verify
echo "✅ Created tables:"
aws dynamodb list-tables --endpoint-url http://localhost:4566

echo "🎉 LocalStack setup completed!"