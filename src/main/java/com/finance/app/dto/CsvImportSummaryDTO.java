package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportSummaryDTO {
    private int totalParsed;
    private int totalImported;
    private int totalSkipped;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<TransactionResponseDTO> importedTransactions = new ArrayList<>();
}
