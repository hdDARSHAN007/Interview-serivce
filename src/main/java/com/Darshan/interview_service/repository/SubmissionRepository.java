package com.Darshan.interview_service.repository;

import com.Darshan.interview_service.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserEmail(String userEmail);

    boolean existsByUserEmailAndProblemIdAndVerdict(String userEmail, Long problemId, String verdict);
    long countByUserEmailAndProblemIdAndVerdictNot(String userEmail, Long problemId, String verdict);

    @Query("SELECT s.topic AS topic, " +
            "COUNT(s) AS attempts, " +
            "SUM(CASE WHEN s.verdict = 'ACCEPTED' THEN 1 ELSE 0 END) AS passed " +
            "FROM Submission s " +
            "WHERE s.userEmail = :email " +
            "GROUP BY s.topic")
    List<TopicStats> getTopicStatsForUser(@Param("email") String email);

    interface TopicStats {
        String getTopic();
        Long getAttempts();
        Long getPassed();
    }
}