package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.course.Course;
import com.example.studentmanagement.course.CourseRepository;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;
import com.example.studentmanagement.student.Student;
import com.example.studentmanagement.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.LongStream;

import static com.example.studentmanagement.TestData.course;
import static com.example.studentmanagement.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long COURSE_ID = 10L;

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private EnrollmentService service;

    @Test
    void enrollAddsCourseUsingLockedStudent() {
        Student student = student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu");
        givenLockedStudent(student);
        givenCourse(course(COURSE_ID, "CS101"));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = service.enroll(STUDENT_ID, COURSE_ID);

        assertThat(response.courseId()).isEqualTo(COURSE_ID);
        assertThat(response.code()).isEqualTo("CS101");
        assertThat(student.isEnrolledIn(COURSE_ID)).isTrue();
        verify(studentRepository).findByIdForUpdate(STUDENT_ID);
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void enrollAllowsTheSixthCourse() {
        Student student = studentWithCourses(EnrollmentService.MAX_COURSES_PER_STUDENT - 1);
        givenLockedStudent(student);
        givenCourse(course(COURSE_ID, "CS101"));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enroll(STUDENT_ID, COURSE_ID);

        assertThat(student.courseCount()).isEqualTo(EnrollmentService.MAX_COURSES_PER_STUDENT);
    }

    @Test
    void enrollRejectsSeventhCourse() {
        givenLockedStudent(studentWithCourses(EnrollmentService.MAX_COURSES_PER_STUDENT));
        givenCourse(course(COURSE_ID, "CS101"));

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, COURSE_ID))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BusinessRuleException.MAX_COURSES_EXCEEDED));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsCourseAlreadyTaken() {
        Course course = course(COURSE_ID, "CS101");
        Student student = student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu");
        student.enroll(course);
        givenLockedStudent(student);
        givenCourse(course);

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, COURSE_ID))
                .isInstanceOfSatisfying(DuplicateResourceException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("courseId"));
    }

    @Test
    void enrollThrowsWhenStudentMissing() {
        when(studentRepository.findByIdForUpdate(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student");
    }

    @Test
    void enrollThrowsWhenCourseMissing() {
        givenLockedStudent(student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu"));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course");
    }

    @Test
    void unenrollRemovesEnrollment() {
        Student student = student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu");
        student.enroll(course(COURSE_ID, "CS101"));
        givenLockedStudent(student);

        service.unenroll(STUDENT_ID, COURSE_ID);

        assertThat(student.isEnrolledIn(COURSE_ID)).isFalse();
    }

    @Test
    void unenrollThrowsWhenNotEnrolled() {
        givenLockedStudent(student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu"));

        assertThatThrownBy(() -> service.unenroll(STUDENT_ID, COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void findForStudentThrowsWhenStudentMissing() {
        when(studentRepository.existsById(STUDENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.findForStudent(STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Student studentWithCourses(int count) {
        Student student = student(STUDENT_ID, "Ada", "Lovelace", "ada@x.edu");
        LongStream.rangeClosed(1, count).forEach(id -> student.enroll(course(100 + id, "CS" + (100 + id))));
        return student;
    }

    private void givenLockedStudent(Student student) {
        when(studentRepository.findByIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenCourse(Course course) {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
    }
}
