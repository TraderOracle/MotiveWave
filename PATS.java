package TraderOracle;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@StudyHeader(
    namespace="com.DickInTheSpleen",
    id="PATS",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="PATS Trading System",
    label="PATS Trading System",
    desc="PATS Trading System",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class PATS extends Study
{
  //region PRIVATE VARS
  private static final Color RED = new Color(255, 0, 0, 255);
  private static final Color GREEN = new Color(0, 255, 0, 255);
  private static final Color WHITE = new Color(255, 255, 255, 255);
  private static final Color YELLOW = new Color(255, 255, 0, 255);
  private static final Color BLUE = new Color(120, 170, 250, 255);
  enum Signals { WICK, TOUCH }
  //endregion

  //region ON BAR CLOSE

  @Override
  public void onBarClose(DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int index = s.getEndIndex();

    //region CANDLE CALCULATIONS
    double close = s.getClose(index);
    double open =  s.getOpen(index);
    double pclose = s.getClose(index - 1);
    double popen =  s.getOpen(index - 1);
    double ppclose = s.getClose(index - 2);
    double ppopen =  s.getOpen(index - 2);
    double low = s.getLow(index);
    double plow = s.getLow(index - 1);
    double high = s.getHigh(index);
    double phigh = s.getHigh(index - 1);
    boolean c0G = close > s.getOpen(index);
    boolean c1G = s.getClose(index - 1) > s.getOpen(index - 1);
    boolean c2G = s.getClose(index - 2) > s.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = s.getClose(index - 1) < s.getOpen(index - 1);
    boolean c2R = s.getClose(index - 2) < s.getOpen(index - 2);
    double body = Math.abs(open - close);
    double pbody = Math.abs(s.getOpen(index - 1) - s.getClose(index - 1));
    boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
    boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
    boolean bDoji = bGDoji || bRDoji;
    //endregion

  }

  //endregion

  //region INITIALIZE

  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();

    var tab = sd.addTab("Lines");
    var grp = tab.addGroup("Alert on These");
    grp.addRow(new BooleanDescriptor("TOUCH21", "Alert EMA 21 touches", true));

    var grp2 = tab.addGroup("Markers");
    grp2.addRow(new MarkerDescriptor("UP", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
    grp2.addRow(new MarkerDescriptor("DOWN", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.WICK, "Line Wick");
    desc.declareSignal(Signals.TOUCH, "Line Touch");
  }

  @Override
  public void onSettingsUpdated(DataContext ctx)
  {
  }

  //endregion

  //region CANDLE COLORS

  private int FindLastRed(DataSeries s, int index, double ema)
  {
    for (int i = index - 1; i > 0; i--){
      Color ch = GetCandleColor(s, i);
      double ema21 = s.ema(i, 21, Enums.BarInput.CLOSE);
      if (ch == RED && s.getOpen(index) > s.getClose(i) && ema > ema21)
        return i;
    }
    return 0;
  }

  private Color GetCandleColor(DataSeries s, int index)
  {
    //region CANDLE CALCULATIONS
    double close = s.getClose(index);
    double open =  s.getOpen(index);
    double pclose = s.getClose(index - 1);
    double popen =  s.getOpen(index - 1);
    double ppclose = s.getClose(index - 2);
    double ppopen =  s.getOpen(index - 2);
    double low = s.getLow(index);
    double plow = s.getLow(index - 1);
    double high = s.getHigh(index);
    double phigh = s.getHigh(index - 1);
    //endregion

    if (high < phigh && low > plow){ // INSIDE BAR
      if (close > pclose && open > popen)
        return GREEN;
      else if (close < pclose && open < popen)
        return RED;
      else
        return YELLOW;
    }
    else if (high > phigh && low < plow) { // OUTSIDE BAR
      if (close > pclose && open > popen)
        return GREEN;
      else if (close > phigh || open > phigh)
        return GREEN;
      else if (close < pclose && open < popen)
        return RED;
      else if (close < plow || open < plow)
        return RED;
      else
        return BLUE;
    }
    else if (high > phigh){  // HIGHER
      return GREEN;
    }
    else if (low < plow){  // LOWER
      return RED;
    }
    return WHITE;
  }

  //endregion

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int last = s.size() - 1;
    if (s == null || index < 202 || !s.isBarComplete(index))
      return;

    //region CANDLE CALCULATIONS
    double close = s.getClose(index);
    double open =  s.getOpen(index);
    double pclose = s.getClose(index - 1);
    double popen =  s.getOpen(index - 1);
    double ppclose = s.getClose(index - 2);
    double ppopen =  s.getOpen(index - 2);
    double low = s.getLow(index);
    double plow = s.getLow(index - 1);
    double high = s.getHigh(index);
    double phigh = s.getHigh(index - 1);
    boolean c0G = close > s.getOpen(index);
    boolean c1G = s.getClose(index - 1) > s.getOpen(index - 1);
    boolean c2G = s.getClose(index - 2) > s.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = s.getClose(index - 1) < s.getOpen(index - 1);
    boolean c2R = s.getClose(index - 2) < s.getOpen(index - 2);
    double body = Math.abs(open - close);
    double pbody = Math.abs(s.getOpen(index - 1) - s.getClose(index - 1));
    boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
    boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
    boolean bDoji = bGDoji || bRDoji;
    //endregion

    Color cc = GetCandleColor(s, index);
    s.setPriceBarColor(index, cc);

    if (cc == RED) {
      double ema21 = s.ema(index, 21, Enums.BarInput.CLOSE);
      int ix = FindLastRed(s, index, ema21);
      debug("ix " + ix + " index " + index);
      if (ix > 0) {
        var marker = getSettings().getMarker("UP");
        Coordinate coords = new Coordinate(s.getStartTime(ix), (double) s.getLow(ix)-0.6);
        this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "ENGULFING CANDLE"));
      }
    }

  }

}











