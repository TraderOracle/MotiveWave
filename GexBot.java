package GexBot;

//region IMPORTS
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.net.*;
import org.json.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;
//endregion

@StudyHeader(
    namespace="com.GexBot",
    id="GexBot",
    rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
    name="GexBot",
    label="GexBot",
    desc="GexBot",
    menu="Custom",
    overlay=true,
    studyOverlay=true,
    signals=true)

public class GexBot extends Study {

  //region VARIABLES
  enum Inputs { RED_LINE, GREEN_LINE, PATH, PATH1, PATH2 }
  enum Signals { LINE_CROSS, LINE_WICK, EMA_COMBO, CROSS_RETEST }
  enum Values { APIKEY, GREEN, RED, GREENLINE, REDLINE }

  private String VolGex = "";
  private String Vol0Gamma = "";
  private String VolMajPos = "";
  private String VolMinNeg = "";
  private String DeltaReversal = "";
  private String Spot = "";
  private String OIGex = "";
  private String OIMajPos = "";
  private String OIMinNeg = "";
  private String APIKey = "";
  private double convFactor = 1.0;
  private double widthScale = 0.03;
  private boolean bPulled = false;

  private static final Color RED = new Color(255, 0, 0);
  private static final Color GREEN = new Color(0, 255, 0);
  private static final Color WHITE = new Color(255, 255, 255);

  private class pLines {
    public double volume;
    public double oi;
    public double price;
    public double call;
    public double put;

    public pLines(double volume, double oi, double price, double call, double put) {
      this.volume = volume;
      this.oi = oi;
      this.price = price;
      this.call = call;
      this.put = put;
    }

    @Override
    public String toString() {
      return String.format(
         "pLines{volume=%.2f, oi=%.2f, price=%.2f, call=%.2f, put=%.2f}", volume, oi, price, call, put
      );
    }
  }
  List<pLines> ll = new ArrayList<pLines>();

  private static class Dots {
    public double volume;
    public double price;
    public int i;
  }
  List<Dots> ld = new ArrayList<Dots>();
  //endregion

