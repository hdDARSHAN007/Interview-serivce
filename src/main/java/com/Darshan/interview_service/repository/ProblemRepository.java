package com.Darshan.interview_service.repository;

import com.Darshan.interview_service.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByTopic(String topic);
    List<Problem> findByDifficulty(String difficulty);
}