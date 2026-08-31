package com.Darshan.interview_service.dto;

import lombok.Data;

@Data
public class SubmissionRequest {
    private Long problemId;
    private String code;
    private String language; // "java", "python", "cpp"
}