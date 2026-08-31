package com.Darshan.interview_service.controller;

import com.Darshan.interview_service.dto.ProblemRequest;
import com.Darshan.interview_service.model.Problem;
import com.Darshan.interview_service.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Darshan.interview_service.dto.TestCaseRequest;
import com.Darshan.interview_service.model.TestCase;
import com.Darshan.interview_service.repository.TestCaseRepository;


import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    @Autowired
    private ProblemRepository problemRepository;

    // Create a new problem
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

    @Autowired
    private TestCaseRepository testCaseRepository;

    @PostMapping("/{id}/testcases")
    public ResponseEntity<?> addTestCase(@PathVariable Long id, @RequestBody TestCaseRequest req) {
        TestCase tc = new TestCase();
        tc.setProblemId(id);
        tc.setInput(req.getInput());
        tc.setExpectedOutput(req.getExpectedOutput());
        tc.setSample(req.isSample());
        return ResponseEntity.ok(testCaseRepository.save(tc));
    }

    @GetMapping("/{id}/testcases")
    public ResponseEntity<?> getTestCases(@PathVariable Long id) {
        return ResponseEntity.ok(testCaseRepository.findByProblemId(id));
    }
}