package HelloTOWorld;

//region IMPORTS
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import org.json.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;
//endregion

@StudyHeader(
        namespace="com.HomieSlice",
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
    enum Signals { KAMA_CROSS }

    private String VolGex = "";
    private String Vol0Gamma = "";
    private String VolMajPos = "";
    private String VolMinNeg = "";
    private String DeltaReversal = "";
    private String Spot = "";
    private String OIGex = "";
    private String OIMajPos = "";
    private String OIMinNeg = "";
    private String Greek = "vanna";
    private String sState = "state";
    private String nextFull = "full";
    private String APIKey = "70x01AzU0p3Z";

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);

    private static class Lines {
        public double volume;
        public double oi;
        public double price;
        public double call;
        public double put;
    }
    List<Lines> ll = new ArrayList<Lines>();

    private static class Dots {
        public double volume;
        public double price;
        public int i;
    }
    List<Dots> ld = new ArrayList<Dots>();

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

    //region FETCH GEXBOT JSON
    public void FetchGexBot()
    {
        String symbol = "ES_SPX";

        if (Greek != "none"){
            sState = "state";
            nextFull = Greek;
        }

        try{
            String jsonString = getHTML("https://api.gexbot.com/" + symbol +
                    "/" + sState + "/" + nextFull + "?key=" + APIKey);
            //debug("jsonString: " + jsonString);
            JSONObject jo = new JSONObject(jsonString);

            String sSection = "strikes";
            if (Greek == "none")
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

            JSONArray arr = jo.getJSONArray(sSection);
            for (int i = 0; i < arr.length(); i++)
            {
                JSONArray MiniContracts = arr.getJSONArray(i);
                double price = arr.getJSONArray(i).getDouble(0);
                double volume = arr.getJSONArray(i).getDouble(1);
                double oi = arr.getJSONArray(i).getDouble(2);
                JSONArray listings = MiniContracts.getJSONArray(2);
                debug("listings: " + listings.length());
            }

/*
            JSONArray arr = obj.getJSONArray(sSection); // notice that `"posts": [...]`
            for (int i = 0; i < arr.length(); i++)
            {
                if (Greek == "none")
                {
                    arr.getJSONObject(i).getString("name");
                    double price = item[0].ToObject<Double>();
                    double volume = item[1].ToObject<Double>();
                    double oi = item[2].ToObject<Double>();
                    lines line = new lines();
                    line.price = price * convFactor;
                    line.volume = volume;
                    line.oi = oi;
                    llT.Add(line);
                    var xxx = item[3].Value<JArray>();
                    int i = 1;
                    foreach (Double qqq in xxx)
                    {
                        dots dotz = new dots();
                        dotz.price = price;
                        dotz.volume = qqq;
                        dotz.i = i;
                        ldT.Add(dotz);
                        i++;
                    }
                    idx++;
                }
                else
                {
                    double price = item[0].ToObject<Double>();
                    double call = item[1].ToObject<Double>();
                    double put = item[2].ToObject<Double>();
                    double sgreek = item[3].ToObject<Double>();
                    lines line = new lines();
                    line.price = price * convFactor;
                    line.volume = sgreek;
                    line.call = call;
                    line.put = put;
                    llT.Add(line);
                    idx++;
                }
            }

 */

        } catch (Exception e) {}
    }
    //endregion

    //region COOL EXTRA FUNCTIONS

    @Override
    public void onBarClose(DataContext ctx) {
        FetchGexBot();

        var series = ctx.getDataSeries();

        this.clearFigures();

        long fStart = series.getVisibleStartTime();
        long fEnd = series.getVisibleEndTime();
        long fMid = fStart + (Math.abs(fEnd - fStart) / 2);
        PathInfo pf = new PathInfo(WHITE, 2, new float[] {4,1,3}, true, true, false, 0, 2);
        Line lk = new Line(new Coordinate(fStart, 20160), new Coordinate(fEnd, 20160), pf);
        lk.setText("VolImb", new Font("Arial", Font.PLAIN, 12));
        this.addFigure(lk);
    }

    @Override
    public void onSettingsUpdated(DataContext ctx) {

    }

    //endregion

    //region INITIALIZE AND MISC

    @Override
    public void initialize(Defaults defaults) {
        var sd = createSD();

        var tab = sd.addTab("Cool Settings");
        var grp = tab.addGroup("Inputs");
        grp.addRow(new BooleanDescriptor("ShowDot", "Show Dot", true));
        grp.addRow(new StringDescriptor("DisplayMe", "Display String", "Hello TO World - screen writing!"));
        grp.addRow(new IntegerDescriptor("KAMAPeriod", "KAMA Period", 9, 1, 9999, 1));
        grp.addRow(new InputDescriptor(Inputs.INPUT, "KAMA Line", Enums.BarInput.CLOSE));

        grp = tab.addGroup("Markers");
        grp.addRow(new MarkerDescriptor("UPMarker", "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
        grp.addRow(new MarkerDescriptor("DOWNMarker", "Down Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getRed(), defaults.getLineColor(), true, true));

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
        desc.declareSignal(Signals.KAMA_CROSS, "Kama Cross");
    }


    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();

        int lastCandleIndex = series.size() - 1;
        if (series == null || index < 202 || !series.isBarComplete(index))
            return;

        // region CONFIG SETTINGS
        boolean bUseKama = getSettings().getBoolean("KAMAPeriod");
        int iKAMAPeriod = getSettings().getInteger("KAMAPeriod");
        //endregion

        //region CANDLE CALCS
        double close = series.getClose(index);
        double open = series.getOpen(index);
        double pclose = series.getClose(index - 1);
        double popen = series.getOpen(index - 1);
        double ppclose = series.getClose(index - 2);
        double ppopen = series.getOpen(index - 2);
        double low = series.getLow(index);
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

