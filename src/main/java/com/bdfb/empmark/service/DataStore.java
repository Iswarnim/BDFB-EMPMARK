package com.bdfb.empmark.service;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DataStore {
    
    // ==========================================
    // 1. CORE USER & BIOMETRIC LOG DATA
    // ==========================================
    public static final Map<String, Map<String, String>> USERS_DB = new ConcurrentHashMap<>();
    public static final Map<String, List<Map<String, String>>> ATTENDANCE_DB = new ConcurrentHashMap<>();
    public static final Map<String, List<Map<String, String>>> MEAL_DB = new ConcurrentHashMap<>();
    
    // ==========================================
    // 2. LEAVE MANAGEMENT WORKFLOWS
    // ==========================================
    public static final Map<String, Map<String, Integer>> LEAVE_BALANCES = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, String>> LEAVE_REQUESTS = new ConcurrentHashMap<>();
    
    // ==========================================
    // 3. SALARY & PAYROLL DATA
    // ==========================================
    public static final Map<String, List<Map<String, String>>> SALARY_DB = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, String>> SALARY_MASTER = new ConcurrentHashMap<>(); 

    // ==========================================
    // 4. MEDIA, CERTIFICATIONS & MEMORIES
    // ==========================================
    public static final Map<String, List<Map<String, String>>> CERTS_DB = new ConcurrentHashMap<>();
    public static final List<Map<String, String>> MEMORIES_DB = new CopyOnWriteArrayList<>();

    // ==========================================
    // INITIALIZATION BLOCK (DEFAULT DATA)
    // ==========================================
    public DataStore() {
        
        // ------------------------------------------
        // Create Default Users with Roles & Departments
        // ------------------------------------------
        
        // System Admin
        createUser("BDB074", "19990101", "Swarnim Mishra", "IT Dept", "ADMIN");
        
        // Human Resources
        createUser("BDB066", "19950520", "Swati Shukla", "HR", "HR");
        
        // Head of Department (STORE)
        createUser("BDB100", "19800101", "Rajesh Gupta", "STORE", "HOD"); 
        
        // Standard Employees
        createUser("BDB133", "19981212", "Shubham Pathak", "IT Dept", "EMPLOYEE");
        createUser("BDB173", "20000101", "Akhilesh Pandey", "STORE", "EMPLOYEE"); 
        createUser("BDB177", "20010101", "Anshuman Singh", "STORE", "EMPLOYEE");  

        // ------------------------------------------
        // Initialize Default Leave Balances
        // ------------------------------------------
        initLeaves("BDB074"); 
        initLeaves("BDB066"); 
        initLeaves("BDB100"); 
        initLeaves("BDB133"); 
        initLeaves("BDB173"); 
        initLeaves("BDB177");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    
    public void createUser(String id, String pass, String name, String dept, String role) {
        Map<String, String> user = new HashMap<>();
        
        // Set user attributes
        user.put("password", pass); 
        user.put("name", name);
        user.put("dept", dept); 
        user.put("role", role); 
        
        // Save to Database
        USERS_DB.put(id, user);
    }

    private void initLeaves(String empId) {
        Map<String, Integer> balances = new HashMap<>();
        
        // Default Annual Allowances
        balances.put("CL", 8);  // Casual Leaves
        balances.put("SL", 5);  // Sick Leaves
        balances.put("EL", 12); // Earned Leaves
        
        // Save to Database
        LEAVE_BALANCES.put(empId, balances);
    }
}