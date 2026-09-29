package com.example.studentmanagement.catalog.stub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * Local stand-in for the external course catalog: accepts any code whose department prefix it knows.
 */
@RestController
@RequestMapping("/stub/catalog")
@ConditionalOnProperty(name = "catalog.stub.enabled", havingValue = "true")
class CatalogStubController {

    private static final Set<String> KNOWN_DEPARTMENTS =
            Set.of("CS", "MATH", "PHYS", "CHEM", "BIO", "ENG", "HIST", "ECON");

    @GetMapping("/courses/{code}")
    ResponseEntity<Map<String, String>> find(@PathVariable String code) {
        String department = code.replaceAll("\\d", "");
        return KNOWN_DEPARTMENTS.contains(department)
                ? ResponseEntity.ok(Map.of("code", code, "department", department))
                : ResponseEntity.notFound().build();
    }
}
