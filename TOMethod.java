package TraderOracle;

import java.awt.*;
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
        name="TraderOracle Method2",
        label="TOMethod",
        desc="TraderOracle Method2",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class TOMethod extends Study
{
    //region VARIABLES
    enum Values { MA, MA2, MA3, MA1, MOMENTUM, BB_KC_DIFF, MACD, SIGNAL, HIST }
    enum Signals { ENG_BB, BOINK }

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);
    private List<Line> al = new ArrayList<>();
    //endregion

    @Override
    public void initialize(Defaults defaults)
    {
        var sd=createSD();
        var tab=sd.addTab("BOINK");
        var grp=tab.addGroup("Inputs");
        grp.addRow(new BooleanDescriptor("ShowBOINK", "Show BOINKs", true));
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
        grp1.addRow(new BooleanDescriptor("ShowEngBB", "Show Engulfing off Bollinger Band", true));
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
        desc.declareSignal(Signals.ENG_BB, "Engulfing candle off BB");
    }

    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();

        int last = series.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (series == null || index < 202 || !series.isBarComplete(index)) return;

        //series.setPriceBarColor(index, Color.white);
        //series.setDouble(index, Values.MA, 19680d);
        //if (index == series.size() - 1)
        //    series.setComplete(index);

        //region CANDLE CALCS

        // GET CONFIG SETTINGS
        boolean bShowBOINK = getSettings().getBoolean("ShowBOINK");
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
        double stdDev = series.std(index, 20, input);
        double upperBB = middleBB + (2 * stdDev);
        double lowerBB = middleBB - (2 * stdDev);
        //endregion

        //region BOINKS
        if (bShowBOINK && (c0R && c1G && high > myEMA && close < myEMA && phigh < high))
        {
            var marker = getSettings().getMarker("DOWNBOINKMarker");
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) high+1);
            this.addFigure(new Marker(coords, Enums.Position.TOP, marker, "Howdy Msg"));
            ctx.signal(index, Signals.BOINK, "BOINK", close);
            return;
        }

        if (bShowBOINK && (c0G && c1R && clow < myEMA && close > myEMA && phigh > high) ||
             (c0G && bGDoji && clow < myEMA && close > myEMA && phigh < myEMA) ||
             (c0G && bRDoji && clow < myEMA && close > myEMA && plow <= clow) ||
             (c0G && c1G && c2G && open <= pclose && popen <= ppclose && clow < myEMA && close > myEMA))
        {
            var marker = getSettings().getMarker("UPBOINKMarker");
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) clow-1);
            this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "Howdy Msg"));
            ctx.signal(index, Signals.BOINK, "BOINK", close);
            return;
        }
        //endregion

        //region ENGULFING CANDLE
        if ((clow < lowerBB || plow < lowerBB) && body > pbody && c1R && bShowEngBB && c0G)
        {
            var marker = getSettings().getMarker("UPEngBBMarker");
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getLow(index) - 1);
            this.addFigure(new Marker(coords, Enums.Position.BOTTOM, marker, "Howdy Msg"));
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
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getHigh(index) + 1);
            Coordinate coordsEnd = new Coordinate(series.getStartTime(index-100), (double) series.getHigh(index) + 2.2);
            this.addFigure(new Marker(coords, Enums.Position.TOP, marker, "Howdy Msg"));

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


        //region VOLUME IMBALANCES
        /*
        // =-=-=-=-=   VOLUME IMBALANCES GREEN   =-=-=-=-=
        if (c0G && c1G && open > pclose)
        {
            int iStart = series.getStartIndex();
            int iEnd = series.getEndIndex();
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) open);
            Coordinate coordsEnd = new Coordinate(series.getStartTime(iEnd), (double) open);
            // PathInfo(Color c, float width, float[] dash, boolean enabled, boolean continuous, boolean showBars, int barCenter, Integer fixedWidth)
            PathInfo pf = new PathInfo(new Color(126, 179, 252), 2, new float[] {4,1,3}, true, true, false, 0, 2);
            Line lk = new Line(coords, coordsEnd, pf);
            lk.setExtendRightBounds(true);
            // lk.setText(Math.abs(open), new Font("Arial", Font.PLAIN, 12));
            if (!al.contains(lk))
                al.add(lk);
            //this.addFigure(lk);
        }

        // =-=-=-=-=   VOLUME IMBALANCES RED   =-=-=-=-=
        if (c0R && c1R && open < pclose)
        {
            int iStart = series.getStartIndex();
            int iEnd = series.getEndIndex();
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) close);
            Coordinate coordsEnd = new Coordinate(series.getStartTime(iEnd), (double) close);
            // PathInfo(Color c, float width, float[] dash, boolean enabled, boolean continuous, boolean showBars, int barCenter, Integer fixedWidth)
            PathInfo pf = new PathInfo(new Color(126, 179, 252), 2, new float[] {4,1,3}, true, true, false, 0, 2);
            Line lk = new Line(coords, coordsEnd, pf);
            lk.setExtendRightBounds(true);
            // lk.setText(Math.abs(open), new Font("Arial", Font.PLAIN, 12));
            if (!al.contains(lk))
                al.add(lk);
            //this.addFigure(lk);
        }
*/
        //endregion
        
        for (Line line : al) {
            boolean bGStop = c0G && close > line.getEndValue() && open < line.getEndValue();
            boolean bG1Stop = c0G && open > line.getEndValue() && clow < line.getEndValue();
            boolean bG2Stop = false; // c0G && close > line.getEndValue() && high > line.getEndValue();
            boolean bRStop = c0R && close < line.getEndValue() && open > line.getEndValue();
            boolean bR1Stop = c0R && clow < line.getEndValue() && close > line.getEndValue();
            int idxLine = series.findIndex(line.getEndTime());
            if (idxLine > index && (bGStop || bRStop || bG1Stop || bR1Stop || bG2Stop))
            {
                line.setEnd(series.getStartTime(index), line.getEndValue());
                line.setExtendRightBounds(false);
                line.setExtendRight(0);
                //this.addFigure(line);
                break;
            }
            //al.remove(item);
        }
        //debug("line after = " + al.size());

        /*
        LineInfo lf = new LineInfo(19680d, Color.red, 2.0f, new float[] {3,3});
        RuntimeDescriptor ts = new RuntimeDescriptor();
        setRuntimeDescriptor(ts);
        Plot t = ts.getDefaultPlot();
        t.setShowLabel(true);
        t.isShowLabel();
        //t.addHorizontalLine(lf);
        ts.addPlot("kjkj", t);
        for (int i = 0; i < al.size(); i++) {
            //ts.addHorizontalLine(new LineInfo(al.get(i), RED, 2.0f, new float[] {3,3}));
        }
*/
        if (index == series.size() - 1)
            series.setComplete(index);
    }

}