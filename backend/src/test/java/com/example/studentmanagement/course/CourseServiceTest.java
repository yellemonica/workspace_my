package com.example.studentmanagement.course;

import com.example.studentmanagement.catalog.CatalogLookup;
import com.example.studentmanagement.catalog.CourseCatalogClient;
import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.course.dto.CourseRequest;
import com.example.studentmanagement.course.dto.CourseResponse;
import com.example.studentmanagement.enrollment.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.studentmanagement.TestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseCatalogClient catalogClient;
    @InjectMocks
    private CourseService service;

    @ParameterizedTest
    @EnumSource(value = CatalogLookup.class, names = {"FOUND", "UNAVAILABLE"})
    void createSavesWhenCatalogConfirmsOrIsUnavailable(CatalogLookup lookup) {
        when(courseRepository.existsByCode("CS101")).thenReturn(false);
        when(catalogClient.lookup("CS101")).thenReturn(lookup);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponse created = service.create(request("cs101"));

        assertThat(created.code()).isEqualTo("CS101");
        assertThat(created.enrolledCount()).isZero();
    }

    @Test
    void createRejectsCodeUnknownToCatalog() {
        when(catalogClient.lookup("XX101")).thenReturn(CatalogLookup.NOT_FOUND);

        assertThatThrownBy(() -> service.create(request("XX101")))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BusinessRuleException.COURSE_CODE_NOT_IN_CATALOG);
                    assertThat(ex.getField()).contains("code");
                });
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateCodeWithoutCallingCatalog() {
        when(courseRepository.existsByCode("CS101")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("CS101")))
                .isInstanceOfSatisfying(DuplicateResourceException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("code"));
        verifyNoInteractions(catalogClient);
    }

    @Test
    void updateWithUnchangedCodeSkipsUniquenessAndCatalogChecks() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "CS101")));

        CourseResponse updated = service.update(1L, new CourseRequest("CS101", "Renamed", 5, "New"));

        assertThat(updated.title()).isEqualTo("Renamed");
        assertThat(updated.credits()).isEqualTo(5);
        verify(courseRepository, never()).existsByCodeAndIdNot(anyString(), anyLong());
        verifyNoInteractions(catalogClient);
    }

    @Test
    void updateRejectsCodeOwnedByAnotherCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "CS101")));
        when(courseRepository.existsByCodeAndIdNot("CS201", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("CS201")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateRejectsChangedCodeUnknownToCatalog() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "CS101")));
        when(catalogClient.lookup("XX101")).thenReturn(CatalogLookup.NOT_FOUND);

        assertThatThrownBy(() -> service.update(1L, request("XX101")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteWithoutEnrollmentsDeletesCourse() {
        Course course = course(1L, "CS101");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.countByCourseId(1L)).thenReturn(0L);

        service.delete(1L, false);

        verify(courseRepository).delete(course);
        verify(enrollmentRepository, never()).deleteByCourseId(anyLong());
    }

    @Test
    void deleteWithEnrollmentsRequiresForce() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "CS101")));
        when(enrollmentRepository.countByCourseId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L, false))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BusinessRuleException.COURSE_HAS_ENROLLMENTS);
                    assertThat(ex.getMessage()).contains("CS101", "3 active enrollment(s)");
                });
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void forcedDeleteRemovesEnrollmentsBeforeCourse() {
        Course course = course(1L, "CS101");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.countByCourseId(1L)).thenReturn(3L);

        service.delete(1L, true);

        InOrder order = inOrder(enrollmentRepository, courseRepository);
        order.verify(enrollmentRepository).deleteByCourseId(1L);
        order.verify(courseRepository).delete(course);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, true)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findStudentsThrowsWhenCourseMissing() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStudents(1L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(enrollmentRepository);
    }

    private static CourseRequest request(String code) {
        return new CourseRequest(code, "Title", 3, null);
    }
}
