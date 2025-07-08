package com.example.student_service;

import com.example.student_service.model.Student;
import com.example.student_service.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StudentRepository studentRepository;

    private String getBaseUrl() {
        return "http://localhost:8069/student";
    }

    @Test
    public void testRegisterStudent() {
        Student student = new Student();
        student.setName("John");
        student.setSurname("Doe");
        student.setEmail("john@example.com");
        student.setPassword("123456");

        ResponseEntity<Student> response = restTemplate.postForEntity(
                getBaseUrl() + "/register",
                student,
                Student.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John", response.getBody().getName());
    }

    @Test
    public void testVerifyStudentExists() {
        Student student = new Student();
        student.setName("Alice");
        student.setSurname("Smith");
        student.setEmail("alice@example.com");
        student.setPassword("password");
        student = studentRepository.save(student);

        ResponseEntity<Void> response = restTemplate.getForEntity(
                getBaseUrl() + "/verify?studentId=" + student.getId(),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetProfileById() {
        Student student = new Student();
        student.setName("Jane");
        student.setSurname("Roe");
        student.setEmail("jane@example.com");
        student.setPassword("secret");
        student = studentRepository.save(student);

        ResponseEntity<Student> response = restTemplate.getForEntity(
                getBaseUrl() + "/" + student.getId(),
                Student.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Jane", response.getBody().getName());
    }
}
