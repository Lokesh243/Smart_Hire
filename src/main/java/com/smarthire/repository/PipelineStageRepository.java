package com.smarthire.repository;

import com.smarthire.entity.Application;
import com.smarthire.entity.PipelineStage;
import com.smarthire.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

    List<PipelineStage> findByApplicationOrderByMovedAtAsc(Application application);

    Optional<PipelineStage> findTopByApplicationOrderByMovedAtDesc(Application application);

    List<PipelineStage> findByStage(ApplicationStatus stage);
}