package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CurrentPercentageRepository extends JpaRepository<CurrentPercentageEntity, LocalDateTime> {
    Optional<CurrentPercentageEntity> findFirstByOrderByHourDesc();
}
