package com.ernichechelski.remoteconfigmanager;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class PullRequestTests {

    @Test
    void checkConfig() throws Exception {
        if (!CommonUtils.isJSONValid(CommonUtils.readConfig())) {
            throw new IOException("Config is not valid JSON!");
        }
    }
}
