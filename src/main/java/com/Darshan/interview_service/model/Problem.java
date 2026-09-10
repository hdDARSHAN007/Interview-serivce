package com.Darshan.interview_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String functionSignature; // e.g. "def two_sum(nums, target):"

    @Column(columnDefinition = "TEXT")
    private String driverCode; // hidden — reads stdin, calls the function, prints result

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulty; // "EASY", "MEDIUM", "HARD"

    private String topic; // "Arrays", "DP", "Graphs", "Recursion" etc.

    @Column(columnDefinition = "TEXT")
    private String sampleInput;

    @Column(columnDefinition = "TEXT")
    private String sampleOutput;

    @Column(columnDefinition = "TEXT")
    private String starterCodeJava;

    @Column(columnDefinition = "TEXT")
    private String driverPrefixJava;

    @Column(columnDefinition = "TEXT")
    private String driverSuffixJava;


    @Column(columnDefinition = "TEXT")
    private String starterCodeCpp;

    @Column(columnDefinition = "TEXT")
    private String driverPrefixCpp;

    @Column(columnDefinition = "TEXT")
    private String driverSuffixCpp;
}
