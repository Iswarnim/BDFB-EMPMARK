package com.bdfb.empmark.controller;

import com.bdfb.empmark.service.DataStore;
import com.bdfb.empmark.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/hr")
public class HRController {

    @Autowired
    private PersistenceService persistenceService;

    // ==========================================
    // 1. MEDIA & CERTIFICATE UPLOADS
    // ==========================================
    
    @PostMapping("/cert/upload")
    public Map<String, Object> uploadCert(@RequestBody Map<String, String> payload) {
        
        String empId = payload.get("empId").trim().toUpperCase();
        
        // Add certificate to the employee's specific list
        DataStore.CERTS_DB.computeIfAbsent(empId, k -> new ArrayList<>()).add(payload);
        
        persistenceService.saveData();
        
        return Map.of("status", "success", "message", "Certificate officially issued to employee!");
    }

    @PostMapping("/memory/upload")
    public Map<String, Object> uploadMemory(@RequestBody Map<String, String> payload) {
        
        // Add to the global company memories feed
        DataStore.MEMORIES_DB.add(payload);
        
        persistenceService.saveData();
        
        return Map.of("status", "success", "message", "Company Memory posted to dashboard!");
    }

    // ==========================================
    // 2. SALARY & PAYROLL MANAGEMENT
    // ==========================================
    
    @PostMapping("/salary/setup")
    public Map<String, Object> setupSalary(@RequestBody Map<String, String> payload) {
        
        String empId = payload.get("empId").trim().toUpperCase(); 
        
        // Save the static profile and salary structure
        DataStore.SALARY_MASTER.put(empId, payload);
        
        persistenceService.saveData();
        
        return Map.of("status", "success", "message", "Salary Master Updated");
    }

    @PostMapping("/salary/generate")
    public Map<String, Object> generateSlip(@RequestBody Map<String, String> payload) {
        
        String empId = payload.get("empId").trim().toUpperCase(); 
        
        Map<String, String> master = DataStore.SALARY_MASTER.get(empId);
        
        if (master == null) {
            return Map.of("status", "error", "message", "Salary not setup for this employee yet!");
        }

        // --- Extract Base Values Safely ---
        double perDay = Double.parseDouble(master.getOrDefault("perDay", "0"));
        double basic = Double.parseDouble(master.getOrDefault("basic", "0"));
        double hra = Double.parseDouble(master.getOrDefault("hra", "0"));
        double conv = Double.parseDouble(master.getOrDefault("conv", "0"));
        double med = Double.parseDouble(master.getOrDefault("med", "0"));
        double spec = Double.parseDouble(master.getOrDefault("spec", "0"));
        double pf = Double.parseDouble(master.getOrDefault("pfAmt", "0"));

        // --- Extract Dynamic Monthly Inputs ---
        double lwpDays = Double.parseDouble(payload.getOrDefault("lwpDays", "0"));
        double arrBasic = Double.parseDouble(payload.getOrDefault("arrBasic", "0"));
        double arrHra = Double.parseDouble(payload.getOrDefault("arrHra", "0"));

        // --- Calculations ---
        double deduction = perDay * lwpDays;
        double totalCurrent = basic + hra + conv + med + spec;
        double totalArrear = arrBasic + arrHra;
        double totalDeductions = deduction + pf;
        double netPay = (totalCurrent + totalArrear) - totalDeductions;

        // --- Build Final Slip Map ---
        Map<String, String> slip = new HashMap<>(master);
        slip.put("month", payload.get("month"));
        slip.put("lwpDays", String.valueOf(lwpDays));
        slip.put("deductionLwp", String.format("%.2f", deduction));
        slip.put("arrBasic", String.valueOf(arrBasic));
        slip.put("arrHra", String.valueOf(arrHra));
        slip.put("totalCurrent", String.format("%.2f", totalCurrent));
        slip.put("totalArrear", String.format("%.2f", totalArrear));
        slip.put("totalDeductions", String.format("%.2f", totalDeductions));
        slip.put("netPay", String.format("%.2f", netPay));

        // Save slip to employee's history
        DataStore.SALARY_DB.computeIfAbsent(empId, k -> new ArrayList<>()).add(slip);
        
        persistenceService.saveData();
        
        return Map.of("status", "success", "message", "Salary Slip Generated!");
    }

    // ==========================================
    // 3. ATTENDANCE MANAGEMENT
    // ==========================================
    
