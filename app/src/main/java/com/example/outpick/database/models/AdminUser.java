package com.example.outpick.database.models;

public class AdminUser {
    private String userId;
    private String username;
    private String status;       // Active / Suspended / Banned
    private String signupDate;
    private String lastLogin;
    private String role;         // User / Admin

    public AdminUser(String userId, String username, String status, String signupDate, String lastLogin, String role) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.signupDate = signupDate;
        this.lastLogin = lastLogin;
        this.role = role;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }
    public String getSignupDate() { return signupDate; }
    public String getLastLogin() { return lastLogin; }
    public String getRole() { return role; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setStatus(String status) { this.status = status; }
    public void setSignupDate(String signupDate) { this.signupDate = signupDate; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public void setRole(String role) { this.role = role; }
}
