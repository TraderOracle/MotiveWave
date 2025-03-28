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
  private int pIndex = 0;
  //endregion

  //region INITIALIZE
  @Override
  public void initialize(Defaults defaults)
  {
    var sd = createSD();
    var tab = sd.addTab("Settings");
    var grp = tab.addGroup("Inputs");
    grp.addRow(new BooleanDescriptor("GREEN", "Show Green Vol Imbalances", true));
    grp.addRow(new BooleanDescriptor("RED", "Show Red Vol Imbalances", true));
    grp.addRow(new BooleanDescriptor("GAPDATA", "Show Gap Data on Line", true));
    grp.addRow(new DoubleDescriptor("MINGAP", "Mimimum gap to show", 0.5, 0, 9999, 0.5));

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
      new Color(166, 200, 255, 250), 2, new float[]{3, 2, 5}, true, true, false, 0, 2);

    for (int iq = 0; iq < s.size() - 1; iq ++) {
      int index = iq;

      double close = s.getClose(index);
      double pclose = s.getClose(index - 1);
      double open = s.getOpen(index);
      boolean c0G = close > s.getOpen(index);
      boolean c1G = s.getClose(index - 1) > s.getOpen(index - 1);

      //region VOLUME IMBALANCES GREEN
      if (c0G && c1G && open > pclose)
      {
        boolean bFoundEnd = false;
        Coordinate cS = new Coordinate(s.getStartTime(index), (double) s.getOpen(index));
        for (int i = index + 1; i < s.size() - 1; i++) {
          if (s.getLow(i) < s.getOpen(index)) {
            debug("index " + index + ", lindex " + i);
            var cE = new Coordinate(s.getEndTime(i), (double) s.getOpen(index));
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