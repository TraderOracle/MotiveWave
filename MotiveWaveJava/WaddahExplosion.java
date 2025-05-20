package com.yourcompany.studies; // Change this to your package structure

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.Enums.*;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.indicator.*;
import com.motivewave.platform.sdk.study.*;

@StudyHeader(
    namespace = "com.yourcompany", // Change this
    id = "WAE",
    name = "Waddah Attar Explosion",
    label = "WAE",
    desc = "Waddah Attar Explosion indicator showing trend strength/direction and explosion volatility.",
    menu = "Waddah Attar", // Optional: Menu group in MotiveWave
    overlay = false, // Plots in its own panel below the price chart
    studyOverlay = true, // Allows plotting lines/histograms in its panel
    helpLink = "https://www.google.com/search?q=Waddah+Attar+Explosion+Indicator", // Or your own help link
    requiresVolume = false,
    requiresBarUpdates = true
)
public class WaddahAttarExplosion extends Study {

  // Define Input Parameters
  final static String FAST_MA_PERIOD = "fastMAPeriod";
  final static String SLOW_MA_PERIOD = "slowMAPeriod";
  final static String SIGNAL_PERIOD = "signalPeriod";
  final static String BB_PERIOD = "bbPeriod";
  final static String BB_STD_DEV = "bbStdDev";
  final static String ATR_PERIOD = "atrPeriod";
  final static String ATR_MULTIPLIER = "atrMultiplier";
  final static String INPUT = "input";

  // Define Output Paths (Values)
  enum Values {
    TREND_UP,    // Positive trend histogram
    TREND_DOWN,  // Negative trend histogram
    EXPLOSION,   // Explosion Line (BB Width)
    DEAD_ZONE    // Dead Zone Line (ATR based)
  }

  @Override
  public void initialize(Defaults defaults) {
    // Settings for the study panel and plots
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    // Input Descriptors
    SettingGroup inputs = new SettingGroup("Inputs");
    sd.addGroup(inputs);
    inputs.addRow(new InputDescriptor(INPUT, "Input", Inputs.CLOSE)); // Default to Close price
    inputs.addRow(new IntegerDescriptor(FAST_MA_PERIOD, "Fast MA Period", 20, 1, 9999, 1));
    inputs.addRow(new IntegerDescriptor(SLOW_MA_PERIOD, "Slow MA Period", 40, 1, 9999, 1));
    inputs.addRow(new IntegerDescriptor(SIGNAL_PERIOD, "Signal Period", 9, 1, 9999, 1));
    inputs.addRow(new IntegerDescriptor(BB_PERIOD, "Bollinger Band Period", 20, 1, 9999, 1));
    inputs.addRow(new DoubleDescriptor(BB_STD_DEV, "Bollinger Band StdDev", 2.0, 0.1, 10.0, 0.1));
    inputs.addRow(new IntegerDescriptor(ATR_PERIOD, "Dead Zone ATR Period", 20, 1, 9999, 1));
    inputs.addRow(new DoubleDescriptor(ATR_MULTIPLIER, "Dead Zone ATR Multiplier", 2.0, 0.1, 10.0, 0.1));


    // Plot Descriptors
    SettingGroup plots = new SettingGroup("Plots");
    sd.addGroup(plots);

    // Trend Up Histogram (Green)
    plots.addRow(new PathDescriptor(Values.TREND_UP, "Trend Up", defaults.getGreen(), PlotType.HISTOGRAM, LineStyle.SOLID, true));
    sd.addPathSetting(Values.TREND_UP, AvailablePlotType.HISTOGRAM, defaults.getGreen());
    sd.setPathShareAxis(Values.TREND_UP, SharedAxis.BOTTOM); // Share axis with Trend Down

    // Trend Down Histogram (Red)
    plots.addRow(new PathDescriptor(Values.TREND_DOWN, "Trend Down", defaults.getRed(), PlotType.HISTOGRAM, LineStyle.SOLID, true));
    sd.addPathSetting(Values.TREND_DOWN, AvailablePlotType.HISTOGRAM, defaults.getRed());
    sd.setPathShareAxis(Values.TREND_DOWN, SharedAxis.BOTTOM); // Share axis with Trend Up

    // Explosion Line (Yellow)
    plots.addRow(new PathDescriptor(Values.EXPLOSION, "Explosion Line", defaults.getYellow(), PlotType.LINE, LineStyle.SOLID, true));
    sd.addPathSetting(Values.EXPLOSION, AvailablePlotType.LINE, defaults.getYellow());
    sd.setPathShareAxis(Values.EXPLOSION, SharedAxis.BOTTOM); // Share axis with others

    // Dead Zone Line (Gray Dashed)
    plots.addRow(new PathDescriptor(Values.DEAD_ZONE, "Dead Zone Line", defaults.getGrey(), PlotType.LINE, LineStyle.DASH, true));
    sd.addPathSetting(Values.DEAD_ZONE, AvailablePlotType.LINE, defaults.getGrey());
    sd.setPathShareAxis(Values.DEAD_ZONE, SharedAxis.BOTTOM); // Share axis with others

    // Zero Line
    sd.addLine(new LineDescriptor(Values.TREND_UP, "Zero Line", 0, defaults.getLineColor(), LineStyle.SOLID, 1));

    // General settings
    sd.setRangeHints(MinimumPlotRange.ZERO, MaximumPlotRange.AUTO); // Values generally start from 0
    sd.setStudyPanelHeightRatio(0.3f); // Suggest panel height as 30% of chart

    // Set runtime properties (what happens when parameters change)
    setRuntimeDescriptor(new RuntimeDescriptor() {
      // Define lookback period based on the longest input period
      @Override
      public int getFixedLookback() {
        // Lookback depends on MACD, BB, and ATR calculations
        int fastPeriod = getSettings().getInteger(FAST_MA_PERIOD);
        int slowPeriod = getSettings().getInteger(SLOW_MA_PERIOD);
        int signalPeriod = getSettings().getInteger(SIGNAL_PERIOD);
        int bbPeriod = getSettings().getInteger(BB_PERIOD);
        int atrPeriod = getSettings().getInteger(ATR_PERIOD);

        // MACD lookback is roughly slowPeriod + signalPeriod
        int macdLookback = slowPeriod + signalPeriod -1; // Simplified, SDK handles exact needs
        // BB lookback is bbPeriod
        int bbLookback = bbPeriod -1;
        // ATR lookback is atrPeriod
        int atrLookback = atrPeriod -1; // Simplified

        // Return the maximum lookback required by any component
        return Util.maxInt(macdLookback, bbLookback, atrLookback) + 1; // Add 1 for safety/indexing
      }

      @Override
      public KahlmanFilter getKahlmanFilter() {
        return null; // No Kalman filtering needed
      }
    });
  }


