package com.example.studentmanagement.course;

import com.example.studentmanagement.common.exception.BusinessRuleException;
import com.example.studentmanagement.course.dto.CourseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void createReturns201() throws Exception {
        when(courseService.create(any())).thenReturn(new CourseResponse(7L, "CS101", "Intro", 4, null, 0));

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"cs101","title":"Intro","credits":4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/courses/7")))
                .andExpect(jsonPath("$.code").value("CS101"));
    }

    @Test
    void createValidatesCodeFormatAndCreditRange() throws Exception {
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"C1","title":"","credits":7}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("code", "title", "credits")));
        verifyNoInteractions(courseService);
    }

    @Test
    void createRejectsZeroCredits() throws Exception {
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CS101","title":"Intro","credits":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("credits"));
    }

    @Test
    void catalogRejectionReturns422MappedToCodeField() throws Exception {
        when(courseService.create(any())).thenThrow(new BusinessRuleException(
                BusinessRuleException.COURSE_CODE_NOT_IN_CATALOG, "code", "not in catalog"));

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ZZ101","title":"Intro","credits":4}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COURSE_CODE_NOT_IN_CATALOG"))
                .andExpect(jsonPath("$.errors[0].field").value("code"));
    }

    @Test
    void deleteWithEnrollmentsReturns422() throws Exception {
        doThrow(new BusinessRuleException(BusinessRuleException.COURSE_HAS_ENROLLMENTS, "has enrollments"))
                .when(courseService).delete(1L, false);

        mvc.perform(delete("/api/courses/1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COURSE_HAS_ENROLLMENTS"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void deleteForwardsForceFlag() throws Exception {
        mvc.perform(delete("/api/courses/1").param("force", "true")).andExpect(status().isNoContent());

        verify(courseService).delete(1L, true);
    }
}
