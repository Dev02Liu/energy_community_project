package com.energy_community_project.gui.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class EnergyValueFormatter {

    private final DecimalFormat decimalFormat = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.ENGLISH));

    public String formatPercent(double value) {
        return decimalFormat.format(value);
    }

    public String formatKwh(double value) {
        return decimalFormat.format(value);
    }
}
