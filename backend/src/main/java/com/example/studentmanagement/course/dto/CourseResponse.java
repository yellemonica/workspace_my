package com.example.studentmanagement.course.dto;

public record CourseResponse(
        Long id,
        String code,
        String title,
        int credits,
        String description,
        long enrolledCount) {
}
