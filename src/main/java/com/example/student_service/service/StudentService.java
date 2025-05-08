package com.example.student_service.service;

import com.example.student_service.model.Student;
import com.example.student_service.dto.AccountDTO;
import com.example.student_service.model.Course;
import com.example.student_service.repository.StudentRepository;
import com.example.student_service.repository.CourseRepository;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);
    
   // private final StudentRepository studentRepository;
    private final StudentRepository repo;
    private final CourseRepository courseRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public StudentService(StudentRepository repo, CourseRepository courseRepo) {
        this.repo = repo;
        this.courseRepo = courseRepo;
    }

    // Register a new student
    public Student register(Student student) {
        Student existing = repo.findByEmail(student.getEmail());
        if (existing != null) {
            throw new IllegalArgumentException("Email already in use");
        }
        // Encode the password before saving
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        Student saved = repo.save(student);

        // Create the payload for /api/register
        Map<String, String> payload = new HashMap<>();
        payload.put("studentId", saved.getId().toString());

        // Integrate with Library microservice to create an account
        try {
            restTemplate.postForObject("http://localhost:80/api/register", payload, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to register with Library microservice: " + e.getMessage());
        }

        Map<String, String> financePayload = new HashMap<>();
        financePayload.put("studentId", saved.getId().toString());
        try {
            restTemplate.postForObject("http://localhost:8081/accounts", financePayload, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to register with Finance microservice: " + e.getMessage());
        }

        return saved;
    }

    // Login a student
    public Student login(String email, String password) {
        Student student = repo.findByEmail(email);
        if (student == null || !passwordEncoder.matches(password, student.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return student;
    }

    // Get student profile
    public Student getProfile(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
    }

    // View all courses
    public List<Course> getAllCourses() {
        return courseRepo.findAll();
    }

    // Update student profile
    public Student updateProfile(Long id, String name, String surname) {
        Student student = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (name != null && !name.trim().isEmpty()) {
            student.setName(name);
        }
        if (surname != null && !surname.trim().isEmpty()) {
            student.setSurname(surname);
        }
        return repo.save(student);
    }


    public void deleteStudent(Long studentId) {
        // 1. Find and validate student
        Student student = repo.findById(studentId)
        .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        // 2. Clear course enrollments
        student.getEnrolledCourses().clear();
        repo.save(student);
        //logger.info("Cleared enrollments for student {}", studentId);

        // 3. Delete financial account (using existing finance endpoints)
        deleteFinancialAccount(studentId.toString());

        // 4. Delete student
        repo.delete(student);
        //logger.info("Successfully deleted student {}", studentId);
    }

    private void deleteFinancialAccount(String studentId) {
        try {
            // First try to get account ID
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:8081/accounts/student/" + studentId,
                HttpMethod.GET,
                null,
                Map.class);

            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().containsKey("id")) {
                
                Long accountId = ((Number) response.getBody().get("id")).longValue();
                restTemplate.delete("http://localhost:8081/accounts/" + accountId);
                logger.info("Deleted finance account {} for student {}", accountId, studentId);
            }
        } catch (RestClientException e) {
            logger.warn("Failed to delete finance account for student {}: {}", studentId, e.getMessage());
        }
    }

    public void processLibraryFine(Long studentId, double amount, String description) {
        try {
            // Create invoice payload for the fine
            Map<String, Object> invoicePayload = new HashMap<>();
            invoicePayload.put("studentId", studentId.toString());
            invoicePayload.put("amount", amount);
            invoicePayload.put("type", "LIBRARY_FINE");
            invoicePayload.put("description", description);
            invoicePayload.put("dueDate", LocalDate.now().plusDays(30).toString());

            // Send to Finance microservice
            restTemplate.postForObject("http://localhost:8081/invoices", 
                                     invoicePayload, 
                                     String.class);
            
            logger.info("Created library fine invoice for student {}", studentId);
        } catch (RestClientException e) {
            logger.error("Failed to create library fine invoice: {}", e.getMessage());
            throw new RuntimeException("Failed to process library fine", e);
        }
    }

    @Scheduled(fixedRate = 86400000) // Runs daily (24 hours in milliseconds)
    public void checkForLibraryFines() {
        try {
            // 1. Get all students
            List<Student> students = repo.findAll();

            // 2. For each student, check overdue books
            for (Student student : students) {
                // Call Library's /admin/overdue endpoint
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "http://localhost:80/admin/overdue",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // 3. Process overdue books for this student
                    List<Map<String, Object>> overdueBooks = response.getBody().stream()
                        .filter(book -> book.get("student_id").equals(student.getId().toString()))
                        .collect(Collectors.toList());

                    for (Map<String, Object> book : overdueBooks) {
                        // Calculate fine (e.g., £5 per overdue book)
                        double fineAmount = 5.0;
                        String bookTitle = (String) book.get("title");

                        // Create invoice in Finance system
                        Map<String, Object> invoicePayload = new HashMap<>();
                        invoicePayload.put("studentId", student.getId());
                        invoicePayload.put("amount", fineAmount);
                        invoicePayload.put("type", "LIBRARY_FINE");
                        invoicePayload.put("description", "Late return: " + bookTitle);

                        restTemplate.postForObject(
                            "http://localhost:8081/invoices",
                            invoicePayload,
                            String.class
                        );
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to check for library fines: {}", e.getMessage());
        }
    }

}