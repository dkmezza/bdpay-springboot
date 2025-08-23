package com.bdpay.dashboard.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bdpay.dashboard.entity.Transaction;
import com.bdpay.dashboard.entity.Transaction.TransactionStatus;
import com.bdpay.dashboard.entity.Transaction.TransactionType;
import com.bdpay.dashboard.service.TransactionService;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // Get recent transactions for user (dashboard table)
    @GetMapping("/recent/user/{userId}")
    public ResponseEntity<?> getRecentTransactions(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.getRecentTransactions(userId);
            
            List<Map<String, Object>> transactionData = transactions.stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("transactions", transactionData));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get paginated transactions for user
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Transaction> transactionPage = transactionService.getUserTransactions(userId, page, size);
            
            List<Map<String, Object>> transactionData = transactionPage.getContent().stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactionData);
            response.put("totalElements", transactionPage.getTotalElements());
            response.put("totalPages", transactionPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get monthly transaction data for money flow chart
    @GetMapping("/chart/user/{userId}")
    public ResponseEntity<?> getChartData(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "2024") int year) {
        try {
            List<Object[]> monthlyData = transactionService.getMonthlyTransactionData(userId, year);
            
            // Process data for frontend chart
            Map<String, Object> chartData = processMonthlyDataForChart(monthlyData);

            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get spending statistics for statistics panel
    @GetMapping("/statistics/user/{userId}")
    public ResponseEntity<?> getStatistics(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "current") String period) {
        try {
            List<Object[]> spendingData;
            
            // Calculate date range based on period
            LocalDateTime startDate;
            LocalDateTime endDate = LocalDateTime.now();
            
            switch (period.toLowerCase()) {
                case "current":
                    // Current month
                    startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    break;
                case "last":
                    // Last month
                    LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
                    startDate = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    endDate = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth())
                                    .withHour(23).withMinute(59).withSecond(59);
                    break;
                case "quarter":
                    // Current quarter
                    int currentQuarter = ((LocalDateTime.now().getMonthValue() - 1) / 3) + 1;
                    int quarterStartMonth = (currentQuarter - 1) * 3 + 1;
                    startDate = LocalDateTime.now().withMonth(quarterStartMonth).withDayOfMonth(1)
                                                .withHour(0).withMinute(0).withSecond(0);
                    break;
                case "year":
                    // Current year
                    startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
                    break;
                default:
                    startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            }
            
            spendingData = transactionService.getSpendingByCategory(userId, startDate, endDate);
            
            // Process data for frontend statistics
            List<Map<String, Object>> statisticsData = spendingData.stream()
                .map(data -> Map.of(
                    "category", data[0] != null ? data[0].toString() : "Others",
                    "amount", data[1] != null ? data[1] : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());

            BigDecimal total = statisticsData.stream()
                .map(item -> (BigDecimal) item.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(Map.of(
                "categories", statisticsData,
                "total", total,
                "period", period,
                "startDate", startDate,
                "endDate", endDate
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Create new transaction
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody CreateTransactionRequest request) {
        try {
            Transaction transaction = transactionService.createTransaction(
                request.getAccountId(),
                request.getBusinessName(),
                request.getCategory(),
                request.getAmount(),
                request.getTransactionType(),
                request.getDescription()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapTransactionToResponse(transaction));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Process transaction (approve/reject)
    @PutMapping("/{transactionId}/status")
    public ResponseEntity<?> processTransaction(@PathVariable Long transactionId,
                                              @RequestBody Map<String, String> request) {
        try {
            TransactionStatus newStatus = TransactionStatus.valueOf(request.get("status"));
            Transaction updatedTransaction = transactionService.processTransaction(transactionId, newStatus);

            return ResponseEntity.ok(mapTransactionToResponse(updatedTransaction));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete transaction
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long transactionId) {
        try {
            transactionService.deleteTransaction(transactionId);
            return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Search transactions
    @GetMapping("/search/user/{userId}")
    public ResponseEntity<?> searchTransactions(@PathVariable Long userId,
                                              @RequestParam String query) {
        try {
            List<Transaction> transactions = transactionService.searchTransactions(userId, query);
            
            List<Map<String, Object>> transactionData = transactions.stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("transactions", transactionData));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method to map Transaction to response
    private Map<String, Object> mapTransactionToResponse(Transaction transaction) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", transaction.getId());
        response.put("businessName", transaction.getBusinessName());
        response.put("category", transaction.getCategory());
        response.put("amount", transaction.getAmount());
        response.put("transactionType", transaction.getTransactionType().toString());
        response.put("status", transaction.getStatus().toString());
        response.put("description", transaction.getDescription());
        response.put("transactionDate", transaction.getTransactionDate());
        response.put("createdAt", transaction.getCreatedAt());
        response.put("accountId", transaction.getAccount().getId());
        
        return response;
    }

    // Helper method to process monthly data for chart
    private Map<String, Object> processMonthlyDataForChart(List<Object[]> monthlyData) {
        Map<String, Object> chartData = new HashMap<>();
        // Initialize arrays for 12 months
        BigDecimal[] income = new BigDecimal[12];
        BigDecimal[] expense = new BigDecimal[12];
        
        // Initialize with zeros
        for (int i = 0; i < 12; i++) {
            income[i] = BigDecimal.ZERO;
            expense[i] = BigDecimal.ZERO;
        }
        
        // Fill with actual data
        for (Object[] data : monthlyData) {
            Integer month = (Integer) data[0];
            String type = data[1].toString();
            BigDecimal amount = (BigDecimal) data[2];
            
            if (month >= 1 && month <= 12) {
                if ("INCOME".equals(type)) {
                    income[month - 1] = amount;
                } else if ("EXPENSE".equals(type)) {
                    expense[month - 1] = amount;
                }
            }
        }
        
        chartData.put("income", income);
        chartData.put("expense", expense);
        chartData.put("months", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"});
        
        return chartData;
    }

    // DTO class
    public static class CreateTransactionRequest {
        private Long accountId;
        private String businessName;
        private String category;
        private BigDecimal amount;
        private TransactionType transactionType;
        private String description;

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        
        public String getBusinessName() { return businessName; }
        public void setBusinessName(String businessName) { this.businessName = businessName; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public TransactionType getTransactionType() { return transactionType; }
        public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}