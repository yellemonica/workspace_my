package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.enrollment.dto.EnrollmentRequest;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<EnrollmentResponse> list(@PathVariable Long studentId) {
        return enrollmentService.findForStudent(studentId);
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable Long studentId, @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse enrollment = enrollmentService.enroll(studentId, request.courseId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{courseId}").buildAndExpand(enrollment.courseId()).toUri();
        return ResponseEntity.created(location).body(enrollment);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long studentId, @PathVariable Long courseId) {
        enrollmentService.unenroll(studentId, courseId);
    }
}
