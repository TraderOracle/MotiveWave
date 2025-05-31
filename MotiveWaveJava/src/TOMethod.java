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
    namespace="com.DickInTheSpleen",
    id="TOMethod2",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="TraderOracle Method",
    label="TraderOracle Method",
    desc="TraderOracle Method",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class TOMethod extends Study
{
  //region VARIABLES
  enum Values { MA, MA2, MA3, MA1, MOMENTUM, BB_KC_DIFF, MACD, SIGNAL, HIST }
  enum Signals { ENG_BB, BOINK, CANDLEOVERCANDLE, VOLIMB_CREATE, VOLIMB_FILL }

  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);
  private static final Color YELLOW = new Color(255, 0, 0);

  private List<Figure> al = new ArrayList<>();
  private boolean bDrawn = false;
  private int pIndex = 0;
  //endregion

  //region INITIALIZE AND MISC
  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();

    var tabQ = sd.addTab("Settings");
    var grpQ = tabQ.addGroup("Inputs");
    grpQ.addRow(new BooleanDescriptor("ShowEngBB", "Show Engulfing off BB", true));
    grpQ.addRow(new BooleanDescriptor("CANDLEBB", "Show Bollinger Push Off", true));
    grpQ.addRow(new BooleanDescriptor("STDBOINK", "Show Standard Boinks", true));
    grpQ.addRow(new BooleanDescriptor("DOUBLEWICKBOINK", "Show Double Wick Boinks", true));
    //grpQ.addRow(new BooleanDescriptor("3RUNUP", "Show Triple Candle Run-up", false));

    var tab2 = sd.addTab("Bollinger Push Off");
    var grp2 = tab2.addGroup("Markers");
    grp2.addRow(new MarkerDescriptor("UPCANDLEBBMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL,
        defaults.getGreen(), defaults.getLineColor(), true, true));
    grp2.addRow(new MarkerDescriptor("DOWNCANDLEBBMarker", "Down Marker", Enums.MarkerType.TRIANGLE,
        Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

    var tab = sd.addTab("BOINK");
    var grp = tab.addGroup("Inputs");

    grp.addRow(new BooleanDescriptor("UseKAMA", "Use KAMA", true));
    grp.addRow(new IntegerDescriptor("KAMAPeriod", "KAMA Period", 9, 1, 9999, 1));
    grp.addRow(new BooleanDescriptor("StdEMA", "Use Standard EMA", false));
    grp.addRow(new IntegerDescriptor("EMAPeriod", "EMA Period", 21, 1, 9999, 1));
    grp.addRow(new BooleanDescriptor("BigEMA", "Use Larger Ema", true));
    grp.addRow(new IntegerDescriptor("LargerEMAPeriod", "Larger EMA Period", 200, 1, 9999, 1));

    grp = tab.addGroup("Markers");
    grp.addRow(new MarkerDescriptor("UPBOINKMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
    grp.addRow(new MarkerDescriptor("DOWNBOINKMarker", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

    var tab1=sd.addTab("Engulfing BB");
    var grp1=tab1.addGroup("Inputs");
    grp1.addRow(new BooleanDescriptor("ColorCandle", "Color candle white", true));

    grp1 = tab1.addGroup("Markers");
    grp1.addRow(new MarkerDescriptor("UPEngBBMarker", "Up Marker",
        Enums.MarkerType.CIRCLE, Enums.Size.SMALL,
        defaults.getGreen(), defaults.getLineColor(), true, true));
    grp1.addRow(new MarkerDescriptor("DOWNEngBBMarker", "Down Marker",
        Enums.MarkerType.CIRCLE, Enums.Size.SMALL,
        defaults.getRed(), defaults.getLineColor(), true, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.BOINK, "BOINK");
    desc.declareSignal(Signals.ENG_BB, "Engulfing Candle Off BB");
    desc.declareSignal(Signals.CANDLEOVERCANDLE, "Bollinger Push Off");
  }
  //endregion

  //region CHECK FOR BOINK
  private int CheckForBoink(DataSeries series, int index, Object input)
  {
    boolean bStdBoink = getSettings().getBoolean("STDBOINK");
    boolean bDoubleWick = getSettings().getBoolean("DOUBLEWICKBOINK");
    boolean b3CandleRunup = getSettings().getBoolean("3RUNUP");

    boolean bUseKama = getSettings().getBoolean("UseKAMA");
    boolean bStdEMA = getSettings().getBoolean("StdEMA");
    int iKamaPeriod = getSettings().getInteger("KAMAPeriod");
    int iEMAPeriod = getSettings().getInteger("EMAPeriod");

    double close = series.getClose(index);
    double open =  series.getOpen(index);
    double pclose = series.getClose(index - 1);
    double popen =  series.getOpen(index - 1);
    double ppclose = series.getClose(index - 2);
    double ppopen =  series.getOpen(index - 2);
    double clow = series.getLow(index);
    double plow = series.getLow(index - 1);
    double pplow = series.getLow(index - 2);
    double high = series.getHigh(index);
    double phigh = series.getHigh(index - 1);
    double pphigh = series.getHigh(index - 2);
    boolean c0G = close > series.getOpen(index);
    boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
    boolean c2G = series.getClose(index - 2) > series.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
    boolean c2R = series.getClose(index - 2) < series.getOpen(index - 2);
    double body = Math.abs(open - close);
    double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));
    double kama = series.kama(index, iKamaPeriod, input);
    double ema21 = series.ema(index, iEMAPeriod, input);
    double ema200 = series.ema(index, 200, input);
    boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
    boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
    boolean bDoji = bGDoji || bRDoji;

    double myEMA = ema21;
    if (bUseKama)
      myEMA = kama;

    // Within High/Low
    boolean bInHiLo = clow < myEMA && high > myEMA;
    boolean bGClose = close > myEMA;
    boolean bRClose = close < myEMA;
    boolean bPrevInHiLo = plow < myEMA && phigh > myEMA;
    boolean bPrevHi = (ppclose > pclose || pphigh > phigh);
    boolean bPrevLo = (ppclose < pclose || pplow < plow);
    boolean bEmaHug =
    (c0R && close < myEMA && open > myEMA) || (c0G && close > myEMA && open < myEMA) &&
    (c1R && pclose < myEMA && popen > myEMA) || (c1G && pclose > myEMA && popen < myEMA) &&
    (c2R && ppclose < myEMA && ppopen > myEMA) || (c2G && ppclose > myEMA && ppopen < myEMA);
    // Within candle body
    boolean bInBODY = (close < myEMA && open > myEMA) || (close > myEMA && open < myEMA);

    double upWick = c0R ? Math.abs(high-open) : Math.abs(high-close); 
    double dnWick = c0R ? Math.abs(close-clow) : Math.abs(open-clow); 

    boolean upWickBigger = upWick > body; 
    boolean dnWickBigger = dnWick > body; 

    if (dnWickBigger && clow < ema200 && close > ema200 && open > ema200)
      return 1;
    if (upWickBigger && high > ema200 && close > ema200 && open > ema200)
      return -1;

    if (false) // (bStdBoink)
    {  // Green, prev RED.  In EMA, Prev candle higher
      if (c0G && c1R && bInHiLo && bGClose)
        return 1;
      if (c0R && c1G && bInHiLo && bRClose)
        return -1;
    }

    if (bStdBoink && !bEmaHug)
    {  // Green, prev RED, prevprev RED. In EMA, Prev candle higher
      if (popen > myEMA && bGClose && c0G && c1R && c2R && bInHiLo && bPrevHi)
        return 1;
      if (popen < myEMA && bRClose && c0R && c1G && c2R && bInHiLo && bPrevLo)
        return -1;
    }

    if (bDoubleWick && !bEmaHug)
    {   //  green  prev red  WICK  PWICK
      if (c0G && c1R && open > myEMA && clow < myEMA && pphigh > phigh)
        return 1;
      if (c0R && c1G && open < myEMA && high > myEMA && pplow < plow)
        return -1;
      //(c0G && bGDoji && clow < myEMA && close > myEMA && phigh < myEMA)
    }

    if (b3CandleRunup)
    {
      if (c0G && c1G && c2G && open <= pclose && popen <= ppclose && bInHiLo)
        return 1;
    }

    return 0;
  }
  //endregion

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    var series = ctx.getDataSeries();
    var s = ctx.getDataSeries();

    int last = series.size() - 1;
    Object input = getSettings().getInput(Inputs.INPUT);
    if (series == null || index < 202 || !series.isBarComplete(index)) return;

    //region CANDLE CALCS

    // GET CONFIG SETTINGS
    boolean bBBPushOff = getSettings().getBoolean("CANDLEBB");
    boolean bShowBOINK = getSettings().getBoolean("STDBOINK");
    boolean bUseKama = getSettings().getBoolean("KAMAPeriod");
    boolean bUseStdEMA = getSettings().getBoolean("StdEMA");
    boolean bUseBigEMA = getSettings().getBoolean("BigEMA");
    int iKAMAPeriod = getSettings().getInteger("KAMAPeriod");
    int iEMAPeriod = getSettings().getInteger("EMAPeriod");
    int iLargerEMAPeriod = getSettings().getInteger("LargerEMAPeriod");
    boolean bShowEngBB = getSettings().getBoolean("ShowEngBB");
    boolean bColorCandle = getSettings().getBoolean("ColorCandle");

    // CANDLE CALCULATIONS
    double close = series.getClose(index);
    double open =  series.getOpen(index);
    double pclose = series.getClose(index - 1);
    double popen =  series.getOpen(index - 1);
    double ppclose = series.getClose(index - 2);
    double ppopen =  series.getOpen(index - 2);
    double clow = series.getLow(index);
    double plow = series.getLow(index - 1);
    double pplow = series.getLow(index - 2);
    double high = series.getHigh(index);
    double phigh = series.getHigh(index - 1);
    double pphigh = series.getHigh(index - 2);
    boolean c0G = close > series.getOpen(index);
    boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
    boolean c2G = series.getClose(index - 2) > series.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
    boolean c2R = series.getClose(index - 2) < series.getOpen(index - 2);
    double body = Math.abs(open - close);
    double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));
    double kama = series.kama(index, iKAMAPeriod, input);
    double ema21 = series.ema(index, iEMAPeriod, input);
    double ema200 = series.ema(index, iLargerEMAPeriod, input);
    boolean bGDoji = c1G && pbody < phigh - pclose && pbody < popen - plow;
    boolean bRDoji = c1R && pbody < phigh - popen && pbody < pclose - plow;
    boolean bDoji = bGDoji || bRDoji;
    double myEMA = kama;
    if (bUseStdEMA)
      myEMA = ema21;

    // Calculate Bollinger Bands
    double middleBB = series.sma(index , 20, input);
    double pmiddleBB = series.sma(index-1, 20, input);
    double ppmiddleBB = series.sma(index-2, 20, input);
    double stdDev = series.std(index, 20, input);
    double upperBB = middleBB + (2 * stdDev);
    double pupperBB = pmiddleBB + (2 * stdDev);
    double ppupperBB = ppmiddleBB + (2 * stdDev);
    double lowerBB = middleBB - (2 * stdDev);
    double plowerBB = pmiddleBB - (2 * stdDev);
    double pplowerBB = ppmiddleBB - (2 * stdDev);
    //endregion

    //region BOINKS

    if (bShowBOINK){
      if (CheckForBoink(series, index, input) == 1) // GREEN
      {
        var marker = getSettings().getMarker("UPBOINKMarker");
        Coordinate coords = new Coordinate(series.getStartTime(index), (double) clow);
        this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "BOINK"));
        ctx.signal(index, Signals.BOINK, "BOINK", close);
        return;
      }
      if (CheckForBoink(series, index, input) == -1) // RED
      {
        var marker = getSettings().getMarker("DOWNBOINKMarker");
        Coordinate coords = new Coordinate(series.getStartTime(index), (double) high);
        this.addFigure(new Marker(coords, Enums.Position.TOP, marker, "BOINK"));
        ctx.signal(index, Signals.BOINK, "BOINK", close);
        return;
      }
    }

    //endregion

    //region BOLLINGER PUSH OFF
    if (bBBPushOff && plow <= plowerBB && clow <= lowerBB && c0G && c1R)
    {
      var marker = getSettings().getMarker("UPCANDLEBBMarker");
      Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getLow(index)  );
      this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "Bollinger Push Off"));
      ctx.signal(index, Signals.CANDLEOVERCANDLE, "Bollinger Push Off", close);
      return;
    }

    if (bBBPushOff && phigh >= pupperBB && high >= upperBB && c0R && c1G)
    {
      var marker = getSettings().getMarker("DOWNCANDLEBBMarker");
      Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getHigh(index)  );
      this.addFigure(new Marker(coords, Enums.Position.TOP, marker, "Bollinger Push Off"));
      ctx.signal(index, Signals.CANDLEOVERCANDLE, "Bollinger Push Off", close);
      return;
    }
    //endregion

    //region ENGULFING CANDLE
    if ((clow < lowerBB || plow < lowerBB) && body > pbody && c1R && bShowEngBB && c0G)
    {
      var marker = getSettings().getMarker("UPEngBBMarker");
      Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getLow(index));
      this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "ENGULFING CANDLE"));
      ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
      if (bColorCandle)
        series.setPriceBarColor(index, WHITE);
      return;
    }

    if ((high > upperBB || phigh > upperBB) && body > pbody && bShowEngBB && c1G && c0R)
     { 
      var marker = getSettings().getMarker("DOWNEngBBMarker");
      int iStart = series.getStartIndex();
      int iEnd = series.getEndIndex();
      Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getHigh(index)  );
      Coordinate coordsEnd = new Coordinate(series.getStartTime(index-100), (double) series.getHigh(index) + 2.2);
      this.addFigure(new Marker(coords, Enums.Position.TOP, marker, "ENGULFING CANDLE"));

      /*
      Box bx = new Box(coords, coordsEnd);
      bx.setFillColor(RED);
      bx.setLineColor(WHITE);
      //this.addFigure(bx);

      Line lk = new Line(coords, coordsEnd);
      lk.setColor(WHITE);
      this.addFigure(lk);
*/
      ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
      if (bColorCandle)
        series.setPriceBarColor(index, WHITE);
      return;
    }
    //endregion

    if (clow > pphigh && false)
    {
      var cS = new Coordinate(s.getEndTime(index-2), (float) s.getHigh(index-2));
      var cE = new Coordinate(s.getStartTime(index) + 500000, (float) s.getLow(index));
      Box lx = new Box(cS, cE);
      lx.setUnderlay(true);
      lx.setFillColor(new Color(50, 153, 34, 128));
      lx.setLineColor(new Color(50, 153, 34, 128));
      this.addFigure(lx);
    }

  }

}
