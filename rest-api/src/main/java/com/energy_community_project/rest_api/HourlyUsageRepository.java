package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.HourlyUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HourlyUsageRepository extends JpaRepository<HourlyUsageEntity, LocalDateTime> {
    List<HourlyUsageEntity> findByHourBetween(LocalDateTime start, LocalDateTime end);
}
