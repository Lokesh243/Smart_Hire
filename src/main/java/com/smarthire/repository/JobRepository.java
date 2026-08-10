package com.smarthire.repository;

import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {

    List<Job> findByStatus(JobStatus status);

    List<Job> findByPostedBy(User postedBy);

    List<Job> findByStatusAndExpiresAtBefore(JobStatus status, LocalDate date);
}