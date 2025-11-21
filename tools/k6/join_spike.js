import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 1000 },   // Ramp up to 1k users
    { duration: '3m', target: 5000 },   // Spike to 5k users  
    { duration: '2m', target: 5000 },   // Stay at 5k users
    { duration: '1m', target: 0 },      // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(90)<300', 'p(95)<500'], // 90% < 300ms, 95% < 500ms
    http_req_failed: ['rate<0.01'],    // Error rate < 1%
  },
};

const API_AWS_BASE = __ENV.API_AWS_BASE || 'http://localhost:8080';
const TEST_KEY = __ENV.TEST_KEY || 'LOADTEST-SECRET-KEY';

export default function () {
  const queueId = 'load-test-queue';
  
  // Test bulk join endpoint 
  const bulkJoinPayload = JSON.stringify({
    queueId: queueId,
    batch: 10
  });
  
  const bulkJoinRes = http.post(
    `${API_AWS_BASE}/queues/test/join-bulk`,
    bulkJoinPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Test-Key': TEST_KEY,
      },
    }
  );
  
  check(bulkJoinRes, {
    'bulk join status 200': (r) => r.status === 200,
    'bulk join response time OK': (r) => r.timings.duration < 1000,
  });
  
  // Test status check
  if (bulkJoinRes.status === 200) {
    const responses = JSON.parse(bulkJoinRes.body).responses;
    if (responses && responses.length > 0) {
      const ticketId = responses[0].ticketId;
      
      const statusRes = http.get(
        `${API_AWS_BASE}/queues/${queueId}/status?ticketId=${ticketId}`
      );
      
      check(statusRes, {
        'status check 200': (r) => r.status === 200,
        'status response time OK': (r) => r.timings.duration < 500,
      });
    }
  }
  
  sleep(Math.random() * 2 + 1); // Random sleep 1-3 seconds
}