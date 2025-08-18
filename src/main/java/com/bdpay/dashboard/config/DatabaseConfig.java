package com.bdpay.dashboard.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.bdpay.dashboard.entity.User;
import com.bdpay.dashboard.service.AccountService;
import com.bdpay.dashboard.service.TransactionService;
import com.bdpay.dashboard.service.UserService;

@Configuration
public class DatabaseConfig {

    // Initialize sample data for development/demo
    @Bean
    @Profile("!test") // Don't run in test environment
    public CommandLineRunner initData(UserService userService, 
                                    AccountService accountService,
                                    TransactionService transactionService) {
        return args -> {
            // Check if Leo DiCaprio user already exists
            if (!userService.emailExists("leo@bdpay.com")) {
                // Create the demo user (Leo DiCaprio)
                User demoUser = userService.registerUser(
                    "Leo", 
                    "DiCaprio", 
                    "leo@bdpay.com", 
                    "password123"
                );

                // Initialize accounts and transactions
                accountService.initializeDefaultAccounts(demoUser.getId());
                transactionService.initializeSampleTransactions(demoUser.getId());

                System.out.println("Demo user created: leo@bdpay.com / password123");
                System.out.println("User ID: " + demoUser.getId());
            }
        };
    }
}

