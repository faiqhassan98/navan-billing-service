package com.faiq.navanbillingservice;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class TransactionApiTest extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    // ── Seed payloads ────────────────────────────────────────────────────

    static final String FLIGHT = """
        {
          "type": "flight",
          "external_id": "flight-TXL-LHR-2026-03-11",
          "currency": "EUR",
          "occurred_at": "2026-03-11T06:40:00Z",
          "total": 220.00,
          "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
          "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
          "metadata": {
            "origin": "TXL", "destination": "LHR", "fare": 180.00,
            "coupons": [{
              "carrier": "BA", "flight_number": "BA982",
              "from": "TXL", "to": "LHR",
              "departure": "2026-03-11T07:00:00Z",
              "arrival":   "2026-03-11T08:20:00Z"
            }]
          }
        }""";

    static final String HOTEL = """
        {
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
            "check_in": "2026-03-11", "check_out": "2026-03-13",
            "nights": 2, "room_rate_per_night": 140.00,
            "extras": [{ "name": "Breakfast", "amount": 30.00 }]
          }
        }""";

    static final String RAIL = """
        {
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
                "origin": "London St Pancras", "destination": "Lille Europe",
                "departure": "2026-03-13T09:00:00Z", "arrival": "2026-03-13T10:30:00Z",
                "train_number": "EST9108", "class": "standard"
              },
              {
                "origin": "Lille Europe", "destination": "Paris Nord",
                "departure": "2026-03-13T11:00:00Z", "arrival": "2026-03-13T12:05:00Z",
                "train_number": "EST9122", "class": "standard"
              }
            ]
          }
        }""";

    static final String TRIP_FEE = """
        {
          "type": "navan_fee",
          "external_id": "navan-trip-fee-2026-03-11",
          "currency": "EUR",
          "occurred_at": "2026-03-11T06:40:00Z",
          "total": 25.00,
          "tax_lines": [], "fee_lines": [],
          "metadata": {
            "fee_kind": "trip_fee",
            "description": "Navan trip fee",
            "related_external_id": "flight-TXL-LHR-2026-03-11"
          }
        }""";

    static final String AGENT_FEE = """
        {
          "type": "navan_fee",
          "external_id": "navan-agent-call-2026-03-11",
          "currency": "EUR",
          "occurred_at": "2026-03-11T18:00:00Z",
          "total": 15.00,
          "tax_lines": [], "fee_lines": [],
          "metadata": {
            "fee_kind": "agent_call_fee",
            "description": "Agent fee for support call",
            "related_external_id": "hotel-LON-2026-03-11"
          }
        }""";

    // Helper: POST a transaction and return its id
    String post(String body) {
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/transactions")
                .then().statusCode(201)
                .extract().path("id");
    }

    // ── Test 1: Create all 5 types and GET each back ──────────────────────

    @Test
    void test1_createAllTypesAndGetBack() {
        // Flight
        String flightId = post(FLIGHT);
        given().when().get("/transactions/" + flightId)
                .then().statusCode(200)
                .body("type", equalTo("flight"))
                .body("metadata.coupons[0].flight_number", equalTo("BA982"))
                .body("tax_lines[0].name", equalTo("Airport tax"))
                .body("fee_lines[0].name", equalTo("YQ"));

        // Hotel
        String hotelId = post(HOTEL);
        given().when().get("/transactions/" + hotelId)
                .then().statusCode(200)
                .body("type", equalTo("hotel"))
                .body("metadata.name", equalTo("Hotel Mitte London"))
                .body("metadata.address.city", equalTo("London"));

        // Rail
        String railId = post(RAIL);
        given().when().get("/transactions/" + railId)
                .then().statusCode(200)
                .body("type", equalTo("rail"))
                .body("metadata.legs", hasSize(2))
                .body("metadata.legs[0].train_number", equalTo("EST9108"));

        // Trip fee
        String tripFeeId = post(TRIP_FEE);
        given().when().get("/transactions/" + tripFeeId)
                .then().statusCode(200)
                .body("type", equalTo("navan_fee"))
                .body("metadata.fee_kind", equalTo("trip_fee"));

        // Agent call fee
        String agentFeeId = post(AGENT_FEE);
        given().when().get("/transactions/" + agentFeeId)
                .then().statusCode(200)
                .body("type", equalTo("navan_fee"))
                .body("metadata.fee_kind", equalTo("agent_call_fee"));
    }

    // ── Test 2: Duplicate external_id returns 409 with existing id ────────

    @Test
    void test2_duplicateExternalIdReturns409() {
        String firstId = post(FLIGHT);

        // Same external_id, different body (empty coupons like the fixture)
        String duplicate = """
            {
              "type": "flight",
              "external_id": "flight-TXL-LHR-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 220.00,
              "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
              "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
              "metadata": {
                "origin": "TXL", "destination": "LHR", "fare": 180.00,
                "coupons": []
              }
            }""";

        given().contentType(ContentType.JSON).body(duplicate)
                .when().post("/transactions")
                .then().statusCode(409)
                .body("existing_id", equalTo(firstId));

        // Only one row in the database
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM transactions WHERE external_id = 'flight-TXL-LHR-2026-03-11'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ── Test 3: Trip invoice totals 685.70 EUR ────────────────────────────

    @Test
    void test3_tripInvoiceTotals() {
        String flightId   = post(FLIGHT);
        String hotelId    = post(HOTEL);
        String railId     = post(RAIL);
        String tripFeeId  = post(TRIP_FEE);
        String agentFeeId = post(AGENT_FEE);

        String invoiceBody = """
            { "transaction_ids": ["%s", "%s", "%s", "%s", "%s"] }
            """.formatted(flightId, hotelId, railId, tripFeeId, agentFeeId);

        given().contentType(ContentType.JSON).body(invoiceBody)
                .when().post("/invoices")
                .then().statusCode(201)
                .body("currency", equalTo("EUR"))
                .body("total",    equalTo(685.70f))
                .body("lines",    hasSize(5));
    }

    // ── Test 4: Snapshot — mutate flight total, invoice stays frozen ──────

    @Test
    void test4_invoiceSnapshotStaysFrozen() {
        String flightId = post(FLIGHT);

        String invoiceId = given().contentType(ContentType.JSON)
                .body("""
                    { "transaction_id": "%s" }
                    """.formatted(flightId))
                .when().post("/invoices")
                .then().statusCode(201)
                .extract().path("id");

        // Mutate flight total directly in DB (PATCH would return 409 since it's invoiced)
        jdbc.update("UPDATE transactions SET total = 999.00 WHERE id = ?", flightId);

        // Invoice must still show 220.00
        given().when().get("/invoices/" + invoiceId)
                .then().statusCode(200)
                .body("lines[0].total", equalTo(220.00f));
    }

    // ── Test 5: Mixed currency returns 409 ───────────────────────────────

    @Test
    void test5_mixedCurrencyReturns409() {
        String eurId = post(FLIGHT);

        String usdTxn = """
            {
              "type": "navan_fee",
              "external_id": "navan-usd-fee-test",
              "currency": "USD",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 10.00,
              "tax_lines": [], "fee_lines": [],
              "metadata": { "fee_kind": "trip_fee" }
            }""";
        String usdId = post(usdTxn);

        given().contentType(ContentType.JSON)
                .body("""
                    { "transaction_ids": ["%s", "%s"] }
                    """.formatted(eurId, usdId))
                .when().post("/invoices")
                .then().statusCode(409);
    }

    // ── Test 6: Concurrent duplicate POST leaves exactly one row ─────────

    @Test
    void test6_concurrentDuplicatePostLeavesOneRow() throws Exception {
        int threads = 5;
        var latch   = new CountDownLatch(1);
        var pool    = Executors.newFixedThreadPool(threads);
        var created = new AtomicInteger(0);
        var conflict= new AtomicInteger(0);

        String body = """
            {
              "type": "navan_fee",
              "external_id": "concurrent-test-ext-id",
              "currency": "EUR",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 5.00,
              "tax_lines": [], "fee_lines": [],
              "metadata": { "fee_kind": "trip_fee" }
            }""";

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();  // all threads fire at once
                    int status = given().contentType(ContentType.JSON).body(body)
                            .when().post("/transactions")
                            .getStatusCode();
                    if (status == 201) created.incrementAndGet();
                    if (status == 409) conflict.incrementAndGet();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();  // release all threads simultaneously
        pool.shutdown();
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(created.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM transactions WHERE external_id = 'concurrent-test-ext-id'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ── Test 7: PATCH invoiced transaction returns 409 ───────────────────

    @Test
    void test7_patchInvoicedTransactionReturns409() {
        String flightId = post(FLIGHT);

        given().contentType(ContentType.JSON)
                .body("""
                    { "transaction_id": "%s" }
                    """.formatted(flightId))
                .when().post("/invoices")
                .then().statusCode(201);

        given().contentType(ContentType.JSON)
                .body("""
                    { "total": 999.00 }
                    """)
                .when().patch("/transactions/" + flightId)
                .then().statusCode(409);

        // Invoice snapshot total unchanged
        given().when().get("/transactions/" + flightId)
                .then().statusCode(200)
                .body("total", equalTo(220.00f));
    }

    // ── Test 8: DELETE invoiced transaction returns 409 ──────────────────

    @Test
    void test8_deleteInvoicedTransactionReturns409() {
        String flightId = post(FLIGHT);

        given().contentType(ContentType.JSON)
                .body("""
                    { "transaction_id": "%s" }
                    """.formatted(flightId))
                .when().post("/invoices")
                .then().statusCode(201);

        given().when().delete("/transactions/" + flightId)
                .then().statusCode(409);

        // Row still exists
        given().when().get("/transactions/" + flightId)
                .then().statusCode(200);
    }

    // ── Test 9: DELETE invoice — invoice gone, transactions survive ───────

    @Test
    void test9_deleteInvoiceKeepsTransactions() {
        String flightId = post(FLIGHT);

        String invoiceId = given().contentType(ContentType.JSON)
                .body("""
                    { "transaction_id": "%s" }
                    """.formatted(flightId))
                .when().post("/invoices")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/invoices/" + invoiceId)
                .then().statusCode(204);

        given().when().get("/invoices/" + invoiceId)
                .then().statusCode(404);

        given().when().get("/transactions/" + flightId)
                .then().statusCode(200);
    }

    // ── Test 10: Pagination — default 50, limit=1000 capped/rejected ─────

    @Test
    void test10_paginationCapOrReject() {
        // Default limit works
        given().when().get("/transactions?page=0")
                .then().statusCode(200)
                .body("page_size", lessThanOrEqualTo(50));

        // limit=1000 must be rejected (400) or capped at 100
        int status = given().when()
                .get("/transactions?page=0&limit=1000")
                .getStatusCode();
        assertThat(status).isIn(400, 200);

        if (status == 200) {
            given().when().get("/transactions?page=0&limit=1000")
                    .then().body("page_size", lessThanOrEqualTo(100));
        }
    }
}