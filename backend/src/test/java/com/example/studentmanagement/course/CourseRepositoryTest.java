package com.example.studentmanagement.course;

import com.example.studentmanagement.common.persistence.SearchPatterns;
import com.example.studentmanagement.student.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static com.example.studentmanagement.TestData.course;
import static com.example.studentmanagement.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CourseRepositoryTest {

    @Autowired
    private CourseRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void searchMatchesCodeOrTitleIgnoringCase() {
        repository.save(new Course("CS101", "Introduction to Programming", 4, null));
        repository.save(new Course("MATH110", "Discrete Mathematics", 3, null));
        repository.save(new Course("HIST210", "History of Computing", 2, null));

        assertThat(repository.search(SearchPatterns.containsIgnoreCase("cs1"), PageRequest.of(0, 10)))
                .extracting(Course::getCode).containsExactly("CS101");
        assertThat(repository.search(SearchPatterns.containsIgnoreCase("MATH"), PageRequest.of(0, 10, Sort.by("code"))))
                .extracting(Course::getCode).containsExactly("MATH110");
        assertThat(repository.search(SearchPatterns.containsIgnoreCase("comput"), PageRequest.of(0, 10)))
                .extracting(Course::getCode).containsExactly("HIST210");
    }

    @Test
    void enrolledCountFormulaCountsEnrollments() {
        Course cs101 = em.persist(course("CS101"));
        Course cs201 = em.persist(course("CS201"));
        Student ada = em.persist(student("Ada", "Lovelace", "ada@x.edu"));
        Student alan = em.persist(student("Alan", "Turing", "alan@x.edu"));
        ada.enroll(cs101);
        alan.enroll(cs101);
        em.flush();
        em.clear();

        assertThat(repository.findById(cs101.getId())).get().extracting(Course::getEnrolledCount).isEqualTo(2L);
        assertThat(repository.findById(cs201.getId())).get().extracting(Course::getEnrolledCount).isEqualTo(0L);
    }

    @Test
    void existsByCodeAndIdNotIgnoresTheCourseItself() {
        Course cs101 = repository.save(course("CS101"));
        repository.save(course("CS201"));

        assertThat(repository.existsByCodeAndIdNot("CS101", cs101.getId())).isFalse();
        assertThat(repository.existsByCodeAndIdNot("CS201", cs101.getId())).isTrue();
    }
}
