package com.bdfb.empmark.model;

// Represents the data sent back to the frontend after a successful login
public class UserSession {
    private String empId;
    private String name;
    private String role;
    private String department;

    // Constructors
    public UserSession(String empId, String name, String role, String department) {
        this.empId = empId;
        this.name = name;
        this.role = role;
        this.department = department;
    }

    // Getters and Setters
    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}