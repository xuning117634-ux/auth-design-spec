package com.huawei.it.roma.liveeda.demoagent.controller;

import com.huawei.it.roma.liveeda.demoagent.service.DemoAgentService;
import com.huawei.it.roma.liveeda.demoagent.web.ChatRequest;
import com.huawei.it.roma.liveeda.demoagent.web.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final DemoAgentService demoAgentService;

    @PostMapping("/send")
    public ChatResponse send(
            @CookieValue(name = DemoAgentService.SITE_SESSION_COOKIE) String siteSessionId,
            @Valid @RequestBody ChatRequest request
    ) {
        return demoAgentService.handleChat(siteSessionId, request.message());
    }
}
