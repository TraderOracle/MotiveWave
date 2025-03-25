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
        label="LineBounces",
        desc="Line Bounces",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class LineBounces extends Study
{
    //region VARIABLES
    enum Values { MA, MA2, MA3, MA1, MOMENTUM, BB_KC_DIFF, MACD, SIGNAL, HIST }
    enum Signals { ENG_BB, BOINK }

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);

    private Map<Double, String> kpMap;
    private Map<Double, String> mqMap;
    private Map<Double, String> bsMap;
    //endregion

    //region INITIALIZE AND MISC

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

    @Override
    public void initialize(Defaults defaults)
    {
        this.kpMap = new HashMap<>();
        this.mqMap = new HashMap<>();
        this.bsMap = new HashMap<>();

        var sd = createSD();

        var tab2 = sd.addTab("Line Services");
        var grp2 = tab2.addGroup("Inputs");

        //grp2.addRow(new InstrumentDescriptor("INSTR", "Instrument"));
        grp2.addRow(new StringDescriptor("KP", "Killpips Values", "vix r1, 5888, vix r2, 5891, vix s1, 5744, vix s2, " +
                "5738, 1DexpMAX, 5895, 1DexpMIN, 5732, RD0, 5829, RD1, 5842, RD2, 5868, SD0, 5802, SD1, 5789, SD2, " +
                "5762, HV, 5815, VAH, 5911, VAL, 5719, range daily max, 5922, range daily min, 5709", 500));
        grp2.addRow(new SpacerDescriptor(100, 100));
        grp2.addRow(new StringDescriptor("MQ", "MenthorQ Values", "Call Resistance, 5850, Put Support, 5700, HVL, " +
                "5750, 1D Min, 5761.88, 1D Max, 5869.12, Call Resistance 0DTE, 5850, Put Support 0DTE, 5740, HVL 0DTE, 5745, Gamma Wall 0DTE, 5850, GEX 1, 5830, GEX 2, 5840, GEX 3, 5825, GEX 4, 5820, GEX 5, 5800, GEX 6, 5900, GEX 7, 5875, GEX 8, 5725, GEX 9, 5880, GEX 10, 5870", 500));
        grp2.addRow(new StringDescriptor("MQBS", "MenthorQ Blind Spots", "BL 1, 5758.07, BL 2, 5928.2, BL 3, 5723.35," +
                " BL 4, 5748.73, BL 5, 5874.68, BL 6, 5806.5, BL 7, 5695.25, BL 8, 5795.08, BL 9, 5825.61, BL 10, 5895.9", 500));
        grp2.addRow(new SpacerDescriptor(100, 100));
        grp2.addRow(new StringDescriptor("ManciniBuy", "Mancini Buy Values", "", 500));
        grp2.addRow(new StringDescriptor("ManciniSell", "Mancini Sell Values", "", 500));
    }

    @Override
    public void onSettingsUpdated(DataContext ctx)
    {
        FillMaps("KP", kpMap);
        FillMaps("MQ", mqMap);
        FillMaps("MQBS", bsMap);
    }

    //endregion

    @Override
    protected void calculate(int index, DataContext ctx)
    {
        var series = ctx.getDataSeries();

        int last = series.size() - 1;
        if (series == null || index < 202 || !series.isBarComplete(index)) return;

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