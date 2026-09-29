package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.course.Course;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;

final class EnrollmentMapper {

    private EnrollmentMapper() {
    }

    static EnrollmentResponse toResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return new EnrollmentResponse(
                course.getId(), course.getCode(), course.getTitle(), course.getCredits(), enrollment.getEnrolledAt());
    }
}
