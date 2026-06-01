package com.huawei.it.roma.liveeda.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    @Test
    void shouldReportStringPresence() {
        assertThat(LogSanitizer.present("token")).isTrue();
        assertThat(LogSanitizer.present(" ")).isFalse();
        assertThat(LogSanitizer.present(null)).isFalse();
    }

    @Test
    void shouldOnlyExposeTailOfSensitiveValues() {
        assertThat(LogSanitizer.tail("abcdef123456")).isEqualTo("***123456");
        assertThat(LogSanitizer.tail("abc")).isEqualTo("***(3)");
        assertThat(LogSanitizer.tail("abc")).doesNotContain("abc");
        assertThat(LogSanitizer.tail(" ")).isEqualTo("<blank>");
        assertThat(LogSanitizer.tail(null)).isEqualTo("<blank>");
    }

    @Test
    void shouldReportCollectionSize() {
        assertThat(LogSanitizer.size(List.of("a", "b"))).isEqualTo(2);
        assertThat(LogSanitizer.size(null)).isZero();
    }

    @Test
    void shouldExtractHostWithoutQueryString() {
        assertThat(LogSanitizer.host("https://example.com/callback?code=secret")).isEqualTo("example.com");
        assertThat(LogSanitizer.host("not a uri")).isEqualTo("<invalid-uri>");
        assertThat(LogSanitizer.host(URI.create("http://localhost:18082/agent"))).isEqualTo("localhost");
    }
}
