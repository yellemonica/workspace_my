package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.course.Course;
import com.example.studentmanagement.student.Student;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.example.studentmanagement.TestData.course;
import static com.example.studentmanagement.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository repository;

    @Autowired
    private TestEntityManager em;

    private Student ada;
    private Student alan;
    private Course cs101;
    private Course math110;

    @BeforeEach
    void setUp() {
        ada = em.persist(student("Ada", "Lovelace", "ada@x.edu"));
        alan = em.persist(student("Alan", "Turing", "alan@x.edu"));
        cs101 = em.persist(course("CS101"));
        math110 = em.persist(course("MATH110"));
        ada.enroll(cs101);
        ada.enroll(math110);
        alan.enroll(cs101);
        em.flush();
        em.clear();
    }

    @Test
    void findByStudentIdWithCourseFetchesCoursesEagerly() {
        List<Enrollment> enrollments = repository.findByStudentIdWithCourse(ada.getId());

        assertThat(enrollments).hasSize(2).allSatisfy(e -> assertThat(Hibernate.isInitialized(e.getCourse())).isTrue());
        assertThat(enrollments).extracting(e -> e.getCourse().getCode()).containsExactlyInAnyOrder("CS101", "MATH110");
        assertThat(enrollments).allSatisfy(e -> assertThat(e.getEnrolledAt()).isNotNull());
    }

    @Test
    void findStudentsByCourseIdOrdersByName() {
        assertThat(repository.findStudentsByCourseId(cs101.getId()))
                .extracting(Student::getLastName).containsExactly("Lovelace", "Turing");
    }

    @Test
    void deleteByCourseIdRemovesOnlyThatCoursesEnrollments() {
        int deleted = repository.deleteByCourseId(cs101.getId());

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.countByCourseId(cs101.getId())).isZero();
        assertThat(repository.countByCourseId(math110.getId())).isEqualTo(1);
    }

    @Test
    void deletingStudentCascadesToEnrollments() {
        em.remove(em.find(Student.class, ada.getId()));
        em.flush();

        assertThat(repository.countByCourseId(cs101.getId())).isEqualTo(1);
        assertThat(repository.countByCourseId(math110.getId())).isZero();
    }

    @Test
    void sameCourseCannotBeEnrolledTwice() {
        Student student = em.find(Student.class, alan.getId());
        Course course = em.find(Course.class, cs101.getId());

        assertThatThrownBy(() -> repository.saveAndFlush(new Enrollment(student, course)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
