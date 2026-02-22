package com.bdfb.empmark.controller;

import com.bdfb.empmark.service.BiometricService;
import com.bdfb.empmark.service.DataStore;
import com.bdfb.empmark.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private BiometricService bioService;
    
    @Autowired
    private PersistenceService persistenceService;

    @PostMapping("/upload-logs")
    public Map<String, Object> uploadLogs(@RequestParam("file") MultipartFile file) {
        return bioService.processCsv(file);
    }

    @PostMapping("/preview-monthly")
    public Map<String, Object> previewMonthly(@RequestParam("file") MultipartFile file) {
        return bioService.previewMonthlyCsv(file);
    }

    @PostMapping("/confirm-merge")
    public Map<String, Object> confirmMerge(@RequestBody Map<String, List<String>> payload) {
        return bioService.confirmUpload(payload.get("lines"));
    }

    @PostMapping("/upload-directory")
    public Map<String, Object> uploadDirectory(@RequestParam("file") MultipartFile file) {
        return bioService.processDirectory(file);
    }

    // Role Management Endpoints
    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        return Map.of("status", "success", "users", DataStore.USERS_DB);
    }

    @PostMapping("/update-role")
    public Map<String, Object> updateRole(@RequestBody Map<String, String> payload) {
        String empId = payload.get("empId");
        String newRole = payload.get("role");
        
        if (DataStore.USERS_DB.containsKey(empId)) {
            DataStore.USERS_DB.get(empId).put("role", newRole);
            persistenceService.saveData(); // Save change to database
            return Map.of("status", "success", "message", "Role updated successfully!");
        }
        return Map.of("status", "error", "message", "User not found");
    }
}