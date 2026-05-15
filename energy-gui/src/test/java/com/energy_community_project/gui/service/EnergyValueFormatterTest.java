package com.energy_community_project.gui.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyValueFormatterTest {

    private final EnergyValueFormatter formatter = new EnergyValueFormatter();

    @Test
    void formatPercent_wholeNumber_omitsDecimalPoint() {
        assertThat(formatter.formatPercent(100.0)).isEqualTo("100");
    }

    @Test
    void formatPercent_twoDecimalPlaces_formatsCorrectly() {
        assertThat(formatter.formatPercent(5.63)).isEqualTo("5.63");
    }

    @Test
    void formatPercent_zero_returnsZero() {
        assertThat(formatter.formatPercent(0.0)).isEqualTo("0");
    }

    @Test
    void formatKwh_withTwoDecimals_formatsCorrectly() {
        assertThat(formatter.formatKwh(18.05)).isEqualTo("18.05");
    }

    @Test
    void formatKwh_zero_returnsZero() {
        assertThat(formatter.formatKwh(0.0)).isEqualTo("0");
    }

    @Test
    void formatKwh_wholeNumber_omitsDecimalPoint() {
        assertThat(formatter.formatKwh(100.0)).isEqualTo("100");
    }

    @Test
    void formatKwh_threeDecimalPlaces_formatsCorrectly() {
        assertThat(formatter.formatKwh(1.076)).isEqualTo("1.076");
    }

    @Test
    void formatKwh_trailingZerosAreDropped() {
        assertThat(formatter.formatKwh(18.10)).isEqualTo("18.1");
    }
}
