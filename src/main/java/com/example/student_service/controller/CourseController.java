package com.example.student_service.controller;
import com.example.student_service.model.Student;
import com.example.student_service.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;


@RestController
@Transactional
@RequestMapping("/student")
public class CourseController {

    private final CourseService courseEnrolmentService;

    @Autowired
    public CourseController(CourseService courseEnrolmentService) {
        this.courseEnrolmentService = courseEnrolmentService;
    }

    @PostMapping("/enrol")
    public ResponseEntity<?> enrolInCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        try {
            Student student = courseEnrolmentService.enrolInCourse(studentId, courseId);
            return ResponseEntity.ok(student);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/enrolments")
    public List<String> getEnrolments(@RequestParam Long studentId) {
        return courseEnrolmentService.getEnrolledCourses(studentId);
    }

    @GetMapping("/{studentId}/invoices")
    public ResponseEntity<?> getStudentInvoices(
            @PathVariable Long studentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            List<Map<String, Object>> invoices = courseEnrolmentService.fetchStudentInvoices(studentId, status, type);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to fetch invoices: " + e.getMessage());
        }
    }

    @DeleteMapping("/unenrol")
    public ResponseEntity<?> unenrolFromCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        try {
            courseEnrolmentService.unenrolFromCourse(studentId, courseId);
            return ResponseEntity.ok("Unenrolled successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
}
