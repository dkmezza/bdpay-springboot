package com.bdpay.dashboard.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bdpay.dashboard.entity.Account;
import com.bdpay.dashboard.entity.Account.AccountType;
import com.bdpay.dashboard.entity.User;
import com.bdpay.dashboard.repository.AccountRepository;
import com.bdpay.dashboard.repository.UserRepository;

@Service
@Transactional
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get all accounts for a user (for dashboard cards)
    public List<Account> getUserAccounts(Long userId) {
        return accountRepository.findByUserIdOrderByAccountTypeAsc(userId);
    }
    
    // Get accounts with transactions
    public List<Account> getUserAccountsWithTransactions(Long userId) {
        return accountRepository.findByUserIdWithTransactions(userId);
    }
    
    // Get specific account by ID
    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }
    
    // Get account by user and type
    public Optional<Account> getAccountByUserAndType(Long userId, AccountType accountType) {
        return accountRepository.findByUserIdAndAccountType(userId, accountType);
    }
    
    // Get wallet account specifically
    public Optional<Account> getWalletAccount(Long userId) {
        return accountRepository.findWalletByUserId(userId);
    }
    
    // Calculate total balance for user
    public BigDecimal getTotalBalance(Long userId) {
        BigDecimal total = accountRepository.getTotalBalanceByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Create new account
    public Account createAccount(Long userId, String accountName, AccountType accountType, 
                               BigDecimal initialBalance) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if account type already exists for user
        Optional<Account> existingAccount = accountRepository.findByUserIdAndAccountType(userId, accountType);
        if (existingAccount.isPresent()) {
            throw new RuntimeException("Account type " + accountType + " already exists for user");
        }
        
        Account account = new Account();
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setCurrentBalance(initialBalance);
        account.setPreviousBalance(BigDecimal.ZERO);
        account.setUser(user);
        
        // Set default limits for wallet accounts
        if (accountType == AccountType.WALLET) {
            account.setTotalLimit(new BigDecimal("13000.00"));
            account.setSpendingLimit(new BigDecimal("9800.00"));
            account.setCardType("VISA");
            account.setCardNumber("**** **** **** 1234"); // Masked number
        }
        
        return accountRepository.save(account);
    }
    
    // Update account
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    // Delete account
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        
        // Check if account has transactions
        if (account.getTransactions() != null && !account.getTransactions().isEmpty()) {
            throw new RuntimeException("Cannot delete account with existing transactions");
        }
        
        accountRepository.delete(account);
    }

    // Check if account can be deleted
    public boolean canDeleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        return account.getTransactions() == null || account.getTransactions().isEmpty();
    }

    // Update account balance
    public Account updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        
        // Store previous balance for trend calculation
        account.setPreviousBalance(account.getCurrentBalance());
        account.setCurrentBalance(newBalance);
        
        return accountRepository.save(account);
    }
    
    // Update spending limit (for wallet accounts)
    public Account updateSpendingLimit(Long accountId, BigDecimal newLimit) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        
        if (account.getAccountType() != AccountType.WALLET) {
            throw new RuntimeException("Spending limit can only be set for wallet accounts");
        }
        
        account.setSpendingLimit(newLimit);
        return accountRepository.save(account);
    }
    
    // Initialize default accounts for new user
    public void initializeDefaultAccounts(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Create default accounts
        createAccount(userId, "Business account", AccountType.BUSINESS, new BigDecimal("24098.00"));
        createAccount(userId, "Tax Reserve", AccountType.TAX_RESERVE, new BigDecimal("2456.89"));
        createAccount(userId, "Savings", AccountType.SAVINGS, new BigDecimal("1980.00"));
        createAccount(userId, "Wallet", AccountType.WALLET, new BigDecimal("1550.62"));
    }
    
    // Get account summary for dashboard
    public List<Object[]> getAccountSummary(Long userId) {
        return accountRepository.getAccountSummaryByUserId(userId);
    }
    
    // Transfer money between accounts
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found"));
        
        // Check if accounts belong to same user
        if (!fromAccount.getUser().getId().equals(toAccount.getUser().getId())) {
            throw new RuntimeException("Can only transfer between accounts of the same user");
        }
        
        // Check sufficient balance
        if (fromAccount.getCurrentBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for transfer");
        }
        
        // Perform transfer
        fromAccount.setPreviousBalance(fromAccount.getCurrentBalance());
        fromAccount.setCurrentBalance(fromAccount.getCurrentBalance().subtract(amount));
        
        toAccount.setPreviousBalance(toAccount.getCurrentBalance());
        toAccount.setCurrentBalance(toAccount.getCurrentBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
