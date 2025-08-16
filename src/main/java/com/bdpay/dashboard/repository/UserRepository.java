package com.bdpay.dashboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bdpay.dashboard.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find user by email (for login)
    Optional<User> findByEmail(String email);
    
    // Check if email exists (for registration validation)
    boolean existsByEmail(String email);
    
    // Find users by name (case-insensitive search)
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find user with their accounts (to avoid N+1 problem)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.id = :userId")
    Optional<User> findByIdWithAccounts(@Param("userId") Long userId);
    
    // Find user by email with accounts
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.email = :email")
    Optional<User> findByEmailWithAccounts(@Param("email") String email);
}


