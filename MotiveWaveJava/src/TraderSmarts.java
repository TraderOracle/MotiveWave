package TraderOracle;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Date;
import java.time.*;
import java.time.format.*;
import java.io.*;
import java.net.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.*;
import com.motivewave.platform.sdk.study.*;
import com.motivewave.platform.sdk.draw.*;

@StudyHeader(
        namespace="com.mycompany",
        id="TraderSmarts",
        rb="TraderOracle.nls.strings",
        name="TraderSmarts",
        label="TraderSmarts",
        desc="TraderSmarts",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class TraderSmarts extends Study {
    //region PRIVATES

    private List<Figure> alFig = new ArrayList<Figure>();
    private boolean bFetched = false;
    private String sLicense = "";
    private boolean bShowTS = false;
    private boolean bShowMTS = true;
    private boolean bShowLIS = true;

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

    //region DoTheFetch
    private void DoTheFetch(DataSeries series)
    {
        alFig.clear();
        //if (!bFetched)
        {
            String sy = getDateString(series.getStartTime(series.getEndIndex()));
            String nq = series.getInstrument().getSymbol().replaceAll("@","").substring(0,2);
            String url = "https://tradersmarts.quantkey.com/api/v1/plan.php?" +
                    "lic=" + sLicense +
                    "&root=" + nq + "&date=" + sy + "&apikey=d8d22b68-1285-4bbd-80d6-e6d5ed13dbbc";
            try {
                debug("url = " + url);
                String returns = getHTML(url);
                String result = returns.replaceAll("\\<[^>]*>","");
                result = result.replaceAll(nq + " Contract Notes:","|Contract Notes:");
                result = result.replaceAll(nq + " Macro Technical:","|Macro Technical:");
                result = result.replaceAll(nq + " Execution/Target Zones:","|");
                result = result.replaceAll("TS TradePlan for " + nq,"|");
                result = result.replaceAll("&nbsp","");
                result = result.replaceAll("Line in the Sand","  Line in the Sand|");
                result = result.replaceAll("Extreme Long","  Extreme Long|");
                result = result.replaceAll("Range Long","  Range Long|");
                result = result.replaceAll("Highest Odds Long FTD","  Highest Odds Long FTD|");
                result = result.replaceAll("Range Short","  Range Short|");
                result = result.replaceAll("Extreme Short","  Extreme Short|");
                result = result.replaceAll("Highest Odds Short FTU","  Highest Odds Short FTU|");
                result = result.replaceAll("TraderSmarts Numbers for ","|TraderSmarts Numbers for ");
                result = result.replaceAll(" - ","-");
                //PrintWriter out = new PrintWriter("c:\\temp\\filename.txt");
                //out.println(result);
                //out.close();

                tokenizeResult(series, result);
                bFetched = true;
                debug("result = " + result);
            } catch (Exception e) {}
        }
    }
    //endregion

    //region BAR CLOSE
    @Override
    public void onBarClose(DataContext ctx)
    {

    }
    //endregion

    //region INITIALIZE
    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tab = sd.addTab("TraderSmarts");
        var grp = tab.addGroup("Inputs");
        grp.addRow(new StringDescriptor("Lic", "TraderSmarts License", ""));
        grp.addRow(new BooleanDescriptor("TS", "Show TS Lines", true));
        grp.addRow(new PathDescriptor("TSPath", "TS Lines", new Color(126, 179, 252), 1.0f, null, true, false, true));
        grp.addRow(new BooleanDescriptor("MTS", "Show MTS Lines", true));
        grp.addRow(new PathDescriptor("MTSPath", "MTS Lines", new Color(126, 179, 252), 1.0f, null, true, false, true));
        grp.addRow(new BooleanDescriptor("LIS", "Show Line in the Sand", true));
        grp.addRow(new PathDescriptor("LISPath", "LIS Lines", new Color(126, 179, 252), 1.0f, null, true, false, true));
        grp.addRow(new PathDescriptor("LongPath", "Long Lines Style", new Color(126, 179, 252), 1.0f, null, true, false, true));
        grp.addRow(new PathDescriptor("ShortPath", "Short Lines Style", new Color(126, 179, 252), 1.0f, null, true, false, true));

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
        desc.declareSignal("LT", "Line Touched");
    }
    //endregion

    @Override
    public void onSettingsUpdated(DataContext ctx)
    {
        debug("onSettingsUpdated = " + alFig.size());
        var series = ctx.getDataSeries();
        new Thread(new Runnable() {
            public void run() {
                DoTheFetch(series);
            }
        }).start();
        bFetched = true;

        beginFigureUpdate();
        this.clearFigures();
        for (Figure f : alFig)
            this.addFigure(f);
        endFigureUpdate();
    }

    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();

        int last = series.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (series == null || index < 202 || !series.isBarComplete(index)) return;

        //region CANDLE CALCS
        double close = series.getClose(index);
        double open =  series.getOpen(index);
        double pclose = series.getClose(index - 1);
        double popen =  series.getOpen(index - 1);
        double clow = series.getLow(index);
        double plow = series.getLow(index - 1);
        double high = series.getHigh(index);
        double phigh = series.getHigh(index - 1);
        boolean c0G = close > series.getOpen(index);
        boolean c1G = series.getClose(index - 1) > series.getOpen(index - 1);
        boolean c0R = close < open;
        boolean c1R = series.getClose(index - 1) < series.getOpen(index - 1);
        double body = Math.abs(open - close);
        double pbody = Math.abs(series.getOpen(index - 1) - series.getClose(index - 1));

         sLicense = getSettings().getString("Lic");
         bShowTS = getSettings().getBoolean("TS");
         bShowMTS = getSettings().getBoolean("MTS");
         bShowLIS = getSettings().getBoolean("LIS");

        //endregion
    }

    //region DRAWING FUNCTIONS
    private void DrawTheBox(DataSeries series, String yYs, String desc)
    {
        Color cc = new Color(126, 179, 252);
        if (desc.contains("Short"))
            cc = new Color(94, 27, 39);
        if (desc.contains("Long"))
            cc = new Color(30, 87, 44);
        if (desc.contains("Sand"))
            cc = new Color(117, 98, 30);

        debug("DrawTheBox = " + yYs + " | " + desc);
        String[] tok = yYs.replaceAll("Contract.","").trim().split("-");
        double ia = Double.parseDouble(tok[0]);
        double ib = Double.parseDouble(tok[1]);
        Coordinate cS = new Coordinate(series.getStartTime(series.getStartIndex()-500), ia);
        Coordinate cE = new Coordinate(series.getStartTime(series.getEndIndex()+500), ib);
        Box bx = new Box(cS, cE);
        bx.setFillColor(cc);
        bx.setLineColor(cc);
        if (!alFig.contains(bx))
            alFig.add(bx);
    }

    private void DrawTheLine(DataSeries series, double yY, String desc)
    {
        Color cc = new Color(126, 179, 252);
        if (desc.contains("Short"))
            cc = new Color(255, 0, 0);
        if (desc.contains("Long"))
            cc = new Color(0, 255, 0);
        if (desc.contains("Sand"))
            cc = new Color(217, 145, 37);

        if (desc != "TS")
            debug("DrawTheLine = " + yY + " | " + desc);
        Coordinate cS = new Coordinate(series.getStartTime(series.getStartIndex()), (double) yY);
        Coordinate cE = new Coordinate(series.getStartTime(series.getEndIndex()), (double) yY);
        PathInfo pf = new PathInfo(cc, 6, new float[]{1, 1}, true, true, false, 0, 2);
        Line lk = new Line(cS, cE, pf);
        lk.setExtendRightBounds(true);
        lk.setExtendLeftBounds(true);
        lk.setText(desc, new Font("Arial", Font.PLAIN, 14));
        if (!alFig.contains(lk))
            alFig.add(lk);
    }
    //endregion

    //region MISC FUNCTIONS
    public static LocalDateTime convertLongToLocalDateTime(long milliseconds) {
        Instant instant = Instant.ofEpochMilli(milliseconds);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
    public static boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getDateString(long dDate)
    {
        LocalDateTime ldate = convertLongToLocalDateTime(dDate);
        int iM = ldate.getMonthValue();
        int iD = ldate.getDayOfMonth();
        int iY = ldate.getYear();
        return String.format("%04d", iY) + String.format("%02d", iM) + String.format("%02d", iD);
    }

    private void HandleString(DataSeries series, String sz, String sDD)
    {
        sz = sz.replaceAll(",","").trim();
        //debug("HandleString = " + sz + " | " + sDD);
        String[] tok = sz.split(" ");
        for (String s1 : tok)
        {
            if (isNumeric(s1.trim()))
                DrawTheLine(series, Double.parseDouble(s1), sDD);
            else if (s1.trim().contains("-"))
                DrawTheBox(series, s1.trim(), sDD);
        }
    }

    private void tokenizeResult(DataSeries series, String su)
    {
        String[] tokens = su.split("\\|");
        for (String s : tokens)
        {
            String si = s.trim();
            //debug("si = " + si);

            if (si.contains("Highest Odds Short") || si.contains("Range Short") || si.contains("Line in the Sand") || si.contains("Range Long") || si.contains("Highest Odds Long") || si.contains("Extreme Long") || si.contains("Extreme Short"))
            {
                String[] tok = si.split(" ");
                for (String s1 : tok)
                {
                    if (si.contains("Highest Odds Short"))
                        HandleString(series, s1.trim(), "Highest Odds Short");
                    if (si.contains("Range Short"))
                        HandleString(series, s1.trim(), "Range Short");
                    if (si.contains("Line in the Sand") && bShowLIS)
                        HandleString(series, s1.trim(), "Line in the Sand");
                    if (si.contains("Range Long"))
                        HandleString(series, s1.trim(), "Range Long");
                    if (si.contains("Highest Odds Long"))
                        HandleString(series, s1.trim(), "Highest Odds Long");
                    if (si.contains("Extreme Long"))
                        HandleString(series, s1.trim(), "Extreme Long");
                    if (si.contains("Extreme Short"))
                        HandleString(series, s1.trim(), "Extreme Short");
                }
            }

            if (s.contains("TraderSmarts Numbers for ") && bShowTS)
            {
                debug("commas = " + si);
                String[] tok = su.split(", ");
                for (String s1 : tok)
                    if (isNumeric(s1))
                        DrawTheLine(series, Double.parseDouble(s1), "TS");
            }
            //debug("si = " + si);
        }
    }
    //endregion

}