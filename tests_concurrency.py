"""Live optimistic-lock concurrency smoke test for the local WorkHub V2 server.

Creates a temporary Ticket, runs 2 simultaneous updates from independent sessions,
then attempts cleanup via soft-delete. Requires MySQL, Redis, and Spring Boot running.
No third-party Python dependencies. Run: py tests_concurrency.py
"""
import base64
import getpass
import json
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from http.cookiejar import CookieJar
from urllib.request import Request, build_opener, HTTPCookieProcessor
from urllib.error import HTTPError

BASE = "http://localhost:8080"


class Session:
    def __init__(self, user, password):
        self.auth = "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()))
        self.csrf = None

    def send(self, method, path, data=None, version=None, idempotency_key=None):
        headers = {"Authorization": self.auth, "Accept": "application/json"}
        body = None
        if data is not None:
            if isinstance(data, (dict, list)):
                headers["Content-Type"] = "application/json"
                body = json.dumps(data).encode("utf-8")
            else:
                headers["Content-Type"] = "text/plain;charset=UTF-8"
                body = str(data).encode("utf-8")
        if version is not None:
            headers["If-Match"] = str(version)
        if idempotency_key is not None:
            headers["Idempotency-Key"] = idempotency_key
        if method not in ("GET", "HEAD"):
            if self.csrf is None:
                status, csrf_data = self.send("GET", "/csrf")
                if status != 200:
                    raise RuntimeError(f"Cannot obtain CSRF: {status}")
                self.csrf = json.loads(csrf_data)
            headers[self.csrf["headerName"]] = self.csrf["token"]
        req = Request(BASE + path, data=body, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=15) as response:
                return response.status, response.read().decode("utf-8")
        except HTTPError as error:
            return error.code, error.read().decode("utf-8")


def main():
    user = input("Manager username [Tom]: ").strip() or "Tom"
    password = getpass.getpass("Manager password: ")
    creator = Session(user, password)
    left = Session(user, password)
    right = Session(user, password)
    created_id = None
    try:
        status, me = creator.send("GET", "/api/me")
        if status != 200 or json.loads(me).get("role") != "MANAGER":
            raise RuntimeError(f"Manager login required, got HTTP {status}")
        payload = {"title": "TEMP_CONCURRENT_TEST", "description": "Temporary test ticket",
                   "category": "BACKEND"}
        # Keyed create returns the exact generated Ticket ID, no ambiguous list scanning.
        key = 'concurrent-' + uuid.uuid4().hex
        code, result = creator.send("POST", "/tickets", payload, idempotency_key=key)
        if code != 200 or not result.strip().isdigit() or int(result.strip()) < 1:
            raise RuntimeError(f"Cannot create keyed test Ticket: HTTP {code}, {result[:150]}")
        created_id = int(result.strip())
        code, detail = creator.send("GET", f"/tickets/{created_id}")
        ticket = json.loads(detail)
        original_version = ticket["version"]
        barrier = threading.Barrier(3)

        def worker(session):
            barrier.wait(timeout=10)
            return session.send("PUT", f"/tickets/{created_id}/status", "IN_PROGRESS", original_version)[0]

        with ThreadPoolExecutor(max_workers=2) as pool:
            one = pool.submit(worker, left)
            two = pool.submit(worker, right)
            barrier.wait(timeout=10)
            results = sorted((one.result(), two.result()))
        print("Concurrent HTTP results:", results)
        code, detail = creator.send("GET", f"/tickets/{created_id}")
        updated = json.loads(detail)
        print("Final status:", updated["status"], "version:", updated["version"])
        if results != [200, 409] or updated["version"] != original_version + 1:
            raise AssertionError("FAIL: concurrent CAS did not yield one success and one conflict")
        print("PASS: one concurrent writer succeeded, the other received HTTP 409")
    finally:
        if created_id is not None:
            try:
                code, data = creator.send("GET", f"/tickets/{created_id}")
                if code == 200:
                    version = json.loads(data)["version"]
                    code, _ = creator.send("DELETE", f"/tickets/{created_id}", version=version)
                    print("Temporary Ticket soft-delete:", code)
            except Exception as exc:
                print("WARNING: cleanup did not finish:", exc)


if __name__ == "__main__":
    main()
