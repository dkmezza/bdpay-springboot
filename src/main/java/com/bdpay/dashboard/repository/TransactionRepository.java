package com.bdpay.dashboard.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bdpay.dashboard.entity.Transaction;
import com.bdpay.dashboard.entity.Transaction.TransactionStatus;
import com.bdpay.dashboard.entity.Transaction.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transactions by account ID (for transaction table)
    Page<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId, Pageable pageable);
    
    // Find transactions by user ID across all accounts
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserIdOrderByTransactionDateDesc(@Param("userId") Long userId, Pageable pageable);
    
    // Find recent transactions (last 10)
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.transactionDate DESC")
    List<Transaction> findTop10ByUserIdOrderByTransactionDateDesc(@Param("userId") Long userId);
    
    // Find transactions by status
    List<Transaction> findByAccountIdAndStatus(Long accountId, TransactionStatus status);
    
    // Find transactions by type and date range
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId " +
           "AND t.transactionType = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndTypeAndDateRange(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Calculate total income for user in date range
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.user.id = :userId " +
           "AND t.transactionType = 'INCOME' " +
           "AND t.status = 'SUCCESS' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Calculate total expenses for user in date range
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.user.id = :userId " +
           "AND t.transactionType = 'EXPENSE' " +
           "AND t.status = 'SUCCESS' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Get monthly transaction data for money flow chart
    @Query("SELECT MONTH(t.transactionDate) as month, " +
           "t.transactionType as type, " +
           "SUM(t.amount) as total " +
           "FROM Transaction t " +
           "WHERE t.account.user.id = :userId " +
           "AND t.status = 'SUCCESS' " +
           "AND YEAR(t.transactionDate) = :year " +
           "GROUP BY MONTH(t.transactionDate), t.transactionType " +
           "ORDER BY month")
    List<Object[]> getMonthlyTransactionData(@Param("userId") Long userId, @Param("year") int year);
    
    // Get spending by category for statistics
    @Query("SELECT t.category, SUM(t.amount) as total " +
           "FROM Transaction t " +
           "WHERE t.account.user.id = :userId " +
           "AND t.transactionType = 'EXPENSE' " +
           "AND t.status = 'SUCCESS' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category " +
           "ORDER BY total DESC")
    List<Object[]> getSpendingByCategory(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find pending transactions that need attention
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId " +
           "AND t.status = 'PENDING' " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findPendingTransactionsByUserId(@Param("userId") Long userId);
    
    // Count transactions by status for user
    @Query("SELECT t.status, COUNT(t) FROM Transaction t " +
           "WHERE t.account.user.id = :userId " +
           "GROUP BY t.status")
    List<Object[]> getTransactionCountByStatus(@Param("userId") Long userId);
    
    // Search transactions by business name
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId " +
           "AND LOWER(t.businessName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> searchTransactionsByBusinessName(
        @Param("userId") Long userId,
        @Param("searchTerm") String searchTerm
    );
}