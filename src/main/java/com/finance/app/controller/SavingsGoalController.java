package com.finance.app.controller;

import com.finance.app.dto.SavingsGoalContributionDTO;
import com.finance.app.dto.SavingsGoalRequestDTO;
import com.finance.app.dto.SavingsGoalResponseDTO;
import com.finance.app.entity.GoalStatus;
import com.finance.app.service.SavingsGoalService;
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
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Savings goals, milestones, and progress allocation")
@CrossOrigin(origins = "*")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @GetMapping
    @Operation(summary = "Get all savings goals, optionally filtered by status (IN_PROGRESS, COMPLETED, CANCELLED)")
    public ResponseEntity<List<SavingsGoalResponseDTO>> getAllGoals(
            @Parameter(description = "Filter by goal status") @RequestParam(required = false) GoalStatus status) {
        return ResponseEntity.ok(savingsGoalService.getAllGoals(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get savings goal details by ID")
    public ResponseEntity<SavingsGoalResponseDTO> getGoalById(@PathVariable Long id) {
        return ResponseEntity.ok(savingsGoalService.getGoalById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new financial savings goal")
    public ResponseEntity<SavingsGoalResponseDTO> createGoal(@Valid @RequestBody SavingsGoalRequestDTO request) {
        SavingsGoalResponseDTO created = savingsGoalService.createGoal(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit funds towards a savings goal (deducts from linked account if present)")
    public ResponseEntity<SavingsGoalResponseDTO> depositToGoal(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalContributionDTO contribution) {
        return ResponseEntity.ok(savingsGoalService.depositToGoal(id, contribution));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw funds from a savings goal (refunds to linked account if present)")
    public ResponseEntity<SavingsGoalResponseDTO> withdrawFromGoal(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalContributionDTO contribution) {
        return ResponseEntity.ok(savingsGoalService.withdrawFromGoal(id, contribution));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        savingsGoalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
