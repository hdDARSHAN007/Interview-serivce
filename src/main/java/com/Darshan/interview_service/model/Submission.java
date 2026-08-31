package com.Darshan.interview_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail; // who submitted — comes from JWT later, hardcoded for now

    private Long problemId;

    private String topic; // copied from Problem, for fast analytics later

    @Column(columnDefinition = "TEXT")
    private String code;

    private String language;

    private String verdict; // "ACCEPTED", "WRONG_ANSWER", "RUNTIME_ERROR", "COMPILE_ERROR" etc.

    @Column(columnDefinition = "TEXT")
    private String output;

    private LocalDateTime submittedAt = LocalDateTime.now();
}