package com.bdfb.empmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmpMarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmpMarkApplication.class, args);
        System.out.println("✅ BDFB EmpMark Server is running on http://localhost:8080");
    }
}