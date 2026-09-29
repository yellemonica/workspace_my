package com.example.studentmanagement;

import com.example.studentmanagement.course.Course;
import com.example.studentmanagement.student.Student;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static Student student(String firstName, String lastName, String email) {
        return new Student(firstName, lastName, email, LocalDate.of(2000, 1, 15), LocalDate.of(2024, 9, 1));
    }

    public static Student student(Long id, String firstName, String lastName, String email) {
        return withId(student(firstName, lastName, email), id);
    }

    public static Course course(String code) {
        return new Course(code, "Course " + code, 3, null);
    }

    public static Course course(Long id, String code) {
        return withId(course(code), id);
    }

    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
