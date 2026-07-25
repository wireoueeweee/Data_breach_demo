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
| v0    | `/v0/api/…` | **Vulnerable.** No auth, sequential IDs. *(this build)*          |
| v1    | `/v1/api/…` | Authentication only — still vulnerable to BOLA (planned)         |
| v2    | `/v2/api/…` | Auth + object-level authorization + UUIDs + rate limit + logging |

The key learning point: adding authentication (v1) does **not** fix the breach.
A logged-in user can still enumerate other users' records until **object-level
authorization** is added (v2). That contrast is the centrepiece experiment.

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
