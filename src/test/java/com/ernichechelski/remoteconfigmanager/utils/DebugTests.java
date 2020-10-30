package com.ernichechelski.remoteconfigmanager.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DebugTests {

    @Test
    void downloadConfig() throws Exception {
        CommonUtils.getAndSaveTemplate("latest.json");
    }
}
