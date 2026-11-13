package com.Darshan.interview_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long problemId;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    private boolean isSample; // true = shown to students, false = hidden

    // PENDING (waiting for the admin), APPROVED (used for grading) or REJECTED
    private String status = "PENDING";

    private String submittedBy; // email of the person who added it
}