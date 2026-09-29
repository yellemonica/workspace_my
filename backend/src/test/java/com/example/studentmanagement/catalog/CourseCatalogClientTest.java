package com.example.studentmanagement.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = CourseCatalogClient.class, properties = "catalog.base-url=http://catalog.test/v1")
@Import(CatalogConfig.class)
class CourseCatalogClientTest {

    private static final String CS101_URL = "/courses/CS101";

    @Autowired
    private CourseCatalogClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void knownCodeIsFound() {
        server.expect(requestTo(CS101_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(client.lookup("CS101")).isEqualTo(CatalogLookup.FOUND);
        server.verify();
    }

    @Test
    void notFoundMeansUnknownCode() {
        server.expect(requestTo(CS101_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.lookup("CS101")).isEqualTo(CatalogLookup.NOT_FOUND);
    }

    @Test
    void serverErrorFallsBackToUnavailable() {
        server.expect(requestTo(CS101_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(client.lookup("CS101")).isEqualTo(CatalogLookup.UNAVAILABLE);
    }

    @Test
    void timeoutFallsBackToUnavailable() {
        server.expect(requestTo(CS101_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(client.lookup("CS101")).isEqualTo(CatalogLookup.UNAVAILABLE);
    }
}
