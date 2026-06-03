package com.energy_community_project.gui.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyGuiApplicationBaseUrlTest {

    private static final String PROPERTY = "energy.api.base-url";

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void resolveBaseUrl_noOverride_returnsLocalhostDefault() {
        System.clearProperty(PROPERTY);
        assertThat(EnergyGuiApplication.resolveBaseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void resolveBaseUrl_systemProperty_overridesDefault() {
        System.setProperty(PROPERTY, "http://example.test:9090");
        assertThat(EnergyGuiApplication.resolveBaseUrl()).isEqualTo("http://example.test:9090");
    }

    @Test
    void resolveBaseUrl_blankSystemProperty_fallsBackToDefault() {
        System.setProperty(PROPERTY, "   ");
        assertThat(EnergyGuiApplication.resolveBaseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void resolveBaseUrl_systemProperty_isTrimmed() {
        System.setProperty(PROPERTY, "  http://trimmed.test:8081  ");
        assertThat(EnergyGuiApplication.resolveBaseUrl()).isEqualTo("http://trimmed.test:8081");
    }
}
