package com.example.studentmanagement.common.persistence;

import java.util.Locale;

public final class SearchPatterns {

    private SearchPatterns() {
    }

    /**
     * Builds a lower-cased {@code LIKE} pattern matching {@code term} anywhere, with wildcard characters in the
     * user input escaped so they match literally. Queries must declare {@code escape '\'}.
     */
    public static String containsIgnoreCase(String term) {
        String escaped = term.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
