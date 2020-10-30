package com.ernichechelski.remoteconfigmanager.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class contains all utility methods.
 * All they are static because running tests contains all triggering logic.
 */
public class CommonUtils {

    // Based on Firebase documentation.
    private final static String BASE_URL = "https://firebaseremoteconfig.googleapis.com";

    // Based on Firebase documentation.
    private final static String[] SCOPES = { "https://www.googleapis.com/auth/firebase.remoteconfig" };

    // Here should be names for local files with required firebase project secrets.
    private static String LOCAL_SECRETS_FILENAME =  "secrets.properties";

    // Please reffer to check.yml and upload.yml files after changing this.
    private static String CI_SECRETS_FILENAME =  "secrets.json";

    // Here should be names for local files with required firebase project id.
    private static String LOCAL_PROJECT_ID_FILENAME =  "projectId.properties";

    // Please reffer to check.yml and upload.yml files after changing this.
    private static String CI_PROJECT_ID_FILENAME =  "projectId.json";

    /**
     * Check if current Config can produce valid JSON.
     *
     * @throws IOException
     */
    public static void checkConfig() throws Exception {
        if (!CommonUtils.isJSONValid(CommonUtils.createConfig())) {
            throw new Exception("Config is not valid JSON!");
        }
    }

    /**
     * Publish Config class to the to Firebase server.
     *
     * @throws IOException
     */
    public static void publishConfig() throws Exception {
        String etag = CommonUtils.getTemplateETag();
        CommonUtils.publishTemplate(etag);
    }

    private static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Publish Config class to the to Firebase server.
     *
     * @throws IOException
     */
    private static void publishTemplate(String etag) throws IOException {
        System.out.println("Publishing template...");
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("PUT");
        httpURLConnection.setRequestProperty("If-Match", etag);
        httpURLConnection.setRequestProperty("Content-Encoding", "gzip");

        String configStr = createConfig();

        if (!isJSONValid(configStr)) {
            throw new IOException("Config is not valid JSON!");
        }

        performRequest(httpURLConnection, configStr);

        int code = httpURLConnection.getResponseCode();
        if (code == 200) {
            System.out.println("Template has been published.");
        } else {
            System.out.println(inputStreamToString(httpURLConnection.getErrorStream()));
        }
    }

    /**
     * Get current Firebase Remote Config template from server and store it locally.
     *
     * @throws IOException
     */
    public static void getAndSaveTemplate(String filename) throws IOException {
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");

        int code = httpURLConnection.getResponseCode();
        if (code == 200) {
            InputStream inputStream = new GZIPInputStream(httpURLConnection.getInputStream());
            String response = inputStreamToString(inputStream);

            // Print ETag
            String etag = httpURLConnection.getHeaderField("ETag");
            System.out.println("ETag from server: " + etag);

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(response);

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String jsonStr = gson.toJson(jsonElement);

            File file = new File(filename);
            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.print(jsonStr);
            printWriter.flush();
            printWriter.close();

            System.out.println("Template retrieved and has been written to " + filename);
        } else {
            System.out.println(inputStreamToString(httpURLConnection.getErrorStream()));
        }
    }

    /**
     * Get current Firebase Remote Config template from server and get Etag required to upload new config.
     *
     * @throws IOException
     */
    private static String getTemplateETag() throws IOException {
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
        int code = httpURLConnection.getResponseCode();
        if (code == 200) {
            String etag = httpURLConnection.getHeaderField("ETag");
            System.out.println("ETag from server: " + etag);
            return etag;
        } else {
            System.out.println(inputStreamToString(httpURLConnection.getErrorStream()));
            throw new IOException();
        }
    }

    /**
     * Sends request with provided body as string.
     *
     * @throws IOException
     */
    private static void performRequest(HttpURLConnection httpURLConnection, String body) throws IOException {
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(httpURLConnection.getOutputStream());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
        outputStreamWriter.write(body);
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    /**
     * Creates the Firebase Remote Config template from Config class.
     */
    private static String createConfig()  {
       return new GsonBuilder()
               .setPrettyPrinting()
               .disableHtmlEscaping()
               .create()
               .toJson(new ConfigWrapper());
    }

    /**
     * Read contents of InputStream into String.
     *
     * @param inputStream InputStream to read.
     * @return String containing contents of InputStream.
     * @throws IOException
     */
    private static String inputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    /**
     * Create HttpURLConnection that can be used for both retrieving and publishing.
     *
     * @return Base HttpURLConnection.
     * @throws IOException
     */
    private static HttpURLConnection getCommonConnection(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        try {
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessTokenOnCI());
        } catch (IOException e) {
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessTokenLocally());
        }
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        return httpURLConnection;
    }

    private static String getEndpoint() throws IOException {
        String projectId;
        try {
            projectId = getProjectIdLocally();
        } catch (IOException e) {
            projectId = getProjectIdOnCI();
        }
        return "/v1/projects/" + projectId + "/remoteConfig";
    }

    private static String getAccessTokenLocally() throws IOException {
        return refreshTokenBasedOn(LOCAL_SECRETS_FILENAME);
    }

    private static String getAccessTokenOnCI() throws IOException {
        return refreshTokenBasedOn(CI_SECRETS_FILENAME);
    }
    private static String getProjectIdLocally() throws IOException {
        return getFileContents(LOCAL_PROJECT_ID_FILENAME);
    }

    private static String getProjectIdOnCI() throws IOException {
        return getFileContents(CI_PROJECT_ID_FILENAME);
    }

    private static String getFileContents(String pathName) throws IOException {
        File file = new File(pathName);
        Scanner scanner = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    private static String refreshTokenBasedOn(String filename) throws IOException {
        GoogleCredential googleCredential = GoogleCredential
                .fromStream(new FileInputStream(filename))
                .createScoped(Arrays.asList(SCOPES));
        googleCredential.refreshToken();
        return googleCredential.getAccessToken();
    }
}
