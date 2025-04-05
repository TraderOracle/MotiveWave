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
        id="LineBounces",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="Line Bounces",
        label="Line Bounces",
        desc="Line Bounces",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class LineBounces extends Study
{
    //region VARIABLES
    enum Values { UP, DOWN, TREND }
    enum Signals { KP_WICK, KP_TOUCH, TS_WICK,TS_TOUCH, MQ_WICK, MQ_TOUCH, MAN_WICK, MAN_TOUCH }

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

        boolean bShowKillpips = getSettings().getBoolean("SHOWKP");
        boolean bShowMenthorQ = getSettings().getBoolean("SHOWMQ");
        boolean bShowTS = getSettings().getBoolean("SHOWTS");
        double close = s.getClose();
        double open = s.getOpen();
        double hi = s.getHigh();
        double lo = s.getLow();
        boolean c0G = s.getClose() > s.getOpen();
        boolean c0R = s.getClose() < s.getOpen();

        if (bShowTS) {
            //sTSMsg = "";
            for (RangeEntry tsi : tsRange) {
                // Single value
                if (tsi.start == tsi.end){
                    if (hi > tsi.start && lo < tsi.start) {
                        String sQ = tsi.text;
                        if (tsi.text == "MTS")
                            sQ += " " + s.getClose();
                        debug("TraderSmarts Touched " + sQ);
                        ctx.signal(index, Signals.TS_TOUCH, "TraderSmarts Touched " + sQ, close);
                        break;
                    }
                    else if ((c0G && hi > tsi.start && close < tsi.start) ||
                        (c0G && lo < tsi.start && open > tsi.start) ||
                        (c0R && hi > tsi.start && open < tsi.start) ||
                        (c0R && lo < tsi.start && close > tsi.start)){
                        debug("TraderSmarts Wicking " + tsi.text);
                        ctx.signal(index, Signals.TS_WICK, "TraderSmarts Wick " + tsi.text, close);
                        break;
                    }
                }

                // Range values
                if (tsi.start > tsi.end && close > tsi.end && close < tsi.start) {
                    debug("TraderSmarts Inside  " + tsi.text);
                    ctx.signal(index, Signals.TS_TOUCH, "TraderSmarts Inside " + tsi.text, close);
                    break;
                }
                if (tsi.start < tsi.end && close < tsi.end && close > tsi.start) {
                    debug("TraderSmarts Inside  " + tsi.text);
                    ctx.signal(index, Signals.TS_TOUCH, "TraderSmarts Inside " + tsi.text, close);
                    break;
                }
            }
        }

        if (bShowKillpips) {
            String sKPMsg = getTouch(ctx, kpMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
            if (sKPMsg != "")
                ctx.signal(index, Signals.KP_TOUCH, "Killips " + sKPMsg, close);
        }

        if (bShowMenthorQ) {

            String sMQMsg = getTouch(ctx, mqMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
            if (sMQMsg != "") {
                debug("MenthorQ " + sMQMsg);
                ctx.signal(index, Signals.MQ_TOUCH, "MenthorQ " + sMQMsg, close);
            }

            String sMQMsg2 = getTouch(ctx, bsMap, s.getHigh(), s.getLow(), s.getOpen(), s.getClose());
            if (sMQMsg2 != "") {
                debug("MenthorQ " + sMQMsg2);
                ctx.signal(index, Signals.MQ_TOUCH, "MenthorQ " + sMQMsg2, close);
            }
        }
    }

    //endregion

    //region TRADER SMARTS

    private void FillTraderSmarts()
    {
        //debug("FillTraderSmarts = " + src + ", " + xx);
        tsRange.clear();
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
                    //debug("RangeEntry = " + nummie + " " + desc);
                    continue;
                } catch (NumberFormatException e) {}

            }
        }
    }

    //endregion

    //region TOUCH LOGIC

    private String getTouch(DataContext ctx, Map<Double, String> map,
       double high, double low, double open, double close) {
        boolean c0G = close > open;
        boolean c0R = close < open;

        String sType = (map == kpMap) ? "Killpips " : (map == mqMap || map == bsMap) ? "MenthorQ " : "";

        for (Map.Entry<Double, String> entry : map.entrySet()) {
            Double price = entry.getKey();
            if (high > price && low < price) {
                ctx.signal(gIndex, Signals.KP_TOUCH, "Price touched " + sType + entry.getValue(), close);
                return " - Touched " + entry.getValue();
            }
            else if ((c0G && high > price && close < price) || (c0G && low < price && open > price) ||
                (c0R && high > price && open < price) || (c0R && low < price && close > price)) {
                ctx.signal(gIndex, Signals.KP_WICK, "Wick off " + sType + entry.getValue(), close);
                return " - WICK off " + entry.getValue();
            }
        }
        return "";
    }

    private void FillMaps(String src, Map xx)
    {
        //Instrument sI = getSettings().getInstrument("INSTR");
        //debug("FillMaps 1 = " + sI.getUnderlying());
        String[] parts = getSettings().getString(src).split(", ");
        for (int i = 0; i < parts.length; i++) {
            try {
                Double nums = Double.parseDouble(parts[i].trim());
                String txt = parts[i-1].trim();
                xx.put(Double.parseDouble(parts[i].trim()), parts[i-1].trim());
                debug("xx = " + nums + ", " + txt);
            } catch (NumberFormatException e) {
            }
        }
    }

    //endregion

    //region INITIALIZE

    @Override
    public void initialize(Defaults defaults)
    {
        this.kpMap = new HashMap<>();
        this.mqMap = new HashMap<>();
        this.bsMap = new HashMap<>();
        this.tsRange = new ArrayList<>();

        var sd = createSD();

        var tab2 = sd.addTab("Lines");
        var grp3 = tab2.addGroup("Show These");
        grp3.addRow(new BooleanDescriptor("SHOWKP", "Show Killpips", true));
        grp3.addRow(new BooleanDescriptor("SHOWMQ", "Show MenthorQ", true));
        grp3.addRow(new BooleanDescriptor("SHOWMANBUY", "Show Mancini Buy", true));
        grp3.addRow(new BooleanDescriptor("SHOWMANSELL", "Show Mancini Sell", true));
        grp3.addRow(new BooleanDescriptor("SHOWTS", "Show TraderSmarts", true));
        grp3.addRow(new BooleanDescriptor("SHOW200", "Show EMA 200 touches", true));
        grp3.addRow(new BooleanDescriptor("SHOWVWAP", "Show VWAP touches", true));

        var grp2 = tab2.addGroup("Paid Lines");
        grp2.addRow(new StringDescriptor("TS", "Values", "YM Execution/Target Zones:41943 - 41909 Extreme Short41368 - 41323 Highest Odds Short FTU41118 Range Short40848 - 40805 Line in the Sand40493 Range Long39771 Highest Odds Long FTD39393 Extreme LongYM MTS Numbers: 43145, 41973, 41370, 40913, 40698, 39807, 39350", 500));
        //grp2.addRow(new InstrumentDescriptor("INSTR", "Instrument"));
        grp2.addRow(new StringDescriptor("KP", "Killpips Values", "vix r1, 41365, vix r2, 41399, vix s1, 40185, vix s2, 40155, 1DexpMAX, 41425, 1DexpMIN, 40129, RD0, 40892, RD1, 40993, RD2, 41215, SD0, 40665, SD1, 40561, SD2, 40343, HV, 40779, VAH, 41557, VAL, 39994, range daily max, 41643, range daily min, 39911", 500));
        grp2.addRow(new StringDescriptor("MQ", "MenthorQ Values", "", 500));
        grp2.addRow(new StringDescriptor("MQBS", "MenthorQ Blind Spots", "BL 1, 41534.35, BL 2, 41136.24, BL 3, 39993.99, BL 4, 42346.55, BL 5, 40996.71, BL 6, 40357.81, BL 7, 40728.68, BL 8, 41794.62, BL 9, 39076.64, BL 10, 41451.87", 500));
        grp2.addRow(new StringDescriptor("ManciniBuy", "Mancini Buy Values", "", 500));
        grp2.addRow(new StringDescriptor("ManciniSell", "Mancini Sell Values", "", 500));

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
        desc.declareSignal(Signals.KP_WICK, "Killpips Line Wick");
        desc.declareSignal(Signals.KP_TOUCH, "Killpips Line Touch");
        desc.declareSignal(Signals.TS_WICK, "TraderSmarts Line Wick");
        desc.declareSignal(Signals.TS_TOUCH, "TraderSmarts Line Touch");
        desc.declareSignal(Signals.MQ_WICK, "MenthorQ Line Wick");
        desc.declareSignal(Signals.MQ_TOUCH, "MenthorQ Line Touch");
        desc.declareSignal(Signals.MAN_WICK, "Mancini Line Wick");
        desc.declareSignal(Signals.MAN_TOUCH, "Mancini Line Touch");
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

        if (kpMap.size() == 0 && getSettings().getBoolean("SHOWKP"))
            FillMaps("KP", kpMap);
        if (mqMap.size() == 0 && getSettings().getBoolean("SHOWMQ"))
            FillMaps("MQ", mqMap);
        if (bsMap.size() == 0 && getSettings().getBoolean("SHOWMQ"))
            FillMaps("MQBS", bsMap);
        if (tsRange.size() == 0 && getSettings().getBoolean("SHOWTS"))
            FillTraderSmarts();

    }

}