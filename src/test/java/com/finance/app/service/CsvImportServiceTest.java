package com.finance.app.service;

import com.finance.app.dto.CsvImportSummaryDTO;
import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.AccountType;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CsvImportService csvImportService;

    private Account testAccount;
    private Category foodCategory;
    private Category salaryCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("Checking Account")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("2000.00"))
                .build();

        foodCategory = Category.builder().id(10L).name("Food & Dining").type(CategoryType.EXPENSE).build();
        salaryCategory = Category.builder().id(20L).name("Salary & Wages").type(CategoryType.INCOME).build();
        transportCategory = Category.builder().id(30L).name("Transportation").type(CategoryType.EXPENSE).build();
    }

    @Test
    @DisplayName("Should parse CSV statement and bulk import transactions with auto-categorization")
    void shouldImportValidCsvSuccessfully() {
        String csvContent = "Date,Description,Amount\n" +
                "2026-08-01,Whole Foods Market,-64.50\n" +
                "2026-08-02,Biweekly Payroll Direct Deposit,3200.00\n" +
                "2026-08-03,Uber Trip,-24.10\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findAll()).thenReturn(List.of(foodCategory, salaryCategory, transportCategory));
        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenAnswer(inv -> {
            TransactionRequestDTO req = inv.getArgument(0);
            return TransactionResponseDTO.builder()
                    .id(99L)
                    .amount(req.getAmount())
                    .description(req.getDescription())
                    .transactionDate(req.getTransactionDate())
                    .build();
        });

        CsvImportSummaryDTO summary = csvImportService.importCsv(file, 1L);

        assertNotNull(summary);
        assertEquals(3, summary.getTotalParsed());
        assertEquals(3, summary.getTotalImported());
        assertEquals(0, summary.getTotalSkipped());
        assertEquals(3, summary.getImportedTransactions().size());
        verify(transactionService, times(3)).createTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("Should skip corrupted lines and continue importing valid rows")
    void shouldHandleCorruptedRowsGracefully() {
        String csvContent = "Date,Description,Amount\n" +
                "2026-08-01,Trader Joe Grocery,-45.00\n" +
                "INVALID_DATE,Some Bad Row,-10.00\n" +
                "2026-08-03,Lyft Ride,-18.50\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findAll()).thenReturn(List.of(foodCategory, salaryCategory, transportCategory));
        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenAnswer(inv -> {
            TransactionRequestDTO req = inv.getArgument(0);
            return TransactionResponseDTO.builder().id(100L).amount(req.getAmount()).build();
        });

        CsvImportSummaryDTO summary = csvImportService.importCsv(file, 1L);

        assertNotNull(summary);
        assertEquals(3, summary.getTotalParsed());
        assertEquals(2, summary.getTotalImported());
        assertEquals(1, summary.getTotalSkipped());
        assertEquals(1, summary.getWarnings().size());
        verify(transactionService, times(2)).createTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("Should sanitize formula injection and parse escaped quotes and accounting parentheses correctly")
    void shouldHandleAccountingFormatAndFormulaSanitization() {
        String csvContent = "Date,Description,Amount\n" +
                "2026-08-01,\"=cmd|' /C calc'!A0\",($75.50)\n" +
                "2026-08-02,\"Trader Joe's, \"\"Organic\"\" Market\",-30.00\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findAll()).thenReturn(List.of(foodCategory, salaryCategory));
        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenAnswer(inv -> {
            TransactionRequestDTO req = inv.getArgument(0);
            return TransactionResponseDTO.builder()
                    .id(101L)
                    .amount(req.getAmount())
                    .description(req.getDescription())
                    .build();
        });

        CsvImportSummaryDTO summary = csvImportService.importCsv(file, 1L);

        assertNotNull(summary);
        assertEquals(2, summary.getTotalParsed());
        assertEquals(2, summary.getTotalImported());

        // Verify formula prefix '=' was neutralized with apostrophe '\''
        assertEquals("'=cmd|' /C calc'!A0", summary.getImportedTransactions().get(0).getDescription());
        // Verify ($75.50) parsed as 75.50 expense
        assertEquals(new BigDecimal("75.50"), summary.getImportedTransactions().get(0).getAmount());
        // Verify escaped quotes were unescaped
        assertEquals("Trader Joe's, \"Organic\" Market", summary.getImportedTransactions().get(1).getDescription());
    }
}
