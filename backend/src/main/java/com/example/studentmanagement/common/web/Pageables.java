package com.example.studentmanagement.common.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

public final class Pageables {

    private Pageables() {
    }

    public static Pageable requireSortableBy(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot sort by '%s'; allowed: %s".formatted(order.getProperty(), allowedProperties));
            }
        }
        return pageable;
    }
}
