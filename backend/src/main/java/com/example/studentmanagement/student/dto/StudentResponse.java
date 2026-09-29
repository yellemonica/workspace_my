package com.example.studentmanagement.student.dto;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        LocalDate enrollmentDate) {
}
