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
        namespace="com.mycompany",
        id="TOMethod",
        rb="TraderOracle.nls.strings", // locale specific strings are loaded from here
        name="TraderOracle Method",
        label="TOMethod",
        desc="TraderOracle Method",
        menu="TraderOracle",
        overlay=true,
        studyOverlay=true,
        signals=true)

public class TOMethod extends Study
{
    enum Values { MA, MA2, MA3, MA1, MOMENTUM, BB_KC_DIFF, MACD, SIGNAL, HIST }
    final static String HIST_IND = "histInd"; // Histogram Parameter

    enum Signals { ENG_BB, BOINK }

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);
    private List<Line> al = new ArrayList<>();

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

    @Override
    public void initialize(Defaults defaults)
    {
        try {
            String returns = getHTML("https://raw.githubusercontent.com/TraderOracle/NinjaTrader/refs/heads/main/Default.xml");
            //debug(returns);
        } catch(Exception e) {        }

        var sd=createSD();
        var tab=sd.addTab("General");
        var grp=tab.addGroup("");
        grp.addRow(new PathDescriptor(Inputs.PATH, "Line", defaults.getLineColor(), 2.0f, null, true, false, true));
        grp.addRow(new InputDescriptor(Inputs.INPUT, get("Input"), Enums.BarInput.CLOSE));

        var desc = createRD();
        desc.declareSignal(Signals.BOINK, "BOINK");
        desc.declareSignal(Signals.ENG_BB, "Engulfing candle off BB");
    }

    @Override
    protected void calculate(int index, DataContext ctx) {
        var series = ctx.getDataSeries();

        //this.addFigure(new Marker(coords, Enums.MarkerType.CIRCLE, Enums.Size.SMALL, Enums.Position.BOTTOM, GREEN, GREEN));

        int last = series.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (series == null || index < 202 || !series.isBarComplete(index)) return;

        //series.setPriceBarColor(index, Color.white);
        //series.setDouble(index, Values.MA, 19680d);
        //if (index == series.size() - 1)
        //    series.setComplete(index);

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
        double kama = series.kama(index, 9, input);
        double ema200 = series.ema(index, 200, input);

        // Calculate Bollinger Bands
        double middleBB = series.sma(index , 20, input);
        double stdDev = series.std(index, 20, input);
        double upperBB = middleBB + (2 * stdDev);
        double lowerBB = middleBB - (2 * stdDev);
        //endregion

        /*
        if ((c0R && high > kama && open < kama && phigh < kama)) // || (c0G && high > kama && close < kama && phigh < kama))
        {
            this.addFigure(new Marker(coords, Enums.MarkerType.CIRCLE, Enums.Size.SMALL, Enums.Position.BOTTOM, GREEN, GREEN));
            //al.add(open);
            //debug("array list = " + al.size());
            series.setPriceBarColor(index, WHITE);
            ctx.signal(index, Signals.BOINK, "BOINK", close);
            return;
        }

        if ((c0G && clow < kama && open > kama && plow > kama)) // || (c0R && clow < kama && close > kama && plow > kama))
        {
            this.addFigure(new Marker(coords, Enums.MarkerType.CIRCLE, Enums.Size.SMALL, Enums.Position.BOTTOM, GREEN, GREEN));
            series.setPriceBarColor(index, WHITE);
            ctx.signal(index, Signals.BOINK, "BOINK", close);
            return;
        }
        */

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
            this.addFigure(lk);
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
            this.addFigure(lk);
        }

        // =-=-=-=-=   ENGULFING LOW   =-=-=-=-=
        if ((clow < lowerBB || plow < lowerBB) && body > pbody && c1R && c0G)
        {
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getLow(index) - 2);
            this.addFigure(new Marker(coords, Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, Enums.Position.BOTTOM, GREEN, GREEN));
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
            series.setPriceBarColor(index, WHITE);
            return;
        }

        // =-=-=-=-=   ENGULFING HIGH   =-=-=-=-=
        if ((high > upperBB || phigh > upperBB) && body > pbody && c1G && c0R)
        {
            /*
            int iStart = series.getStartIndex();
            int iEnd = series.getEndIndex();
            Coordinate coords = new Coordinate(series.getStartTime(index), (double) series.getHigh(index) + 2);
            Coordinate coordsEnd = new Coordinate(series.getStartTime(index-100), (double) series.getHigh(index) + 2.2);
            this.addFigure(new Marker(coords, Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, Enums.Position.TOP, RED, RED));

            Box bx = new Box(coords, coordsEnd);
            bx.setFillColor(RED);
            bx.setLineColor(WHITE);
            //this.addFigure(bx);

            Line lk = new Line(coords, coordsEnd);
            lk.setColor(WHITE);
            this.addFigure(lk);
*/
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
            series.setPriceBarColor(index, WHITE);
            return;
        }

        // clear(): Removes all of the elements from this list.
        // add(element): Appends the specified element to the end of the list.
        // remove(element): Removes the first occurrence of the specified element from this list, if it is present.
        // contains(element): Returns true if this list contains the specified element.
        //debug("line before = " + al.size());

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
                this.addFigure(line);
                break;
                // setEnd(long time, double value) Sets the location of the end of the line.
                // double getEndValue() - Gets the value of the ending location of the line.

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