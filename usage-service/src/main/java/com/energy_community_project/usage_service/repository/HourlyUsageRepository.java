package com.energy_community_project.usage_service.repository;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface HourlyUsageRepository extends JpaRepository<HourlyUsageEntity, LocalDateTime> {
}
