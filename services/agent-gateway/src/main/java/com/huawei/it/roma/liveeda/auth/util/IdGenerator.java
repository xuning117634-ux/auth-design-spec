package com.huawei.it.roma.liveeda.auth.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    public String next(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
