package com.ernichechelski.remoteconfigmanager;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SpringBootTest
class PushMasterTests {

	@Test
	void uploadConfig() throws Exception {
		String etag = CommonUtils.getTemplateETag();
		CommonUtils.publishTemplate(etag);
	}
}
