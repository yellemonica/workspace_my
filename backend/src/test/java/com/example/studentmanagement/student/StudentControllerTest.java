package com.example.studentmanagement.student;

import com.example.studentmanagement.common.exception.DuplicateResourceException;
import com.example.studentmanagement.common.exception.ResourceNotFoundException;
import com.example.studentmanagement.student.dto.StudentRequest;
import com.example.studentmanagement.student.dto.StudentResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String VALID_BODY = """
            {"firstName":"Ada","lastName":"Lovelace","email":"ADA@x.edu","dateOfBirth":"2000-01-01"}
            """;
    private static final StudentResponse ADA = new StudentResponse(
            1L, "Ada", "Lovelace", "ada@x.edu", LocalDate.of(2000, 1, 1), LocalDate.of(2024, 9, 1));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void createReturns201WithLocationAndNormalisedRequest() throws Exception {
        when(studentService.create(any())).thenReturn(ADA);

        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/students/1")))
                .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<StudentRequest> request = ArgumentCaptor.forClass(StudentRequest.class);
        verify(studentService).create(request.capture());
        assertThat(request.getValue().email()).isEqualTo("ada@x.edu");
    }

    @Test
    void createWithInvalidFieldsReturnsProblemWithFieldErrors() throws Exception {
        String body = """
                {"firstName":" ","lastName":"Lovelace","email":"not-an-email","dateOfBirth":"2999-01-01"}
                """;

        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"))
                .andExpect(jsonPath("$.instance").value("/api/students"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("firstName", "email", "dateOfBirth")));
        verifyNoInteractions(studentService);
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void createWithDuplicateEmailReturns409MappedToField() throws Exception {
        when(studentService.create(any())).thenThrow(new DuplicateResourceException("email", "taken"));

        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("taken"));
    }

    @Test
    void getMissingStudentReturns404() throws Exception {
        when(studentService.findById(9L)).thenThrow(new ResourceNotFoundException("Student", 9L));

        mvc.perform(get("/api/students/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Student with id 9 was not found"));
    }

    @Test
    void getWithNonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/students/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void listPassesSearchAndPagingAndWrapsPage() throws Exception {
        Pageable expected = PageRequest.of(1, 5, Sort.by("email"));
        when(studentService.findAll(eq("ada"), eq(expected))).thenReturn(new PageImpl<>(List.of(ADA), expected, 6));

        mvc.perform(get("/api/students").param("search", "ada").param("page", "1").param("size", "5")
                        .param("sort", "email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ada@x.edu"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listRejectsUnsupportedSortProperty() throws Exception {
        mvc.perform(get("/api/students").param("sort", "dateOfBirth"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("dateOfBirth")));
        verifyNoInteractions(studentService);
    }

    @Test
    void emailAvailableRequiresEmail() throws Exception {
        mvc.perform(get("/api/students/email-available").param("email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void emailAvailableReturnsFlag() throws Exception {
        when(studentService.isEmailAvailable("ada@x.edu", 1L)).thenReturn(true);

        mvc.perform(get("/api/students/email-available").param("email", "ada@x.edu").param("excludeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void updateValidatesBody() throws Exception {
        mvc.perform(put("/api/students/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/students/1")).andExpect(status().isNoContent());
        verify(studentService).delete(1L);
    }

    @Test
    void deleteMissingStudentReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Student", 1L)).when(studentService).delete(1L);

        mvc.perform(delete("/api/students/1")).andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturnsGeneric500() throws Exception {
        when(studentService.findById(1L)).thenThrow(new IllegalStateException("database password is hunter2"));

        mvc.perform(get("/api/students/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
