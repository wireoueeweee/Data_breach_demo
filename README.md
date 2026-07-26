# Breach Demo — Recreating the Optus-style API vulnerability

An educational project that recreates, exploits, and then fixes the class of API
vulnerability behind the 2022 Optus data breach: an API endpoint with **no
authentication** serving records keyed by **predictable, sequential IDs**, which
lets an attacker enumerate and exfiltrate the entire dataset.

> **Ethics / scope:** All data is randomly generated and fictitious. The system
> runs entirely on localhost against its own throwaway in-memory database. No
> real people, real systems, or third parties are involved at any point.

## Stack

- Java 17, Spring Boot 3.3, Maven
- H2 in-memory database (zero setup, reseeds on every run; swappable for MySQL by
  changing only the `spring.datasource` config)
- Spring Data JPA
- datafaker for generating fake customer records

## Stages

This repo is built to demonstrate the security posture evolving in stages, so the
same attack can be run against each:

| Stage | Path        | State                                                             |
|-------|-------------|------------------------------------------------------------------|
| v0    | `/v0/api/…` | **Vulnerable.** No auth, sequential IDs.                          |
| v1    | `/v1/api/…` | **Authentication only — still vulnerable to BOLA.**              |
| v2    | `/v2/api/…` | **Secured.** Auth + object-level authorization + UUIDs + rate limit. *(this build)* |

The key learning point: adding authentication (v1) does **not** fix the breach.
A logged-in user can still enumerate other users' records until **object-level
authorization** is added (v2). That contrast is the centrepiece experiment.

### The v1 "half-fix failure" experiment

v1 puts every `/v1/**` endpoint behind a JWT. Log in to get a token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"user1","password":"password"}'
```

Every seeded account is `user1`, `user2`, ... and (for this demo only) shares the
password `password`. Customer record with `id = N` is owned by `userN`.

Now run the experiment:

```bash
cd attack
python enumerate_authenticated.py
```

It (1) confirms `/v1` blocks unauthenticated requests, (2) logs in as `user1`,
(3) reads `user1`'s own record legitimately, then (4) enumerates **all** records
with that same single-user token — proving authentication did not stop the
breach. The missing control is object-level authorization, added in v2.

### v2 — the fix

v2 adds three things on top of v1's authentication:

- **Object-level authorization** (`OwnershipAuthorizationService`): a record is
  only returned if the caller owns it. This is the actual fix for the breach.
- **UUID identifiers**: `/v2` exposes records by their unguessable `publicId`,
  never the sequential database id, so there is nothing to count.
- **Per-user rate limiting** (`RateLimitFilter`): bursts are throttled with 429,
  as defence in depth (configurable via `app.ratelimit.max-per-minute`).

"Record not found" and "record exists but is not yours" both return an identical
**404**, so status codes cannot be used to probe which records exist.

Run the attack against v2 and watch every layer hold:

```bash
cd attack
python enumerate_v2.py
```

It shows sequential enumeration is dead, that even a *leaked* UUID is rejected for
a non-owner (404), and that a burst trips the rate limiter — while a user reading
their own record via `/v2/api/customers/me` still works.

## Run the server

```bash
mvn spring-boot:run
```

On startup it seeds 10,000 fake customer records (configurable via
`app.seed.count` in `application.yml`). Inspect the data at
<http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:breachdemo`, user `sa`,
no password).

Try a request:

```bash
curl http://localhost:8080/v0/api/customers/1
curl http://localhost:8080/v0/api/customers/count
```

## Run the attack

```bash
cd attack
python enumerate.py
```

This pulls every record by counting IDs from 1 upward and writes the full haul to
`stolen_data.json`, reporting how long the dump took.
