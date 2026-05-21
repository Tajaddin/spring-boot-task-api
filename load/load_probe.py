"""HTTP load probe for the task API.

Registers a user, seeds a few tasks, then drives concurrent authenticated
GET /api/tasks requests and reports throughput (req/s) and latency p50/p95/p99.

Standard-library only (urllib + threads), so it runs anywhere Python does.
For a heavier, scriptable load test see k6-script.js.

Usage:
    python load/load_probe.py --base http://localhost:8080 --requests 5000 --concurrency 50
"""
from __future__ import annotations

import argparse
import json
import statistics
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor


def _post(base: str, path: str, body: dict, token: str | None = None) -> tuple[int, dict]:
    data = json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, json.loads(resp.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, {}


def _get_latency(base: str, path: str, token: str) -> float:
    req = urllib.request.Request(base + path, method="GET")
    req.add_header("Authorization", "Bearer " + token)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            resp.read()
            return (time.perf_counter() - t0) * 1000 if resp.status == 200 else -1.0
    except urllib.error.HTTPError:
        return -1.0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--requests", type=int, default=5000)
    ap.add_argument("--concurrency", type=int, default=50)
    ap.add_argument("--out", default="load/results.json")
    args = ap.parse_args()

    email = f"load-{uuid.uuid4().hex[:8]}@example.com"
    status, body = _post(args.base, "/api/auth/register", {"email": email, "password": "password123"})
    if status != 201:
        print(f"register failed: {status}")
        return 1
    token = body["token"]

    # Seed 10 tasks so the list endpoint returns a realistic page.
    for i in range(10):
        _post(args.base, "/api/tasks", {"title": f"seed task {i}", "priority": "MEDIUM"}, token)

    # Warm up.
    for _ in range(100):
        _get_latency(args.base, "/api/tasks?size=20", token)

    latencies: list[float] = []
    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = [
            pool.submit(_get_latency, args.base, "/api/tasks?size=20", token)
            for _ in range(args.requests)
        ]
        for f in futures:
            ms = f.result()
            if ms >= 0:
                latencies.append(ms)
    elapsed = time.perf_counter() - t0

    latencies.sort()
    n = len(latencies)
    qps = n / elapsed if elapsed > 0 else 0.0
    summary = {
        "endpoint": "GET /api/tasks?size=20 (JWT-authenticated, H2 demo profile)",
        "requests_ok": n,
        "requests_total": args.requests,
        "concurrency": args.concurrency,
        "wall_seconds": round(elapsed, 3),
        "throughput_rps": round(qps, 1),
        "latency_ms": {
            "p50": round(latencies[int(0.50 * n)], 2),
            "p95": round(latencies[int(0.95 * n)], 2),
            "p99": round(latencies[int(0.99 * n)], 2),
            "max": round(latencies[-1], 2),
            "mean": round(statistics.mean(latencies), 2),
        },
    }
    with open(args.out, "w") as fh:
        json.dump(summary, fh, indent=2)
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
