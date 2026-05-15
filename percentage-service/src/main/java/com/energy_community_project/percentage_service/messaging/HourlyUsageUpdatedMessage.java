package com.energy_community_project.percentage_service.messaging;

import java.time.LocalDateTime;

public class HourlyUsageUpdatedMessage {

    private LocalDateTime hour;

    public HourlyUsageUpdatedMessage() {
    }

    public LocalDateTime getHour() {
        return hour;
    }

    public void setHour(LocalDateTime hour) {
        this.hour = hour;
    }
}
