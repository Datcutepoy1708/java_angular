package com.store.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-+|-+$)");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noMarks = normalized.replaceAll("\\p{M}", "");
        // Replace Vietnamese 'đ' / 'Đ'
        noMarks = noMarks.replace('đ', 'd').replace('Đ', 'd');

        String nowhitespace = WHITESPACE.matcher(noMarks).replaceAll("-");
        String normalizedString = NONLATIN.matcher(nowhitespace).replaceAll("");
        String slug = EDGES_DASHES.matcher(normalizedString).replaceAll("").toLowerCase(Locale.ENGLISH);
        return slug.replaceAll("-+", "-");
    }
}
