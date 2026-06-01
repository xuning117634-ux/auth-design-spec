package com.huawei.it.roma.liveeda.auth.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record TokenResultExchangeRequest(
        @JsonAlias("agent_id")
        @NotBlank String agentId,
        @JsonAlias("request_id")
        @NotBlank String requestId,
        @JsonAlias("token_result_ticket")
        @NotBlank String tokenResultTicket
) {
}
