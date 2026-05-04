package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CurrentPercentageRepository extends JpaRepository<CurrentPercentageEntity, LocalDateTime> {
    CurrentPercentageEntity findFirstByOrderByHourDesc();
}
