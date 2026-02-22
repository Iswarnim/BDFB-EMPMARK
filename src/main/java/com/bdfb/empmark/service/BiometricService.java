package com.bdfb.empmark.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class BiometricService {

    @Autowired
    private PersistenceService persistenceService;

    // 1. STANDARD UPLOAD
    public Map<String, Object> processCsv(MultipartFile file) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.trim().startsWith("'")) continue;
                parseAndSaveLine(line);
                count++;
            }
            persistenceService.saveData(); 
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("status", "success", "count", count);
    }

    // 2. MONTHLY PREVIEW
    public Map<String, Object> previewMonthlyCsv(MultipartFile file) {
        List<Map<String, String>> newRecords = new ArrayList<>();
        int ignored = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(!line.trim().startsWith("'")) continue;
                
                String[] cols = line.split(",");
                if(cols.length < 7) continue;
                String empId = cols[0].replaceAll("['\" ]", "").trim().toUpperCase();
                String date = cols[4].trim();
                String time = cols[5].trim();
                String device = cols[6].trim();

                boolean exists = false;
                List<Map<String, String>> existingLogs = device.contains("ATTENDANCE") ? 
                    DataStore.ATTENDANCE_DB.get(empId) : DataStore.MEAL_DB.get(empId);

                if (existingLogs != null) {
                    for (Map<String, String> log : existingLogs) {
                        if (log.get("date").equals(date) && log.get("time").equals(time)) {
                            exists = true; break;
                        }
                    }
                }
                if (!exists) {
                    Map<String, String> previewItem = new HashMap<>();
                    previewItem.put("id", empId);
                    previewItem.put("date", date);
                    previewItem.put("time", time);
                    previewItem.put("type", device.contains("ATTENDANCE") ? "ATT" : "MEAL");
                    previewItem.put("raw", line);
                    newRecords.add(previewItem);
                } else { ignored++; }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("status", "success", "newRecords", newRecords, "ignoredCount", ignored);
    }

    // 3. CONFIRM UPLOAD
    public Map<String, Object> confirmUpload(List<String> rawLines) {
        int saved = 0;
        for(String line : rawLines) {
            parseAndSaveLine(line);
            saved++;
        }
        persistenceService.saveData(); 
        return Map.of("status", "success", "saved", saved);
    }

    // 4. DIRECTORY UPLOAD
    public Map<String, Object> processDirectory(MultipartFile file) {
        int usersCreated = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            br.readLine(); // Skip Header
            
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(","); 
                if (cols.length < 5) continue;   

                String id = cols[0].trim().toUpperCase();
                String name = cols[1].trim();
                String desig = cols[2].trim();
                String dept = cols[3].trim();
                String dob = cols[4].trim(); 

                // Clean DOB for Password
                String password = dob.replaceAll("[^0-9]", "");

                Map<String, String> user = new HashMap<>();
                user.put("password", password);
                user.put("name", name);
                user.put("role", "EMPLOYEE");
                user.put("dept", dept);

                DataStore.USERS_DB.put(id, user);
                
                // Init Leaves if new
                if(!DataStore.LEAVE_BALANCES.containsKey(id)) {
                    Map<String, Integer> balances = new HashMap<>();
                    balances.put("CL", 8); balances.put("SL", 5); balances.put("EL", 12);
                    DataStore.LEAVE_BALANCES.put(id, balances);
                }
                usersCreated++;
            }
            persistenceService.saveData(); 
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("status", "success", "usersCreated", usersCreated);
    }

    // SHARED PARSER (WITH DEDUPLICATION)
    private void parseAndSaveLine(String line) {
        try {
            String[] cols = line.split(",");
            if(cols.length < 7) return;

            String empId = cols[0].replaceAll("['\" ]", "").trim().toUpperCase();
            String status = cols[3].trim();
            String date = cols[4].trim();
            String time = cols[5].trim();
            String device = cols[6].trim();

            Map<String, String> record = new HashMap<>();
            record.put("date", date);
            record.put("time", time);

            if (device.contains("ATTENDANCE")) {
                record.put("status", status);
                List<Map<String, String>> logs = DataStore.ATTENDANCE_DB.computeIfAbsent(empId, k -> new ArrayList<>());
                
                // STRICT DUPLICATE CHECK
                boolean exists = logs.stream().anyMatch(l -> l.get("date").equals(date) && l.get("time").equals(time));
                if(!exists) {
                    logs.add(record);
                    System.out.println("✅ Saved Attendance: " + empId + " on " + date + " " + time);
                }

            } else if (device.contains("CANTEEN")) {
                record.put("meal", determineMealType(time));
                List<Map<String, String>> logs = DataStore.MEAL_DB.computeIfAbsent(empId, k -> new ArrayList<>());
                
                // STRICT DUPLICATE CHECK
                boolean exists = logs.stream().anyMatch(l -> l.get("date").equals(date) && l.get("time").equals(time));
                if(!exists) logs.add(record);
            }
        } catch (Exception e) { }
    }

    private String determineMealType(String time) {
        try {
            int hour = Integer.parseInt(time.split(":")[0]);
            if (hour >= 6 && hour < 11) return "BREAKFAST";
            if (hour >= 11 && hour < 15) return "LUNCH";
            if (hour >= 18 && hour < 23) return "DINNER";
        } catch (Exception e) {}
        return "SNACK";
    }
}