  @Override
  protected void calculate(int index, DataContext ctx) {
    // Retrieve settings
    DataSeries series = ctx.getDataSeries();
    Object input = getSettings().getInput(INPUT);
    int fastPeriod = getSettings().getInteger(FAST_MA_PERIOD);
    int slowPeriod = getSettings().getInteger(SLOW_MA_PERIOD);
    int signalPeriod = getSettings().getInteger(SIGNAL_PERIOD);
    int bbPeriod = getSettings().getInteger(BB_PERIOD);
    double bbStdDev = getSettings().getDouble(BB_STD_DEV);
    int atrPeriod = getSettings().getInteger(ATR_PERIOD);
    double atrMultiplier = getSettings().getDouble(ATR_MULTIPLIER);

    // Ensure enough data points for calculation
    if (index < getRuntimeDescriptor().getFixedLookback()) {
      // Not enough data yet, set all to NaN (Not a Number)
      series.setDouble(index, Values.TREND_UP, Double.NaN);
      series.setDouble(index, Values.TREND_DOWN, Double.NaN);
      series.setDouble(index, Values.EXPLOSION, Double.NaN);
      series.setDouble(index, Values.DEAD_ZONE, Double.NaN);
      return;
    }

    // --- Calculate MACD Components ---
    // Using MotiveWave's built-in MACD function is efficient
    MACD macd = series.macd(index, input, fastPeriod, slowPeriod, signalPeriod);
    if (macd == null) { // Check if MACD calculation was successful
      series.setDouble(index, Values.TREND_UP, Double.NaN);
      series.setDouble(index, Values.TREND_DOWN, Double.NaN);
      series.setDouble(index, Values.EXPLOSION, Double.NaN); // Also set others to NaN if MACD fails
      series.setDouble(index, Values.DEAD_ZONE, Double.NaN);
      return;
    }
    double macdHistogram = macd.getHistogram(); // This is (MACD Line - Signal Line)


    // --- Calculate Trend Lines ---
    double trendUp = 0;
    double trendDown = 0;

    if (macdHistogram >= 0) {
      trendUp = macdHistogram;
      trendDown = 0; // Explicitly set to 0
    } else {
      trendUp = 0; // Explicitly set to 0
      trendDown = -macdHistogram; // Make it positive for histogram height
    }


    // --- Calculate Bollinger Bands Width (Explosion Line) ---
    BollingerBands bb = series.bb(index, input, bbPeriod, bbStdDev);
    double explosionLine = Double.NaN; // Default to NaN
    if (bb != null) {
      double upperBand = bb.getTop();
      double lowerBand = bb.getBottom();
      if (!Double.isNaN(upperBand) && !Double.isNaN(lowerBand)) {
        explosionLine = upperBand - lowerBand;
      }
    }


    // --- Calculate Dead Zone Line (ATR based) ---
    double atrValue = series.atr(index, atrPeriod);
    double deadZoneLine = Double.NaN; // Default to NaN
    if (!Double.isNaN(atrValue)) {
      deadZoneLine = atrValue * atrMultiplier;
    }


    // --- Set the calculated values for the current bar index ---
    series.setDouble(index, Values.TREND_UP, trendUp);
    series.setDouble(index, Values.TREND_DOWN, trendDown);
    series.setDouble(index, Values.EXPLOSION, explosionLine);
    series.setDouble(index, Values.DEAD_ZONE, deadZoneLine);

    // Optional: You could add logic here to color the explosion line based on trend
    // For example:
    // if (macdHistogram > 0) {
    //     series.setPathColor(index, Values.EXPLOSION, defaults.getGreen());
    // } else if (macdHistogram < 0) {
    //     series.setPathColor(index, Values.EXPLOSION, defaults.getRed());
    // } else {
    //     series.setPathColor(index, Values.EXPLOSION, defaults.getYellow()); // Neutral color
    // }
  }
}