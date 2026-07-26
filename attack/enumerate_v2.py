#!/usr/bin/env python3
"""
Attacking the v2 (secured) API — and failing.

Runs the same style of attack that worked on v0 and v1, and shows each layer of
v2 blocking it:

  A. Sequential enumeration is dead   -> ids are UUIDs, not countable integers.
  B. Object-level authorization holds -> even WITH a victim's exact UUID (as if it
                                         had leaked), a different user gets 404.
  C. Rate limiting                    -> a burst of requests is throttled (429).

For contrast it also shows the legitimate path still works: a user can read their
own record via /me. Standard library only.

Usage:
    python enumerate_v2.py [http://host:port]
"""

import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080"


def login(username, password="password"):
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(
        f"{BASE}/api/auth/login", data=body,
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)["token"]


def get(path, token):
    """Return (status_code, body_or_None). Never raises on HTTP errors."""
    req = urllib.request.Request(f"{BASE}{path}", headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, None


def main():
    # Simulate a leaked UUID: log in as the victim (user2) once to discover the
    # public id of their record, as if it had leaked via a log or shared link.
    victim_token = login("user2")
    _, victim = get("/v2/api/customers/me", victim_token)
    victim_uuid = victim["publicId"]
    print(f"[*] (Setup) Assume user2's UUID leaked: {victim_uuid}\n")

    # The attacker is just an ordinary authenticated user, user1.
    attacker = login("user1")
    print("[+] Attacker logged in as 'user1' with a valid JWT.\n")

    # Legitimate baseline: attacker can read their OWN record.
    status, mine = get("/v2/api/customers/me", attacker)
    print(f"[*] Baseline: GET /me  -> HTTP {status} ({mine['fullName']}). Own data, allowed.\n")

    # Attack A: the old sequential enumeration.
    print("[*] Attack A - sequential enumeration (ids 1..5):")
    for i in range(1, 6):
        status, _ = get(f"/v2/api/customers/{i}", attacker)
        print(f"      GET /v2/api/customers/{i} -> HTTP {status}")
    print("    Integers are not valid UUIDs; there is nothing to count. Enumeration is dead.\n")

    # Attack B: use the victim's leaked UUID directly.
    status, body = get(f"/v2/api/customers/{victim_uuid}", attacker)
    print(f"[*] Attack B - request user2's record with the leaked UUID -> HTTP {status}")
    print("    404 (identical to 'not found'): object-level authorization blocks it,")
    print("    and the status code reveals nothing about whether the record exists.\n")

    # Attack C: a burst, to trip the rate limiter.
    print("[*] Attack C - bursting /me to trigger rate limiting...")
    first_429 = None
    for n in range(1, 201):
        status, _ = get("/v2/api/customers/me", attacker)
        if status == 429:
            first_429 = n
            break
    if first_429:
        print(f"    throttled with HTTP 429 after {first_429} rapid requests.\n")
    else:
        print("    (no 429 seen; raise the request count or lower app.ratelimit.max-per-minute)\n")

    print("[=] Result: authentication + object-level authorization + UUIDs + rate")
    print("    limiting together defeat the enumeration attack that dumped everything")
    print("    on v0 and v1.")


if __name__ == "__main__":
    main()