    @PostMapping("/attendance/edit")
    public Map<String, Object> editAttendance(@RequestBody Map<String, String> payload) {
        
        String empId = payload.get("empId").trim().toUpperCase();
        
        Map<String, String> newLog = new HashMap<>();
        newLog.put("date", payload.get("date"));
        newLog.put("time", payload.get("time"));
        newLog.put("status", payload.get("status") + " (Edited by HR)");
        
        DataStore.ATTENDANCE_DB.computeIfAbsent(empId, k -> new ArrayList<>()).add(newLog);
        
        persistenceService.saveData();
        
        return Map.of("status", "success", "message", "Attendance corrected!");
    }

    // ==========================================
    // 4. MASTER ATTENDANCE EXCEL EXPORT
    // ==========================================
    @GetMapping("/attendance/download")
    public ResponseEntity<String> downloadCsv(
            @RequestParam(required = false) String month, 
            @RequestParam(required = false) String year) {
        
        // Use current month/year if none provided
        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
            year = String.valueOf(now.getYear());
        }

        int daysInMonth = 31;
        try {
            YearMonth ym = YearMonth.parse(month + " " + year, DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
            daysInMonth = ym.lengthOfMonth();
        } catch (Exception e) {
            // Fallback to 31 if parsing fails
        }

        StringBuilder csv = new StringBuilder();
        
        // --- 1. BUILD HEADERS (COLUMNS A TO AP) ---
        csv.append("Employee ID,");             // A
        csv.append("Name,");                    // B
        csv.append("Date of Joining,");         // C
        csv.append("Designation,");             // D
        csv.append("Department,");              // E
        csv.append("Account No.,");             // F
        csv.append("IFSC Code,");               // G
        csv.append("Bank Name,");               // H
        csv.append("Branch,");                  // I
        
        // Columns J to AN (Days 1 to 31)
        for(int i = 1; i <= 31; i++) { 
            csv.append(i).append(","); 
        }
        
        csv.append("Count of Present,");        // AO
        csv.append("Count of Absent\n");        // AP

        LocalDate today = LocalDate.now();

        // --- 2. POPULATE DATA ROWS FOR EACH EMPLOYEE ---
        for (Map.Entry<String, Map<String, String>> entry : DataStore.USERS_DB.entrySet()) {
            
            String empId = entry.getKey();
            Map<String, String> user = entry.getValue();
            Map<String, String> master = DataStore.SALARY_MASTER.getOrDefault(empId, new HashMap<>());
            
            // Extract Profile Fields
            String name = user.getOrDefault("name", "");
            String doj = master.getOrDefault("joinDate", "");
            String desig = user.getOrDefault("role", "EMPLOYEE");
            String dept = user.getOrDefault("dept", "");
            String accNo = master.getOrDefault("accNo", "");
            
            // Empty placeholders for future banking details to ensure column alignment
            String ifsc = ""; 
            String bankName = ""; 
            String branch = ""; 

            // Write Columns A to I
            csv.append(empId).append(",")
               .append(name).append(",")
               .append(doj).append(",")
               .append(desig).append(",")
               .append(dept).append(",")
               .append(accNo).append(",")
               .append(ifsc).append(",")
               .append(bankName).append(",")
               .append(branch).append(",");

            int presentCount = 0;
            int absentCount = 0;
            List<Map<String, String>> attLogs = DataStore.ATTENDANCE_DB.getOrDefault(empId, new ArrayList<>());

            // Write Columns J to AN (Days 1 to 31)
            for (int i = 1; i <= 31; i++) {
                
                // If month has fewer than 31 days (e.g. Feb), leave remaining columns blank
                if (i > daysInMonth) {
                    csv.append(","); 
                    continue;
                }
                
                String dayStr = (i < 10 ? "0" + i : String.valueOf(i));
                String dateStr = dayStr + "-" + month + "-" + year; // e.g. "05-Feb-2026"
                
                boolean isPresent = attLogs.stream().anyMatch(l -> dateStr.equals(l.get("date")));
                
                boolean isSunday = false;
                boolean isFuture = false;
                
                try {
                    LocalDate dateObj = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
                    
                    if (dateObj.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        isSunday = true;
                    }
                    if (dateObj.isAfter(today)) {
                        isFuture = true;
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for individual days
                }

                // Apply Logic for the day's status
                if (isPresent) {
                    csv.append("P,");
                    presentCount++;
                } 
                else if (isFuture) {
                    csv.append(","); // Leave future days entirely blank
                } 
                else if (isSunday) {
                    csv.append("WO,"); // Week Off for Sundays
                } 
                else {
                    csv.append("A,"); // Absent
                    absentCount++;
                }
            }

            // Write Columns AO and AP
            csv.append(presentCount).append(",").append(absentCount).append("\n");
        }

        // --- 3. RETURN AS CSV FILE DOWNLOAD ---
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Master_Attendance_" + month + "_" + year + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }
}