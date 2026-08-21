package com.finance.app.service;

import com.finance.app.dto.CsvImportSummaryDTO;
import com.finance.app.dto.TransactionRequestDTO;
import com.finance.app.dto.TransactionResponseDTO;
import com.finance.app.entity.Account;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // 2026-08-20
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),       // 08/20/2026
            DateTimeFormatter.ofPattern("M/d/yyyy"),         // 8/20/2026
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),       // 2026/08/20
            DateTimeFormatter.ofPattern("dd-MM-yyyy")        // 20-08-2026
    );

    @Transactional
    public CsvImportSummaryDTO importCsv(MultipartFile file, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            throw new IllegalStateException("No categories available in the system for categorization.");
        }

        CsvImportSummaryDTO summary = CsvImportSummaryDTO.builder()
                .importedTransactions(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("The uploaded CSV file is empty.");
            }

            Map<String, Integer> headerMap = parseHeader(headerLine);
            int dateIdx = headerMap.getOrDefault("date", 0);
            int descIdx = headerMap.getOrDefault("description", 1);
            int amountIdx = headerMap.getOrDefault("amount", 2);
            int catIdx = headerMap.getOrDefault("category", -1);

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                summary.setTotalParsed(summary.getTotalParsed() + 1);

                try {
                    String[] columns = parseCsvLine(line);
                    if (columns.length < 3) {
                        summary.setTotalSkipped(summary.getTotalSkipped() + 1);
                        summary.getWarnings().add("Line " + lineNumber + ": Insufficient columns, skipped.");
                        continue;
                    }

                    LocalDate date = parseDate(columns[dateIdx].trim());
                    String description = columns[descIdx].trim();
                    BigDecimal rawAmount = parseAmount(columns[amountIdx].trim());

                    BigDecimal amount = rawAmount.abs();
                    if (amount.compareTo(BigDecimal.ZERO) == 0) {
                        summary.setTotalSkipped(summary.getTotalSkipped() + 1);
                        summary.getWarnings().add("Line " + lineNumber + ": Zero amount transaction skipped.");
                        continue;
                    }

                    // Determine Category
                    String explicitCategoryName = (catIdx >= 0 && catIdx < columns.length) ? columns[catIdx].trim() : "";
                    Category matchedCategory = matchCategory(description, explicitCategoryName, rawAmount, allCategories);

                    TransactionRequestDTO request = TransactionRequestDTO.builder()
                            .accountId(account.getId())
                            .categoryId(matchedCategory.getId())
                            .amount(amount)
                            .transactionDate(date)
                            .description(description)
                            .build();

                    TransactionResponseDTO created = transactionService.createTransaction(request);
                    summary.getImportedTransactions().add(created);
                    summary.setTotalImported(summary.getTotalImported() + 1);

                } catch (Exception e) {
                    log.warn("Failed to parse CSV line {}: {}", lineNumber, e.getMessage());
                    summary.setTotalSkipped(summary.getTotalSkipped() + 1);
                    summary.getWarnings().add("Line " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read CSV statement: " + e.getMessage(), e);
        }

        return summary;
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> map = new HashMap<>();
        String[] headers = parseCsvLine(headerLine.toLowerCase());

        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().replaceAll("[^a-z]", "");
            if (h.contains("date") || h.contains("postingdate") || h.contains("transdate")) {
                map.putIfAbsent("date", i);
            } else if (h.contains("desc") || h.contains("payee") || h.contains("memo") || h.contains("name")) {
                map.putIfAbsent("description", i);
            } else if (h.contains("amount") || h.contains("sum") || h.contains("value") || h.contains("total")) {
                map.putIfAbsent("amount", i);
            } else if (h.contains("cat") || h.contains("type") || h.contains("tag")) {
                map.putIfAbsent("category", i);
            }
        }
        return map;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unsupported date format: '" + dateStr + "'");
    }

    private BigDecimal parseAmount(String amountStr) {
        String clean = amountStr.replaceAll("[$,\\s]", "");
        return new BigDecimal(clean);
    }

    private Category matchCategory(String description, String explicitCat, BigDecimal rawAmount, List<Category> allCategories) {
        // 1. Explicit Category Match if given in CSV
        if (!explicitCat.isBlank()) {
            for (Category c : allCategories) {
                if (c.getName().equalsIgnoreCase(explicitCat)) {
                    return c;
                }
            }
        }

        // 2. Keyword heuristic matching from description
        String descLower = description.toLowerCase();

        if (descLower.contains("payroll") || descLower.contains("salary") || descLower.contains("direct dep") || rawAmount.compareTo(BigDecimal.ZERO) > 0 && descLower.contains("deposit")) {
            return findCategory(allCategories, "Salary & Wages", CategoryType.INCOME);
        }
        if (descLower.contains("dividend") || descLower.contains("interest") || descLower.contains("yield") || descLower.contains("capital gain")) {
            return findCategory(allCategories, "Investment Returns", CategoryType.INCOME);
        }
        if (descLower.contains("grocery") || descLower.contains("whole foods") || descLower.contains("trader joe") || descLower.contains("safeway") || descLower.contains("restaurant") || descLower.contains("cafe") || descLower.contains("coffee") || descLower.contains("doordash") || descLower.contains("ubereats") || descLower.contains("mcdonald")) {
            return findCategory(allCategories, "Food & Dining", CategoryType.EXPENSE);
        }
        if (descLower.contains("rent") || descLower.contains("mortgage") || descLower.contains("lease") || descLower.contains("apartment")) {
            return findCategory(allCategories, "Housing & Rent", CategoryType.EXPENSE);
        }
        if (descLower.contains("uber") || descLower.contains("lyft") || descLower.contains("gas") || descLower.contains("chevron") || descLower.contains("shell") || descLower.contains("transit") || descLower.contains("metro") || descLower.contains("parking")) {
            return findCategory(allCategories, "Transportation", CategoryType.EXPENSE);
        }
        if (descLower.contains("electric") || descLower.contains("water") || descLower.contains("utility") || descLower.contains("internet") || descLower.contains("wifi") || descLower.contains("comcast") || descLower.contains("at&t") || descLower.contains("verizon")) {
            return findCategory(allCategories, "Utilities", CategoryType.EXPENSE);
        }
        if (descLower.contains("netflix") || descLower.contains("spotify") || descLower.contains("cinema") || descLower.contains("movie") || descLower.contains("steam") || descLower.contains("hulu") || descLower.contains("disney") || descLower.contains("concert") || descLower.contains("ticket")) {
            return findCategory(allCategories, "Entertainment", CategoryType.EXPENSE);
        }
        if (descLower.contains("pharmacy") || descLower.contains("cvs") || descLower.contains("walgreens") || descLower.contains("doctor") || descLower.contains("hospital") || descLower.contains("gym") || descLower.contains("fitness") || descLower.contains("dental")) {
            return findCategory(allCategories, "Healthcare & Fitness", CategoryType.EXPENSE);
        }

        // 3. Fallback: Miscellaneous or first category of matching sign
        CategoryType targetType = (rawAmount.compareTo(BigDecimal.ZERO) > 0 && descLower.contains("income")) ? CategoryType.INCOME : CategoryType.EXPENSE;
        for (Category c : allCategories) {
            if (c.getName().equalsIgnoreCase("Miscellaneous") || c.getName().toLowerCase().contains("misc")) {
                return c;
            }
        }
        return allCategories.stream()
                .filter(c -> c.getType() == targetType)
                .findFirst()
                .orElse(allCategories.get(0));
    }

    private Category findCategory(List<Category> categories, String name, CategoryType fallbackType) {
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categories.stream()
                        .filter(c -> c.getType() == fallbackType)
                        .findFirst()
                        .orElse(categories.get(0)));
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim().replaceAll("^\"|\"$", ""));
        return tokens.toArray(new String[0]);
    }
}
