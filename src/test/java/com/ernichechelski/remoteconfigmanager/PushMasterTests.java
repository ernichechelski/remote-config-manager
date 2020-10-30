package com.ernichechelski.remoteconfigmanager;

import com.ernichechelski.remoteconfigmanager.utils.CommonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PushMasterTests {

	@Test
	void uploadConfig() throws Exception {
		CommonUtils.publishConfig();
	}
}
