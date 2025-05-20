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
    id="FairValueGap",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="Fair Value Gap",
    label="Fair Value Gap",
    desc="Fair Value Gap",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true)

public class FairValueGap extends Study
{
  //region VARIABLES
  enum Signals { FVG_FILLED, FVG_CREATED }
  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);
  //endregion

  //region INITIALIZE
  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();
    var tab = sd.addTab("Settings");
    var grp = tab.addGroup("Inputs");
    grp.addRow(new BooleanDescriptor("UP", "Show Green FVGs", true));
    grp.addRow(new BooleanDescriptor("DOWN", "Show Red FVGs", true));
    grp.addRow(new DoubleDescriptor("MINGAP", "Mimimum gap to show", 0.5, 0, 9999, 0.5));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.FVG_CREATED, "FVG Created");
    desc.declareSignal(Signals.FVG_FILLED, "FVG Filled");
  }
  //endregion

  //region DRAW LINES
  private void DrawLines(DataSeries s)
  {
    double dMinGap = getSettings().getDouble("MINGAP");
    boolean bShowGreen = getSettings().getBoolean("UP");
    boolean bShowRed = getSettings().getBoolean("DOWN");

    for (int iq = 0; iq < s.size() - 1; iq ++) {
      double low = s.getLow(iq);
      double pplow = s.getLow(iq-2);
      double high = s.getHigh(iq);
      double pphigh = s.getHigh(iq - 2);

      //region FAIR VALUE RED
      if (pplow > high && bShowRed)
      {
        boolean bFoundEnd = false;
        for (int i = iq + 1; i < s.size() - 1; i++) {
          if (s.getHigh(i) > high) {
            Box lx = new Box(new Coordinate(s.getEndTime(iq-2), (float) s.getLow(iq-2)),
                new Coordinate(s.getStartTime(i), (float) s.getHigh(iq)), new PathInfo(
                new Color(153, 34, 56, 250), 1, new float[]{1}, true, true, false, 0, 2));
            lx.setUnderlay(true);
            lx.setFillColor(new Color(153, 34, 56, 148));
            lx.setLineColor(new Color(153, 34, 56, 255));
            this.addFigure(lx);
            bFoundEnd = true;
            break;
          }
        }
        if (!bFoundEnd) {
          int iStart = s.getStartIndex();
          int iEnd = s.getEndIndex();
          Box lx = new Box(new Coordinate(s.getEndTime(iq-2), (float) s.getLow(iq-2)), new Coordinate(s.getEndTime(iEnd) + 1000, (double) s.getHigh(iq)), new PathInfo(
              new Color(153, 34, 56, 250), 1, new float[]{1}, true, true, false, 0, 2));
          lx.setUnderlay(true);
          lx.setFillColor(new Color(153, 34, 56, 158));
          lx.setLineColor(new Color(153, 34, 56, 255));
          this.addFigure(lx);
          bFoundEnd = true;
        }
      }
      //endregion

      //region FAIR VALUE GREEN
      if (low > pphigh && bShowGreen)
      {
        boolean bFoundEnd = false;
        Coordinate cS = new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2));
        for (int i = iq + 1; i < s.size() - 1; i++) {
          if (s.getLow(i) < low) {
            Box lx = new Box(new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2)), new Coordinate(s.getStartTime(i), (float) s.getLow(iq)), new PathInfo(
                new Color(153, 34, 56, 250), 1, new float[]{1}, true, true, false, 0, 2));
            lx.setUnderlay(true);
            lx.setFillColor(new Color(50, 153, 34, 148));
            lx.setLineColor(new Color(50, 153, 34, 255));
            this.addFigure(lx);
            bFoundEnd = true;
            break;
          }
        }
        if (!bFoundEnd) {
          int iStart = s.getStartIndex();
          int iEnd = s.getEndIndex();
          Box lx = new Box(new Coordinate(s.getEndTime(iq-2), (float) s.getHigh(iq-2)), new Coordinate(s.getEndTime(iEnd) + 1000, (double) s.getLow(iq)), new PathInfo(
              new Color(153, 34, 56, 250), 1, new float[]{1}, true, true, false, 0, 2));
          lx.setUnderlay(true);
          lx.setFillColor(new Color(50, 153, 34, 158));
          lx.setLineColor(new Color(50, 153, 34, 255));
          this.addFigure(lx);
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

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int last = s.size() - 1;
    Object input = getSettings().getInput(Inputs.INPUT);
    if (s == null || index < 2 || !s.isBarComplete(index))
      return;

  }
}