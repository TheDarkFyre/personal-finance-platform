package com.finance.app.controller;

import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction recording, search, and balance management")
public class TransactionController {

    private final TransactionService transactionService;
    private final com.finance.app.service.CsvImportService csvImportService;

    @PostMapping(value = "/import-csv", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import transactions from a bank statement CSV with intelligent auto-categorization")
    public ResponseEntity<com.finance.app.dto.CsvImportSummaryDTO> importCsv(
            @Parameter(description = "Account ID to associate imported transactions with") @RequestParam Long accountId,
            @Parameter(description = "CSV statement file") @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(csvImportService.importCsv(file, accountId));
    }

    @GetMapping
    @Operation(summary = "Get paginated transactions with optional account, category, or date range filtering")
    public ResponseEntity<Page<TransactionResponseDTO>> getTransactions(
            @Parameter(description = "Filter by Account ID") @RequestParam(required = false) Long accountId,
            @Parameter(description = "Filter by Category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter start date (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter end date (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactions(accountId, categoryId, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @PostMapping
    @Operation(summary = "Record a new transaction and atomically update the account balance")
    public ResponseEntity<TransactionResponseDTO> createTransaction(@Valid @RequestBody TransactionRequestDTO request) {
        TransactionResponseDTO created = transactionService.createTransaction(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction and atomically revert the balance adjustment")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}

