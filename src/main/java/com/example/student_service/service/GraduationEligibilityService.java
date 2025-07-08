package com.example.student_service.service;

import com.example.student_service.model.Student;
import com.example.student_service.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GraduationEligibilityService {

    @Autowired
    private StudentRepository repo;

    private final RestTemplate restTemplate = new RestTemplate();

    // In StudentService.java
    public boolean checkGraduationEligibility(Long studentId) {
        Student student = repo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        // Check no enrolled courses
        if (!student.getEnrolledCourses().isEmpty()) {
            student.setGraduated(false);
            repo.save(student);
            return false;
        }
        
        // Check for OUTSTANDING invoices using /invoices/{id}
        try {
            for (long invoiceId = 1; invoiceId <= 100; invoiceId++) {
                try {
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        "http://192.168.0.20:8081/invoices/" + invoiceId,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> invoice = response.getBody();
                        Object invoiceStudentId = invoice.get("studentId");
                        Object status = invoice.get("status");
                        if (invoiceStudentId != null && 
                            invoiceStudentId.toString().equals(studentId.toString()) &&
                            status != null && status.toString().equals("OUTSTANDING")) {
                            student.setGraduated(false);
                            repo.save(student);
                            return false;
                        }
                    }
                } catch (RestClientException e) {
                    continue;
                }
            }
            
            student.setGraduated(true);
            repo.save(student);
            return true;
        } catch (Exception e) {
            student.setGraduated(false);
            repo.save(student);
            throw new RuntimeException("Failed to check invoices: " + e.getMessage());
        }
    }
}
