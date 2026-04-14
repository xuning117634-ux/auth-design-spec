package com.huawei.it.roma.liveeda.demoagent.web;

public record ChatResponse(
        String answer,
        String source,
        String status,
        String redirectUrl,
        String state
) {
    public static ChatResponse answer(String answer, String source) {
        return new ChatResponse(answer, source, "answer", null, null);
    }

    public static ChatResponse redirect(String redirectUrl, String state) {
        return new ChatResponse(null, null, "redirect", redirectUrl, state);
    }
}
