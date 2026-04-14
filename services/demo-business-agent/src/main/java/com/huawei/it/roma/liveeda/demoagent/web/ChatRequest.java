package com.huawei.it.roma.liveeda.demoagent.web;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
