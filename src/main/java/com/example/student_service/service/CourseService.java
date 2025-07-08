package com.example.student_service.service;

import com.example.student_service.model.Course;
import com.example.student_service.model.Student;
import com.example.student_service.repository.CourseRepository;
import com.example.student_service.repository.StudentRepository;
import com.oracle.bmc.ospgateway.InvoiceService;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger; 
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;

@Service
public class CourseService {

    @Autowired
    private StudentRepository repo;

    @Autowired
    private CourseRepository courseRepo;

    private final RestTemplate restTemplate = new RestTemplate();

    private final Logger logger = LoggerFactory.getLogger(CourseService.class);

   
public Student enrolInCourse(Long studentId, Long courseId) {
    Student student = repo.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
    Course course = courseRepo.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));

    if (student.getEnrolledCourses().stream()
            .anyMatch(c -> c.getId().equals(courseId))) {
        throw new RuntimeException("Student already enrolled in this course");
    }

    student.getEnrolledCourses().add(course);
    Student saved = repo.save(student);

    // Ensure the studentId is valid and part of the account map
    Map<String, Object> invoicePayload = new HashMap<>();
    invoicePayload.put("amount", 100.0);
    invoicePayload.put("type", "TUITION_FEES");
    invoicePayload.put("description", "Tuition fee for course: " + course.getName());
    invoicePayload.put("dueDate", LocalDate.now().plusDays(30).toString());

    // 👇 Nested map for 'account' with a valid studentId
    Map<String, Object> accountMap = new HashMap<>();
    accountMap.put("studentId", String.valueOf(studentId)); // or just studentId if Long is accepted
    invoicePayload.put("account", accountMap);

    try {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://192.168.0.20:8081/invoices",
            invoicePayload,
            Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String invoiceReference = (String) response.getBody().get("reference");
            course.setInvoiceReference(invoiceReference);
            courseRepo.save(course);
        } else {
            throw new RuntimeException("Invoice service returned status: " + response.getStatusCode());
        }

    } catch (RestClientException e) {
        student.getEnrolledCourses().remove(course);
        repo.save(student);
        throw new RuntimeException("Failed to create invoice: " + e.getMessage());
    }

    return saved;
}



    public List<String> getEnrolledCourses(Long studentId) {
        Student student = repo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    
        return student.getEnrolledCourses().stream()
                .map(course -> "Course ID: " + course.getId() + " - " + course.getName())
                .collect(Collectors.toList());
    }


    public List<Map<String, Object>> fetchStudentInvoices(Long studentId, String status, String type) {
        List<Map<String, Object>> invoices = new ArrayList<>();

        // Step 1: Verify student has an account
        String accountUrl = "http://192.168.0.20:8081/accounts/student/" + studentId;
        try {
            ResponseEntity<Map<String, Object>> accountResponse = restTemplate.exchange(
                accountUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (!accountResponse.getStatusCode().is2xxSuccessful() || accountResponse.getBody() == null) {
                return invoices; // No account, return empty list
            }
        } catch (RestClientException e) {
            logger.warn("No account found for student ID {}: {}", studentId, e.getMessage());
            return invoices;
        }

        // Step 2: Fetch invoices by iterating over possible IDs
        for (long invoiceId = 1; invoiceId <= 100; invoiceId++) { // Arbitrary upper limit
            try {
                String invoiceUrl = "http://192.168.0.20:8081/invoices/" + invoiceId;
                ResponseEntity<Map<String, Object>> invoiceResponse = restTemplate.exchange(
                    invoiceUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                if (invoiceResponse.getStatusCode().is2xxSuccessful() && invoiceResponse.getBody() != null) {
                    Map<String, Object> invoice = invoiceResponse.getBody();
                    Object invoiceStudentId = invoice.get("studentId");

                    if (invoiceStudentId != null && invoiceStudentId.toString().equals(studentId.toString())) {
                        boolean matchesStatus = status == null || status.isEmpty() || 
                                                invoice.get("status").toString().equalsIgnoreCase(status);
                        boolean matchesType = type == null || type.isEmpty() || 
                                              invoice.get("type").toString().equalsIgnoreCase(type);

                        if (matchesStatus && matchesType) {
                            invoices.add(invoice);
                        }
                    }
                }
            } catch (RestClientException e) {
                logger.debug("Skipping invoice ID {} for student ID {}: {}", invoiceId, studentId, e.getMessage());
            }
        }

        return invoices;
    }
    

    // Unenroll from a course
    @Transactional
    public void unenrolFromCourse(Long studentId, Long courseId) {
        Student student = repo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        boolean wasEnrolled = student.getEnrolledCourses()
                .removeIf(c -> c.getId().equals(courseId));
        
        if (!wasEnrolled) {
            throw new RuntimeException("Student was not enrolled in this course");
        }
        
        repo.save(student);

        String invoiceReference = course.getInvoiceReference();
        if (invoiceReference != null) {
            try {
                restTemplate.delete("http://192.168.0.20:8081/invoices/" + invoiceReference + "/cancel");
                course.setInvoiceReference(null);
                courseRepo.save(course);
            } catch (RestClientException e) {
                System.out.println("Invoice cancellation failed: " + e.getMessage());
            }
        }
    }

}
