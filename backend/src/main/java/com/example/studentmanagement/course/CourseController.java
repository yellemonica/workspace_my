package com.example.studentmanagement.course;

import com.example.studentmanagement.common.web.AvailabilityResponse;
import com.example.studentmanagement.common.web.PageResponse;
import com.example.studentmanagement.common.web.Pageables;
import com.example.studentmanagement.course.dto.CourseRequest;
import com.example.studentmanagement.course.dto.CourseResponse;
import com.example.studentmanagement.student.dto.StudentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
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
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Set<String> SORTABLE = Set.of("code", "title", "credits");

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public PageResponse<CourseResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return PageResponse.from(courseService.findAll(search, Pageables.requireSortableBy(pageable, SORTABLE)));
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @GetMapping("/{id}/students")
    public List<StudentResponse> students(@PathVariable Long id) {
        return courseService.findStudents(id);
    }

    @GetMapping("/code-available")
    public AvailabilityResponse codeAvailable(
            @RequestParam @NotBlank String code,
            @RequestParam(required = false) Long excludeId) {
        return new AvailabilityResponse(courseService.isCodeAvailable(code, excludeId));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
        courseService.delete(id, force);
    }
}