  //region INITIALIZE
  @Override
  public void initialize(Defaults defaults) {
    var sd = createSD();

    var tab = sd.addTab("Settings");
    var grp = tab.addGroup("Inputs");

    List<NVP> nn = new ArrayList<NVP>();
    nn.add(new NVP("Full - up to 90 days out", "full"));
    nn.add(new NVP("Zero - only 0dte", "zero"));
    nn.add(new NVP("One - 1dte", "one"));
    grp.addRow(new DiscreteDescriptor("NEXTFULL", "Full/Zero: ", "zero", nn));
    List<NVP> n1n = new ArrayList<NVP>();
    n1n.add(new NVP("Classic", "classic"));
    n1n.add(new NVP("State", "state"));
    List<NVP> n2n = new ArrayList<NVP>();
    n2n.add(new NVP("(none)", "none"));
    n2n.add(new NVP("Delta 0dte", "delta"));
    n2n.add(new NVP("Gamma 0dte", "gamma"));
    n2n.add(new NVP("Charm 0dte", "charm"));
    n2n.add(new NVP("Vanna 0dte", "vanna"));
    n2n.add(new NVP("Delta 1dte", "onedelta"));
    n2n.add(new NVP("Gamma 1dte", "onegamma"));
    n2n.add(new NVP("Charm 1dte", "onecharm"));
    n2n.add(new NVP("Vanna 1dte", "onevanna"));
    grp.addRow(new DiscreteDescriptor("STATE", "Classic or State: ", "classic", n1n));
    grp.addRow(new DiscreteDescriptor("GREEK", "Greek: ", "delta", n2n));
    grp.addRow(new StringDescriptor("SYMBOL", "Symbol", "ES_SPX"));
    grp.addRow(new IntegerDescriptor("STDDOT", "Standard Dot Size", 4, 1, 9999, 1));
    grp.addRow(new IntegerDescriptor("GREEKDOT", "Greek Dot Size", 7, 1, 9999, 1));
    grp.addRow(new DoubleDescriptor("SCALE", "Width Scale", 0.03, 0, 9999, 0.001));

    grp.addRow(new PathDescriptor("GREENLINE", "Positive Volume", GREEN, 1.0f, null, true, false,
        false));
    grp.addRow(new PathDescriptor("REDLINE", "Negative Volume", RED, 1.0f, null, true, false,
        false));

    grp.addRow(new StringDescriptor("APIKEY", "API Key", ""));

    var tab1 = sd.addTab("Alerts and Etc");
    var grp1 = tab1.addGroup("Line Touch");

    grp1.addRow(new BooleanDescriptor("SHOWVOL", "Show Volume on Line", true));
    grp1.addRow(new IntegerDescriptor("VOLSIZE", "Font Size for Volume", 11, 5, 9999, 1));
    grp1.addRow(new IntegerDescriptor("STDEMA", "Standard EMA Size", 21, 1, 9999, 1));
    grp1.addRow(new IntegerDescriptor("LONGEMA", "Long EMA Size", 200, 1, 9999, 1));
    grp1.addRow(new DoubleDescriptor("MINVOL", "Minimum Volume Size for Alerts", 1, 1, 999999999, 1));

    grp1.addRow(new MarkerDescriptor("UPMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL,
        defaults.getGreen(), defaults.getLineColor(), true, true));
    grp1.addRow(new MarkerDescriptor("DOWNMarker", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL,
        defaults.getRed(), defaults.getLineColor(), true, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.declareSignal(Signals.LINE_CROSS, "Line Touch");
    desc.declareSignal(Signals.LINE_WICK, "Line Wick");
    desc.declareSignal(Signals.CROSS_RETEST, "Line Cross and Re-test From Other Side");
    desc.declareSignal(Signals.EMA_COMBO, "EMA Combo Touch/Wick");

    desc.declarePath(Values.GREEN, "GREENLINE");
    desc.declarePath(Values.RED, "REDLINE");
  }
  //endregion

  //region FETCH GEXBOT JSON
  public void FetchGexBot()
  {
    //region HTTP CALL
    String sSymbol = getSettings().getString("SYMBOL");
    String sGreek = getSettings().getString("GREEK");
    String sState = getSettings().getString("STATE");
    String nextFull = getSettings().getString("NEXTFULL");
    String APIKey = getSettings().getString("APIKEY");
    ll.clear();

    if (sGreek != "none"){
      sState = "state";
      nextFull = sGreek;
    }

    //debug("1Greek " + sGreek + " sState " + sState + " nextFull " + nextFull);

    try{
      String url = "https://api.gexbot.com/" + sSymbol + "/" + sState + "/" + nextFull + "?key=" + APIKey;
      debug("url: " + url);
      String jsonString = getHTML("https://api.gexbot.com/" + sSymbol +
          "/" + sState + "/" + nextFull + "?key=" + APIKey);
      //debug("jsonString: " + jsonString);
      JSONObject jo = new JSONObject(jsonString);
      //endregion

      //region STRIKES
      String sSection = "strikes";
      if (sGreek == "none")
      {
        VolGex = jo.getString("sum_gex_vol");
        //debug("sum_gex_vol: " + VolGex);
        OIGex = jo.getString("sum_gex_oi");
        DeltaReversal = jo.getString("delta_risk_reversal");
        Spot = jo.getString("spot");
        Vol0Gamma = jo.getString("zero_gamma");
        VolMajPos = jo.getString("major_pos_vol");
        OIMajPos = jo.getString("major_pos_oi");
        VolMinNeg = jo.getString("major_neg_vol");
        //debug("major_neg_vol: " + VolMinNeg);
        OIMinNeg = jo.getString("major_neg_oi");
      }
      else
      {
        sSection = "mini_contracts";
        VolMajPos = jo.getString("major_positive");
        //debug("major_positive: " + VolMajPos);
        VolMinNeg = jo.getString("major_negative");
      }
      //endregion

      //region GREEKS
      JSONArray arr = jo.getJSONArray(sSection);
      for (int i = 0; i < arr.length(); i++)
      {
        JSONArray MiniContracts = arr.getJSONArray(i);
        if (sGreek == "none") {
          double price = arr.getJSONArray(i).getDouble(0);
          double volume = arr.getJSONArray(i).getDouble(1);
          double oi = arr.getJSONArray(i).getDouble(2);
          if (i == 1 && false)
            debug("NO GREEK price " + price + " vol " + volume + " oi " + oi);
          ll.add(new pLines(
              volume * convFactor, // double volume;
              oi, // double oi;
              price, // double price;
              0.0d, // double call;
              0.0d // double put;
          ));
          JSONArray listings = MiniContracts.getJSONArray(3);
          for (int iq = 0; iq < listings.length(); iq ++) {
            //debug("Greekie price " + listings.getJSONArray(iq).getDouble(0));
            //JSONObject jjj = new JSONObject(listings(i));
          }
          //debug("listings: " + listings.toString());
        }
        else {
          double price = arr.getJSONArray(i).getDouble(0);
          double call = arr.getJSONArray(i).getDouble(1);
          double put = arr.getJSONArray(i).getDouble(2);
          double volume = arr.getJSONArray(i).getDouble(3);
          if (i == 1 && false)
            debug("Greekie price " + price + " call " + call + " put " + put + " vol " + volume);
          ll.add(new pLines(
              volume * convFactor, // double volume;
              0.0d, // double oi;
              price, // double price;
              call, // double call;
              put // double put;
          ));
        }
        //JSONArray listings = MiniContracts.getJSONArray(2);
        //debug("listings: " + listings.toString());
        //endregion
      }
    } catch (Exception e) {}
    bPulled = false;
  }
  //endregion

  //region HTTP
  public String getHTML(String urlToRead) throws Exception {
    StringBuilder result = new StringBuilder();
    URL url = new URL(urlToRead);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream()))) {
      for (String line; (line = reader.readLine()) != null; ) {
        result.append(line);
      }
    }
    return result.toString();
  }
  //endregion

  //region PAINT CHART
  private void RefreshChart(DataSeries s) {
    boolean bShowVol = getSettings().getBoolean("SHOWVOL");
    int iFontSize = getSettings().getInteger("VOLSIZE");
    PathInfo pf;
    if (ll.size() < 1)
      return;

    //debug("bPulled size : " + ll.size());
    this.clearFigures();
    // fMid = fStart + (Math.abs(fEnd - fStart) / 2);
    for (pLines line : ll) {
      double dScale = getSettings().getDouble("SCALE");
      long lScreenWidth = s.getVisibleEndTime() - s.getVisibleStartTime();
      double dAdjVol = Math.abs(line.volume) * (dScale);
      double dVolAdjustedWidth = (lScreenWidth * dAdjVol) / 100;
      //double dScale = 0.001; // getSettings().getDouble("SCALE");
      //double dAdjustedPlusScale = dVolAdjustedWidth + dScale;
      long volEnd = (long) Math.round(dVolAdjustedWidth);
      if (line.volume > 0)
        pf = getSettings().getPath("GREENLINE");
      else
        pf = getSettings().getPath("REDLINE");
      Line lk = new Line(new Coordinate(s.getVisibleStartTime(), line.price),
          new Coordinate(s.getVisibleStartTime() + volEnd, line.price), pf);
      if (bShowVol){
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMaximumFractionDigits(0);
        String formattedNum = formatter.format(line.volume);
        lk.setText("Vol " + formattedNum, new Font("Arial", Font.PLAIN, iFontSize));
      }

      this.addFigure(lk);
      //debug("addFigure at " + line.price + " start " + " vol " + line.volume + s.getVisibleStartTime() +
      // " end " + s.getVisibleEndTime() + " volend " + volEnd);
    }
    bPulled = true;
  }
  //endregion

  //region ON BAR CLOSE
  @Override
  public void onBarClose(DataContext ctx) {
    FetchGexBot();

    var s = ctx.getDataSeries();
    int index = s.size() - 1;
    int iEMAPeriod = getSettings().getInteger("STDEMA");
    int iLargerEMAPeriod = getSettings().getInteger("LONGEMA");
    double ema21 = s.ema(index, iEMAPeriod, Enums.BarInput.CLOSE);
    double ema200 = s.ema(index, iLargerEMAPeriod, Enums.BarInput.CLOSE);
    debug("ema21 " + ema21 + " ema200 " + ema200);

    //region CANDLE VARS
    double close = s.getClose(index);
    double open = s.getOpen(index);
    double pclose = s.getClose(index - 1);
    double popen = s.getOpen(index - 1);
    double ppclose = s.getClose(index - 2);
    double ppopen = s.getOpen(index - 2);
    double low = s.getLow(index);
    double plow = s.getLow(index - 1);
    double high = s.getHigh(index);
    boolean c0G = close > s.getOpen(index);
    boolean c1G = s.getClose(index - 1) > s.getOpen(index - 1);
    boolean c2G = s.getClose(index - 2) > s.getOpen(index - 2);
    boolean c0R = close < open;
    boolean c1R = s.getClose(index - 1) < s.getOpen(index - 1);
    boolean c2R = s.getClose(index - 2) < s.getOpen(index - 2);
    //endregion

    for (pLines line : ll) {
      if (high > line.price && low < line.price){
        if ((c0G && high > line.price && close < line.price) ||
           (c0G && low < line.price && open > line.price) ||
           (c0R && high > line.price && open < line.price) ||
           (c0R && low < line.price && close > line.price)) {
          ctx.signal(index, Signals.LINE_WICK, "Line Wick at " + close, close);
        }
        else
          ctx.signal(index, Signals.LINE_CROSS, "Line Cross at " + close, close);
      }

    }
  }

  @Override
  public void onSettingsUpdated(DataContext ctx) {
    RefreshChart(ctx.getDataSeries());
  }
  //endregion

  @Override
  protected void calculate(int index, DataContext ctx) {
    var s = ctx.getDataSeries();

    int lastCandleIndex = s.size() - 1;
    if (s == null || index < 20 || !s.isBarComplete(index))
      return;

    if(!bPulled)
      RefreshChart(s);

    //region CANDLE CALCS
    double close = s.getClose(index);
    double open = s.getOpen(index);
    double pclose = s.getClose(index - 1);
    double popen = s.getOpen(index - 1);
    double ppclose = s.getClose(index - 2);
    double ppopen = s.getOpen(index - 2);
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
}

