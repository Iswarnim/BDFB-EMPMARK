package com.bdfb.empmark.controller;

import com.bdfb.empmark.service.DataStore;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    // ==========================================
    // 1. MASTER DATA FETCH (DASHBOARD)
    // ==========================================
    @GetMapping("/data")
    public Map<String, Object> getEmployeeData(@RequestParam String id) {
        
        // Clean the ID to prevent mismatch errors
        String cleanId = id.trim().toUpperCase(); 
        
        // 1. Fetch Biometrics (Attendance & Meals)
        List<Map<String, String>> attLogs = DataStore.ATTENDANCE_DB.getOrDefault(cleanId, new ArrayList<>());
        List<Map<String, String>> mealLogs = DataStore.MEAL_DB.getOrDefault(cleanId, new ArrayList<>());
        
        // 2. Fetch all APPROVED leaves for this specific employee (Used to color the calendar)
        List<Map<String, String>> approvedLeaves = DataStore.LEAVE_REQUESTS.values().stream()
                .filter(req -> cleanId.equals(req.get("empId")) && "APPROVED".equals(req.get("status")))
                .collect(Collectors.toList());

        // 3. Fetch Media and Certifications
        List<Map<String, String>> certs = DataStore.CERTS_DB.getOrDefault(cleanId, new ArrayList<>());
        List<Map<String, String>> memories = DataStore.MEMORIES_DB;

        // Return everything in one massive payload to keep the frontend fast
        return Map.of(
            "status", "success", 
            "attendance", attLogs, 
            "meals", mealLogs, 
            "leaves", approvedLeaves, 
            "certs", certs, 
            "memories", memories
        );
    }

    // ==========================================
    // 2. HOD DROPDOWN LIST FETCH
    // ==========================================
    @GetMapping("/hod-list")
    public List<Map<String, String>> getHodList() {
        
        List<Map<String, String>> hods = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, String>> entry : DataStore.USERS_DB.entrySet()) {
            
            String role = entry.getValue().get("role");
            
            // Allow both HOD and ADMIN to receive leave requests (so you can see yourself!)
            if ("HOD".equals(role) || "ADMIN".equals(role)) {
                
                Map<String, String> hod = new HashMap<>();
                hod.put("id", entry.getKey());
                hod.put("name", entry.getValue().get("name"));
                hod.put("dept", entry.getValue().get("dept"));
                
                hods.add(hod);
            }
        }
        
        return hods;
    }

    // ==========================================
    // 3. APPLY FOR LEAVE
    // ==========================================
    @PostMapping("/leave/apply")
    public Map<String, Object> applyLeave(@RequestBody Map<String, String> req) {
        
        String empId = req.get("empId");
        Map<String, String> user = DataStore.USERS_DB.get(empId);
        
        // Generate a unique Request ID based on current server time
        String reqId = "REQ-" + System.currentTimeMillis();
        
        // Create a new Leave Request object starting with the data sent from the frontend
        Map<String, String> leaveReq = new HashMap<>(req);
        
        // Add backend-controlled fields
        leaveReq.put("status", "PENDING_HOD");
        leaveReq.put("reqId", reqId);
        leaveReq.put("name", user.get("name"));
        leaveReq.put("dept", user.get("dept")); 
        
        // Save to Database
        DataStore.LEAVE_REQUESTS.put(reqId, leaveReq);
        
        return Map.of("status", "success", "message", "Sent to selected HOD for Approval");
    }

    // ==========================================
    // 4. FETCH LEAVE REQUESTS (ROLE-BASED ROUTING)
    // ==========================================
    @GetMapping("/leave/requests")
    public List<Map<String, String>> getLeaves(@RequestParam String role, @RequestParam String empId) {
        
        List<Map<String, String>> allReqs = new ArrayList<>(DataStore.LEAVE_REQUESTS.values());

        // IF EMPLOYEE: See only your own leave history
        if (role.equals("EMPLOYEE")) {
            return allReqs.stream()
                    .filter(r -> r.get("empId").equals(empId))
                    .collect(Collectors.toList());
        } 
        
        // IF HOD or ADMIN: See only requests where the employee specifically chose your ID in the dropdown
        else if (role.equals("HOD") || role.equals("ADMIN")) {
            return allReqs.stream()
                    .filter(r -> r.get("status").equals("PENDING_HOD") && empId.equals(r.get("hodId")))
                    .collect(Collectors.toList());
        } 
        
        // IF HR: See only requests that have been cleared by an HOD and are waiting for final HR approval
        else if (role.equals("HR")) {
            return allReqs.stream()
                    .filter(r -> r.get("status").equals("PENDING_HR"))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }

    // ==========================================
    // 5. PROCESS LEAVE ACTIONS (APPROVE/REJECT)
    // ==========================================
    @PostMapping("/leave/action")
    public Map<String, Object> actionLeave(@RequestBody Map<String, String> req) {
        
        String reqId = req.get("reqId");
        Map<String, String> leaveReq = DataStore.LEAVE_REQUESTS.get(reqId);
        
        if (leaveReq == null) {
            return Map.of("status", "error", "message", "Leave request not found");
        }

        // Action 1: Rejected
        if (req.get("action").equals("REJECT")) {
            leaveReq.put("status", "REJECTED_BY_" + req.get("role"));
        } 
        // Action 2: Approved
        else {
            // If HOD or ADMIN approves, it moves up the chain to HR
            if (req.get("role").equals("HOD") || req.get("role").equals("ADMIN")) {
                leaveReq.put("status", "PENDING_HR");
            }
            // If HR approves, it is fully approved and will now show on the Calendar
            else {
                leaveReq.put("status", "APPROVED");
                
                // Optional logic can be added here to automatically deduct from LEAVE_BALANCES
            }
        }
        
        return Map.of("status", "success");
    }

    // ==========================================
    // 6. FETCH LEAVE BALANCES
    // ==========================================
    @GetMapping("/leave-balance")
    public Map<String, Object> getLeaveBalance(@RequestParam String id) {
        // Return existing balances, or an empty map if user has none assigned yet
        Map<String, Integer> balances = DataStore.LEAVE_BALANCES.getOrDefault(id, new HashMap<>());
        
        return Map.of("status", "success", "balances", balances);
    }

    // ==========================================
    // 7. FETCH CORPORATE SALARY SLIPS
    // ==========================================
    @GetMapping("/salary/slips")
    public List<Map<String, String>> getSalaries(@RequestParam String empId) {
        // Return all salary slips generated for this employee
        return DataStore.SALARY_DB.getOrDefault(empId, new ArrayList<>());
    }
}