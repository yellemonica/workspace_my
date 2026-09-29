package com.example.studentmanagement.common.exception;

import java.util.Optional;

public class BusinessRuleException extends RuntimeException {

    public static final String MAX_COURSES_EXCEEDED = "MAX_COURSES_EXCEEDED";
    public static final String COURSE_HAS_ENROLLMENTS = "COURSE_HAS_ENROLLMENTS";
    public static final String COURSE_CODE_NOT_IN_CATALOG = "COURSE_CODE_NOT_IN_CATALOG";

    private final String code;
    private final String field;

    public BusinessRuleException(String code, String message) {
        this(code, null, message);
    }

    public BusinessRuleException(String code, String field, String message) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public Optional<String> getField() {
        return Optional.ofNullable(field);
    }
}
