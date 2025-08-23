package com.bdpay.dashboard.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdpay.dashboard.entity.User;
import com.bdpay.dashboard.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    // Get user profile
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId, HttpServletRequest request) {
        try {
            // Verify the requesting user matches the profile being accessed
            Long requestingUserId = (Long) request.getAttribute("userId");
            if (!userId.equals(requestingUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("createdAt", user.getCreatedAt());
            response.put("updatedAt", user.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Update user profile
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId, 
                                             @Valid @RequestBody UpdateProfileRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            // Verify the requesting user matches the profile being updated
            Long requestingUserId = (Long) httpRequest.getAttribute("userId");
            if (!userId.equals(requestingUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            User updatedUser = userService.updateUser(userId, 
                request.getFirstName(), 
                request.getLastName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "firstName", updatedUser.getFirstName(),
                "lastName", updatedUser.getLastName(),
                "email", updatedUser.getEmail(),
                "fullName", updatedUser.getFullName(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Change user password
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changeUserPassword(@PathVariable Long userId,
                                              @Valid @RequestBody ChangePasswordRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            // Verify the requesting user matches the profile being updated
            Long requestingUserId = (Long) httpRequest.getAttribute("userId");
            if (!userId.equals(requestingUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            userService.changePassword(userId, 
                request.getCurrentPassword(), 
                request.getNewPassword());

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get user statistics (account count, total balance, etc.)
    @GetMapping("/{userId}/statistics")
    public ResponseEntity<?> getUserStatistics(@PathVariable Long userId, HttpServletRequest request) {
        try {
            // Verify the requesting user matches the profile being accessed
            Long requestingUserId = (Long) request.getAttribute("userId");
            if (!userId.equals(requestingUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Get user with accounts loaded
            User user = userService.getUserWithAccounts(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Calculate statistics
            int totalAccounts = user.getAccounts() != null ? user.getAccounts().size() : 0;
            
            // You can add more statistics here
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalAccounts", totalAccounts);
            statistics.put("memberSince", user.getCreatedAt());
            statistics.put("lastLogin", user.getUpdatedAt()); // You might want to track this separately
            statistics.put("accountStatus", "Active");

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DTO classes
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}