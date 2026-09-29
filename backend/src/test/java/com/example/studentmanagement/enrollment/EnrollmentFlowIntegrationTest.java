package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.catalog.CatalogLookup;
import com.example.studentmanagement.catalog.CourseCatalogClient;
import com.example.studentmanagement.course.dto.CourseResponse;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;
import com.example.studentmanagement.student.dto.StudentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EnrollmentFlowIntegrationTest {

    private static final ParameterizedTypeReference<List<EnrollmentResponse>> ENROLLMENT_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final AtomicInteger NEXT_COURSE_NUMBER = new AtomicInteger(100);

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private CourseCatalogClient catalogClient;

    @BeforeEach
    void catalogKnowsEveryCode() {
        when(catalogClient.lookup(anyString())).thenReturn(CatalogLookup.FOUND);
    }

    @Test
    void enrollUpToLimitThenGuardCourseDeletionAndUnenroll() {
        StudentResponse student = createStudent();
        List<CourseResponse> courses = IntStream.range(0, 7).mapToObj(i -> createCourse()).toList();

        courses.subList(0, 6).forEach(course ->
                assertThat(enroll(student.id(), course.id()).getStatusCode()).isEqualTo(HttpStatus.CREATED));

        ResponseEntity<JsonNode> seventh = enrollForProblem(student.id(), courses.get(6).id());
        assertThat(seventh.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(seventh.getBody().get("code").asText()).isEqualTo("MAX_COURSES_EXCEEDED");

        ResponseEntity<JsonNode> duplicate = enrollForProblem(student.id(), courses.get(0).id());
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(enrollments(student.id())).hasSize(6);
        assertThat(rest.getForObject("/api/courses/{id}", CourseResponse.class, courses.get(0).id()).enrolledCount())
                .isEqualTo(1);

        Long firstCourse = courses.get(0).id();
        assertThat(delete("/api/courses/{id}", firstCourse)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(delete("/api/courses/{id}?force=true", firstCourse)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(enrollments(student.id())).extracting(EnrollmentResponse::courseId).doesNotContain(firstCourse);

        assertThat(delete("/api/students/{s}/enrollments/{c}", student.id(), courses.get(1).id()))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(enroll(student.id(), courses.get(6).id()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(enrollments(student.id())).hasSize(5);
    }

    @Test
    void concurrentEnrollmentsCannotExceedTheLimit() throws Exception {
        StudentResponse student = createStudent();
        List<CourseResponse> courses = IntStream.range(0, 7).mapToObj(i -> createCourse()).toList();
        courses.subList(0, 5).forEach(course -> enroll(student.id(), course.id()));

        CountDownLatch start = new CountDownLatch(1);
        List<HttpStatus> statuses;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<CompletableFuture<HttpStatus>> requests = courses.subList(5, 7).stream()
                    .map(course -> CompletableFuture.supplyAsync(() -> {
                        await(start);
                        return HttpStatus.valueOf(enrollForProblem(student.id(), course.id()).getStatusCode().value());
                    }, pool))
                    .toList();
            start.countDown();
            statuses = requests.stream().map(CompletableFuture::join).toList();
        }

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(enrollments(student.id())).hasSize(6);
    }

    private StudentResponse createStudent() {
        Map<String, Object> body = Map.of(
                "firstName", "Test",
                "lastName", "Student",
                "email", UUID.randomUUID() + "@example.edu",
                "dateOfBirth", "2001-05-05");
        ResponseEntity<StudentResponse> response = rest.postForEntity("/api/students", body, StudentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CourseResponse createCourse() {
        String code = "CS" + NEXT_COURSE_NUMBER.getAndIncrement();
        Map<String, Object> body = Map.of("code", code, "title", "Course " + code, "credits", 3);
        ResponseEntity<CourseResponse> response = rest.postForEntity("/api/courses", body, CourseResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<EnrollmentResponse> enroll(Long studentId, Long courseId) {
        return rest.postForEntity("/api/students/{id}/enrollments", Map.of("courseId", courseId),
                EnrollmentResponse.class, studentId);
    }

    private ResponseEntity<JsonNode> enrollForProblem(Long studentId, Long courseId) {
        return rest.postForEntity("/api/students/{id}/enrollments", Map.of("courseId", courseId),
                JsonNode.class, studentId);
    }

    private List<EnrollmentResponse> enrollments(Long studentId) {
        return rest.exchange("/api/students/{id}/enrollments", HttpMethod.GET, null, ENROLLMENT_LIST, studentId)
                .getBody();
    }

    private HttpStatus delete(String url, Object... uriVariables) {
        return HttpStatus.valueOf(rest.exchange(url, HttpMethod.DELETE, null, Void.class, uriVariables)
                .getStatusCode().value());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
