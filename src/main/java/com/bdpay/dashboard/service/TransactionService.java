package com.bdpay.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bdpay.dashboard.entity.Account;
import com.bdpay.dashboard.entity.Transaction;
import com.bdpay.dashboard.entity.Transaction.TransactionStatus;
import com.bdpay.dashboard.entity.Transaction.TransactionType;
import com.bdpay.dashboard.repository.AccountRepository;
import com.bdpay.dashboard.repository.TransactionRepository;

@Service
@Transactional
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Get recent transactions for user (for dashboard table)
    public List<Transaction> getRecentTransactions(Long userId) {
        return transactionRepository.findTop10ByUserIdOrderByTransactionDateDesc(userId);
    }
    
    // Get paginated transactions for user
    public Page<Transaction> getUserTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId, pageable);
    }
    
    // Get transactions by account
    public Page<Transaction> getAccountTransactions(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId, pageable);
    }
    
    // Get transaction by ID
    public Optional<Transaction> getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    // Create new transaction
    public Transaction createTransaction(Long accountId, String businessName, String category,
                                       BigDecimal amount, TransactionType type, String description) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        
        Transaction transaction = new Transaction();
        transaction.setBusinessName(businessName);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(description);
        transaction.setAccount(account);
        transaction.setTransactionDate(LocalDateTime.now());
        
        return transactionRepository.save(transaction);
    }
    
    // Process pending transaction (approve/reject)
    public Transaction processTransaction(Long transactionId, TransactionStatus newStatus) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be processed");
        }
        
        transaction.setStatus(newStatus);
        
        // Update account balance if transaction is successful
        if (newStatus == TransactionStatus.SUCCESS) {
            Account account = transaction.getAccount();
            account.setPreviousBalance(account.getCurrentBalance());
            
            if (transaction.getTransactionType() == TransactionType.INCOME) {
                account.setCurrentBalance(account.getCurrentBalance().add(transaction.getAmount()));
            } else {
                account.setCurrentBalance(account.getCurrentBalance().subtract(transaction.getAmount()));
            }
            
            accountRepository.save(account);
        }
        
        return transactionRepository.save(transaction);
    }
    
    // Get monthly data for money flow chart
    public List<Object[]> getMonthlyTransactionData(Long userId, int year) {
        return transactionRepository.getMonthlyTransactionData(userId, year);
    }
    
    // Get monthly data for current year
    public List<Object[]> getCurrentYearTransactionData(Long userId) {
        int currentYear = Year.now().getValue();
        return getMonthlyTransactionData(userId, currentYear);
    }
    
    // Get spending by category for statistics panel
    public List<Object[]> getSpendingByCategory(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.getSpendingByCategory(userId, startDate, endDate);
    }
    
    // Get current month spending by category
    public List<Object[]> getCurrentMonthSpendingByCategory(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        return getSpendingByCategory(userId, startOfMonth, endOfMonth);
    }
    
    // Calculate total income for period
    public BigDecimal getTotalIncome(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = transactionRepository.getTotalIncomeByUserIdAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Calculate total expenses for period
    public BigDecimal getTotalExpenses(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = transactionRepository.getTotalExpensesByUserIdAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Get pending transactions
    public List<Transaction> getPendingTransactions(Long userId) {
        return transactionRepository.findPendingTransactionsByUserId(userId);
    }
    
    // Search transactions
    public List<Transaction> searchTransactions(Long userId, String searchTerm) {
        return transactionRepository.searchTransactionsByBusinessName(userId, searchTerm);
    }
    
    // Delete transaction
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
        
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            throw new RuntimeException("Cannot delete successful transactions");
        }
        
        transactionRepository.delete(transaction);
    }
    
    // Initialize sample transactions for new user
    public void initializeSampleTransactions(Long userId) {
        // Get user's accounts
        List<Account> accounts = accountRepository.findByUserIdOrderByAccountTypeAsc(userId);
        if (accounts.isEmpty()) {
            throw new RuntimeException("No accounts found for user");
        }
        
        Account businessAccount = accounts.stream()
            .filter(a -> a.getAccountType() == Account.AccountType.BUSINESS)
            .findFirst().orElse(accounts.get(0));
        
        // Create sample transactions
        createTransaction(businessAccount.getId(), "Gym", "Payment", 
                         new BigDecimal("300.00"), TransactionType.EXPENSE, "Monthly gym membership");
        
        createTransaction(businessAccount.getId(), "Al-Bank", "Deposit", 
                         new BigDecimal("890.00"), TransactionType.INCOME, "Bank deposit");
        
        createTransaction(businessAccount.getId(), "Facebook Ads", "Payment", 
                         new BigDecimal("123.00"), TransactionType.EXPENSE, "Marketing campaign");
        
        // Process some transactions
        List<Transaction> recentTransactions = getRecentTransactions(userId);
        if (recentTransactions.size() >= 2) {
            processTransaction(recentTransactions.get(1).getId(), TransactionStatus.SUCCESS);
        }
        if (recentTransactions.size() >= 3) {
            processTransaction(recentTransactions.get(2).getId(), TransactionStatus.FAILED);
        }
    }
}