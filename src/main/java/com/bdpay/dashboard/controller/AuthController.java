package com.bdpay.dashboard.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bdpay.dashboard.entity.User;
import com.bdpay.dashboard.service.AccountService;
import com.bdpay.dashboard.service.TransactionService;
import com.bdpay.dashboard.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if email already exists
            if (userService.emailExists(request.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already exists"));
            }

            // Create user
            User user = userService.registerUser(
                request.getFirstName(),
                request.getLastName(), 
                request.getEmail(),
                request.getPassword()
            );

            // Initialize default accounts and sample data
            accountService.initializeDefaultAccounts(user.getId());
            transactionService.initializeSampleTransactions(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("user", Map.of(
                "id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Login user
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Validate credentials
            if (userService.validateCredentials(request.getEmail(), request.getPassword())) {
                User user = userService.getUserByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("user", Map.of(
                    "id", user.getId(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName()
                ));

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get current user info
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam Long userId) {
        try {
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = Map.of(
                "id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "createdAt", user.getCreatedAt()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DTO classes for request bodies
    public static class RegisterRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}

