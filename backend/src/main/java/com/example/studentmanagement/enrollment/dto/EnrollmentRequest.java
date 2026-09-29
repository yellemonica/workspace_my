package com.example.studentmanagement.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long courseId) {
}
