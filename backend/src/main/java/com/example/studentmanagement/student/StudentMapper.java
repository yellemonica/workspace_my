package com.example.studentmanagement.student;

import com.example.studentmanagement.student.dto.StudentRequest;
import com.example.studentmanagement.student.dto.StudentResponse;

import java.time.LocalDate;

public final class StudentMapper {

    private StudentMapper() {
    }

    public static StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDateOfBirth(),
                student.getEnrollmentDate());
    }

    static Student toEntity(StudentRequest request, LocalDate defaultEnrollmentDate) {
        return new Student(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.dateOfBirth(),
                request.enrollmentDate() != null ? request.enrollmentDate() : defaultEnrollmentDate);
    }

    static void updateEntity(Student student, StudentRequest request) {
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setDateOfBirth(request.dateOfBirth());
        if (request.enrollmentDate() != null) {
            student.setEnrollmentDate(request.enrollmentDate());
        }
    }
}
