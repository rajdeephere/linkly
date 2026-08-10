// k6 load test for the redirect hot path (Day 9).
// Hammers a single HOT link through the edge and measures latency + throughput.
// A hot link must be served entirely from edge KV — the origin resolver should see ~zero traffic.
//
// Run (Docker, nothing to install):
//   docker run --rm -i --add-host=host.docker.internal:host-gateway \
//     -e TARGET="http://host.docker.internal:3000/<code>" \
//     grafana/k6 run - < infra/loadtest/redirect.js
//
// TARGET must be an already-warmed hot link (resolve it once first so KV is populated).

import http from "k6/http";
import { check } from "k6";

const TARGET = __ENV.TARGET;

export const options = {
  scenarios: {
    hot_link: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "5s", target: 50 },   // ramp up
        { duration: "20s", target: 50 },  // sustained load
        { duration: "5s", target: 0 },    // ramp down
      ],
      gracefulStop: "2s",
    },
  },
  thresholds: {
    // The hot path is a KV lookup + 302 — should be fast and never error.
    http_req_duration: ["p(95)<50", "p(99)<150"],
    http_req_failed: ["rate<0.01"],
    redirect_302: ["rate>0.99"],
  },
};

import { Rate } from "k6/metrics";
const got302 = new Rate("redirect_302");

export default function () {
  // redirects:0 so k6 records the 302 itself, not the followed destination.
  const res = http.get(TARGET, { redirects: 0 });
  got302.add(res.status === 302);
  check(res, {
    "status is 302": (r) => r.status === 302,
    "has Location": (r) => !!r.headers["Location"],
  });
}
