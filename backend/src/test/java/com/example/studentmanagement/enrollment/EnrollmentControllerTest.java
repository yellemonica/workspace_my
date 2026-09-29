package com.example.studentmanagement.enrollment;

import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.enrollment.dto.EnrollmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    private static final String ENROLL_IN_2 = """
            {"courseId":2}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void enrollReturns201WithLocation() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenReturn(
                new EnrollmentResponse(2L, "CS201", "Data Structures", 4, Instant.parse("2026-09-01T09:00:00Z")));

        mvc.perform(post("/api/students/1/enrollments").contentType(MediaType.APPLICATION_JSON).content(ENROLL_IN_2))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/students/1/enrollments/2")))
                .andExpect(jsonPath("$.code").value("CS201"))
                .andExpect(jsonPath("$.enrolledAt").value("2026-09-01T09:00:00Z"));
    }

    @Test
    void enrollRequiresCourseId() throws Exception {
        mvc.perform(post("/api/students/1/enrollments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("courseId"));
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollBeyondLimitReturns422() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(
                new BusinessRuleException(BusinessRuleException.MAX_COURSES_EXCEEDED, "max 6"));

        mvc.perform(post("/api/students/1/enrollments").contentType(MediaType.APPLICATION_JSON).content(ENROLL_IN_2))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MAX_COURSES_EXCEEDED"));
    }

    @Test
    void enrollTwiceReturns409() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(new DuplicateResourceException("courseId", "already"));

        mvc.perform(post("/api/students/1/enrollments").contentType(MediaType.APPLICATION_JSON).content(ENROLL_IN_2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("courseId"));
    }

    @Test
    void unenrollReturns204() throws Exception {
        mvc.perform(delete("/api/students/1/enrollments/2")).andExpect(status().isNoContent());

        verify(enrollmentService).unenroll(1L, 2L);
    }

    @Test
    void unenrollWhenNotEnrolledReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("not enrolled")).when(enrollmentService).unenroll(1L, 2L);

        mvc.perform(delete("/api/students/1/enrollments/2")).andExpect(status().isNotFound());
    }
}
