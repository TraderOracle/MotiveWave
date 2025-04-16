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
    id="CandyCandles",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="Candy Candles",
    label="Candy Candles",
    desc="Candy Candles",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class CandleCandies extends Study
{
  private static final Color RED = new Color(255, 0, 0, 255);
  private static final Color GREEN = new Color(0, 255, 0, 255);
  private static final Color WHITE = new Color(255, 255, 255, 255);
  private static final Color YELLOW = new Color(255, 255, 0, 255);
  private static final Color BLUE = new Color(120, 170, 250, 255);
  enum Signals { WICK, TOUCH }

  //region ON BAR CLOSE

  @Override
  public void onBarClose(DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int index = s.getEndIndex();

    // CANDLE CALCULATIONS
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

    var tab2 = sd.addTab("Lines");
    var grp3 = tab2.addGroup("Alert on These");
    grp3.addRow(new BooleanDescriptor("TOUCH21", "Alert EMA 21 touches", true));
    grp3.addRow(new BooleanDescriptor("TOUCH200", "Alert EMA 200 touches", true));
    grp3.addRow(new BooleanDescriptor("TOUCHVWAP", "Alert VWAP touches", true));
    grp3.addRow(new BooleanDescriptor("WICK21", "Alert EMA 21 WICKs", true));
    grp3.addRow(new BooleanDescriptor("WICK200", "Alert EMA 200 WICKs", true));
    grp3.addRow(new BooleanDescriptor("WICKVWAP", "Alert VWAP WICKs", true));

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

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    var s = ctx.getDataSeries();

    int last = s.size() - 1;
    if (s == null || index < 202 || !s.isBarComplete(index))
      return;

    // CANDLE CALCULATIONS
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

    if (high < phigh && low > plow){
      s.setPriceBarColor(index, YELLOW);
    }
    else if (high > phigh){
      s.setPriceBarColor(index, GREEN);
    }
    else if (low < plow){
      s.setPriceBarColor(index, RED);
    }
    else{
      s.setPriceBarColor(index, BLUE);
    }

  }

}