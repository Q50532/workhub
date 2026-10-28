#!/usr/bin/env python3
"""Bounded READ-ONLY benchmark (Python stdlib). Do not point at production."""
import argparse
import base64
import concurrent.futures
import getpass
import json
import statistics
import time
import urllib.error
import urllib.request


def once(url, authorization, timeout):
    headers = {"Authorization": authorization, "Accept": "application/json"}
    request = urllib.request.Request(url, headers=headers, method="GET")
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            resp.read()
            status = resp.status
    except urllib.error.HTTPError as e:
        status = e.code
    except Exception:
        status = 0
    return status, (time.perf_counter() - start) * 1000


def percentile(numbers, p):
    if not numbers: return 0
    arr = sorted(numbers)
    idx = (len(arr)-1) * p
    lo = int(idx); hi = min(lo + 1, len(arr)-1)
    return arr[lo] + (arr[hi] - arr[lo]) * (idx - lo)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--ticket", type=int, default=None)
    parser.add_argument("--requests", type=int, default=100)
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--user", default="Tom")
    args = parser.parse_args()
    if not (1 <= args.requests <= 10000 and 1 <= args.concurrency <= 64):
        parser.error("Use 1..10000 requests and 1..64 concurrent workers")
    if not args.base.startswith(('http://localhost:', 'http://127.0.0.1:')):
        parser.error("Only localhost permitted by default")
    password = getpass.getpass("WorkHub password: ")
    auth = 'Basic ' + base64.b64encode(f'{args.user}:{password}'.encode()).decode()
    path = f'/tickets/{args.ticket}' if args.ticket else '/tickets?size=10&page=1'
    url = args.base.rstrip('/') + path
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(lambda _: once(url, auth, 10), range(args.requests)))
    elapsed = time.perf_counter() - started
    latencies = [ms for _, ms in results]
    tally = {}
    for status, _ in results:
        tally[str(status)] = tally.get(str(status), 0) + 1
    print(json.dumps({"endpoint": path, "requests": args.requests,
        "concurrency": args.concurrency, "seconds": round(elapsed, 3),
        "requests_per_second": round(args.requests / elapsed, 2),
        "p50_ms": round(percentile(latencies,.50), 2),
        "p95_ms": round(percentile(latencies,.95), 2),
        "p99_ms": round(percentile(latencies,.99), 2),
        "statuses": tally}, ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
