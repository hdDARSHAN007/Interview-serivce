package com.Darshan.interview_service.controller;

import org.springframework.security.core.Authentication;
import com.Darshan.interview_service.dto.SubmissionRequest;
import com.Darshan.interview_service.model.Problem;
import com.Darshan.interview_service.model.Submission;
import com.Darshan.interview_service.repository.ProblemRepository;
import com.Darshan.interview_service.repository.SubmissionRepository;
import com.Darshan.interview_service.service.Judge0Service;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.Darshan.interview_service.model.TestCase;
import com.Darshan.interview_service.repository.TestCaseRepository;
import java.util.List;

import java.util.Base64;

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

        List<TestCase> testCases = testCaseRepository.findByProblemId(problem.getId());

        // Fallback: if no test cases added yet, use the problem's original sample input/output
        if (testCases.isEmpty()) {
            TestCase fallback = new TestCase();
            fallback.setInput(problem.getSampleInput());
            fallback.setExpectedOutput(problem.getSampleOutput());
            testCases = List.of(fallback);
        }

        String finalVerdict = "ACCEPTED";
        String combinedOutput = "";
        int passedCount = 0;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
//            JsonNode result = judge0Service.submitAndWait(req.getCode(), req.getLanguage(), tc.getInput());
            String codeToRun = req.getCode();
            if (problem.getDriverCode() != null && !problem.getDriverCode().isBlank()) {
                codeToRun = req.getCode() + "\n\n" + problem.getDriverCode();
            }

            JsonNode result = judge0Service.submitAndWait(codeToRun, req.getLanguage(), tc.getInput());
            String statusDescription = result.get("status").get("description").asText();
            String actualOutput = "";
            if (result.has("stdout") && !result.get("stdout").isNull()) {
                actualOutput = new String(Base64.getDecoder().decode(result.get("stdout").asText()));
            }

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

            if (passed) {
                passedCount++;
            } else if (finalVerdict.equals("ACCEPTED")) {
                // Record the first failure as the overall verdict
                finalVerdict = caseVerdict;
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

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", "");
    }
    public ResponseEntity<?> getAnalytics(Authentication authentication) {
        String email = authentication.getName();
        List<SubmissionRepository.TopicStats> stats = submissionRepository.getTopicStatsForUser(email);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<?> getUserSubmissions(@PathVariable String email) {
        return ResponseEntity.ok(submissionRepository.findByUserEmail(email));
    }
//    private String normalize(String s) {
//        return s.trim().replaceAll("\\s+", "");
//    }
}