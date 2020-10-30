package com.ernichechelski.remoteconfigmanager;


import com.ernichechelski.remoteconfigmanager.utils.CommonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * This test is triggered on every pull request.
 */
@SpringBootTest
public class PullRequestTests {

    @Test
    void checkConfig() throws Exception {
        CommonUtils.checkConfig();
    }
}
