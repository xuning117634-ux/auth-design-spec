package com.huawei.it.roma.liveeda.auth.util;

import java.net.URI;
import java.util.Collection;

public final class LogSanitizer {

    private static final int TAIL_LENGTH = 6;

    private LogSanitizer() {
    }

    public static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public static String tail(String value) {
        if (value == null || value.isBlank()) {
            return "<blank>";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= TAIL_LENGTH) {
            return "***(" + trimmed.length() + ")";
        }
        return "***" + trimmed.substring(trimmed.length() - TAIL_LENGTH);
    }

    public static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    public static String host(String uri) {
        if (uri == null || uri.isBlank()) {
            return "<blank>";
        }
        try {
            String host = URI.create(uri).getHost();
            return host == null || host.isBlank() ? "<no-host>" : host;
        } catch (IllegalArgumentException exception) {
            return "<invalid-uri>";
        }
    }

    public static String host(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) {
            return "<no-host>";
        }
        return uri.getHost();
    }

    public static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
