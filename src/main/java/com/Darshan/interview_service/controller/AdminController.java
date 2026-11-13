package com.Darshan.interview_service.controller;

import com.Darshan.interview_service.model.Problem;
import com.Darshan.interview_service.model.TestCase;
import com.Darshan.interview_service.repository.ProblemRepository;
import com.Darshan.interview_service.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// Every endpoint here is admin only (enforced in SecurityConfig)
@RestController
@RequestMapping("/api/admin/testcases")
public class AdminController {

    @Autowired private TestCaseRepository testCaseRepository;
    @Autowired private ProblemRepository problemRepository;

    record PendingTestCase(Long id, Long problemId, String problemTitle,
                           String input, String expectedOutput, String submittedBy) {}

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        List<PendingTestCase> pending = testCaseRepository.findByStatus("PENDING")
                .stream()
                .map(tc -> {
                    String title = problemRepository.findById(tc.getProblemId())
                            .map(Problem::getTitle)
                            .orElse("(problem removed)");
                    return new PendingTestCase(tc.getId(), tc.getProblemId(), title,
                            tc.getInput(), tc.getExpectedOutput(), tc.getSubmittedBy());
                })
                .toList();
        return ResponseEntity.ok(pending);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return setStatus(id, "APPROVED");
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        return setStatus(id, "REJECTED");
    }

    private ResponseEntity<?> setStatus(Long id, String status) {
        Optional<TestCase> found = testCaseRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TestCase tc = found.get();
        tc.setStatus(status);
        return ResponseEntity.ok(testCaseRepository.save(tc));
    }
}