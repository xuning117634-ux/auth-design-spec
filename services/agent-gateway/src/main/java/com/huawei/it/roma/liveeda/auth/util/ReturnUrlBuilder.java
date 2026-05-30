package com.huawei.it.roma.liveeda.auth.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

public final class ReturnUrlBuilder {

    private ReturnUrlBuilder() {
    }

    public static URI appendParams(URI returnUrl, Map<String, String> params) {
        if (returnUrl.getRawFragment() == null) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUri(returnUrl);
            params.forEach(builder::queryParam);
            return builder.build(true).toUri();
        }

        String baseUrl = UriComponentsBuilder.fromUri(returnUrl)
                .fragment(null)
                .build(true)
                .toUriString();
        String fragment = returnUrl.getRawFragment();
        return URI.create(baseUrl + "#" + appendFragmentParams(fragment, params));
    }

    private static String appendFragmentParams(String fragment, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(fragment);
        builder.append(fragmentParamSeparator(fragment));
        params.forEach((name, value) -> builder
                .append(UriUtils.encodeQueryParam(name, StandardCharsets.UTF_8))
                .append("=")
                .append(UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8))
                .append("&"));
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private static String fragmentParamSeparator(String fragment) {
        if (fragment.endsWith("?") || fragment.endsWith("&")) {
            return "";
        }
        return fragment.contains("?") ? "&" : "?";
    }
}
