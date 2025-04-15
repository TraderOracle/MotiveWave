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
    id="BoinkBuzzer",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="Boink Buzzer",
    label="Boink Buzzer",
    desc="Boink Buzzer",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class BoinkBuzzer extends Study
{
  //region VARIABLES
  enum Values { UP, DOWN, TREND }
  //enum Signals { KP_WICK, KP_TOUCH, TS_WICK,TS_TOUCH, MQ_WICK, MQ_TOUCH, MAN_WICK, MAN_TOUCH }
  enum Signals { WICK, TOUCH }

  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);
  private static final Color YELLOW = new Color(255, 0, 0);

  private Map<Double, String> kpMap;
  private Map<Double, String> mqMap;
  private Map<Double, String> bsMap;
  private List<RangeEntry> tsRange;
  private int currIndex = 0;
  private int gIndex = 0;
  private String sMsg = "";

  public static class RangeEntry {
    private final double start;
    private final double end;
    private final String text;

    public RangeEntry(double start, double end, String text) {
      this.start = start;
      this.end = end;
      this.text = text;
    }

    public boolean between(double number) {
      return number >= start && number <= end;
    }

    public String getText() {
      return text;
    }
  }

  //endregion

  //region ON BAR CLOSE

  @Override
  public void onBarClose(DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int index = s.getEndIndex();

    this.clearFigures();
    Box bx = new Box();
    addFigure(bx);

    boolean bTouch200 = getSettings().getBoolean("TOUCH200");
    boolean bTouch21 = getSettings().getBoolean("TOUCH21");
    boolean bTouchVWAP = getSettings().getBoolean("TOUCHVWAP");
    boolean bTouchKAMA = getSettings().getBoolean("TOUCHKAMA");

    boolean bWick200 = getSettings().getBoolean("WICK200");
    boolean bWick21 = getSettings().getBoolean("WICK21");
    boolean bWickVWAP = getSettings().getBoolean("WICKVWAP");
    boolean bWickKAMA = getSettings().getBoolean("WICKKAMA");

    double close = s.getClose();
    double open = s.getOpen();
    double hi = s.getHigh();
    double lo = s.getLow();
    boolean c0G = s.getClose() > s.getOpen();
    boolean c0R = s.getClose() < s.getOpen();
    double kama = s.kama(index, 9, Enums.BarInput.CLOSE);
    //float dVWAP = s.getVWAP();
    double ema21 = s.ema(index, 21, Enums.BarInput.CLOSE);
    double ema200 = s.ema(index, 200, Enums.BarInput.CLOSE);

    if (bWick21 || bTouch21){
      String sa = getTouch(ctx, "EMA 21", ema21, hi, lo, open, close);
      if (sa != "") {
        ctx.signal(index, Signals.TOUCH, sa, close);
        return;
      }
    }

    if (bWick200 || bTouch200){
      String sa = getTouch(ctx, "EMA 200", ema21, hi, lo, open, close);
      if ( sa != "") {
        ctx.signal(index, Signals.TOUCH, sa, close);
        return;
      }
    }

    if (bWickVWAP || bTouchVWAP){
     // String sa = getTouch(ctx, "VWAP", ema21, hi, lo, open, close);
     // if ( sa != "") {
     //   ctx.signal(index, Signals.TOUCH, sa, close);
     //   return;
     // }
    }

    if (bWickKAMA || bTouchKAMA){
      String sa = getTouch(ctx, "KAMA", kama, hi, lo, open, close);
      if (sa != "") {
        ctx.signal(index, Signals.TOUCH, sa, close);
        return;
      }
    }

  }

  //endregion

  private class Box extends Figure
  {
    @Override
    public void draw(Graphics2D gc, DrawContext ctx)
    {
      try {
        Font font = new Font("Dialog", Font.PLAIN, 14);
        gc.setFont(font);
        gc.setColor(WHITE);
        gc.drawString(sMsg, 450, 40);
      } catch (java.lang.Exception e){}
    }
  }

  //region TOUCH LOGIC

  private String getTouch(DataContext ctx, String sLine, double line, double high, double low, double open, double close) {
    boolean c0G = close > open;
    boolean c0R = close < open;

    if (high > line && low < line) {
      return " - Touched " + sLine;
    } else if ((c0G && high > line && close < line) || (c0G && low < line && open > line) ||
        (c0R && high > line && open < line) || (c0R && low < line && close > line)) {
      return " - WICK off " + sLine;
    }
    return "";
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
    var series = ctx.getDataSeries();

    int last = series.size() - 1;
    if (series == null || index < 202 || !series.isBarComplete(index))
      return;

    // CANDLE CALCULATIONS
    double close = series.getClose(index);
    double open =  series.getOpen(index);
    double pclose = series.getClose(index - 1);
    double popen =  series.getOpen(index - 1);
    double ppclose = series.getClose(index - 2);
    double ppopen =  series.getOpen(index - 2);
    double clow = series.getLow(index);
    double plow = series.getLow(index - 1);
    double high = series.getHigh(index);
    double phigh = series.getHigh(index - 1);
    boolean c0G = close > series.getOpen(index);
    boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
    boolean c2G = series.getClose(index - 2) > series.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
    boolean c2R = series.getClose(index - 2) < series.getOpen(index - 2);
    double body = Math.abs(open - close);
    double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));
    boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
    boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
    boolean bDoji = bGDoji || bRDoji;
    //endregion

  }

}