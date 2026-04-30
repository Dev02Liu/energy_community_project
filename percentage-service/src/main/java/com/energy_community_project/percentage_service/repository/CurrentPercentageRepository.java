package com.energy_community_project.percentage_service.repository;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CurrentPercentageRepository extends JpaRepository<CurrentPercentageEntity, LocalDateTime> {
}
