import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 2000 },   // Ramp up to 2k users
    { duration: '15m', target: 2000 },  // Soak test for 15 minutes
    { duration: '2m', target: 0 },      // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(90)<500', 'p(95)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

const API_AWS_BASE = __ENV.API_AWS_BASE || 'http://localhost:8080';
const API_ALIYUN_BASE = __ENV.API_ALIYUN_BASE || 'http://localhost:8081';

export default function () {
  const queueId = 'soak-test-queue';
  const ticketId = 'test-ticket-' + Math.floor(Math.random() * 10000);
  
  // Test status endpoint (read-heavy)
  const statusRes = http.get(
    `${API_AWS_BASE}/queues/${queueId}/status?ticketId=${ticketId}`
  );
  
  check(statusRes, {
    'status check works': (r) => r.status === 200 || r.status === 400, // 400 is OK for non-existent ticket
    'status response time OK': (r) => r.timings.duration < 800,
  });
  
  // Test ETA endpoint  
  const etaRes = http.get(
    `${API_ALIYUN_BASE}/eta?queueId=${queueId}&ticketId=${ticketId}&position=5`
  );
  
  check(etaRes, {
    'eta check works': (r) => r.status === 200 || r.status === 400,
    'eta response time OK': (r) => r.timings.duration < 1000,
  });
  
  sleep(Math.random() * 3 + 2); // Random sleep 2-5 seconds
}