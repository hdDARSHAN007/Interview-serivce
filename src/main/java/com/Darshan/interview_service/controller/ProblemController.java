package com.Darshan.interview_service.controller;

import com.Darshan.interview_service.dto.ProblemRequest;
import com.Darshan.interview_service.dto.TestCaseRequest;
import com.Darshan.interview_service.model.Problem;
import com.Darshan.interview_service.model.TestCase;
import com.Darshan.interview_service.repository.ProblemRepository;
import com.Darshan.interview_service.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    // Create a new problem (admin only, enforced in SecurityConfig)
    @PostMapping
    public ResponseEntity<?> createProblem(@RequestBody ProblemRequest req) {
        Problem problem = new Problem();
        problem.setTitle(req.getTitle());
        problem.setDescription(req.getDescription());
        problem.setDifficulty(req.getDifficulty());
        problem.setTopic(req.getTopic());
        problem.setSampleInput(req.getSampleInput());
        problem.setSampleOutput(req.getSampleOutput());
        Problem saved = problemRepository.save(problem);
        return ResponseEntity.ok(saved);
    }

    // Get all problems
    @GetMapping
    public ResponseEntity<List<Problem>> getAllProblems() {
        return ResponseEntity.ok(problemRepository.findAll());
    }

    // Get one problem by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        return problemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get problems filtered by topic
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<Problem>> getByTopic(@PathVariable String topic) {
        return ResponseEntity.ok(problemRepository.findByTopic(topic));
    }

    // Anyone logged in can suggest a test case. It stays PENDING until the admin approves it.
    // A test case added by the admin is approved immediately.
    @PostMapping("/{id}/testcases")
    public ResponseEntity<?> addTestCase(@PathVariable Long id,
                                         @RequestBody TestCaseRequest req,
                                         Authentication authentication) {
        if (!problemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (req.getInput() == null || req.getInput().isBlank()
                || req.getExpectedOutput() == null || req.getExpectedOutput().isBlank()) {
            return ResponseEntity.badRequest().body("Both the input and the expected output are required");
        }

        boolean admin = isAdmin(authentication);

        TestCase tc = new TestCase();
        tc.setProblemId(id);
        tc.setInput(req.getInput());
        tc.setExpectedOutput(req.getExpectedOutput());
        tc.setSubmittedBy(authentication.getName());
        tc.setSample(admin && req.isSample());
        tc.setStatus(admin ? "APPROVED" : "PENDING");
        return ResponseEntity.ok(testCaseRepository.save(tc));
    }

    // The admin sees every test case. Students only see approved sample cases,
    // so the hidden ones stay hidden.
    @GetMapping("/{id}/testcases")
    public ResponseEntity<?> getTestCases(@PathVariable Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(testCaseRepository.findByProblemId(id));
        }
        List<TestCase> visible = testCaseRepository.findByProblemIdAndStatus(id, "APPROVED")
                .stream()
                .filter(TestCase::isSample)
                .toList();
        return ResponseEntity.ok(visible);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}