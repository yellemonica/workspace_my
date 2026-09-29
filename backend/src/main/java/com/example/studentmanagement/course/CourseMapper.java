package com.example.studentmanagement.course;

import com.example.studentmanagement.course.dto.CourseRequest;
import com.example.studentmanagement.course.dto.CourseResponse;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getCredits(),
                course.getDescription(),
                course.getEnrolledCount());
    }

    static Course toEntity(CourseRequest request) {
        return new Course(request.code(), request.title(), request.credits(), request.description());
    }

    static void updateEntity(Course course, CourseRequest request) {
        course.setCode(request.code());
        course.setTitle(request.title());
        course.setCredits(request.credits());
        course.setDescription(request.description());
    }
}
