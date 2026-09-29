package com.example.studentmanagement.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CourseRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z]{2,4}\\d{3}$", message = "must be 2-4 letters followed by 3 digits, e.g. CS101")
        String code,
        @NotBlank @Size(max = 150) String title,
        @NotNull @Min(1) @Max(6) Integer credits,
        @Size(max = 2000) String description) {

    public CourseRequest {
        code = normalizeCode(code);
        title = title == null ? null : title.strip();
        description = description == null || description.isBlank() ? null : description.strip();
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.strip().toUpperCase(Locale.ROOT);
    }
}
