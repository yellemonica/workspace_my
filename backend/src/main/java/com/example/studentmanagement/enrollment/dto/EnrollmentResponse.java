package com.example.studentmanagement.enrollment.dto;

import java.time.Instant;

public record EnrollmentResponse(Long courseId, String code, String title, int credits, Instant enrolledAt) {
}
