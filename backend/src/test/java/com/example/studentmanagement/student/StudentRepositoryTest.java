package com.example.studentmanagement.student;

import com.example.studentmanagement.common.persistence.SearchPatterns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static com.example.studentmanagement.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class StudentRepositoryTest {

    private static final PageRequest BY_LAST_NAME = PageRequest.of(0, 10, Sort.by("lastName"));

    @Autowired
    private StudentRepository repository;

    private Student ada;

    @BeforeEach
    void setUp() {
        ada = repository.save(student("Ada", "Lovelace", "ada.lovelace@example.edu"));
        repository.save(student("Alan", "Turing", "alan.turing@example.edu"));
        repository.save(student("Grace", "Hopper", "grace@navy.example"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ada", "LOVE", "ada love", "lovelace@example"})
    void searchMatchesFirstNameLastNameFullNameAndEmailIgnoringCase(String term) {
        Page<Student> result = repository.search(SearchPatterns.containsIgnoreCase(term), BY_LAST_NAME);

        assertThat(result.getContent()).extracting(Student::getLastName).containsExactly("Lovelace");
    }

    @Test
    void searchAppliesPagingAndSorting() {
        Page<Student> result = repository.search(
                SearchPatterns.containsIgnoreCase("example"), PageRequest.of(0, 2, Sort.by("lastName")));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).extracting(Student::getLastName).containsExactly("Hopper", "Lovelace");
    }

    @Test
    void searchTreatsWildcardsInInputLiterally() {
        assertThat(repository.search(SearchPatterns.containsIgnoreCase("a_a"), BY_LAST_NAME)).isEmpty();
        assertThat(repository.search(SearchPatterns.containsIgnoreCase("%"), BY_LAST_NAME)).isEmpty();
    }

    @Test
    void existsByEmailAndIdNotIgnoresTheStudentItself() {
        assertThat(repository.existsByEmailAndIdNot("ada.lovelace@example.edu", ada.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("alan.turing@example.edu", ada.getId())).isTrue();
    }

    @Test
    void findByIdForUpdateLoadsStudent() {
        assertThat(repository.findByIdForUpdate(ada.getId())).get()
                .extracting(Student::getEmail).isEqualTo("ada.lovelace@example.edu");
    }

    @Test
    void uniqueEmailConstraintIsNamedSoTheErrorHandlerCanMapIt() {
        assertThatThrownBy(() -> repository.saveAndFlush(student("Other", "Ada", "ada.lovelace@example.edu")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(((DataIntegrityViolationException) ex).getMostSpecificCause().getMessage())
                        .containsIgnoringCase("uk_students_email"));
    }
}
