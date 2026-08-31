package com.Darshan.interview_service.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Judge0Service {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${judge0.url}")
    private String judge0Url;

    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "java", 62,
            "python", 71,
            "cpp", 54,
            "c", 50,
            "javascript", 63
    );

    public JsonNode submitAndWait(String code, String language, String stdin) throws Exception {
        Integer languageId = LANGUAGE_IDS.get(language.toLowerCase());
        if (languageId == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        // Step 1: submit the code, get back a token
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("source_code", Base64.getEncoder().encodeToString(code.getBytes()));
        bodyMap.put("language_id", languageId);
        bodyMap.put("stdin", Base64.getEncoder().encodeToString(stdin.getBytes()));

        String jsonBody = mapper.writeValueAsString(bodyMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> submitRequest = new HttpEntity<>(jsonBody, headers);
        String submitUrl = judge0Url + "/submissions?base64_encoded=true";

        ResponseEntity<String> submitResponse = restTemplate.postForEntity(submitUrl, submitRequest, String.class);
        JsonNode submitResult = mapper.readTree(submitResponse.getBody());
        String token = submitResult.get("token").asText();

        // Step 2: poll for the result — try up to 10 times, waiting 1 second between each
        String resultUrl = judge0Url + "/submissions/" + token + "?base64_encoded=true";
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);

            ResponseEntity<String> pollResponse = restTemplate.getForEntity(resultUrl, String.class);
            JsonNode result = mapper.readTree(pollResponse.getBody());

            int statusId = result.get("status").get("id").asInt();
            // status_id 1 = In Queue, 2 = Processing — keep polling
            // anything else means it's finished (Accepted, error, etc.)
            if (statusId != 1 && statusId != 2) {
                return result;
            }
        }

        throw new RuntimeException("Judge0 execution timed out after polling");
    }
}