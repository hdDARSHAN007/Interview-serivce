package com.Darshan.interview_service.dto;

import lombok.Data;

@Data
public class ProblemRequest {
    private String title;
    private String description;
    private String difficulty;
    private String topic;
    private String sampleInput;
    private String sampleOutput;
}