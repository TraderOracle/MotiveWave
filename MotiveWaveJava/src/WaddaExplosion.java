package TraderOracle;

import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;

@StudyHeader(
    namespace = "com.mycompany",
    id = "WaddahExplosion",
    name = "Waddah Explosion",
    menu = "TraderOracle",
    desc = "Waddah Explosion",
    overlay = false)

public class WaddaExplosion extends Study
{
  enum Values { MACD, SIGNAL, HIST_GREEN, HIST_RED };
  final static String HIST_INDG = "histIndG"; // Histogram Parameter
  final static String HIST_INDR = "histIndR"; // Histogram Parameter
  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);
  private static final Color YELLOW = new Color(255, 0, 0);

  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();
    var tab = sd.addTab("General");

    var grp = tab.addGroup("Inputs");
    grp.addRow(new InputDescriptor(Inputs.INPUT, "Input", Enums.BarInput.CLOSE));
    grp.addRow(new IntegerDescriptor(Inputs.PERIOD, "Period 1", 12, 1, 9999, 1));
    grp.addRow(new IntegerDescriptor(Inputs.PERIOD2, "Period 2", 26, 1, 9999, 1));
    grp.addRow(new IntegerDescriptor(Inputs.SIGNAL_PERIOD, "Signal Period", 9, 1, 9999, 1));

    tab = sd.addTab("Display");
    grp = tab.addGroup("Paths");
    grp.addRow(new PathDescriptor(Inputs.PATH, "MACD Path",
        defaults.getLineColor(), 1.5f, null, true, false, true));
    grp.addRow(new PathDescriptor(Inputs.SIGNAL_PATH, "Signal Path",
        defaults.getRed(), 1.0f, null, true, false, true));
    grp.addRow(new BarDescriptor(Inputs.BAR, "Bar Color",
        defaults.getBarColor(), true, true));
    grp = tab.addGroup("Indicators");
    grp.addRow(new IndicatorDescriptor(Inputs.IND, "MACD Ind",
        null, null, false, true, true));
    grp.addRow(new IndicatorDescriptor(Inputs.SIGNAL_IND, "Signal Ind",
        defaults.getRed(), null, false, false, true));
    grp.addRow(new IndicatorDescriptor(HIST_INDR, "Hist R",
        RED, null, false, false, true));
    grp.addRow(new IndicatorDescriptor(HIST_INDG, "Hist G",
        RED, null, false, false, true));

    var desc = createRD();
    desc.setMinTick(0.0001);
    desc.setLabelSettings(Inputs.INPUT, Inputs.PERIOD, Inputs.PERIOD2, Inputs.SIGNAL_PERIOD);
    desc.exportValue(new ValueDescriptor(Values.MACD, "MACD",
        new String[] { Inputs.INPUT, Inputs.PERIOD, Inputs.PERIOD2 }));
    desc.exportValue(new ValueDescriptor(Values.SIGNAL, "MACD Signal",
        new String[] { Inputs.SIGNAL_PERIOD}));
    desc.exportValue(new ValueDescriptor(Values.HIST_GREEN, "Hist Green",
        new String[] { Inputs.PERIOD, Inputs.PERIOD2, Inputs.SIGNAL_PERIOD }));
    desc.exportValue(new ValueDescriptor(Values.HIST_RED, "Hist Red",
        new String[] { Inputs.PERIOD, Inputs.PERIOD2, Inputs.SIGNAL_PERIOD }));
    // There are two paths, the MACD path and the Signal path
    desc.declarePath(Values.MACD, Inputs.PATH);
    desc.declarePath(Values.SIGNAL, Inputs.SIGNAL_PATH);
    // Bars displayed as the histogram
    desc.declareBars(Values.HIST_GREEN, Inputs.BAR);
    desc.declareBars(Values.HIST_RED, Inputs.BAR);
    // These are the indicators that are displayed in the vertical axis
    desc.declareIndicator(Values.MACD, Inputs.IND);
    desc.declareIndicator(Values.SIGNAL, Inputs.SIGNAL_IND);
    desc.declareIndicator(Values.HIST_GREEN, HIST_INDG);
    desc.declareIndicator(Values.HIST_RED, HIST_INDR);
    // Display a 'Zero' line that is dashed.
    desc.addHorizontalLine(new LineInfo(0, null, 1.0f, new float[] {3,3}));
  }

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    int fastPeriod = 20;
    int slowPeriod = 40;
    int channelPeriod = 20;
    double bbStdDev = 2;
    double sensitivity = 150;
    double deadZoneValue = 25;

    Object input = getSettings().getInput(Inputs.INPUT);
    var series = ctx.getDataSeries();

    Double fastEMA = series.ema(index, fastPeriod, Enums.BarInput.CLOSE);
    Double slowEMA = series.ema(index, slowPeriod, Enums.BarInput.CLOSE);
    if (fastEMA == null || slowEMA == null)
      return;

    double trendValue = (fastEMA - slowEMA) * sensitivity;
    double middleBB = series.sma(index , 20, input);
    double stdDev = series.std(index, 20, input);
    double upperBB = middleBB + (2 * stdDev);
    double lowerBB = middleBB - (2 * stdDev);
    double explosionValue = (upperBB - lowerBB) * sensitivity;

    // Define the MACD value for this index
    //double MACD = MA1 - MA2;
    //debug("Setting MACD value for index: " + index + " MACD: " + MACD);
    //series.setDouble(index, Values.MACD, MACD);

    //int signalPeriod = getSettings().getInteger(Inputs.SIGNAL_PERIOD);
    //if (index < period + signalPeriod) return; // Not enough data yet

    // Calculate moving average of MACD (signal path)
    //Double signal = series.sma(index, signalPeriod, Values.MACD);
    //series.setDouble(index, Values.SIGNAL, signal);
    //if (signal == null) return;

    // Histogram is the difference between the MACD and the signal path
    //series.setDouble(index, Values.HIST_GREEN, explosionValue);
    series.setDouble(index, Values.HIST_RED, explosionValue);
    series.setComplete(index);
  }
}
