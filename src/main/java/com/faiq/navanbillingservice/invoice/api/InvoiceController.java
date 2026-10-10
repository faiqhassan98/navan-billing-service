package com.faiq.navanbillingservice.invoice.api;

import com.faiq.navanbillingservice.invoice.application.InvoiceService;
import com.faiq.navanbillingservice.transaction.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<InvoiceResponse>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(invoiceService.list(page, limit));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InvoiceResponse> patch(
            @PathVariable String id,
            @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.ok(invoiceService.patch(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}