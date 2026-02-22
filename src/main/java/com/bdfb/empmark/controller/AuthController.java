package com.bdfb.empmark.controller;

import com.bdfb.empmark.service.DataStore;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        // CLEAN THE ID: Remove any accidental spaces and force uppercase
        String id = credentials.get("id").trim().toUpperCase();
        String pass = credentials.get("password").trim();

        if (!DataStore.USERS_DB.containsKey(id)) {
            return Map.of("status", "error", "message", "User ID not found: " + id);
        }

        Map<String, String> user = DataStore.USERS_DB.get(id);
        String storedPass = user.get("password");

        if (storedPass != null && storedPass.equals(pass)) {
            return Map.of("status", "success", "id", id, "name", user.get("name"), 
                          "role", user.get("role"), "designation", user.get("dept"));
        } else {
            return Map.of("status", "error", "message", "Wrong Password");
        }
    }
}