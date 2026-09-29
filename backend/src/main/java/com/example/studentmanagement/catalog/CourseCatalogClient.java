package com.example.studentmanagement.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CourseCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogClient.class);

    private final RestTemplate restTemplate;

    public CourseCatalogClient(RestTemplateBuilder builder, CatalogProperties properties) {
        this.restTemplate = builder
                .rootUri(properties.baseUrl().toString())
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .build();
    }

    public CatalogLookup lookup(String courseCode) {
        try {
            restTemplate.getForEntity("/courses/{code}", Void.class, courseCode);
            return CatalogLookup.FOUND;
        } catch (HttpClientErrorException.NotFound e) {
            return CatalogLookup.NOT_FOUND;
        } catch (RestClientException e) {
            log.warn("Course catalog unavailable, skipping validation of {}: {}", courseCode, e.getMessage());
            return CatalogLookup.UNAVAILABLE;
        }
    }
}
