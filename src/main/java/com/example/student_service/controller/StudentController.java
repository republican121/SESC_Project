package com.example.student_service.controller;

import com.example.student_service.model.Student;
import com.example.student_service.repository.StudentRepository;
import com.example.student_service.model.Course;
import com.example.student_service.service.StudentService;

import com.example.student_service.service.GraduationEligibilityService;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger; // Add this import
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;


import com.example.student_service.dto.LoginRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequestMapping("/student")
public class StudentController {

    private final StudentRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();

    private final StudentService service;

    private final GraduationEligibilityService graduationEligibilityService;

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    public StudentController(StudentService service, StudentRepository repo, 
    GraduationEligibilityService graduationEligibilityService) {
        this.service = service;
        this.repo = repo;
        this.graduationEligibilityService = graduationEligibilityService;
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyStudent(@RequestParam Long studentId) {
        try {
            boolean exists = repo.existsById(studentId);
            if (exists) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Student not found");
            }
        } catch (Exception e) {
            logger.error("Error verifying student ID {}: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Verification failed");
        }
    }

    @PostMapping("/fines")
    public ResponseEntity<?> createLibraryFine(
            @RequestParam Long studentId,
            @RequestParam double amount,
            @RequestParam String description) {
        
        try {
            service.processLibraryFine(studentId, amount, description);
            return ResponseEntity.ok("Library fine processed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/login")
    public String redirectToHome() {
        return "redirect:/";
    }

    @PostMapping("/register")
    public Student register(@RequestBody Student student) {
        return service.register(student);
    }

    @PostMapping("/login")
        public Student login(@RequestBody LoginRequest loginRequest) {
            return service.login(loginRequest.getEmail(), loginRequest.getPassword());
        }

    @GetMapping("/courses")
    public List<Course> getAllCourses() {
        return service.getAllCourses();
    }

    @GetMapping("/{id}")
    public Student getProfile(@PathVariable Long id) {
        return service.getProfile(id);
    }

    @PutMapping("/{id}")
    public Student updateProfile(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String surname) {
        return service.updateProfile(id, name, surname);
    }

    @DeleteMapping("/delete/{studentId}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long studentId) {
        try {
            service.deleteStudent(studentId);
            return ResponseEntity.ok("Student deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/graduate/{id}")
    public boolean checkGraduationEligibility(@PathVariable Long id) {
        return graduationEligibilityService.checkGraduationEligibility(id);
    }
}