package com.example.student_service.dto;

public class AccountDTO {
    private Long id;
    private String studentId;
    private boolean hasOutstandingBalance;  // Add this field

    // Constructors
    public AccountDTO() {}
    
    public AccountDTO(Long id, String studentId, boolean hasOutstandingBalance) {
        this.id = id;
        this.studentId = studentId;
        this.hasOutstandingBalance = hasOutstandingBalance;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public boolean isHasOutstandingBalance() { return hasOutstandingBalance; }
    public void setHasOutstandingBalance(boolean hasOutstandingBalance) {
        this.hasOutstandingBalance = hasOutstandingBalance;
    }
}