package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.course.Course;
import com.example.studentmanagement.course.CourseRepository;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;
import com.example.studentmanagement.student.Student;
import com.example.studentmanagement.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    public static final int MAX_COURSES_PER_STUDENT = 6;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<EnrollmentResponse> findForStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student", studentId);
        }
        return enrollmentRepository.findByStudentIdWithCourse(studentId).stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    /**
     * The student row is locked for the whole transaction so concurrent enrollments for the same student are
     * serialised; otherwise two requests could both observe five courses and both insert a sixth.
     */
    @Transactional
    public EnrollmentResponse enroll(Long studentId, Long courseId) {
        Student student = lockStudent(studentId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (student.isEnrolledIn(courseId)) {
            throw new DuplicateResourceException("courseId",
                    "Student %d is already enrolled in %s".formatted(studentId, course.getCode()));
        }
        if (student.courseCount() >= MAX_COURSES_PER_STUDENT) {
            throw new BusinessRuleException(BusinessRuleException.MAX_COURSES_EXCEEDED,
                    "A student can be enrolled in at most %d courses".formatted(MAX_COURSES_PER_STUDENT));
        }

        Enrollment enrollment = enrollmentRepository.save(student.enroll(course));
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Transactional
    public void unenroll(Long studentId, Long courseId) {
        Student student = lockStudent(studentId);
        if (!student.unenroll(courseId)) {
            throw new ResourceNotFoundException(
                    "Student %d is not enrolled in course %d".formatted(studentId, courseId));
        }
    }

    private Student lockStudent(Long studentId) {
        return studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
    }
}
