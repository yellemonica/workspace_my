package com.example.studentmanagement.student;

import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.student.dto.StudentRequest;
import com.example.studentmanagement.student.dto.StudentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.example.studentmanagement.TestData.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Mock
    private StudentRepository studentRepository;

    private StudentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);
        service = new StudentService(studentRepository, clock);
    }

    @Test
    void findAllWithoutSearchReturnsEveryStudent() {
        when(studentRepository.findAll(PAGE)).thenReturn(page(student(1L, "Ada", "Lovelace", "ada@x.edu")));

        Page<StudentResponse> result = service.findAll("  ", PAGE);

        assertThat(result.getContent()).extracting(StudentResponse::email).containsExactly("ada@x.edu");
        verify(studentRepository, never()).search(anyString(), any());
    }

    @Test
    void findAllWithSearchUsesEscapedContainsPattern() {
        when(studentRepository.search("%50\\%\\_off%", PAGE)).thenReturn(page());

        service.findAll(" 50%_OFF ", PAGE);

        verify(studentRepository).search("%50\\%\\_off%", PAGE);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(studentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student with id 9 was not found");
    }

    @Test
    void createDefaultsEnrollmentDateToToday() {
        when(studentRepository.existsByEmail("ada@x.edu")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse created = service.create(request("ADA@x.edu", null));

        assertThat(created.email()).isEqualTo("ada@x.edu");
        assertThat(created.enrollmentDate()).isEqualTo(TODAY);
    }

    @Test
    void createKeepsExplicitEnrollmentDate() {
        LocalDate enrolled = LocalDate.of(2025, 9, 1);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request("ada@x.edu", enrolled));

        ArgumentCaptor<Student> saved = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(saved.capture());
        assertThat(saved.getValue().getEnrollmentDate()).isEqualTo(enrolled);
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(studentRepository.existsByEmail("ada@x.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("ada@x.edu", null)))
                .isInstanceOfSatisfying(DuplicateResourceException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("email"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateAppliesChanges() {
        Student existing = student(1L, "Ada", "Lovelace", "ada@x.edu");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("countess@x.edu", 1L)).thenReturn(false);

        StudentResponse updated = service.update(1L, new StudentRequest(
                "Augusta", "King", "countess@x.edu", LocalDate.of(1990, 12, 10), null));

        assertThat(updated.firstName()).isEqualTo("Augusta");
        assertThat(updated.email()).isEqualTo("countess@x.edu");
        assertThat(updated.enrollmentDate()).isEqualTo(LocalDate.of(2024, 9, 1));
    }

    @Test
    void updateRejectsEmailOwnedByAnotherStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "Ada", "Lovelace", "ada@x.edu")));
        when(studentRepository.existsByEmailAndIdNot("alan@x.edu", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("alan@x.edu", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateThrowsWhenMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request("ada@x.edu", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesStudent() {
        Student existing = student(1L, "Ada", "Lovelace", "ada@x.edu");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(studentRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(studentRepository, never()).delete(any());
    }

    @Test
    void emailAvailabilityNormalisesAndHonoursExcludedId() {
        when(studentRepository.existsByEmail("ada@x.edu")).thenReturn(true);
        when(studentRepository.existsByEmailAndIdNot("ada@x.edu", 1L)).thenReturn(false);

        assertThat(service.isEmailAvailable(" ADA@x.edu ", null)).isFalse();
        assertThat(service.isEmailAvailable("ada@x.edu", 1L)).isTrue();
    }

    private static StudentRequest request(String email, LocalDate enrollmentDate) {
        return new StudentRequest("Ada", "Lovelace", email, LocalDate.of(2000, 1, 1), enrollmentDate);
    }

    private static Page<Student> page(Student... students) {
        return new PageImpl<>(List.of(students), PAGE, students.length);
    }
}
