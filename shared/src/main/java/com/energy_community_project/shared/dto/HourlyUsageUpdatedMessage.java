package com.energy_community_project.shared.dto;

import java.time.LocalDateTime;

public class HourlyUsageUpdatedMessage {
    private LocalDateTime hour;

    public HourlyUsageUpdatedMessage() {
    }

    public HourlyUsageUpdatedMessage(LocalDateTime hour) {
        this.hour = hour;
    }

    public LocalDateTime getHour() {
        return hour;
    }

    public void setHour(LocalDateTime hour) {
        this.hour = hour;
    }
}
