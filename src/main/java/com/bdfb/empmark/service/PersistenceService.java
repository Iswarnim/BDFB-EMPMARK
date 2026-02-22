package com.bdfb.empmark.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class PersistenceService {
    
    // ==========================================
    // CONFIGURATION CONSTANTS
    // ==========================================
    private static final String FILE_PATH = "bdfb_database.json";
    private static final String BACKUP_PATH = "bdfb_database.json.bak";
    
    private final ObjectMapper mapper = new ObjectMapper();

    // ==========================================
    // SYSTEM STARTUP: LOAD DATA INTO MEMORY
    // ==========================================
    @PostConstruct
    public void loadData() {
        try {
            File primaryFile = new File(FILE_PATH);
            File backupFile = new File(BACKUP_PATH);
            
            // ------------------------------------------
            // FAIL-SAFE: Restore from Backup if needed
            // ------------------------------------------
            if (!primaryFile.exists() && backupFile.exists()) {
                System.out.println("⚠️ Primary database file is missing. Restoring from backup (.bak)...");
                Files.copy(backupFile.toPath(), primaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Backup restoration successful.");
            }

            // ------------------------------------------
            // LOAD DATA
            // ------------------------------------------
            if (primaryFile.exists()) {
                
                // Read the entire JSON file into a generic Map
                Map<String, Object> data = mapper.readValue(primaryFile, new TypeReference<Map<String, Object>>() {});
                
                // --- 1. Load Core User & Log Data ---
                if (data.containsKey("users")) {
                    Map<String, Map<String, String>> usersData = (Map<String, Map<String, String>>) data.get("users");
                    DataStore.USERS_DB.putAll(usersData);
                }
                
                if (data.containsKey("attendance")) {
                    Map<String, List<Map<String, String>>> attData = (Map<String, List<Map<String, String>>>) data.get("attendance");
                    DataStore.ATTENDANCE_DB.putAll(attData);
                }
                
                if (data.containsKey("meals")) {
                    Map<String, List<Map<String, String>>> mealData = (Map<String, List<Map<String, String>>>) data.get("meals");
                    DataStore.MEAL_DB.putAll(mealData);
                }
                
                // --- 2. Load Leave Workflows ---
                if (data.containsKey("leaveBalances")) {
                    Map<String, Map<String, Integer>> leaveBalData = (Map<String, Map<String, Integer>>) data.get("leaveBalances");
                    DataStore.LEAVE_BALANCES.putAll(leaveBalData);
                }
                
                if (data.containsKey("leaveRequests")) {
                    Map<String, Map<String, String>> leaveReqData = (Map<String, Map<String, String>>) data.get("leaveRequests");
                    DataStore.LEAVE_REQUESTS.putAll(leaveReqData);
                }
                
                // --- 3. Load Salary & Payroll Data ---
                if (data.containsKey("salaryDb")) {
                    Map<String, List<Map<String, String>>> salaryData = (Map<String, List<Map<String, String>>>) data.get("salaryDb");
                    DataStore.SALARY_DB.putAll(salaryData);
                }
                
                if (data.containsKey("salaryMaster")) {
                    Map<String, Map<String, String>> salaryMasterData = (Map<String, Map<String, String>>) data.get("salaryMaster");
                    DataStore.SALARY_MASTER.putAll(salaryMasterData);
                }
                
                // --- 4. Load Media & Certifications ---
                if (data.containsKey("certsDb")) {
                    Map<String, List<Map<String, String>>> certsData = (Map<String, List<Map<String, String>>>) data.get("certsDb");
                    DataStore.CERTS_DB.putAll(certsData);
                }
                
                if (data.containsKey("memoriesDb")) {
                    List<Map<String, String>> memoriesData = (List<Map<String, String>>) data.get("memoriesDb");
                    DataStore.MEMORIES_DB.addAll(memoriesData);
                }
                
                System.out.println("✅ All 9 Databases Loaded Successfully! System is ready.");
                
            } else {
                System.out.println("ℹ️ No existing database found. Starting fresh with default users.");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Critical Error loading database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // SYSTEM UPDATE: SAVE DATA TO HARD DRIVE
    // ==========================================
    // "synchronized" ensures thread-safety if multiple HRs/HODs save at the exact same time
    public synchronized void saveData() {
        try {
            File currentFile = new File(FILE_PATH);
            File backupFile = new File(BACKUP_PATH);
            
            // ------------------------------------------
            // 1. CREATE BACKUP (.bak) BEFORE OVERWRITING
            // ------------------------------------------
            if (currentFile.exists()) {
                Files.copy(currentFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // ------------------------------------------
            // 2. PACKAGE ALL DATABASES INTO ONE MAP
            // ------------------------------------------
            Map<String, Object> dataToSave = new HashMap<>();
            
            // Core Data
            dataToSave.put("users", DataStore.USERS_DB);
            dataToSave.put("attendance", DataStore.ATTENDANCE_DB);
            dataToSave.put("meals", DataStore.MEAL_DB);
            
            // Leaves Data
            dataToSave.put("leaveBalances", DataStore.LEAVE_BALANCES);
            dataToSave.put("leaveRequests", DataStore.LEAVE_REQUESTS);
            
            // Salary Data
            dataToSave.put("salaryDb", DataStore.SALARY_DB);
            dataToSave.put("salaryMaster", DataStore.SALARY_MASTER);
            
            // Media Data
            dataToSave.put("certsDb", DataStore.CERTS_DB);
            dataToSave.put("memoriesDb", DataStore.MEMORIES_DB);

            // ------------------------------------------
            // 3. WRITE TO JSON FILE
            // ------------------------------------------
            mapper.writerWithDefaultPrettyPrinter().writeValue(currentFile, dataToSave);
            
        } catch (Exception e) {
            System.out.println("❌ Critical Error saving database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}