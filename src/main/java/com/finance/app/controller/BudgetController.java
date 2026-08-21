package com.finance.app.controller;

import com.finance.app.dto.BudgetProgressDTO;
import com.finance.app.dto.BudgetRequestDTO;
import com.finance.app.dto.BudgetResponseDTO;
import com.finance.app.service.BudgetService;
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
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Monthly category budget limits and overspending tracking")
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/progress")
    @Operation(summary = "Get monthly budget progress, remaining amounts, and overspending alerts")
    public ResponseEntity<List<BudgetProgressDTO>> getBudgetProgress(
            @Parameter(description = "Year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month) {
        return ResponseEntity.ok(budgetService.getBudgetProgressForMonth(year, month));
    }

    @GetMapping
    @Operation(summary = "Get configured budgets for a specific month and year")
    public ResponseEntity<List<BudgetResponseDTO>> getBudgets(
            @Parameter(description = "Year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month) {
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(year, month));
    }

    @PostMapping
    @Operation(summary = "Set or update a monthly category spending limit")
    public ResponseEntity<BudgetResponseDTO> createOrUpdateBudget(@Valid @RequestBody BudgetRequestDTO request) {
        BudgetResponseDTO saved = budgetService.createOrUpdateBudget(request);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category budget limit")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
