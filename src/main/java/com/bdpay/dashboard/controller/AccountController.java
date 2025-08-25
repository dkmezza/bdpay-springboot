package com.bdpay.dashboard.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import com.bdpay.dashboard.entity.Account;
import com.bdpay.dashboard.entity.Account.AccountType;
import com.bdpay.dashboard.service.AccountService;

@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "http://localhost:3000")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // Get all accounts for user (for dashboard cards)
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAccounts(@PathVariable Long userId) {
        try {
            List<Account> accounts = accountService.getUserAccounts(userId);
            
            List<Map<String, Object>> accountData = accounts.stream()
                .map(this::mapAccountToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "accounts", accountData,
                "totalBalance", accountService.getTotalBalance(userId)
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get specific account by ID
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable Long accountId) {
        try {
            Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

            return ResponseEntity.ok(mapAccountToResponse(account));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }


    // Update account
    @PutMapping("/{accountId}")
    public ResponseEntity<?> updateAccount(@PathVariable Long accountId,
                                        @RequestBody UpdateAccountRequest request) {
        try {
            Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

            // Update account fields
            if (request.getAccountName() != null) {
                account.setAccountName(request.getAccountName());
            }
            if (request.getCurrentBalance() != null) {
                account.setPreviousBalance(account.getCurrentBalance());
                account.setCurrentBalance(request.getCurrentBalance());
            }
            
            // Handle wallet-specific fields
            if (account.getAccountType() == AccountType.WALLET) {
                if (request.getSpendingLimit() != null) {
                    account.setSpendingLimit(request.getSpendingLimit());
                }
                if (request.getTotalLimit() != null) {
                    account.setTotalLimit(request.getTotalLimit());
                }
                if (request.getCardType() != null) {
                    account.setCardType(request.getCardType());
                }
            }

            Account updatedAccount = accountService.updateAccount(account);
            return ResponseEntity.ok(mapAccountToResponse(updatedAccount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete account
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        try {
            accountService.deleteAccount(accountId);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get wallet account for user
    @GetMapping("/wallet/user/{userId}")
    public ResponseEntity<?> getWalletAccount(@PathVariable Long userId) {
        try {
            Account wallet = accountService.getWalletAccount(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

            return ResponseEntity.ok(mapAccountToResponse(wallet));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Update account balance
    @PutMapping("/{accountId}/balance")
    public ResponseEntity<?> updateBalance(@PathVariable Long accountId, 
                                         @RequestBody Map<String, BigDecimal> request) {
        try {
            BigDecimal newBalance = request.get("balance");
            Account updatedAccount = accountService.updateBalance(accountId, newBalance);

            return ResponseEntity.ok(mapAccountToResponse(updatedAccount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Update spending limit (wallet only)
    @PutMapping("/{accountId}/spending-limit")
    public ResponseEntity<?> updateSpendingLimit(@PathVariable Long accountId,
                                               @RequestBody Map<String, BigDecimal> request) {
        try {
            BigDecimal newLimit = request.get("spendingLimit");
            Account updatedAccount = accountService.updateSpendingLimit(accountId, newLimit);

            return ResponseEntity.ok(mapAccountToResponse(updatedAccount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Create new account
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createAccount(@PathVariable Long userId,
                                         @RequestBody CreateAccountRequest request) {
        try {
            Account account = accountService.createAccount(
                userId,
                request.getAccountName(),
                request.getAccountType(),
                request.getInitialBalance()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapAccountToResponse(account));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Transfer money between accounts
    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest request) {
        try {
            accountService.transferMoney(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
            );

            return ResponseEntity.ok(Map.of("message", "Transfer completed successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method to map Account to response
    private Map<String, Object> mapAccountToResponse(Account account) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", account.getId());
        response.put("accountName", account.getAccountName());
        response.put("accountType", account.getAccountType().toString());
        response.put("currentBalance", account.getCurrentBalance());
        response.put("previousBalance", account.getPreviousBalance());
        response.put("percentageChange", account.getPercentageChange());
        
        if (account.getAccountType() == AccountType.WALLET) {
            response.put("spendingLimit", account.getSpendingLimit());
            response.put("totalLimit", account.getTotalLimit());
            response.put("cardNumber", account.getCardNumber());
            response.put("cardType", account.getCardType());
        }
        
        response.put("createdAt", account.getCreatedAt());
        response.put("updatedAt", account.getUpdatedAt());
        
        return response;
    }

    // DTO classes
    public static class CreateAccountRequest {
        private String accountName;
        private AccountType accountType;
        private BigDecimal initialBalance;

        // Getters and setters
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        
        public AccountType getAccountType() { return accountType; }
        public void setAccountType(AccountType accountType) { this.accountType = accountType; }
        
        public BigDecimal getInitialBalance() { return initialBalance; }
        public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
    }


    public static class UpdateAccountRequest {
        private String accountName;
        private BigDecimal currentBalance;
        private BigDecimal spendingLimit;
        private BigDecimal totalLimit;
        private String cardType;

        // Getters and setters
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        
        public BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
        
        public BigDecimal getSpendingLimit() { return spendingLimit; }
        public void setSpendingLimit(BigDecimal spendingLimit) { this.spendingLimit = spendingLimit; }
        
        public BigDecimal getTotalLimit() { return totalLimit; }
        public void setTotalLimit(BigDecimal totalLimit) { this.totalLimit = totalLimit; }
        
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
    }

    public static class TransferRequest {
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;

        // Getters and setters
        public Long getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
        
        public Long getToAccountId() { return toAccountId; }
        public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}

