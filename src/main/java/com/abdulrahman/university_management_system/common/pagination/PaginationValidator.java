package com.abdulrahman.university_management_system.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PaginationValidator {

    private static final int MAX_PAGE_SIZE = 100;

    public Pageable validate(Pageable pageable, Set<String> allowedSortFields) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new InvalidPaginationException("Pagination is required");
        }

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must not exceed " + MAX_PAGE_SIZE
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new InvalidPaginationException(
                        "Unsupported sort field: " + order.getProperty()
                );
            }
        }

        Sort sort = pageable.getSort();

        // Add a stable tie-breaker when sorting by another field.
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }
}
