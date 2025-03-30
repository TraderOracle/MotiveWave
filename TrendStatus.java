package TraderOracle;

import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import org.json.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;

@StudyHeader(
    namespace="com.DickInTheSpleen",
    id="TrendStatus",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="Trend Status",
    label="Trend Status",
    desc="Trend Status",
    menu="TraderOracle",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class TrendStatus extends Study {

  //region VARIABLES

  enum Values {UP, DOWN, TREND}
  enum Signals { WICK, TOUCH }

  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);
  private boolean bDrawn = false;
  private Coordinate cS;
  private Coordinate cE;
  private int currIndex = 0;
  private int gIndex = 0;
  private String sKPMsg = "";
  private String sMQMsg = "";
  private Map<Double, String> kpMap;
  private Map<String, Double> kpMapS;
  private Map<Double, String> mqMap;
  private Map<String, Double> mqMapS;
  private Map<Double, String> bsMap;
  private Map<String, Double> bsMapS;
  private Map<Double, String> tsSingle;
  private List<RangeEntry> tsRange;

  private static class RangeEntry {
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

  //region INITIALIZE

  @Override
  public void initialize(Defaults defaults) {
    this.kpMap = new HashMap<>();
    this.kpMapS = new HashMap<>();
    this.mqMap = new HashMap<>();
    this.mqMapS = new HashMap<>();
    this.bsMap = new HashMap<>();
    this.bsMapS = new HashMap<>();
    this.tsSingle = new HashMap<>();
    this.tsRange = new ArrayList<>();

    var sd = createSD();

    var tabQ = sd.addTab("Settings");
    var grpQ = tabQ.addGroup("Inputs");

    grpQ.addRow(new BooleanDescriptor("SHOWKP", "Show Killpips", true),
        new SpacerDescriptor(1, 20),
        new StringDescriptor("KP", "Values", "vix r1, 20280, vix r2, 20294, vix s1, 19701, vix s2, 19687, 1DexpMAX, 20309, 1DexpMIN, 19674, RD0, 20044, RD1, 20096, RD2, 20202, SD0, 19637, SD1, 19484, SD2, 19778, HV, 19992, VAH, 20371, VAL, 196008, range daily max, 20415, range daily min, 19565", 400));
    grpQ.addRow(new BooleanDescriptor("SHOWMQ", "Show MenthorQ", true),
        new SpacerDescriptor(1, 20),
        new StringDescriptor("MQ", "Values", "Call Resistance, 20300, Put Support, 18800, HVL, 19980, 1D Min, 19728.98, 1D Max, 20251.52, Call Resistance 0DTE, 20200, Put Support 0DTE, 19800, HVL 0DTE, 19980, Gamma Wall 0DTE, 20200, GEX 1, 20000, GEX 2, 19900, GEX 3, 19750, GEX 4, 19850, GEX 5, 20250, GEX 6, 20500, GEX 7, 19500, GEX 8, 19700, GEX 9, 20350, GEX 10, 19600", 400));
    grpQ.addRow(new StringDescriptor("MQBS", "Blind Spot Values", "BL 1, 20336.84, BL 2, 19709.19, BL 3, 20096.7, BL " +
        "4, 19736.23, BL 5, 19579.28, BL 6, 19597.77, BL 7, 20402.86, BL 8, 20493.97, BL 9, 20231.02, BL 10, 19914.84", 400));
    grpQ.addRow(new BooleanDescriptor("SHOWMANCINI", "Show Mancini Buy", true),
        new SpacerDescriptor(1, 20),
        new StringDescriptor("ManciniBuy", "Values", "", 400));
    grpQ.addRow(new BooleanDescriptor("SHOWMANCINI", "Show Mancini Sell", true),
        new SpacerDescriptor(1, 20),
        new StringDescriptor("ManciniSell", "Values", "", 400));

    grpQ.addRow(
        new BooleanDescriptor("SHOWMTS", "Show TraderSmarts", true),
        new SpacerDescriptor(1, 20),
        new StringDescriptor("TS", "Values", "20679.75 - 20663.25 Extreme Short20423.50 - 20420.75 Highest Odds Short FTU20314.00 Range Short20129.50 Line in the Sand19961.75 Range Long19929.75 - 19911.50 Highest Odds Long FTD19715.75 - 19705.00 Extreme LongNQ MTS Numbers: 20905.75, 20663.25, 19905.25, 18922.00, 18808.00, 18369.25, 17721.50, 17672.00, 17558.00", 400));

    grpQ.addRow(new IntegerDescriptor("KAMAPeriod", "KAMA Period", 9, 1, 9999, 1),
        new SpacerDescriptor(34, 40),
        new IntegerDescriptor("EMAPeriod", "   EMA Period", 21, 1, 9999, 1),
        new SpacerDescriptor(34, 20),
        new IntegerDescriptor("LargerEMAPeriod", "   Larger EMA Period", 200, 1, 9999, 1));

    grpQ.addRow(new IntegerDescriptor("XPOS", "Text X", 450, 1, 9999, 1),
        new SpacerDescriptor(34, 40),
        new IntegerDescriptor("YPOS", "   Text Y", 40, 1, 9999, 1),
        new SpacerDescriptor(34, 40),
        new IntegerDescriptor("LINESPACE", "   Space Between Lines", 22, 1, 9999, 1));

    grpQ.addRow(new SpacerDescriptor(34, 40));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.WICK, "Price wicked a line");
    desc.declareSignal(Signals.TOUCH, "Price closed within a line");
  }

  //endregion

  private void FillTraderSmarts()
  {
    //debug("FillTraderSmarts = " + src + ", " + xx);
    tsRange.clear();
    tsSingle.clear();
    String tsMTS = "";
    String ts = getSettings().getString("TS");

    //debug("FillTraderSmarts = " + ts);
    int idx = ts.indexOf("MTS Numbers:");
    if (idx != -1) {
      tsMTS = ts.substring(idx + "MTS Numbers:".length()).trim();
      tsMTS = tsMTS.replaceAll(", ", ",");
      //debug("tsMTS = " + tsMTS);
      ts = ts.substring(0, idx).trim();
      String[] nums = tsMTS.replaceAll("\\s+", "").split(",");
      for (int i = 0; i < nums.length; i++) {
        try {
          Double nummie = Double.parseDouble(nums[i].trim());
          tsRange.add(new RangeEntry(nummie, nummie, "MTS"));
          //debug("tsRange.add = " + nummie + " MTS");
        } catch (NumberFormatException e) {}
      }
    }

    int idxq = ts.indexOf("Target Zones:");
    if (idxq != -1)
      ts = ts.substring(idxq + "Target Zones:".length()).trim();
    ts = ts.replaceAll("FTD", "");
    ts = ts.replaceAll("FTU", "");
    ts = ts.replaceAll(" - ", "-");
    ts = ts.replaceAll("Short", "Short,");
    ts = ts.replaceAll("Long", "Long,");
    ts = ts.replaceAll("Sand", "Sand,");
    //debug("Target Zones: " + ts);

    // Values between the commas = 19825.75 Highest Odds Long
    String[] nums = ts.split(",");
    for (int i = 0; i < nums.length; i++) {
      //debug("nums " + i + " = " + nums[i]);
      // Values between the spaces
      String[] spaces = nums[i].trim().split(" ");
      for (int ip = 0; ip < spaces.length; ip++) {

        if (spaces[ip].trim().contains("-")) {
          // Dash separated range = 20129.50-20111.25 Range Short
          String[] dash = spaces[ip].trim().replaceAll("\\s+", "").split("-");
          try {
            Double numone = Double.parseDouble(dash[0].trim());
            Double numtwo = Double.parseDouble(dash[1].trim());
            String[] descs = nums[ip].trim().split(" ");
            String desc = nums[ip].replaceAll(descs[0], "");
            tsRange.add(new RangeEntry(numone, numtwo, desc));
            //debug("Dash RangeEntry = " + numone + ", " + numtwo + ", " + desc);
            continue;
          } catch (NumberFormatException e) {}
        }

        try {
          // Simple strings = 19825.75 Highest Odds Long
          Double nummie = Double.parseDouble(spaces[ip].trim());
          String[] descs = nums[ip].trim().split(" ");
          String desc = nums[ip].replaceAll(descs[0], "");
          tsRange.add(new RangeEntry(nummie, nummie, desc));
          debug("RangeEntry = " + nummie + " " + desc);
          continue;
        } catch (NumberFormatException e) {}

      }
    }
    //debug("ts = " + ts);
    //debug("tsMTS = " + tsMTS);
  }

  //region BAR CLOSE

  @Override
  public void onBarClose(DataContext ctx)
  {
    var s = ctx.getDataSeries();
    this.clearFigures();
    boolean bShowBOINK = getSettings().getBoolean("SHOWKP");

    if (getSettings().getBoolean("SHOWKP")) {
      sKPMsg = getKillStatus(s.getClose()) +
          getTouch(ctx, kpMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
    }

    if (getSettings().getBoolean("SHOWMQ")) {
      sMQMsg = getTouch(ctx, mqMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
      if (sMQMsg.equals(""))
        sMQMsg = getTouch(ctx, bsMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
    }

    Box bx = new Box();
    addFigure(bx);
    debug("onBarClose = " + sKPMsg);
    bDrawn = false;
  }

  //endregion

  //region KILLPIPS and MQ LOOKUPS

  public double findKPMap(String key) {
    if (key == null || key.isEmpty()) return 0;
    return kpMapS.get(key);
  }

  private boolean TouchKPScan(String src, double hi, double low) {
    double hola = findKPMap(src);
    if (hi > hola && low < hola) return true;
    else return false;
  }

  private String getTouch(DataContext ctx, Map<Double, String> map,
  double high, double low, double open, double close) {
    boolean c0G = close > open;
    boolean c0R = close < open;

    String sType = (map == kpMap) ? "Killpips " : (map == mqMap || map == bsMap) ? "MenthorQ " : "";

    for (Map.Entry<Double, String> entry : map.entrySet()) {
      Double price = entry.getKey();
      if (high > price && low < price) {
        ctx.signal(gIndex, Signals.TOUCH, "Price touched " + sType + entry.getValue(), close);
        return " - Touching " + entry.getValue();
      }
      else if ((c0G && high > price && close < price) || (c0G && low < price && open > price) ||
        (c0R && high > price && open < price) || (c0R && low < price && close > price)) {
        ctx.signal(gIndex, Signals.WICK, "Wick off " + sType + entry.getValue(), close);
        return " - WICK off " + entry.getValue();
      }
    }
    return "";
  }

  private String getKillStatus(double fuc) {
    if (fuc > findKPMap("HV") && fuc < findKPMap("RD0")) return "Long";
    else if (fuc > findKPMap("RD0") && fuc < findKPMap("RD1")) return "🡅 Long ";
    else if (fuc > findKPMap("RD1") && fuc < findKPMap("RD2")) return "🡅🡅 Long ";
    else if (fuc > findKPMap("RD2") && fuc < findKPMap("vix r1")) return "🡅🡅🡅 Long ";
    else if (fuc > findKPMap("vix r1") && fuc < findKPMap("vix r2")) return "😧 Extreme Long";
    else if (fuc > findKPMap("vix r2") && fuc < findKPMap("VAH")) return "⛔ Absolute TOP";
    else if (fuc < findKPMap("HV") && fuc > findKPMap("SD0")) return "Short";
    else if (fuc < findKPMap("SD0") && fuc > findKPMap("SD1")) return "🡇 Short ";
    else if (fuc < findKPMap("SD1") && fuc > findKPMap("SD2")) return "🡇🡇 Short ";
    else if (fuc < findKPMap("SD2") && fuc > findKPMap("vix s1")) return "🡇🡇🡇 Short ";
    else if (fuc < findKPMap("vix s1") && fuc > findKPMap("vix s2")) return "😧 Extreme Short";
    else if (fuc < findKPMap("vix s2") && fuc > findKPMap("VAL")) return "⛔ Absolute BOTTOM";
    else return "";
  }

  //endregion

  //region DRAW, FILLMAPS

  private class Box extends Figure
  {
    @Override
    public void draw(Graphics2D gc, DrawContext ctx)
    {
      int iY = getSettings().getInteger("YPOS");
      int iX = getSettings().getInteger("XPOS");
      int iSpacer = getSettings().getInteger("LINESPACE");
      Color cKP = sKPMsg.contains("Long") ? GREEN :
          sKPMsg.contains("Short") ? RED : WHITE;
      Color cMQ = sMQMsg.contains("GEX") ? new Color(255, 187, 61) :
          sMQMsg.contains("BL ") ? new Color(185, 99, 255) :
          sMQMsg.contains("Resist") || sMQMsg.contains("Max") ? RED :
          sMQMsg.contains("Support") || sMQMsg.contains("Min") ? GREEN : WHITE;

      try {
        Rectangle rect = gc.getClipBounds();
        Font font = new Font("Dialog", Font.PLAIN, 14);
        gc.setFont(font);

        if (getSettings().getBoolean("SHOWKP")) {
          gc.setColor(cKP);
          gc.drawString("Killpips: " + sKPMsg, iX, iY);
          iY += iSpacer;
        }

        if (getSettings().getBoolean("SHOWMQ")) {
          gc.setColor(cMQ);
          gc.drawString("MenthorQ " + sMQMsg, iX, iY);
          iY += iSpacer;
        }

      } catch (java.lang.Exception e) {
        debug("Error: " + e);
        bDrawn = false;
      }
    }
  }

  private void FillMaps(String src, Map xx, Map yy)
  {
    debug("FillMaps = " + src + ", " + xx);

    xx.clear();
    yy.clear();
    String[] parts = getSettings().getString(src).split(", ");
    for (int i = 0; i < parts.length; i++) {
      try {
        Double nums = Double.parseDouble(parts[i].trim());
        String txt = parts[i-1].trim();
        xx.put(nums, txt);
        yy.put(txt, nums);
        //debug("xx = " + nums + ", " + txt);
      } catch (NumberFormatException e) {
      }
    }
  }

  //endregion

  //region CALCULATE

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    var s = ctx.getDataSeries();
    int last = s.size() - 1;
    if (s == null || index < 20 || !s.isBarComplete(index))
      return;

    gIndex = index;
    if (kpMap.size() == 0 && kpMapS.size() == 0)
      FillMaps("KP", kpMap, kpMapS);
    if (mqMap.size() == 0 && mqMapS.size() == 0)
      FillMaps("MQ", mqMap, mqMapS);
    if (bsMap.size() == 0 && bsMapS.size() == 0)
      FillMaps("MQBS", bsMap, bsMapS);
    //if (map.size() == 0)
      FillTraderSmarts();
  }

  @Override
  public void onSettingsUpdated(DataContext ctx)
  {
    bDrawn = false;
  }

  //endregion

}