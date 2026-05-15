package com.energy_community_project.gui.service;

import java.text.DecimalFormat;

public class EnergyValueFormatter {

    private final DecimalFormat decimalFormat = new DecimalFormat("0.###");

    public String formatPercent(double value) {
        return decimalFormat.format(value);
    }

    public String formatKwh(double value) {
        return decimalFormat.format(value);
    }
}
