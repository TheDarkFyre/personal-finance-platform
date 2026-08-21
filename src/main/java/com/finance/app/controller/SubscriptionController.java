package com.finance.app.controller;

import com.finance.app.dto.SubscriptionRequestDTO;
import com.finance.app.dto.SubscriptionResponseDTO;
import com.finance.app.dto.SubscriptionSummaryDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.SubscriptionStatus;
import com.finance.app.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Recurring bills, subscriptions, and monthly burn rate tracking")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Get all subscriptions, optionally filtered by status (ACTIVE, PAUSED, CANCELLED)")
    public ResponseEntity<List<SubscriptionResponseDTO>> getAllSubscriptions(
            @Parameter(description = "Filter by status") @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(status));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall subscription summary, monthly recurring burn rate, and upcoming bills")
    public ResponseEntity<SubscriptionSummaryDTO> getSubscriptionSummary() {
        return ResponseEntity.ok(subscriptionService.getSubscriptionSummary());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription details by ID")
    public ResponseEntity<SubscriptionResponseDTO> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @PostMapping
    @Operation(summary = "Create and track a new recurring subscription or bill")
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(@Valid @RequestBody SubscriptionRequestDTO request) {
        SubscriptionResponseDTO created = subscriptionService.createSubscription(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing subscription")
    public ResponseEntity<SubscriptionResponseDTO> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequestDTO request) {
        return ResponseEntity.ok(subscriptionService.updateSubscription(id, request));
    }

    @PutMapping("/{id}/pause")
    @Operation(summary = "Pause an active subscription")
    public ResponseEntity<SubscriptionResponseDTO> pauseSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.updateStatus(id, SubscriptionStatus.PAUSED));
    }

    @PutMapping("/{id}/resume")
    @Operation(summary = "Resume a paused subscription")
    public ResponseEntity<SubscriptionResponseDTO> resumeSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.updateStatus(id, SubscriptionStatus.ACTIVE));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "1-Click Pay: Records transaction payment and advances next billing due date")
    public ResponseEntity<TransactionResponseDTO> paySubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.paySubscription(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tracked subscription")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}
