# Navan Billing Service

A production-grade REST API for flight, hotel, rail, and Navan fee transactions with frozen invoice snapshots. Built with Java 21, Spring Boot 3.4.5, and PostgreSQL 16.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Database | PostgreSQL 16 |
| Schema migrations | Flyway |
| ORM | Spring Data JPA (Hibernate 6) |
| JSON | Jackson (snake_case, BigDecimal) |
| Testing | JUnit 5, REST Assured, Spring Boot Test |

## Prerequisites

- Java 21
- Docker and Docker Compose v2

Verify your setup:
```bash
java -version      # openjdk 21.x
docker compose version   # v2.x
```

## Run the API (one command)

```bash
docker compose up -d && ./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**

Health check:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Run the tests (one command)

```bash
docker compose up -d && ./mvnw test
```

All 10 required tests hit the real HTTP API against a running PostgreSQL instance. The tests load directly from `fixtures/task-b/seed-payloads.json` and `fixtures/task-b/expected-invoice.json`.

Expected output:
```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Money

All monetary values use `NUMERIC(19,4)` in PostgreSQL and `java.math.BigDecimal` in Java throughout — never `double` or `float`. Scale: 4 decimal places. Invoice rollup totals are computed with `BigDecimal::add` and compared with `compareTo`, never `==`.

---

## API Reference with example curls

### Create transactions

#### Flight
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.00,
      "coupons": [{
        "carrier": "BA",
        "flight_number": "BA982",
        "from": "TXL",
        "to": "LHR",
        "departure": "2026-03-11T07:00:00Z",
        "arrival": "2026-03-11T08:20:00Z"
      }]
    }
  }'
```

#### Hotel
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "hotel",
    "external_id": "hotel-LON-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T15:00:00Z",
    "total": 331.70,
    "tax_lines": [{ "name": "VAT", "rate": 0.07, "amount": 21.70 }],
    "fee_lines": [],
    "metadata": {
      "name": "Hotel Mitte London",
      "address": { "street": "12 Kingsway", "city": "London", "country": "GB" },
      "check_in": "2026-03-11",
      "check_out": "2026-03-13",
      "nights": 2,
      "room_rate_per_night": 140.00,
      "extras": [{ "name": "Breakfast", "amount": 30.00 }]
    }
  }'
```

#### Rail
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "rail",
    "external_id": "rail-STP-PAR-2026-03-13",
    "currency": "EUR",
    "occurred_at": "2026-03-13T09:00:00Z",
    "total": 94.00,
    "tax_lines": [],
    "fee_lines": [{ "name": "Booking fee", "amount": 4.00 }],
    "metadata": {
      "legs": [
        {
          "origin": "London St Pancras",
          "destination": "Lille Europe",
          "departure": "2026-03-13T09:00:00Z",
          "arrival": "2026-03-13T10:30:00Z",
          "train_number": "EST9108",
          "class": "standard"
        },
        {
          "origin": "Lille Europe",
          "destination": "Paris Nord",
          "departure": "2026-03-13T11:00:00Z",
          "arrival": "2026-03-13T12:05:00Z",
          "train_number": "EST9122",
          "class": "standard"
        }
      ]
    }
  }'
```

#### Navan trip fee
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "navan-trip-fee-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 25.00,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "trip_fee",
      "description": "Navan trip fee",
      "related_external_id": "flight-TXL-LHR-2026-03-11"
    }
  }'
```

#### Navan agent call fee
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "navan_fee",
    "external_id": "navan-agent-call-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T18:00:00Z",
    "total": 15.00,
    "tax_lines": [],
    "fee_lines": [],
    "metadata": {
      "fee_kind": "agent_call_fee",
      "description": "Agent fee for support call",
      "related_external_id": "hotel-LON-2026-03-11"
    }
  }'
```

---

### Duplicate create — only one row is ever stored

```bash
# First POST → 201 Created
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
    "metadata": {
      "origin": "TXL", "destination": "LHR", "fare": 180.00, "coupons": []
    }
  }'

# Second POST with same external_id → 409 Conflict with existing_id in body
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
    "metadata": {
      "origin": "TXL", "destination": "LHR", "fare": 180.00, "coupons": []
    }
  }'
# Response: 409 {"existing_id": "txn_..."}
```

---

### Get and list transactions

```bash
# Get by id → 200 or 404
curl -s http://localhost:8080/transactions/txn_abc123

# List with pagination (default limit 50, max 100)
curl -s "http://localhost:8080/transactions?page=0&limit=10"

# limit > 100 → 400 Bad Request
curl -s "http://localhost:8080/transactions?page=0&limit=1000"
```

---

### PATCH transaction

```bash
# Update fields — returns 409 if transaction is on any invoice
curl -s -X PATCH http://localhost:8080/transactions/txn_abc123 \
  -H "Content-Type: application/json" \
  -d '{ "total": 250.00 }'
```

### DELETE transaction

```bash
# Returns 409 if transaction is on any invoice, 404 if not found
curl -s -X DELETE http://localhost:8080/transactions/txn_abc123
```

---

### Create invoice

```bash
# Single transaction
curl -s -X POST http://localhost:8080/invoices \
  -H "Content-Type: application/json" \
  -d '{ "transaction_id": "txn_abc123" }'

# Trip invoice: flight + hotel + rail + both Navan fees = 685.70 EUR
curl -s -X POST http://localhost:8080/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_ids": [
      "<flight_id>",
      "<hotel_id>",
      "<rail_id>",
      "<trip_fee_id>",
      "<agent_fee_id>"
    ]
  }'

# Mixed currencies → 409 Conflict
# Duplicate transaction_ids in one request → 409 Conflict
```

### Get and list invoices

```bash
# Get by id → 200 or 404 (includes snapshot lines, tax rollup, fee rollup)
curl -s http://localhost:8080/invoices/inv_abc123

# List with pagination
curl -s "http://localhost:8080/invoices?page=0&limit=10"
```

### PATCH invoice

```bash
# Replace the transaction set and rebuild the snapshot atomically
curl -s -X PATCH http://localhost:8080/invoices/inv_abc123 \
  -H "Content-Type: application/json" \
  -d '{ "transaction_ids": ["txn_new1", "txn_new2"] }'
```

### DELETE invoice

```bash
# Removes the invoice and all snapshot lines
# The underlying transactions are NOT deleted
curl -s -X DELETE http://localhost:8080/invoices/inv_abc123
```

---

## Error responses

All errors follow RFC 7807 ProblemDetail format:

```json
{
  "type": "urn:navan:duplicate-transaction",
  "title": "Duplicate transaction",
  "status": 409,
  "detail": "Duplicate external_id — existing transaction id: txn_abc123",
  "existing_id": "txn_abc123"
}
```

| Status | Condition |
|---|---|
| 400 | Unknown type, unknown navan_fee.fee_kind, validation error, limit > 100 |
| 404 | Transaction or invoice not found |
| 409 | Duplicate external_id, mixed currencies, PATCH/DELETE on invoiced transaction |