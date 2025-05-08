package com.example.student_service;

import com.example.student_service.model.Course;
import com.example.student_service.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class StudentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(CourseRepository courseRepository) {
        return args -> {
            // Step 1: Clean up duplicates
            List<Course> allCourses = courseRepository.findAll();
            Map<String, Course> uniqueCourses = new HashMap<>();
            List<Course> duplicates = new ArrayList<>();

            for (Course course : allCourses) {
                String key = course.getName() + "|" + course.getDescription();
                if (uniqueCourses.containsKey(key)) {
                    duplicates.add(course);
                } else {
                    uniqueCourses.put(key, course);
                }
            }

            duplicates.sort(Comparator.comparingLong(Course::getId).reversed());
            for (Course duplicate : duplicates) {
                String key = duplicate.getName() + "|" + duplicate.getDescription();
                Course keeper = uniqueCourses.get(key);
                if (duplicate.getId() > keeper.getId()) {
                    courseRepository.delete(duplicate);
                } else {
                    courseRepository.delete(keeper);
                    uniqueCourses.put(key, duplicate);
                }
            }

            // Step 2: Ensure the required courses exist
            if (courseRepository.findByName("Introduction to Java") == null) {
                courseRepository.save(new Course("Introduction to Java", "Learn Java programming basics"));
            }
            if (courseRepository.findByName("Web Development") == null) {
                courseRepository.save(new Course("Web Development", "Build web applications"));
            }
            if (courseRepository.findByName("Database Systems") == null) {
                courseRepository.save(new Course("Database Systems", "Understand databases and SQL"));
            }
        };
    }
}