package com.Darshan.interview_service.controller;

import com.Darshan.interview_service.dto.SubmissionRequest;
import com.Darshan.interview_service.model.Problem;
import com.Darshan.interview_service.model.Submission;
import com.Darshan.interview_service.model.TestCase;
import com.Darshan.interview_service.repository.ProblemRepository;
import com.Darshan.interview_service.repository.SubmissionRepository;
import com.Darshan.interview_service.repository.TestCaseRepository;
import com.Darshan.interview_service.service.Judge0Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private Judge0Service judge0Service;
    @Autowired private TestCaseRepository testCaseRepository;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody SubmissionRequest req, Authentication authentication) throws Exception {
        Problem problem = problemRepository.findById(req.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        // Only test cases the admin has approved are used for grading
        List<TestCase> testCases = testCaseRepository.findByProblemIdAndStatus(problem.getId(), "APPROVED");

        // Fallback: if there are no approved test cases, use the problem's own sample input/output
        if (testCases.isEmpty()) {
            TestCase fallback = new TestCase();
            fallback.setInput(problem.getSampleInput());
            fallback.setExpectedOutput(problem.getSampleOutput());
            testCases = List.of(fallback);
        }

        // Join the student's code with the hidden driver code for the chosen language
        String lang = req.getLanguage().toLowerCase();
        String codeToRun = req.getCode();

        if (lang.equals("python") && notBlank(problem.getDriverCode())) {
            codeToRun = req.getCode() + "\n\n" + problem.getDriverCode();
        } else if (lang.equals("java") && notBlank(problem.getDriverSuffixJava())) {
            String prefix = problem.getDriverPrefixJava() != null ? problem.getDriverPrefixJava() : "";
            codeToRun = prefix + "\n" + req.getCode() + "\n\n" + problem.getDriverSuffixJava();
        } else if (lang.equals("cpp") && notBlank(problem.getDriverSuffixCpp())) {
            String prefix = problem.getDriverPrefixCpp() != null ? problem.getDriverPrefixCpp() : "";
            codeToRun = prefix + "\n" + req.getCode() + "\n\n" + problem.getDriverSuffixCpp();
        }

        String finalVerdict = "ACCEPTED";
        String combinedOutput = "";
        int passedCount = 0;
        boolean errorShown = false;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);

            JsonNode result = judge0Service.submitAndWait(codeToRun, req.getLanguage(), tc.getInput());
            String statusDescription = result.get("status").get("description").asText();
            String actualOutput = decodeField(result, "stdout");

            boolean passed;
            String caseVerdict;
            if (!"Accepted".equals(statusDescription)) {
                caseVerdict = statusDescription.toUpperCase().replace(" ", "_");
                passed = false;
            } else if (normalize(actualOutput).equals(normalize(tc.getExpectedOutput()))) {
                caseVerdict = "ACCEPTED";
                passed = true;
            } else {
                caseVerdict = "WRONG_ANSWER";
                passed = false;
            }

            combinedOutput += "Test " + (i + 1) + ": " + caseVerdict + " | Output: " + actualOutput.trim() + "\n";

            // Show the real error message once (compile error, crash message, and so on)
            if (!"Accepted".equals(statusDescription) && !errorShown) {
                String errorText = firstNonBlank(
                        decodeField(result, "compile_output"),
                        decodeField(result, "stderr"),
                        decodeField(result, "message"));
                if (!errorText.isBlank()) {
                    combinedOutput += "Error message:\n" + truncate(errorText.trim(), 800) + "\n";
                    errorShown = true;
                }
            }

            if (passed) {
                passedCount++;
            } else if (finalVerdict.equals("ACCEPTED")) {
                // The first failure becomes the overall verdict
                finalVerdict = caseVerdict;
            }

            // A compile error will fail every test the same way, so stop early
            if (caseVerdict.startsWith("COMPILATION")) {
                break;
            }
        }

        // Only ACCEPTED if every test case passed
        if (passedCount < testCases.size() && finalVerdict.equals("ACCEPTED")) {
            finalVerdict = "WRONG_ANSWER";
        }

        combinedOutput += "\nPassed " + passedCount + "/" + testCases.size() + " test cases.";

        Submission submission = new Submission();
        submission.setUserEmail(authentication.getName());
        submission.setProblemId(problem.getId());
        submission.setTopic(problem.getTopic());
        submission.setCode(req.getCode());
        submission.setLanguage(req.getLanguage());
        submission.setVerdict(finalVerdict);
        submission.setOutput(combinedOutput);
        submissionRepository.save(submission);

        return ResponseEntity.ok(submission);
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(Authentication authentication) {
        String email = authentication.getName();
        List<SubmissionRepository.TopicStats> stats = submissionRepository.getTopicStatsForUser(email);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<?> getUserSubmissions(@PathVariable String email) {
        return ResponseEntity.ok(submissionRepository.findByUserEmail(email));
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", "");
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    // Judge0 sends text fields base64-encoded; this turns one back into plain text
    private String decodeField(JsonNode result, String field) {
        if (result.has(field) && !result.get(field).isNull()) {
            String raw = result.get(field).asText();
            try {
                return new String(Base64.getMimeDecoder().decode(raw));
            } catch (IllegalArgumentException e) {
                return raw;
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}