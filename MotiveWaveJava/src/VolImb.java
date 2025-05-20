package TraderOracle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
  id="VolumeImbalance",
  rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
  name="Volume Imbalance",
  label="Volume Imbalance",
  desc="Volume Imbalance",
  menu="TraderOracle",
  overlay=true,
  studyOverlay=true)

public class VolImb extends Study
{
  //region VARIABLES
  enum Signals { VOLIMB_FILLED, VOLIMB_WICK, VOLIMB_CREATED }
  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);

  private List<Figure> al = new ArrayList<>();
  private int candleFirst = 0;
  private int candleLast = 0;
  //endregion

  //region INITIALIZE
  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();
    var tab = sd.addTab("Settings");
    var grp = tab.addGroup("Inputs");
    grp.addRow(new BooleanDescriptor("StdMethod", "Use standard candle gap calculation", true));
    grp.addRow(new BooleanDescriptor("FVGMethod", "Use FVG calculation, instead", true));

    grp.addRow(new BooleanDescriptor("GREEN", "Show Green Vol Imbalances", true));
    grp.addRow(new BooleanDescriptor("RED", "Show Red Vol Imbalances", true));
    grp.addRow(new BooleanDescriptor("GAPDATA", "Show Gap Data on Line", true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.VOLIMB_CREATED, "Volume Imbalance Created");
    desc.declareSignal(Signals.VOLIMB_FILLED, "Volume Imbalance Filled");
    desc.declareSignal(Signals.VOLIMB_WICK, "Volume Imbalance Wicked");
  }
  //endregion

  //region DRAW LINES
  private void DrawLines(DataSeries s)
  {
    PathInfo pf = new PathInfo(
      new Color(255, 255, 255), 2, new float[]{3, 2, 5}, true, true, false, 0, 2);

    for (int iq = 0; iq < s.size() - 1; iq ++) {
      int index = iq;

      // region CANDLE CALCULATIONS
      double close = s.getClose(index);
      double pclose = s.getClose(index - 1);
      double open = s.getOpen(index);
      double low = s.getLow(index);
      double pplow = s.getLow(index-2);
      double high = s.getHigh(index);
      double pphigh = s.getHigh(index - 2);
      boolean c0G = close > s.getOpen(index);
      boolean c1G = s.getClose(index - 1) > s.getOpen(index - 1);
      boolean c0R = close < s.getOpen(index);
      boolean c1R = s.getClose(index - 1) < s.getOpen(index - 1);

      boolean bShowGreen = getSettings().getBoolean("GREEN");
      boolean bShowRed = getSettings().getBoolean("RED");
      boolean bStdMethod = getSettings().getBoolean("StdMethod");
      boolean bFVGMethod = getSettings().getBoolean("FVGMethod");
      //endregion

      //region FAIR VALUE GAPS GREEN
      if (false && bShowGreen && (low > pphigh && bFVGMethod))
      {
        boolean bFoundEnd = false;
        //Coordinate cS = new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2));
        Coordinate cS = new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2));
        for (int i = index + 1; i < s.size() - 1; i++) {
          //if (s.getLow(i) < low) {
          if (s.getLow(i) < low) {
            //new Box(new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2)),
            //        new Coordinate(s.getStartTime(i), (float) s.getLow(iq))
            var cE = new Coordinate(s.getStartTime(i), (float) s.getLow(iq));
            float middle = Math.abs(s.getHigh(iq-2) - s.getLow(iq)) + s.getLow(iq);
            var cMidE = new Coordinate(s.getStartTime(i), middle);
            Coordinate cMidS = new Coordinate(s.getEndTime(iq-2), middle);

            //debug("StartTime(i) " + s.getStartTime(i) + " s.getLow(iq) " + s.getLow(iq) + " s.getHigh(iq-2) " + s.getHigh(iq-2) + " middle " + middle);

            Line lk = new Line(cMidS, cMidE, pf);
            lk.setExtendRightBounds(false);
            lk.setExtendLeftBounds(false);
            lk.setText("fvg", new Font("Arial", Font.PLAIN, 12));
            this.addFigure(lk);
            bFoundEnd = true;
            break;
          }
        }
        if (!bFoundEnd) {
          int iStart = s.getStartIndex();
          int iEnd = s.getEndIndex();
          float middle = Math.abs(s.getHigh(iq-2) - s.getLow(iq)) + s.getLow(iq);
          var cMidE = new Coordinate(s.getStartTime(iq), middle);
          Coordinate cMidS = new Coordinate(s.getEndTime(iq-2), middle);
          var cE = new Coordinate(s.getEndTime(iEnd) + 1000, (double) middle);
          Line lk = new Line(cMidS, cMidE, pf);
          lk.setExtendRightBounds(true);
          lk.setText("fvg", new Font("Arial", Font.PLAIN, 12));
          this.addFigure(lk);
          bFoundEnd = true;
        }
      }
      //endregion

      //region VOLUME IMBALANCES GREEN
      if (c0G && c1G && open > pclose)
      {
        boolean bFoundEnd = false;
        Coordinate cS = new Coordinate(s.getStartTime(index), (double) s.getOpen(index));
        for (int i = index + 1; i < s.size() - 1; i++) {
          if (s.getLow(i) < s.getOpen(index)) {
            //debug("index " + index + ", lindex " + i);
            var cE = new Coordinate(s.getEndTime(i-1), (double) s.getOpen(index));
            Line lk = new Line(cS, cE, pf);
            lk.setExtendRightBounds(false);
            lk.setExtendLeftBounds(false);
            //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
            this.addFigure(lk);
            bFoundEnd = true;
            break;
          }
        }
        if (!bFoundEnd) {
          int iStart = s.getStartIndex();
          int iEnd = s.getEndIndex();
          var cE = new Coordinate(s.getEndTime(iEnd) + 1000, (double) s.getOpen(index));
          Line lk = new Line(cS, cE, pf);
          lk.setExtendRightBounds(true);
          //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
          this.addFigure(lk);
          bFoundEnd = true;
        }
      }
      //endregion

      //region VOLUME IMBALANCES RED
      if (c0R && c1R && open < pclose)
      {
        boolean bFoundEnd = false;
        Coordinate cS = new Coordinate(s.getStartTime(index), (double) s.getOpen(index));
        for (int i = index + 1; i < s.size() - 1; i++) {
          if (s.getHigh(i) > s.getOpen(index)) {
            //debug("index " + index + ", lindex " + i);
            var cE = new Coordinate(s.getEndTime(i-1), (double) s.getOpen(index));
            Line lk = new Line(cS, cE, pf);
            lk.setExtendRightBounds(false);
            lk.setExtendLeftBounds(false);
            //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
            this.addFigure(lk);
            bFoundEnd = true;
            break;
          }
        }
        if (!bFoundEnd) {
          int iStart = s.getStartIndex();
          int iEnd = s.getEndIndex();
          var cE = new Coordinate(s.getEndTime(iEnd) + 1000, (double) s.getOpen(index));
          Line lk = new Line(cS, cE, pf);
          lk.setExtendRightBounds(true);
          //lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
          this.addFigure(lk);
          bFoundEnd = true;
        }
      }
      //endregion

    }
  }
  //endregion

  //region SETTINGS AND BARCLOSE
  @Override
  public void onSettingsUpdated(DataContext ctx)
  {
    this.clearFigures();
    DrawLines(ctx.getDataSeries());
  }

  @Override
  public void onBarClose(DataContext ctx)
  {
    this.clearFigures();
    DrawLines(ctx.getDataSeries());
  }
  //endregion

  //region TIME CONVERSION
  public static long getSecondsSinceMidnight(long epochMillis) {
    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    ZonedDateTime midnight = zdt.truncatedTo(ChronoUnit.DAYS);
    return ChronoUnit.SECONDS.between(midnight, zdt);
  }

  private long convertTimeToSeconds(String timeString) {
    String[] parts = timeString.split(":");
    int hours = Integer.parseInt(parts[0]);
    int minutes = Integer.parseInt(parts[1]);
    int totalSeconds = hours * 3600 + minutes * 60;

    return (long) totalSeconds;
  }
  //endregion

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    /*
    var s = ctx.getDataSeries();
    int last = s.size() - 1;
    if (s == null || index < last-600 || !s.isBarComplete(index))
      return;

    long candleStart = getSecondsSinceMidnight(s.getStartTime(index));
    long candleEnd = getSecondsSinceMidnight(s.getEndTime(index));
    long EightThirty = convertTimeToSeconds("08:30");
    long EightFortyFive = convertTimeToSeconds("08:45");

    if (EightThirty >= candleStart && EightThirty <= candleEnd) {
      candleFirst = index;
      s.setPriceBarColor(index, WHITE);
    }

    if (EightFortyFive >= candleStart && EightFortyFive <= candleEnd) {
      candleLast = index;
      s.setPriceBarColor(index, WHITE);
    }

    if (candleFirst > 0 && candleLast > 0){
      double howdy = s.lowest(candleLast, candleLast-candleFirst, Enums.BarInput.LOW);
      debug("Lowest " + howdy + " last " + last);
    }

    /*
    List<Figure> figures = this.getFigures();
    for (Figure figure : figures) {
      figure.setPopupMessage("FdsASD");
      figure.setUnderlay(false);
    }

    Object input = getSettings().getInput(Inputs.INPUT);
    debug("getLabel " + this.getLabel());

    RuntimeDescriptor ts = this.getRuntimeDescriptor();

    Plot t = ts.getPricePlot();
    t.clearIndicators();
    t.clearPaths();
    t.clearPriceBars();
    //debug("Plot " + t.getTabName());
    List<Figure> figures = this.getFigures(t.getName());
    //debug("List<Figure> " + figures.size());

    Plot t2 = ts.getDefaultPlot();
    t2.clearIndicators();
    t2.clearPaths();
    t2.clearPriceBars();
    //debug("Plot " + t2.getTabName());

    List<Figure> figures2 = this.getFigures(t2.getName());
    debug("List<Figure2> " + figures2.size());
    List<Figure> figures3 = this.getFigures();
    debug("List<Figure3> " + figures3.size());

    //if (figures == null || figures.isEmpty()) return;

    //int count = ctx.getBarCount();
    //int x = getBarCoordinate(ctx, count-1);
    //int y = getYCoordinate(ctx, price);

    for (Figure figure : figures) {
      debug("figure " + index);
      figure.setPopupMessage("FdsASD");
      figure.setUnderlay(false);
      var rect = figure.getBounds();
      double low = s.getLow(index);
      double high = s.getHigh(index);
      if (rect.contains(s.getEndTime(index), low)){
        debug("bounds " + low);
      }
      //if (isRectangleFigure(figure)) {
        //Rectangle bounds = figure.getBounds();
        //debug("bounds " + bounds.x);
        //if (bounds.contains(x, y)) {
          //return true;
        //}
      //}
    }

     */

  }
}