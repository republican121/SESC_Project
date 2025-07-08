package com.example.student_service.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Student {

    @Id
    private Long id;  // Removed @GeneratedValue to assign manually

    private String name;
    private String surname;
    private String email;
    private boolean isGraduated;
    private String password;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> enrolledCourses = new ArrayList<>();

    // Constructors
    public Student() {
        this.id = generateEightDigitId();
    }

    // Utility method to generate 8-digit ID
    private Long generateEightDigitId() {
        long min = 10000000L;
        long max = 99999999L;
        return min + (long)(Math.random() * (max - min));
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; } // Optional: can make this private to restrict external assignment

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isGraduated() { return isGraduated; }
    public void setGraduated(boolean graduated) { isGraduated = graduated; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Course> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }
}
