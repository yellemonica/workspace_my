package com.example.studentmanagement.student;

import com.example.studentmanagement.common.web.AvailabilityResponse;
import com.example.studentmanagement.common.web.PageResponse;
import com.example.studentmanagement.common.web.Pageables;
import com.example.studentmanagement.student.dto.StudentRequest;
import com.example.studentmanagement.student.dto.StudentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final Set<String> SORTABLE = Set.of("firstName", "lastName", "email", "enrollmentDate");

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public PageResponse<StudentResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = {"lastName", "firstName"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PageResponse.from(studentService.findAll(search, Pageables.requireSortableBy(pageable, SORTABLE)));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @GetMapping("/email-available")
    public AvailabilityResponse emailAvailable(
            @RequestParam @NotBlank String email,
            @RequestParam(required = false) Long excludeId) {
        return new AvailabilityResponse(studentService.isEmailAvailable(email, excludeId));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = studentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }
}
