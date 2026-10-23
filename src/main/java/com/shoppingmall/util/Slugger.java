package com.shoppingmall.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Slugger {

    private Slugger() {
    }

    public static String slug(String... parts) {
        String joined = Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        String normalized = Normalizer.normalize(joined, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "item";
        }
        return slug.length() > 160 ? slug.substring(0, 160).replaceAll("-$", "") : slug;
    }
}
