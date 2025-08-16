package com.bdpay.dashboard.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bdpay.dashboard.entity.Account;
import com.bdpay.dashboard.entity.Account.AccountType;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    // Find all accounts for a specific user
    List<Account> findByUserIdOrderByAccountTypeAsc(Long userId);
    
    // Find account by user and account type
    Optional<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);
    
    // Find all accounts with their transactions
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.transactions WHERE a.user.id = :userId")
    List<Account> findByUserIdWithTransactions(@Param("userId") Long userId);
    
    // Calculate total balance for a user
    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.user.id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
    
    // Find accounts by type across all users (for admin purposes)
    List<Account> findByAccountType(AccountType accountType);
    
    // Find wallet accounts specifically
    @Query("SELECT a FROM Account a WHERE a.accountType = 'WALLET' AND a.user.id = :userId")
    Optional<Account> findWalletByUserId(@Param("userId") Long userId);
    
    // Find accounts with balance above certain amount
    @Query("SELECT a FROM Account a WHERE a.currentBalance > :minBalance AND a.user.id = :userId")
    List<Account> findAccountsWithBalanceAbove(@Param("userId") Long userId, @Param("minBalance") BigDecimal minBalance);
    
    // Get account summary for dashboard cards
    @Query("SELECT a.accountType, a.currentBalance, a.previousBalance " +
           "FROM Account a WHERE a.user.id = :userId ORDER BY a.accountType")
    List<Object[]> getAccountSummaryByUserId(@Param("userId") Long userId);
}
