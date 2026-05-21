// k6 load test for the task API. Heavier and more scriptable than load_probe.py.
//
//   docker compose up -d            # start app + Postgres
//   k6 run load/k6-script.js        # needs k6 installed (https://k6.io)
//
// Registers a unique user per VU, seeds tasks, then drives the authenticated
// list endpoint. Thresholds fail the run if p95 > 250ms or error rate > 1%.

import http from "k6/http";
import { check, sleep } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    ramp: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 50 },
        { duration: "30s", target: 50 },
        { duration: "10s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<250"],
  },
};

export function setup() {
  const email = `k6-${Date.now()}-${Math.random().toString(36).slice(2)}@example.com`;
  const reg = http.post(
    `${BASE}/api/auth/register`,
    JSON.stringify({ email, password: "password123" }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(reg, { "registered": (r) => r.status === 201 });
  const token = reg.json("token");

  const auth = { headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` } };
  for (let i = 0; i < 10; i++) {
    http.post(`${BASE}/api/tasks`, JSON.stringify({ title: `seed ${i}`, priority: "MEDIUM" }), auth);
  }
  return { token };
}

export default function (data) {
  const res = http.get(`${BASE}/api/tasks?size=20`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  check(res, { "list 200": (r) => r.status === 200 });
  sleep(0.1);
}
