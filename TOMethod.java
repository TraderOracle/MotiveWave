package TraderOracle;

import com.motivewave.platform.sdk.common.desc.SignalDescriptor;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.desc.InputDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.motivewave.platform.sdk.common.BaseInfo;
import com.motivewave.platform.sdk.common.LineInfo;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.common.menu.MenuSeparator;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.motivewave.platform.sdk.study.Plot;
import com.motivewave.platform.sdk.draw.*;

import java.awt.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.net.*;

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
    enum Values { MA, MOMENTUM, BB_KC_DIFF }
    enum Signals { ENG_BB, BOINK }

    private static final Color RED = new Color(255, 0, 0);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color YELLOW = new Color(255, 0, 0);
    private int bodyToTailRatio = 1; // Customize this value to adjust the sensitivity
    private List<Double> al = new ArrayList<>();

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
    public void onLoad(Defaults defaults)
    {
        hor = new Line();
    }

    @Override
    public void initialize(Defaults defaults)
    {
        //Plot t = new Plot();
        //var dd = createRD();
        //t.addHorizontalLine(new LineInfo(19680d, RED, 5.0f, new float[] {3,3}));
        //t.addHorizontalLine(new LineInfo(19640d, GREEN, 5.0f, new float[] {3,3}));
        //t.addHorizontalLine(new LineInfo(19630d, YELLOW, 5.0f, new float[] {3,3}));

        //List<LineInfo> lf = dd.getHorizontalLines();
       //debug("line count = " + lf.size());

       // var gb = ctx.getBounds(); // this is the bounds of the graph
        //var start = gb.getX();
        //var end = gb.getMaxX();
        //var line = new Line2D.Double(start, end);

        try {
            String returns = getHTML("https://raw.githubusercontent.com/TraderOracle/NinjaTrader/refs/heads/main/Default.xml");
            //debug(returns);
        } catch(Exception e) {
            // do something, e.g. print e.getMessage()
        }

        var desc = createRD();
        // Signals
        desc.declareSignal(Signals.BOINK, "BOINK");
        desc.declareSignal(Signals.ENG_BB, "Engulfing candle off BB");

        // Describe the settings that may be configured by the user.
        // Settings may be organized using a combination of tabs and groups.
        var sd = createSD();
        var tab = sd.addTab(get("TAB_GENERAL"));

        var grp = tab.addGroup(get("LBL_INPUTS"));
        // Declare the inputs that are used to calculate the moving average.
        // Note: the 'Inputs' class defines several common input keys.
        // You can use any alpha-numeric string that you like.
        grp.addRow(new InputDescriptor(Inputs.INPUT, get("Input"), Enums.BarInput.CLOSE));
        grp.addRow(new IntegerDescriptor(Inputs.PERIOD, get("Period"), 20, 1, 9999, 1));
        grp.addRow(new PathDescriptor(Inputs.PATH, "Line", defaults.getLineColor(), 1.0f, null, true, false, true));


        grp = tab.addGroup(get("TAB_DISPLAY"));
        // Allow the user to change the settings for the path that will
        // draw the moving average on the graph.  In this case, we are going
        // to use the input key Inputs.PATH
        grp.addRow(new PathDescriptor(Inputs.PATH, get("Path"), null, 1.0f, null, true, true, false));

        // Describe the runtime settings using a 'RuntimeDescriptor'

        // Describe how to create the label.  The label uses the
        // 'label' attribute in the StudyHeader (see above) and adds the input values
        // defined below to generate a label.
        desc.setLabelSettings(Inputs.INPUT, Inputs.PERIOD);
        // Exported values can be used to display cursor data
        // as well as provide input parameters for other studies,
        // generate alerts or scan for study patterns (see study scanner).
        desc.exportValue(new ValueDescriptor(Values.MA, get("My MA"),
                new String[] {Inputs.INPUT, Inputs.PERIOD}));
        // MotiveWave will automatically draw a path using the path settings
        // (described above with the key 'Inputs.LINE')  In this case
        // it will use the values generated in the 'calculate' method
        // and stored in the data series using the key 'Values.MA'
        desc.declarePath(Values.MA, Inputs.PATH);
    }

    @Override
    protected void calculate(int index, DataContext ctx) {

        addFigure(hor);

        var series = ctx.getDataSeries();
        int last = series.size() - 1;
        Object input = getSettings().getInput(Inputs.INPUT);
        if (series == null) return;
        if (index < 202) return;

        //series.setPriceBarColor(index, Color.white);
        series.setDouble(index, Values.MA, 19680d);
        if (index == series.size() - 1)
            series.setComplete(index);

        double close = series.getClose(index);
        double open =  series.getOpen(index);
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

        //double bodyHeight = Math.abs(open - close);
        //double tailHeight = Math.min(Math.abs(high - open), Math.abs(high - close));
        //double bodyToTailHeightRatio = bodyHeight / tailHeight;

        double kama = series.kama(index, 9, input);
        double ema200 = series.ema(index, 200, input);

        // Calculate Bollinger Bands
        double middleBB = series.sma(index , 20, input);
        double stdDev = series.std(index, 20, input);
        double upperBB = middleBB + (2 * stdDev);
        double lowerBB = middleBB - (2 * stdDev);
        double pmiddleBB = series.sma(index-1 , 20, input);
        double pstdDev = series.std(index-1, 20, input);
        double pupperBB = middleBB + (2 * stdDev);
        double plowerBB = middleBB - (2 * stdDev);

        // Calculate Keltner Channels
        double middleKC = series.ema(index, 20, input);
        double range = series.atr(index, 20);
        double upperKC = middleKC + (1.5d * range);
        double lowerKC = middleKC - (1.5d * range);
        double pmiddleKC = series.ema(index-1, 20, input);
        double prange = series.atr(index-1, 20);
        double pupperKC = middleKC + (1.5d * range);
        double plowerKC = middleKC - (1.5d * range);

        if (c0R && high > kama && series.getOpen(index) < kama && phigh < kama)
        {
            al.add(open);
            series.setPriceBarColor(index, GREEN);
            ctx.signal(index, Signals.BOINK, "BOINK", close);
        }

        if (c0G && clow < kama && open > kama && plow > kama)
        {
            series.setPriceBarColor(index, RED);
            ctx.signal(index, Signals.BOINK, "BOINK", close);
        }

        if ((clow < lowerBB || plow < lowerBB) && body > pbody && c1R && c0G)
        {
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
            series.setPriceBarColor(index, GREEN);
            return;
        }

        if ((high > upperBB || phigh > upperBB) && body > pbody && c1G && c0R)
        {
            ctx.signal(index, Signals.ENG_BB, "ENG_BB", close);
            series.setPriceBarColor(index, RED);
            return;
        }

        //debug("upperBB = " + upperBB);
        //debug("lowerBB = " + lowerBB);
        // Check for squeeze (when BB is inside KC)
        boolean squeeze = (lowerBB > lowerKC) && (upperBB < upperKC);
        boolean psqueeze = (plowerBB > plowerKC) && (pupperBB < pupperKC);

        if (!squeeze && psqueeze) {
            series.setPriceBarColor(index, WHITE);
        }
        else {
            //series.setPriceBarColor(index, Color.white);
        }

        for(Double item: al) {
            series.setDouble(index, Values.MA, item);
        }
        if (index == series.size() - 1)
            series.setComplete(index);

/*
        for (int i = 1; i <= series.size() - 1; i++)
        {
            double open = series.getOpen(i);
            double high = series.getHigh(i);
            double low = series.getLow(i);
            double close = series.getClose(i);

            double bodyHeight = Math.abs(open - close);
            double tailHeight = Math.min(Math.abs(high - open), Math.abs(high - close));

            double bodyToTailHeightRatio = bodyHeight / tailHeight;

            if (bodyHeight > 0 && tailHeight > 0 && bodyToTailHeightRatio >= bodyToTailRatio)
            {
                series.setPriceBarColor(i, Color.white);
                //pinbarAlert.setValue(new Object());
                return;
            }
        }

            */

    }

    private class Line extends Figure
    {
        Line() {}

        @Override
        public void layout(DrawContext ctx)
        {
            var gb = ctx.getBounds(); // this is the bounds of the graph
            line = new Line2D.Double(gb.getX(), gb.getMaxX());
        }
        /*
                @Override
                public void draw(Graphics2D gc, DrawContext ctx)
                {
                    var path = getSettings().getPath(Inputs.PATH);
                    gc.setStroke(path.getStroke());
                    gc.setColor(path.getColor());
                    gc.draw(line);
                }
        */
        private Line2D line;
    }

    private Line hor;
}
