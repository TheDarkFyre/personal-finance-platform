package com.finance.app.controller;

import com.finance.app.dto.CashFlowSummaryDTO;
import com.finance.app.dto.CategorySummaryDTO;
import com.finance.app.dto.MonthlySummaryDTO;
import com.finance.app.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Financial insights, category spending breakdowns, and cash flow reports")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/spending-by-category")
    @Operation(summary = "Get total spending broken down by expense category for a date range")
    public ResponseEntity<List<CategorySummaryDTO>> getSpendingByCategory(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to current date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getSpendingByCategory(startDate, endDate));
    }

    @GetMapping("/income-by-category")
    @Operation(summary = "Get total income broken down by category for a date range")
    public ResponseEntity<List<CategorySummaryDTO>> getIncomeByCategory(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to current date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getIncomeByCategory(startDate, endDate));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Get monthly financial summary (income, expenses, savings rate)")
    public ResponseEntity<MonthlySummaryDTO> getMonthlySummary(
            @Parameter(description = "Year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month) {
        return ResponseEntity.ok(analyticsService.getMonthlySummary(year, month));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Get overall cash flow analysis and net worth summary")
    public ResponseEntity<CashFlowSummaryDTO> getCashFlowSummary(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getCashFlowSummary(startDate, endDate));
    }
}
