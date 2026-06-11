package com.energy_community_project.gui.controller;

import com.energy_community_project.gui.controller.EnergyDashboardController.UsageSummary;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsageSummaryTest {

    @Test
    void summarize_addsUpEachColumn() {
        UsageSummary summary = EnergyDashboardController.summarize(List.of(
                entry(10.0, 8.0, 2.0),
                entry(5.0, 4.0, 1.0)
        ));

        assertThat(summary.produced()).isEqualTo(15.0);
        assertThat(summary.used()).isEqualTo(12.0);
        assertThat(summary.gridUsed()).isEqualTo(3.0);
    }

    @Test
    void summarize_emptyList_returnsZeros() {
        UsageSummary summary = EnergyDashboardController.summarize(List.of());

        assertThat(summary.produced()).isZero();
        assertThat(summary.used()).isZero();
        assertThat(summary.gridUsed()).isZero();
    }

    private HistoricalUsageDTO entry(double produced, double used, double gridUsed) {
        HistoricalUsageDTO dto = new HistoricalUsageDTO();
        dto.setCommunityProduced(produced);
        dto.setCommunityUsed(used);
        dto.setGridUsed(gridUsed);
        return dto;
    }
}
