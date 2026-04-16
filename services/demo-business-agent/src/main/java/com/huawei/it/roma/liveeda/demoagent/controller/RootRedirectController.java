package com.huawei.it.roma.liveeda.demoagent.controller;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootRedirectController {

    @GetMapping("/")
    public ResponseEntity<Void> redirectToAgent() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/agent"))
                .build();
    }
}
