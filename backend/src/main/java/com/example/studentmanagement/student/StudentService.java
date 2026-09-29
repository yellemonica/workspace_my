package com.example.studentmanagement.student;

import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.common.persistence.SearchPatterns;
import com.example.studentmanagement.student.dto.StudentRequest;
import com.example.studentmanagement.student.dto.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final Clock clock;

    public StudentService(StudentRepository studentRepository, Clock clock) {
        this.studentRepository = studentRepository;
        this.clock = clock;
    }

    public Page<StudentResponse> findAll(String search, Pageable pageable) {
        Page<Student> page = StringUtils.hasText(search)
                ? studentRepository.search(SearchPatterns.containsIgnoreCase(search), pageable)
                : studentRepository.findAll(pageable);
        return page.map(StudentMapper::toResponse);
    }

    public StudentResponse findById(Long id) {
        return StudentMapper.toResponse(getStudent(id));
    }

    public boolean isEmailAvailable(String email, Long excludeId) {
        String normalized = StudentRequest.normalizeEmail(email);
        return excludeId == null
                ? !studentRepository.existsByEmail(normalized)
                : !studentRepository.existsByEmailAndIdNot(normalized, excludeId);
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        if (studentRepository.existsByEmail(request.email())) {
            throw duplicateEmail(request.email());
        }
        Student student = StudentMapper.toEntity(request, LocalDate.now(clock));
        return StudentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = getStudent(id);
        if (studentRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw duplicateEmail(request.email());
        }
        StudentMapper.updateEntity(student, request);
        return StudentMapper.toResponse(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.delete(getStudent(id));
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private static DuplicateResourceException duplicateEmail(String email) {
        return new DuplicateResourceException("email", "A student with email '%s' already exists".formatted(email));
    }
}
