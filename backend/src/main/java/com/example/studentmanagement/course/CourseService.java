package com.example.studentmanagement.course;

import com.example.studentmanagement.catalog.CatalogLookup;
import com.example.studentmanagement.catalog.CourseCatalogClient;
import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.common.persistence.SearchPatterns;
import com.example.studentmanagement.course.dto.CourseRequest;
import com.example.studentmanagement.course.dto.CourseResponse;
import com.example.studentmanagement.enrollment.EnrollmentRepository;
import com.example.studentmanagement.student.StudentMapper;
import com.example.studentmanagement.student.dto.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseCatalogClient catalogClient;

    public CourseService(CourseRepository courseRepository,
                         EnrollmentRepository enrollmentRepository,
                         CourseCatalogClient catalogClient) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.catalogClient = catalogClient;
    }

    public Page<CourseResponse> findAll(String search, Pageable pageable) {
        Page<Course> page = StringUtils.hasText(search)
                ? courseRepository.search(SearchPatterns.containsIgnoreCase(search), pageable)
                : courseRepository.findAll(pageable);
        return page.map(CourseMapper::toResponse);
    }

    public CourseResponse findById(Long id) {
        return CourseMapper.toResponse(getCourse(id));
    }

    public List<StudentResponse> findStudents(Long courseId) {
        getCourse(courseId);
        return enrollmentRepository.findStudentsByCourseId(courseId).stream()
                .map(StudentMapper::toResponse)
                .toList();
    }

    public boolean isCodeAvailable(String code, Long excludeId) {
        String normalized = CourseRequest.normalizeCode(code);
        return excludeId == null
                ? !courseRepository.existsByCode(normalized)
                : !courseRepository.existsByCodeAndIdNot(normalized, excludeId);
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw duplicateCode(request.code());
        }
        requireInCatalog(request.code());
        return CourseMapper.toResponse(courseRepository.save(CourseMapper.toEntity(request)));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getCourse(id);
        if (!course.getCode().equals(request.code())) {
            if (courseRepository.existsByCodeAndIdNot(request.code(), id)) {
                throw duplicateCode(request.code());
            }
            requireInCatalog(request.code());
        }
        CourseMapper.updateEntity(course, request);
        return CourseMapper.toResponse(course);
    }

    @Transactional
    public void delete(Long id, boolean force) {
        Course course = getCourse(id);
        long enrollments = enrollmentRepository.countByCourseId(id);
        if (enrollments > 0 && !force) {
            throw new BusinessRuleException(BusinessRuleException.COURSE_HAS_ENROLLMENTS,
                    "Course %s has %d active enrollment(s); delete with force=true to remove them"
                            .formatted(course.getCode(), enrollments));
        }
        if (enrollments > 0) {
            enrollmentRepository.deleteByCourseId(id);
        }
        courseRepository.delete(course);
    }

    private void requireInCatalog(String code) {
        if (catalogClient.lookup(code) == CatalogLookup.NOT_FOUND) {
            throw new BusinessRuleException(BusinessRuleException.COURSE_CODE_NOT_IN_CATALOG, "code",
                    "Course code '%s' is not in the course catalog".formatted(code));
        }
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private static DuplicateResourceException duplicateCode(String code) {
        return new DuplicateResourceException("code", "A course with code '%s' already exists".formatted(code));
    }
}
