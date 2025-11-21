#!/bin/bash

# SmartQueue Load Testing Script
# Usage: ./run_load_tests.sh [environment]

set -e

ENVIRONMENT=${1:-local}
RESULTS_DIR="./results/$(date +%Y%m%d_%H%M%S)"

mkdir -p $RESULTS_DIR

echo "🚀 Starting SmartQueue Load Testing - Environment: $ENVIRONMENT"

if [ "$ENVIRONMENT" = "local" ]; then
    export API_AWS_BASE="http://localhost:8080"
    export API_ALIYUN_BASE="http://localhost:8081"
elif [ "$ENVIRONMENT" = "staging" ]; then
    export API_AWS_BASE="https://api-aws-staging.smartqueue.dev"
    export API_ALIYUN_BASE="https://api-aliyun-staging.smartqueue.dev"
elif [ "$ENVIRONMENT" = "prod" ]; then
    export API_AWS_BASE="https://api-aws.smartqueue.dev" 
    export API_ALIYUN_BASE="https://api-aliyun.smartqueue.dev"
fi

export TEST_KEY="LOADTEST-SECRET-KEY"

echo "📊 Test 1: Join Spike Test (1k -> 5k users)"
k6 run --out json=$RESULTS_DIR/join_spike.json join_spike.js

echo "⏱️  Test 2: Soak Test (15 minutes at 2k users)"
k6 run --out json=$RESULTS_DIR/soak_test.json soak_test.js

echo "📈 Test Results saved to: $RESULTS_DIR"
echo "✅ Load testing completed!"

# Generate summary report
echo "📋 Generating summary report..."
echo "# SmartQueue Load Test Results" > $RESULTS_DIR/summary.md
echo "Date: $(date)" >> $RESULTS_DIR/summary.md
echo "Environment: $ENVIRONMENT" >> $RESULTS_DIR/summary.md
echo "" >> $RESULTS_DIR/summary.md
echo "## Test Results" >> $RESULTS_DIR/summary.md
echo "- Join Spike Test: See join_spike.json" >> $RESULTS_DIR/summary.md
echo "- Soak Test: See soak_test.json" >> $RESULTS_DIR/summary.md

echo "📄 Summary report: $RESULTS_DIR/summary.md"